/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.ms;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyExchangeOfferFacade;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.exchange.ExchangeRequestService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MSExchangeSellAttachment;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
public class MSExchangeSellTransactionType extends MSExchangeTransactionType {
    private final AccountCurrencyService accountCurrencyService;
    private final ExchangeRequestService exchangeRequestService;
    private final CurrencyExchangeOfferFacade exchangeOfferFacade;

    @Inject
    public MSExchangeSellTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, CurrencyService currencyService, AccountCurrencyService accountCurrencyService, ExchangeRequestService exchangeRequestService, CurrencyExchangeOfferFacade exchangeOfferFacade) {
        super(blockchainConfig, accountService, currencyService);
        this.accountCurrencyService = accountCurrencyService;
        this.exchangeRequestService = exchangeRequestService;
        this.exchangeOfferFacade = exchangeOfferFacade;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.MS_EXCHANGE_SELL;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.CURRENCY_EXCHANGE_SELL;
    }

    @Override
    public String getName() {
        return "ExchangeSell";
    }

    @Override
    public MSExchangeSellAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new MSExchangeSellAttachment(buffer);
    }

    @Override
    public MSExchangeSellAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new MSExchangeSellAttachment(attachmentData);
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        super.doStateDependentValidation(transaction);
        MSExchangeSellAttachment attachment = (MSExchangeSellAttachment) transaction.getAttachment();
        long accountCurrencyBalance = accountCurrencyService.getUnconfirmedCurrencyUnits(transaction.getSenderId(), attachment.getCurrencyId());
        if (accountCurrencyBalance < attachment.getUnits()) {
            throw new AplException.NotCurrentlyValidException("Account " + Long.toUnsignedString(transaction.getSenderId())
                + " has not enough " + Long.toUnsignedString(attachment.getCurrencyId()) + " currency to place currency " +
                " exchange sell order: required " + attachment.getUnits() + ", but has only " + accountCurrencyBalance);
        }
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MSExchangeSellAttachment attachment = (MSExchangeSellAttachment) transaction.getAttachment();
        if (accountCurrencyService.getUnconfirmedCurrencyUnits(senderAccount, attachment.getCurrencyId()) >= attachment.getUnits()) {
            accountCurrencyService.addToUnconfirmedCurrencyUnits(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getCurrencyId(), -attachment.getUnits());
            return true;
        }
        return false;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        MSExchangeSellAttachment attachment = (MSExchangeSellAttachment) transaction.getAttachment();
        Currency currency = currencyService.getCurrency(attachment.getCurrencyId());
        if (currency != null) {
            accountCurrencyService.addToUnconfirmedCurrencyUnits(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getCurrencyId(), attachment.getUnits());
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        MSExchangeSellAttachment attachment = (MSExchangeSellAttachment) transaction.getAttachment();
        exchangeRequestService.addExchangeRequest(transaction, attachment);
        exchangeOfferFacade.exchangeCurrencyForAPL(
            transaction, senderAccount, attachment.getCurrencyId(), attachment.getRateATM(), attachment.getUnits());
    }

}
