/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencySupplyTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyFounder;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencySupply;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyType;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchUpdater;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyFounderService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig.DEFAULT_SCHEMA;

@Slf4j
@Singleton
public class CrowdFundingObserver {

    private final AccountService accountService;
    private final AccountCurrencyService accountCurrencyService;
    private final CurrencyService currencyService;
    private final CurrencyTable currencyTable;
    private final CurrencyFounderService currencyFounderService;
    private final BlockChainInfoService blockChainInfoService;
    private final CurrencySupplyTable currencySupplyTable;
    private final FullTextSearchUpdater fullTextSearchUpdater;

    @Inject
    public CrowdFundingObserver(AccountService accountService,
                                AccountCurrencyService accountCurrencyService,
                                CurrencyService currencyService,
                                CurrencyTable currencyTable,
                                CurrencyFounderService currencyFounderService,
                                BlockChainInfoService blockChainInfoService,
                                CurrencySupplyTable currencySupplyTable,
                                FullTextSearchUpdater fullTextSearchUpdater
    ) {
        this.accountService = accountService;
        this.accountCurrencyService = accountCurrencyService;
        this.currencyService = currencyService;
        this.currencyTable = currencyTable;
        this.currencyFounderService = currencyFounderService;
        this.blockChainInfoService = blockChainInfoService;
        this.currencySupplyTable = currencySupplyTable;
        this.fullTextSearchUpdater = fullTextSearchUpdater;
    }

    public void onBlockApplied(@Observes @BlockEvent(BlockEventType.AFTER_BLOCK_APPLY) Block block) {
        log.trace(":accept:CrowdFundingListener: START onBlockApplied at = {}", block.getHeight());
        try (DbIterator<Currency> issuedCurrencies = currencyService.getIssuedCurrenciesByHeight(block.getHeight(), 0, -1)) {
            for (Currency currency : issuedCurrencies) {
                if (currencyService.getCurrentReservePerUnitATM(currency) < currency.getMinReservePerUnitATM()) {
                    undoCrowdFunding(currency);
                } else {
                    distributeCurrency(currency);
                }
            }
        }
        log.trace(":accept:CrowdFundingListener: END onBlockApplied AFTER_BLOCK_APPLY at = {}", block.getHeight());
    }

    private void undoCrowdFunding(Currency currency) {
        try (DbIterator<CurrencyFounder> founders =
                 currencyFounderService.getCurrencyFounders(currency.getId(), 0, Integer.MAX_VALUE)) {
            for (CurrencyFounder founder : founders) {
                accountService.addToBalanceAndUnconfirmedBalanceATM(
                    accountService.getAccount(founder.getAccountId()),
                    LedgerEvent.CURRENCY_UNDO_CROWDFUNDING,
                    currency.getId(),
                    Math.multiplyExact(currency.getReserveSupply(),
                        founder.getAmountPerUnitATM()));
            }
        }
        accountCurrencyService.addToCurrencyAndUnconfirmedCurrencyUnits(
            accountService.getAccount(currency.getAccountId()),
            LedgerEvent.CURRENCY_UNDO_CROWDFUNDING, currency.getId(),
            currency.getId(), -currency.getInitialSupply());
        int height = blockChainInfoService.getHeight();
        currency.setHeight(height);
        currencyTable.deleteAtHeight(currency, height);
        createAndFireFullTextSearchDataEvent(currency, FullTextOperationData.OperationType.DELETE);
        currencyFounderService.remove(currency.getId());
    }

    private void distributeCurrency(Currency currency) {
        long totalAmountPerUnit = 0;
        final long remainingSupply = currency.getReserveSupply() - currency.getInitialSupply();
        List<CurrencyFounder> currencyFounders = new ArrayList<>();
        try (DbIterator<CurrencyFounder> founders = currencyFounderService.getCurrencyFounders(currency.getId(), 0, Integer.MAX_VALUE)) {
            for (CurrencyFounder founder : founders) {
                totalAmountPerUnit += founder.getAmountPerUnitATM();
                currencyFounders.add(founder);
            }
        }
        CurrencySupply currencySupply = currencyService.loadCurrencySupplyByCurrency(currency);
        for (CurrencyFounder founder : currencyFounders) {
            long units = Math.multiplyExact(remainingSupply, founder.getAmountPerUnitATM()) / totalAmountPerUnit;
            long value = currencySupply.getCurrentSupply() + units;
            currencySupply.setCurrentSupply(value);
            accountCurrencyService.addToCurrencyAndUnconfirmedCurrencyUnits(
                accountService.getAccount(founder.getAccountId()),
                LedgerEvent.CURRENCY_DISTRIBUTION, currency.getId(),
                currency.getId(), units);
        }
        Account issuerAccount = accountService.getAccount(currency.getAccountId());
        accountCurrencyService.addToCurrencyAndUnconfirmedCurrencyUnits(
            issuerAccount, LedgerEvent.CURRENCY_DISTRIBUTION, currency.getId(),
            currency.getId(),
            currency.getReserveSupply() - currencyService.getCurrentSupply(currency));
        if (!currency.is(CurrencyType.CLAIMABLE)) {
            accountService.addToBalanceAndUnconfirmedBalanceATM(
                issuerAccount, LedgerEvent.CURRENCY_DISTRIBUTION, currency.getAccountId(),
                Math.multiplyExact(totalAmountPerUnit, currency.getReserveSupply()));
        }
        currencySupply.setCurrentSupply(currency.getReserveSupply());
        currencySupply.setHeight(blockChainInfoService.getHeight());
        currencySupplyTable.insert(currencySupply);
    }

    private void createAndFireFullTextSearchDataEvent(Currency currency, FullTextOperationData.OperationType operationType) {
        FullTextOperationData operationData = new FullTextOperationData(
            DEFAULT_SCHEMA, currencyTable.getTableName(), Thread.currentThread().getName());
        operationData.setOperationType(operationType);
        operationData.setDbIdValue(currency.getDbId());
        operationData.addColumnData(currency.getName()).addColumnData(currency.getDescription());
        // send data into Lucene index component
        log.debug("Put lucene index update data = {}", operationData);
        // call to update FullTextSearch index for record deletion (as we are in separate event loop thread)
        fullTextSearchUpdater.putFullTextOperationData(operationData);
    }
}
