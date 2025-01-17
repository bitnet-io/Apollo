/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http;

import com.apollocurrency.aplwallet.api.dto.BlockDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountCurrencyDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountDTO;
import com.apollocurrency.aplwallet.apl.core.app.GenesisAccounts;
import com.apollocurrency.aplwallet.apl.core.app.Token;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.GeneratorMemoryEntity;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.funding.FundingMonitorInstance;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.funding.MonitoredAccount;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.DataTag;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.PrunableMessage;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.TaggedData;
import com.apollocurrency.aplwallet.apl.core.entity.state.Trade;
import com.apollocurrency.aplwallet.apl.core.entity.state.Vote;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlPhasing;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountLease;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountProperty;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEntry;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerHolding;
import com.apollocurrency.aplwallet.apl.core.entity.state.alias.Alias;
import com.apollocurrency.aplwallet.apl.core.entity.state.alias.AliasOffer;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.AssetDelete;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.AssetDividend;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.AssetTransfer;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.AvailableOffers;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyExchangeOffer;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyFounder;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyTransfer;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyType;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSFeedback;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSGoods;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPublicFeedback;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPurchase;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSTag;
import com.apollocurrency.aplwallet.apl.core.entity.state.exchange.Exchange;
import com.apollocurrency.aplwallet.apl.core.entity.state.exchange.ExchangeRequest;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.AskOrder;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.BidOrder;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.Order;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPollResult;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingVote;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.Poll;
import com.apollocurrency.aplwallet.apl.core.entity.state.poll.PollOptionResult;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffler;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.model.HoldingType;
import com.apollocurrency.aplwallet.apl.core.peer.Hallmark;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.service.appdata.funding.FundingMonitorService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.service.state.AliasService;
import com.apollocurrency.aplwallet.apl.core.service.state.DGSService;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.PollService;
import com.apollocurrency.aplwallet.apl.core.service.state.ShufflingService;
import com.apollocurrency.aplwallet.apl.core.service.state.TradeService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountLeaseService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetTransferService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyTransferService;
import com.apollocurrency.aplwallet.apl.core.service.state.exchange.ExchangeService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionJsonSerializer;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCAssetDeleteAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCAssetTransferAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsOrderCancellationAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCOrderPlacementAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyIssuanceAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MSCurrencyTransferAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemExchangeAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MSPublishExchangeOfferAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.Filter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Vetoed
public final class JSONData {
    private static final AliasService ALIAS_SERVICE = CDI.current().select(AliasService.class).get();
    private static final TradeService TRADE_SERVICE = CDI.current().select(TradeService.class).get();
    private static Logger LOG = LoggerFactory.getLogger(JSONData.class);
    private static BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
    private static Blockchain blockchain = CDI.current().select(Blockchain.class).get();
    private static PhasingPollService phasingPollService = CDI.current().select(PhasingPollService.class).get();
    private static AccountService accountService = CDI.current().select(AccountService.class).get();
    private static AccountLeaseService accountLeaseService = CDI.current().select(AccountLeaseService.class).get();
    private static AccountAssetService accountAssetService = CDI.current().select(AccountAssetService.class).get();
    private static DGSService dgsService = CDI.current().select(DGSService.class).get();
    private static AssetService assetService = CDI.current().select(AssetService.class).get();
    private static final PollService POLL_SERVICE = CDI.current().select(PollService.class).get();
    private static AssetTransferService assetTransferService = CDI.current().select(AssetTransferService.class).get();
    private static ExchangeService exchangeService = CDI.current().select(ExchangeService.class).get();
    private static CurrencyTransferService currencyTransferService = CDI.current().select(CurrencyTransferService.class).get();
    private static CurrencyService currencyService = CDI.current().select(CurrencyService.class).get();
    private static ShufflingService shufflingService = CDI.current().select(ShufflingService.class).get();
    private static FundingMonitorService fundingMonitorService = CDI.current().select(FundingMonitorService.class).get();
    private static PrunableLoadingService prunableLoadingService = CDI.current().select(PrunableLoadingService.class).get();
    private static TransactionJsonSerializer transactionJsonSerializer = CDI.current().select(TransactionJsonSerializer.class).get();
    private static GenesisAccounts genesisAccounts = CDI.current().select(GenesisAccounts.class).get();

    private JSONData() {
    } // never

    public static JSONObject alias(Alias alias) {
        JSONObject json = new JSONObject();
        putAccount(json, "account", alias.getAccountId());
        json.put("aliasName", alias.getAliasName());
        json.put("aliasURI", alias.getAliasURI());
        json.put("timestamp", alias.getTimestamp());
        json.put("alias", Long.toUnsignedString(alias.getId()));
        AliasOffer offer = ALIAS_SERVICE.getOffer(alias);
        if (offer != null) {
            json.put("priceATM", String.valueOf(offer.getPriceATM()));
            if (offer.getBuyerId() != 0) {
                json.put("buyer", Long.toUnsignedString(offer.getBuyerId()));
            }
        }
        return json;
    }

    static void putAccountBalancePercentage(JSONObject json, Account account, long totalAmount) {
        json.put("percentage", String.format("%.4f%%", 100D * account.getBalanceATM() / totalAmount));
    }

    /**
     * Use com.apollocurrency.aplwallet.apl.core.rest.service.AccountService#getAccountBalances(com.apollocurrency.aplwallet.apl.core.account.Account, boolean, java.lang.String, java.lang.String)
     */
    @Deprecated
    public static JSONObject accountBalance(Account account, boolean includeEffectiveBalance) {
        return accountBalance(account, includeEffectiveBalance, blockchain.getHeight());
    }

    /**
     * Use com.apollocurrency.aplwallet.apl.core.rest.service.AccountService#getAccountBalances(com.apollocurrency.aplwallet.apl.core.account.Account, boolean, java.lang.String, java.lang.String)
     */
    @Deprecated
    public static JSONObject accountBalance(Account account, boolean includeEffectiveBalance, int height) {
        JSONObject json = new JSONObject();
        if (account == null) {
            json.put("balanceATM", "0");
            json.put("unconfirmedBalanceATM", "0");
            json.put("forgedBalanceATM", "0");
            if (includeEffectiveBalance) {
                json.put("effectiveBalanceAPL", "0");
                json.put("guaranteedBalanceATM", "0");
            }
        } else {
            json.put("balanceATM", String.valueOf(account.getBalanceATM()));
            json.put("unconfirmedBalanceATM", String.valueOf(account.getUnconfirmedBalanceATM()));
            json.put("forgedBalanceATM", String.valueOf(account.getForgedBalanceATM()));
            if (includeEffectiveBalance) {
                json.put("effectiveBalanceAPL", accountService.getEffectiveBalanceAPL(account, height, false));
                json.put("guaranteedBalanceATM", String.valueOf(accountService.getGuaranteedBalanceATM(account, blockchainConfig.getGuaranteedBalanceConfirmations(), height)));
            }
        }
        return json;
    }

    /**
     * Use {@link com.apollocurrency.aplwallet.apl.core.rest.converter.AccountConverter#addAccountLessors(AccountDTO, List, boolean)}
     */
    @Deprecated
    public static JSONObject lessor(Account account, boolean includeEffectiveBalance) {
        JSONObject json = new JSONObject();
        AccountLease accountLease = accountLeaseService.getAccountLease(account);
        if (accountLease != null) {
            if (accountLease.getCurrentLesseeId() != 0) {
                putAccount(json, "currentLessee", accountLease.getCurrentLesseeId());
                json.put("currentHeightFrom", String.valueOf(accountLease.getCurrentLeasingHeightFrom()));
                json.put("currentHeightTo", String.valueOf(accountLease.getCurrentLeasingHeightTo()));
                if (includeEffectiveBalance) {
                    json.put("effectiveBalanceAPL", String.valueOf(accountService.getGuaranteedBalanceATM(account) / blockchainConfig.getOneAPL()));
                }
            }
            if (accountLease.getNextLesseeId() != 0) {
                putAccount(json, "nextLessee", accountLease.getNextLesseeId());
                json.put("nextHeightFrom", String.valueOf(accountLease.getNextLeasingHeightFrom()));
                json.put("nextHeightTo", String.valueOf(accountLease.getNextLeasingHeightTo()));
            }
        }
        return json;
    }

    public static JSONObject asset(Asset asset, boolean includeCounts) {
        JSONObject json = new JSONObject();
        putAccount(json, "account", asset.getAccountId());
        json.put("name", asset.getName());
        json.put("description", asset.getDescription());
        json.put("decimals", asset.getDecimals());
        json.put("initialQuantityATU", String.valueOf(asset.getInitialQuantityATU()));
        json.put("quantityATU", String.valueOf(asset.getQuantityATU()));
        json.put("asset", Long.toUnsignedString(asset.getId()));
        if (includeCounts) {
            json.put("numberOfTrades", TRADE_SERVICE.getTradeCount(asset.getId()));
            json.put("numberOfTransfers", assetTransferService.getTransferCount(asset.getId()));
            json.put("numberOfAccounts", accountAssetService.getCountByAsset(asset.getId()));
        }
        return json;
    }

    public static JSONObject currency(Currency currency, boolean includeCounts) {
        JSONObject json = new JSONObject();
        json.put("currency", Long.toUnsignedString(currency.getId()));
        putAccount(json, "account", currency.getAccountId());
        json.put("name", currency.getName());
        json.put("code", currency.getCode());
        json.put("description", currency.getDescription());
        json.put("type", currency.getType());
        json.put("initialSupply", String.valueOf(currency.getInitialSupply()));
        json.put("currentSupply", String.valueOf(currencyService.getCurrentSupply(currency)));
        json.put("reserveSupply", String.valueOf(currency.getReserveSupply()));
        json.put("maxSupply", String.valueOf(currency.getMaxSupply()));
        json.put("creationHeight", currency.getCreationHeight());
        json.put("issuanceHeight", currency.getIssuanceHeight());
        json.put("minReservePerUnitATM", String.valueOf(currency.getMinReservePerUnitATM()));
        json.put("currentReservePerUnitATM", String.valueOf(currencyService.getCurrentReservePerUnitATM(currency)));
        json.put("minDifficulty", currency.getMinDifficulty());
        json.put("maxDifficulty", currency.getMaxDifficulty());
        json.put("algorithm", currency.getAlgorithm());
        json.put("decimals", currency.getDecimals());
        if (includeCounts) {
            json.put("numberOfExchanges", exchangeService.getExchangeCount(currency.getId()));
            json.put("numberOfTransfers", currencyTransferService.getTransferCount(currency.getId()));
        }
        JSONArray types = new JSONArray();
        for (CurrencyType type : CurrencyType.values()) {
            if (currency.is(type)) {
                types.add(type.toString());
            }
        }
        json.put("types", types);
        return json;
    }

    public static JSONObject currencyFounder(CurrencyFounder founder) {
        JSONObject json = new JSONObject();
        json.put("currency", Long.toUnsignedString(founder.getCurrencyId()));
        putAccount(json, "account", founder.getAccountId());
        json.put("amountPerUnitATM", String.valueOf(founder.getAmountPerUnitATM()));
        return json;
    }

    /**
     * Use {@link com.apollocurrency.aplwallet.apl.core.rest.converter.AccountConverter#addAccountAssets(AccountDTO, List)}
     */
    @Deprecated
    public static JSONObject accountAsset(AccountAsset accountAsset, boolean includeAccount, boolean includeAssetInfo) {
        JSONObject json = new JSONObject();
        if (includeAccount) {
            putAccount(json, "account", accountAsset.getAccountId());
        }
        json.put("asset", Long.toUnsignedString(accountAsset.getAssetId()));
        json.put("quantityATU", String.valueOf(accountAsset.getQuantityATU()));
        json.put("unconfirmedQuantityATU", String.valueOf(accountAsset.getUnconfirmedQuantityATU()));
        if (includeAssetInfo) {
            putAssetInfo(json, accountAsset.getAssetId());
        }
        return json;
    }

    /**
     * Use {@link com.apollocurrency.aplwallet.apl.core.rest.converter.AccountConverter#addAccountCurrencies(AccountDTO, List)}
     */
    @Deprecated
    public static JSONObject accountCurrency(AccountCurrency accountCurrency, boolean includeAccount, boolean includeCurrencyInfo) {
        JSONObject json = new JSONObject();
        if (includeAccount) {
            putAccount(json, "account", accountCurrency.getAccountId());
        }
        json.put("currency", Long.toUnsignedString(accountCurrency.getCurrencyId()));
        json.put("units", String.valueOf(accountCurrency.getUnits()));
        json.put("unconfirmedUnits", String.valueOf(accountCurrency.getUnconfirmedUnits()));
        if (includeCurrencyInfo) {
            putCurrencyInfo(json, accountCurrency.getCurrencyId());
        }
        return json;
    }

    public static JSONObject accountProperty(AccountProperty accountProperty, boolean includeAccount, boolean includeSetter) {
        JSONObject json = new JSONObject();
        if (includeAccount) {
            putAccount(json, "recipient", accountProperty.getRecipientId());
        }
        if (includeSetter) {
            putAccount(json, "setter", accountProperty.getSetterId());
        }
        json.put("property", accountProperty.getProperty());
        json.put("value", accountProperty.getValue());
        return json;
    }

    public static JSONObject askOrder(AskOrder order) {
        JSONObject json = order(order);
        json.put("type", "ask");
        return json;
    }

    public static JSONObject bidOrder(BidOrder order) {
        JSONObject json = order(order);
        json.put("type", "bid");
        return json;
    }

    public static <T extends Order> JSONObject order(T order) {
        JSONObject json = new JSONObject();
        json.put("order", Long.toUnsignedString(order.getId()));
        json.put("asset", Long.toUnsignedString(order.getAssetId()));
        putAccount(json, "account", order.getAccountId());
        json.put("quantityATU", String.valueOf(order.getQuantityATU()));
        json.put("priceATM", String.valueOf(order.getPriceATM()));
        json.put("height", order.getHeight());
        json.put("transactionIndex", order.getTransactionIndex());
        json.put("transactionHeight", order.getTransactionHeight());
        return json;
    }

    public static JSONObject expectedAskOrder(Transaction transaction) {
        JSONObject json = expectedOrder(transaction);
        json.put("type", "ask");
        return json;
    }

    public static JSONObject expectedBidOrder(Transaction transaction) {
        JSONObject json = expectedOrder(transaction);
        json.put("type", "bid");
        return json;
    }

    private static JSONObject expectedOrder(Transaction transaction) {
        JSONObject json = new JSONObject();
        CCOrderPlacementAttachment attachment = (CCOrderPlacementAttachment) transaction.getAttachment();
        json.put("order", transaction.getStringId());
        json.put("asset", Long.toUnsignedString(attachment.getAssetId()));
        putAccount(json, "account", transaction.getSenderId());
        json.put("quantityATU", String.valueOf(attachment.getQuantityATU()));
        json.put("priceATM", String.valueOf(attachment.getPriceATM()));
        putExpectedTransaction(json, transaction);
        return json;
    }

    public static JSONObject expectedOrderCancellation(Transaction transaction) {
        JSONObject json = new JSONObject();
        ColoredCoinsOrderCancellationAttachment attachment = (ColoredCoinsOrderCancellationAttachment) transaction.getAttachment();
        json.put("order", Long.toUnsignedString(attachment.getOrderId()));
        putAccount(json, "account", transaction.getSenderId());
        putExpectedTransaction(json, transaction);
        return json;
    }

    public static JSONObject offer(CurrencyExchangeOffer offer) {
        JSONObject json = new JSONObject();
        json.put("offer", Long.toUnsignedString(offer.getId()));
        putAccount(json, "account", offer.getAccountId());
        json.put("height", offer.getHeight());
        json.put("expirationHeight", offer.getExpirationHeight());
        json.put("currency", Long.toUnsignedString(offer.getCurrencyId()));
        json.put("rateATM", String.valueOf(offer.getRateATM()));
        json.put("limit", String.valueOf(offer.getLimit()));
        json.put("supply", String.valueOf(offer.getSupply()));
        return json;
    }

    public static JSONObject expectedBuyOffer(Transaction transaction) {
        JSONObject json = expectedOffer(transaction);
        MSPublishExchangeOfferAttachment attachment = (MSPublishExchangeOfferAttachment) transaction.getAttachment();
        json.put("rateATM", String.valueOf(attachment.getBuyRateATM()));
        json.put("limit", String.valueOf(attachment.getTotalBuyLimit()));
        json.put("supply", String.valueOf(attachment.getInitialBuySupply()));
        return json;
    }

    public static JSONObject expectedSellOffer(Transaction transaction) {
        JSONObject json = expectedOffer(transaction);
        MSPublishExchangeOfferAttachment attachment = (MSPublishExchangeOfferAttachment) transaction.getAttachment();
        json.put("rateATM", String.valueOf(attachment.getSellRateATM()));
        json.put("limit", String.valueOf(attachment.getTotalSellLimit()));
        json.put("supply", String.valueOf(attachment.getInitialSellSupply()));
        return json;
    }

    private static JSONObject expectedOffer(Transaction transaction) {
        MSPublishExchangeOfferAttachment attachment = (MSPublishExchangeOfferAttachment) transaction.getAttachment();
        JSONObject json = new JSONObject();
        json.put("offer", transaction.getStringId());
        putAccount(json, "account", transaction.getSenderId());
        json.put("expirationHeight", attachment.getExpirationHeight());
        json.put("currency", Long.toUnsignedString(attachment.getCurrencyId()));
        putExpectedTransaction(json, transaction);
        return json;
    }

    public static JSONObject genesisBalancesJson(int firstIndex, int lastIndex) {
        JSONObject result = new JSONObject();
        List<Map.Entry<String, Long>> genesisBalances = genesisAccounts.getGenesisBalances(firstIndex, lastIndex);
        JSONArray accountArray = new JSONArray();
        for (int i = 0; i < genesisBalances.size(); i++) {
            Map.Entry<String, Long> accountBalanceEntry = genesisBalances.get(i);
            JSONObject accountBalanceJson = new JSONObject();
            putAccount(accountBalanceJson, "account", Convert.parseAccountId(accountBalanceEntry.getKey()));
            accountBalanceJson.put("balanceATM", accountBalanceEntry.getValue());
            accountArray.add(accountBalanceJson);
        }
        result.put("accounts", accountArray);
        result.put("total", genesisAccounts.getGenesisBalancesNumber());
        return result;
    }

    /**
     * Replaced by {@link com.apollocurrency.aplwallet.apl.core.rest.service.AccountStatisticsService#getAccountsStatistic(int)}
     */
    @Deprecated
    public static JSONObject getAccountsStatistic(int numberOfAccounts) {
        long totalSupply = accountService.getTotalSupply();
        long totalAccounts = accountService.getTotalNumberOfAccounts();
        long totalAmountOnTopAccounts = accountService.getTotalAmountOnTopAccounts(numberOfAccounts);
        List<Account> topAccounts = accountService.getTopHolders(numberOfAccounts);

        JSONObject result = new JSONObject();
        result.put("totalSupply", totalSupply);
        result.put("totalNumberOfAccounts", totalAccounts);
        result.put("numberOfTopAccounts", numberOfAccounts);
        result.put("totalAmountOnTopAccounts", totalAmountOnTopAccounts);
        JSONArray holders = new JSONArray();
        topAccounts.forEach(account -> {
            JSONObject accountJson = JSONData.accountBalance(account, false);
            JSONData.putAccount(accountJson, "account", account.getId());
            holders.add(accountJson);
        });
        result.put("topHolders", holders);
        return result;
    }

    public static JSONObject availableOffers(AvailableOffers availableOffers) {
        JSONObject json = new JSONObject();
        json.put("rateATM", String.valueOf(availableOffers.getRateATM()));
        json.put("units", String.valueOf(availableOffers.getUnits()));
        json.put("amountATM", String.valueOf(availableOffers.getAmountATM()));
        return json;
    }

    public static JSONObject shuffling(Shuffling shuffling, boolean includeHoldingInfo) {
        JSONObject json = new JSONObject();
        json.put("shuffling", Long.toUnsignedString(shuffling.getId()));
        putAccount(json, "issuer", shuffling.getIssuerId());
        json.put("holding", Long.toUnsignedString(shuffling.getHoldingId()));
        json.put("holdingType", shuffling.getHoldingType().getCode());
        if (shuffling.getAssigneeAccountId() != 0) {
            putAccount(json, "assignee", shuffling.getAssigneeAccountId());
        }
        json.put("amount", String.valueOf(shuffling.getAmount()));
        json.put("blocksRemaining", shuffling.getBlocksRemaining());
        json.put("participantCount", shuffling.getParticipantCount());
        json.put("registrantCount", shuffling.getRegistrantCount());
        json.put("stage", shuffling.getStage().getCode());
        json.put("shufflingStateHash", Convert.toHexString(shufflingService.getStageHash(shuffling)));
        json.put("shufflingFullHash", Convert.toHexString(shufflingService.getFullHash(shuffling.getId())));
        JSONArray recipientPublicKeys = new JSONArray();
        for (byte[] recipientPublicKey : shuffling.getRecipientPublicKeys()) {
            recipientPublicKeys.add(Convert.toHexString(recipientPublicKey));
        }
        if (recipientPublicKeys.size() > 0) {
            json.put("recipientPublicKeys", recipientPublicKeys);
        }
        if (includeHoldingInfo && shuffling.getHoldingType() != HoldingType.APL) {
            JSONObject holdingJson = new JSONObject();
            if (shuffling.getHoldingType() == HoldingType.ASSET) {
                putAssetInfo(holdingJson, shuffling.getHoldingId());
            } else if (shuffling.getHoldingType() == HoldingType.CURRENCY) {
                putCurrencyInfo(holdingJson, shuffling.getHoldingId());
            }
            json.put("holdingInfo", holdingJson);
        }
        return json;
    }

    public static JSONObject participant(ShufflingParticipant participant) {
        JSONObject json = new JSONObject();
        json.put("shuffling", Long.toUnsignedString(participant.getShufflingId()));
        putAccount(json, "account", participant.getAccountId());
        putAccount(json, "nextAccount", participant.getNextAccountId());
        json.put("state", participant.getState().getCode());
        return json;
    }

    public static JSONObject shuffler(Shuffler shuffler, boolean includeParticipantState) {
        JSONObject json = new JSONObject();
        putAccount(json, "account", shuffler.getAccountId());
        putAccount(json, "recipient", AccountService.getId(shuffler.getRecipientPublicKey()));
        json.put("shufflingFullHash", Convert.toHexString(shuffler.getShufflingFullHash()));
        json.put("shuffling", Long.toUnsignedString(Convert.transactionFullHashToId(shuffler.getShufflingFullHash())));
        if (shuffler.getFailedTransaction() != null) {
            json.put("failedTransaction", unconfirmedTransaction(shuffler.getFailedTransaction()));
            json.put("failureCause", shuffler.getFailureCause().getMessage());
        }
        if (includeParticipantState) {
            ShufflingParticipant participant = shufflingService.getParticipant(Convert.transactionFullHashToId(shuffler.getShufflingFullHash()), shuffler.getAccountId());
            if (participant != null) {
                json.put("participantState", participant.getState().getCode());
            }
        }
        return json;
    }

    /**
     * Use {@link com.apollocurrency.aplwallet.apl.core.rest.converter.BlockConverter#convert(Object)}
     */
    @Deprecated
    public static JSONObject block(Block block, boolean includeTransactions, boolean includeExecutedPhased) {
        JSONObject json = new JSONObject();
        json.put("block", block.getStringId());
        json.put("height", block.getHeight());
        putAccount(json, "generator", block.getGeneratorId());
        byte[] generatorPublicKey = block.getGeneratorPublicKey();
        json.put("generatorPublicKey", Convert.toHexString(generatorPublicKey));
        json.put("timestamp", block.getTimestamp());

        json.put("timeout", block.getTimeout());
        json.put("numberOfTransactions", blockchain.getBlockTransactionCount(block.getId()));
        json.put("totalFeeATM", String.valueOf(block.getTotalFeeATM()));
        json.put("payloadLength", block.getPayloadLength());
        json.put("version", block.getVersion());
        json.put("baseTarget", Long.toUnsignedString(block.getBaseTarget()));
        json.put("cumulativeDifficulty", block.getCumulativeDifficulty().toString());
        if (block.getPreviousBlockId() != 0) {
            json.put("previousBlock", Long.toUnsignedString(block.getPreviousBlockId()));
        }
        if (block.getNextBlockId() != 0) {
            json.put("nextBlock", Long.toUnsignedString(block.getNextBlockId()));
        }
        json.put("payloadHash", Convert.toHexString(block.getPayloadHash()));
        json.put("generationSignature", Convert.toHexString(block.getGenerationSignature()));
        json.put("previousBlockHash", Convert.toHexString(block.getPreviousBlockHash()));
        json.put("blockSignature", Convert.toHexString(block.getBlockSignature()));
        JSONArray transactions = new JSONArray();
        Long totalAmountATM = 0L;
        for (Transaction transaction : blockchain.loadBlockData(block).getTransactions()) {
            JSONObject transactionJson = transaction(true, transaction);
            Long amountATM = Long.parseLong((String) transactionJson.get("amountATM"));
            totalAmountATM += amountATM;
            if (includeTransactions) {
                transactions.add(transactionJson);
            }
        }
        json.put("totalAmountATM", String.valueOf(totalAmountATM));
        json.put("transactions", transactions);
        if (includeExecutedPhased) {
            JSONArray phasedTransactions = new JSONArray();
            List<PhasingPollResult> approved = phasingPollService.getApproved(block.getHeight());
                for (PhasingPollResult phasingPollResult : approved) {
                    long phasedTransactionId = phasingPollResult.getId();
                    if (includeTransactions) {
                        phasedTransactions.add(transaction(false, blockchain.getTransaction(phasedTransactionId)));
                    } else {
                        phasedTransactions.add(Long.toUnsignedString(phasedTransactionId));
                    }
                }
            json.put("executedPhasedTransactions", phasedTransactions);
        }
        JSONArray txErrorHashesArray = new JSONArray();
        json.put("numberOfFailedTxs", block.getTxErrorHashes().size());
        block.getTxErrorHashes().forEach(e-> {
            JSONObject txErrorHash = new JSONObject();
            txErrorHash.put("id", Long.toUnsignedString(e.getId()));
            txErrorHash.put("errorHash", Convert.toHexString(e.getErrorHash()));
            txErrorHash.put("error", e.getError());
            txErrorHashesArray.add(txErrorHash);
        });
        json.put("txErrorHashes", txErrorHashesArray);

        return json;
    }


    private static JSONObject accounts(long totalAmountOnTopAccounts, long totalSupply, long totalAccounts, int numberOfAccounts) {
        JSONObject result = new JSONObject();
        result.put("totalSupply", totalSupply);
        result.put("totalNumberOfAccounts", totalAccounts);
        result.put("totalAmountOnTopAccounts", totalAmountOnTopAccounts);
        result.put("numberOfTopAccounts", numberOfAccounts);
        return result;
    }

    public static JSONObject encryptedData(EncryptedData encryptedData) {
        JSONObject json = new JSONObject();
        json.put("data", Convert.toHexString(encryptedData.getData()));
        json.put("nonce", Convert.toHexString(encryptedData.getNonce()));
        return json;
    }

    public static JSONObject goods(DGSGoods goods, boolean includeCounts) {
        JSONObject json = new JSONObject();
        json.put("goods", Long.toUnsignedString(goods.getId()));
        json.put("name", goods.getName());
        json.put("description", goods.getDescription());
        json.put("quantity", goods.getQuantity());
        json.put("priceATM", String.valueOf(goods.getPriceATM()));
        putAccount(json, "seller", goods.getSellerId());
        json.put("tags", goods.getTags());
        JSONArray tagsJSON = new JSONArray();
        Collections.addAll(tagsJSON, goods.getParsedTags());
        json.put("parsedTags", tagsJSON);
        json.put("delisted", goods.isDelisted());
        json.put("timestamp", goods.getTimestamp());
        json.put("hasImage", goods.hasImage());
        if (includeCounts) {
            json.put("numberOfPurchases", dgsService.getGoodsPurchaseCount(goods.getId(), false, true));
            json.put("numberOfPublicFeedbacks", dgsService.getGoodsPurchaseCount(goods.getId(), true, true));
        }
        return json;
    }

    public static JSONObject tag(DGSTag tag) {
        JSONObject json = new JSONObject();
        json.put("tag", tag.getTag());
        json.put("inStockCount", tag.getInStockCount());
        json.put("totalCount", tag.getTotalCount());
        return json;
    }

    public static JSONObject hallmark(Hallmark hallmark) {
        JSONObject json = new JSONObject();
        putAccount(json, "account", AccountService.getId(hallmark.getPublicKey()));
        json.put("host", hallmark.getHost());
        json.put("port", hallmark.getPort());
        json.put("weight", hallmark.getWeight());
        String dateString = Hallmark.formatDate(hallmark.getDate());
        json.put("date", dateString);
        json.put("valid", hallmark.isValid());
        return json;
    }

    public static JSONObject token(Token token) {
        JSONObject json = new JSONObject();
        putAccount(json, "account", AccountService.getId(token.getPublicKey()));
        json.put("timestamp", token.getTimestamp());
        json.put("valid", token.isValid());
        return json;
    }

    public static JSONObject peer(Peer peer) {
        JSONObject json = new JSONObject();
        json.put("address", peer.getHost());
        json.put("port", peer.getPort());
        json.put("state", peer.getState().ordinal());
        json.put("announcedAddress", peer.getAnnouncedAddress());
        json.put("shareAddress", peer.shareAddress());
        if (peer.getHallmark() != null) {
            json.put("hallmark", peer.getHallmark().getHallmarkString());
        }
        json.put("weight", peer.getWeight());
        json.put("downloadedVolume", peer.getDownloadedVolume());
        json.put("uploadedVolume", peer.getUploadedVolume());
        json.put("application", peer.getApplication());
        json.put("version", peer.getVersion());
        json.put("platform", peer.getPlatform());
        if (peer.getApiPort() != 0) {
            json.put("apiPort", peer.getApiPort());
        }
        if (peer.getApiSSLPort() != 0) {
            json.put("apiSSLPort", peer.getApiSSLPort());
        }
        json.put("blacklisted", peer.isBlacklisted());
        json.put("lastUpdated", peer.getLastUpdated());
        json.put("lastConnectAttempt", peer.getLastConnectAttempt());
        json.put("inbound", peer.isInbound());
        json.put("inboundWebSocket", peer.isInboundSocket());
        json.put("outboundWebSocket", peer.isOutboundSocket());
        if (peer.isBlacklisted()) {
            json.put("blacklistingCause", peer.getBlacklistingCause());
        }
        JSONArray servicesArray = new JSONArray();
        for (Peer.Service service : Peer.Service.values()) {
            if (peer.providesService(service)) {
                servicesArray.add(service.name());
            }
        }
        json.put("services", servicesArray);
        json.put("blockchainState", peer.getBlockchainState());
        json.put("chainId", peer.getChainId());
        return json;
    }

    public static JSONObject poll(Poll poll) {
        JSONObject json = new JSONObject();
        putAccount(json, "account", poll.getAccountId());
        json.put("poll", Long.toUnsignedString(poll.getId()));
        json.put("name", poll.getName());
        json.put("description", poll.getDescription());
        JSONArray options = new JSONArray();
        Collections.addAll(options, poll.getOptions());
        json.put("options", options);
        json.put("finishHeight", poll.getFinishHeight());
        json.put("minNumberOfOptions", poll.getMinNumberOfOptions());
        json.put("maxNumberOfOptions", poll.getMaxNumberOfOptions());
        json.put("minRangeValue", poll.getMinRangeValue());
        json.put("maxRangeValue", poll.getMaxRangeValue());
        putVoteWeighting(json, poll.getVoteWeighting());
        json.put("finished", POLL_SERVICE.isFinished(poll.getFinishHeight()));
        json.put("timestamp", poll.getTimestamp());
        return json;
    }

    public static JSONObject pollResults(Poll poll, List<PollOptionResult> results, VoteWeighting voteWeighting) {
        JSONObject json = new JSONObject();
        json.put("poll", Long.toUnsignedString(poll.getId()));
        if (voteWeighting.getMinBalanceModel() == VoteWeighting.MinBalanceModel.ASSET) {
            json.put("decimals", assetService.getAsset(voteWeighting.getHoldingId()).getDecimals());
        } else if (voteWeighting.getMinBalanceModel() == VoteWeighting.MinBalanceModel.CURRENCY) {
            Currency currency = currencyService.getCurrency(voteWeighting.getHoldingId());
            if (currency != null) {
                json.put("decimals", currency.getDecimals());
            } else {
                Transaction currencyIssuance = blockchain.getTransaction(voteWeighting.getHoldingId());
                MonetarySystemCurrencyIssuanceAttachment currencyIssuanceAttachment = (MonetarySystemCurrencyIssuanceAttachment) currencyIssuance.getAttachment();
                json.put("decimals", currencyIssuanceAttachment.getDecimals());
            }
        }
        putVoteWeighting(json, voteWeighting);
        json.put("finished", POLL_SERVICE.isFinished(poll.getFinishHeight()));
        JSONArray options = new JSONArray();
        Collections.addAll(options, poll.getOptions());
        json.put("options", options);

        JSONArray resultsJson = new JSONArray();
        for (PollOptionResult option : results) {
            JSONObject optionJSON = new JSONObject();
            if (!option.isUndefined()) {
                optionJSON.put("result", String.valueOf(option.getResult()));
                optionJSON.put("weight", String.valueOf(option.getWeight()));
            } else {
                optionJSON.put("result", "");
                optionJSON.put("weight", "0");
            }
            resultsJson.add(optionJSON);
        }
        json.put("results", resultsJson);
        return json;
    }

    public static JSONObject vote(Vote vote, VoteWeighter weighter) {
        JSONObject json = new JSONObject();
        putAccount(json, "voter", vote.getVoterId());
        json.put("transaction", Long.toUnsignedString(vote.getId()));
        JSONArray votesJson = new JSONArray();
        for (byte v : vote.getVoteBytes()) {
            if (v == Constants.NO_VOTE_VALUE) {
                votesJson.add("");
            } else {
                votesJson.add(Byte.toString(v));
            }
        }
        json.put("votes", votesJson);
        if (weighter != null) {
            json.put("weight", String.valueOf(weighter.calcWeight(vote.getVoterId())));
        }
        return json;
    }

    public static JSONObject phasingPoll(PhasingPoll poll, boolean countVotes) {
        JSONObject json = new JSONObject();
        json.put("transaction", Long.toUnsignedString(poll.getId()));
        json.put("transactionFullHash", Convert.toHexString(poll.getFullHash()));
        json.put("finishHeight", poll.getFinishHeight());
        json.put("quorum", String.valueOf(poll.getQuorum()));
        putAccount(json, "account", poll.getAccountId());
        JSONArray whitelistJson = new JSONArray();
        for (long accountId : poll.getWhitelist()) {
            JSONObject whitelisted = new JSONObject();
            putAccount(whitelisted, "whitelisted", accountId);
            whitelistJson.add(whitelisted);
        }
        json.put("whitelist", whitelistJson);
        List<byte[]> linkedFullHashes = poll.getLinkedFullHashes();
        if (linkedFullHashes.size() > 0) {
            JSONArray linkedFullHashesJSON = new JSONArray();
            for (byte[] hash : linkedFullHashes) {
                linkedFullHashesJSON.add(Convert.toHexString(hash));
            }
            json.put("linkedFullHashes", linkedFullHashesJSON);
        }
        if (poll.getHashedSecret() != null) {
            json.put("hashedSecret", Convert.toHexString(poll.getHashedSecret()));
        }
        putVoteWeighting(json, poll.getVoteWeighting());
        PhasingPollResult phasingPollResult = phasingPollService.getResult(poll.getId());
        json.put("finished", phasingPollResult != null);
        if (phasingPollResult != null) {
            json.put("approved", phasingPollResult.isApproved());
            json.put("result", String.valueOf(phasingPollResult.getResult()));
            json.put("executionHeight", phasingPollResult.getHeight());
        } else if (countVotes) {
            json.put("result", String.valueOf(phasingPollService.countVotes(poll)));
        }
        return json;
    }

    public static JSONObject phasingPollResult(PhasingPollResult phasingPollResult) {
        JSONObject json = new JSONObject();
        json.put("transaction", Long.toUnsignedString(phasingPollResult.getId()));
        json.put("approved", phasingPollResult.isApproved());
        json.put("result", String.valueOf(phasingPollResult.getResult()));
        json.put("executionHeight", phasingPollResult.getHeight());
        return json;
    }

    public static JSONObject phasingPollVote(PhasingVote vote) {
        JSONObject json = new JSONObject();
        JSONData.putAccount(json, "voter", vote.getVoterId());
        json.put("transaction", Long.toUnsignedString(vote.getVoteId()));
        return json;
    }

    /**
     * Use {@link com.apollocurrency.aplwallet.apl.core.rest.converter.VoteWeightingConverter#apply(VoteWeighting)}
     */
    @Deprecated
    private static void putVoteWeighting(JSONObject json, VoteWeighting voteWeighting) {
        json.put("votingModel", voteWeighting.getVotingModel().getCode());
        json.put("minBalance", String.valueOf(voteWeighting.getMinBalance()));
        json.put("minBalanceModel", voteWeighting.getMinBalanceModel().getCode());
        if (voteWeighting.getHoldingId() != 0) {
            json.put("holding", Long.toUnsignedString(voteWeighting.getHoldingId()));
        }
    }

/*
    @Deprecated
    public static JSONObject phasingOnly(PhasingOnly phasingOnly) {
        JSONObject json = new JSONObject();
        putAccount(json, "account", phasingOnly.getAccountId());
        json.put("quorum", String.valueOf(phasingOnly.getPhasingParams().getQuorum()));
        JSONArray whitelistJson = new JSONArray();
        for (long accountId : phasingOnly.getPhasingParams().getWhitelist()) {
            JSONObject whitelisted = new JSONObject();
            putAccount(whitelisted, "whitelisted", accountId);
            whitelistJson.add(whitelisted);
        }
        json.put("whitelist", whitelistJson);
        json.put("maxFees", String.valueOf(phasingOnly.getMaxFees()));
        json.put("minDuration", phasingOnly.getMinDuration());
        json.put("maxDuration", phasingOnly.getMaxDuration());
        putVoteWeighting(json, phasingOnly.getPhasingParams().getVoteWeighting());
        return json;
    }
*/

    /**
     * Use {@link com.apollocurrency.aplwallet.apl.core.rest.converter.AccountControlPhasingConverter#apply(AccountControlPhasing)}
     */
    @Deprecated
    public static JSONObject phasingOnly(AccountControlPhasing phasingOnly) {
        JSONObject json = new JSONObject();
        putAccount(json, "account", phasingOnly.getAccountId());
        json.put("quorum", String.valueOf(phasingOnly.getPhasingParams().getQuorum()));
        JSONArray whitelistJson = new JSONArray();
        for (long accountId : phasingOnly.getPhasingParams().getWhitelist()) {
            JSONObject whitelisted = new JSONObject();
            putAccount(whitelisted, "whitelisted", accountId);
            whitelistJson.add(whitelisted);
        }
        json.put("whitelist", whitelistJson);
        json.put("maxFees", String.valueOf(phasingOnly.getMaxFees()));
        json.put("minDuration", phasingOnly.getMinDuration());
        json.put("maxDuration", phasingOnly.getMaxDuration());
        putVoteWeighting(json, phasingOnly.getPhasingParams().getVoteWeighting());
        return json;
    }

    //    public static JSONObject purchase(DigitalGoodsStore.Purchase purchase) {
    public static JSONObject purchase(DGSService service, DGSPurchase purchase) {
        JSONObject json = new JSONObject();
        json.put("purchase", Long.toUnsignedString(purchase.getId()));
        json.put("goods", Long.toUnsignedString(purchase.getGoodsId()));
        DGSGoods goods = service.getGoods(purchase.getGoodsId());
        json.put("name", goods.getName());
        json.put("hasImage", goods.hasImage());
        putAccount(json, "seller", purchase.getSellerId());
        json.put("priceATM", String.valueOf(purchase.getPriceATM()));
        json.put("quantity", purchase.getQuantity());
        putAccount(json, "buyer", purchase.getBuyerId());
        json.put("timestamp", purchase.getTimestamp());
        json.put("deliveryDeadlineTimestamp", purchase.getDeliveryDeadlineTimestamp());
        if (purchase.getNote() != null) {
            json.put("note", encryptedData(purchase.getNote()));
        }
        json.put("pending", purchase.isPending());
        if (purchase.getEncryptedGoods() != null) {
            json.put("goodsData", encryptedData(purchase.getEncryptedGoods()));
            json.put("goodsIsText", purchase.goodsIsText());
        }
        if (purchase.getFeedbacks() != null) {
            JSONArray feedbacks = new JSONArray();
            for (DGSFeedback feedback : purchase.getFeedbacks()) {
                feedbacks.add(0, encryptedData(feedback.getFeedbackEncryptedData()));
            }
            json.put("feedbackNotes", feedbacks);
        }
        if (purchase.getPublicFeedbacks() != null) {
            JSONArray publicFeedbacks = new JSONArray();
            for (DGSPublicFeedback publicFeedback : purchase.getPublicFeedbacks()) {
                publicFeedbacks.add(0, publicFeedback.getFeedback());
            }
            json.put("publicFeedbacks", publicFeedbacks);
        }
        if (purchase.getRefundNote() != null) {
            json.put("refundNote", encryptedData(purchase.getRefundNote()));
        }
        if (purchase.getDiscountATM() > 0) {
            json.put("discountATM", String.valueOf(purchase.getDiscountATM()));
        }
        if (purchase.getRefundATM() > 0) {
            json.put("refundATM", String.valueOf(purchase.getRefundATM()));
        }
        return json;
    }

    public static JSONObject trade(Trade trade, boolean includeAssetInfo) {
        JSONObject json = new JSONObject();
        json.put("timestamp", trade.getTimestamp());
        json.put("quantityATU", String.valueOf(trade.getQuantityATU()));
        json.put("priceATM", String.valueOf(trade.getPriceATM()));
        json.put("asset", Long.toUnsignedString(trade.getAssetId()));
        json.put("askOrder", Long.toUnsignedString(trade.getAskOrderId()));
        json.put("bidOrder", Long.toUnsignedString(trade.getBidOrderId()));
        json.put("askOrderHeight", trade.getAskOrderHeight());
        json.put("bidOrderHeight", trade.getBidOrderHeight());
        putAccount(json, "seller", trade.getSellerId());
        putAccount(json, "buyer", trade.getBuyerId());
        json.put("block", Long.toUnsignedString(trade.getBlockId()));
        json.put("height", trade.getHeight());
        json.put("tradeType", trade.isBuy() ? "buy" : "sell");
        if (includeAssetInfo) {
            putAssetInfo(json, trade.getAssetId());
        }
        return json;
    }

    public static JSONObject assetTransfer(AssetTransfer assetTransfer, boolean includeAssetInfo) {
        JSONObject json = new JSONObject();
        json.put("assetTransfer", Long.toUnsignedString(assetTransfer.getId()));
        json.put("asset", Long.toUnsignedString(assetTransfer.getAssetId()));
        putAccount(json, "sender", assetTransfer.getSenderId());
        putAccount(json, "recipient", assetTransfer.getRecipientId());
        json.put("quantityATU", String.valueOf(assetTransfer.getQuantityATM())); //'...ATU' is returned from '...ATM' field
        json.put("height", assetTransfer.getHeight());
        json.put("timestamp", assetTransfer.getTimestamp());
        if (includeAssetInfo) {
            putAssetInfo(json, assetTransfer.getAssetId());
        }
        return json;
    }

    public static JSONObject expectedAssetTransfer(Transaction transaction, boolean includeAssetInfo) {
        JSONObject json = new JSONObject();
        CCAssetTransferAttachment attachment = (CCAssetTransferAttachment) transaction.getAttachment();
        json.put("assetTransfer", transaction.getStringId());
        json.put("asset", Long.toUnsignedString(attachment.getAssetId()));
        putAccount(json, "sender", transaction.getSenderId());
        putAccount(json, "recipient", transaction.getRecipientId());
        json.put("quantityATU", String.valueOf(attachment.getQuantityATU()));
        if (includeAssetInfo) {
            putAssetInfo(json, attachment.getAssetId());
        }
        putExpectedTransaction(json, transaction);
        return json;
    }

    public static JSONObject assetDelete(AssetDelete assetDelete, boolean includeAssetInfo) {
        JSONObject json = new JSONObject();
        json.put("assetDelete", Long.toUnsignedString(assetDelete.getId()));
        json.put("asset", Long.toUnsignedString(assetDelete.getAssetId()));
        putAccount(json, "account", assetDelete.getAccountId());
        json.put("quantityATU", String.valueOf(assetDelete.getQuantityATU()));
        json.put("height", assetDelete.getHeight());
        json.put("timestamp", assetDelete.getTimestamp());
        if (includeAssetInfo) {
            putAssetInfo(json, assetDelete.getAssetId());
        }
        return json;
    }

    public static JSONObject expectedAssetDelete(Transaction transaction, boolean includeAssetInfo) {
        JSONObject json = new JSONObject();
        CCAssetDeleteAttachment attachment = (CCAssetDeleteAttachment) transaction.getAttachment();
        json.put("assetDelete", transaction.getStringId());
        json.put("asset", Long.toUnsignedString(attachment.getAssetId()));
        putAccount(json, "account", transaction.getSenderId());
        json.put("quantityATU", String.valueOf(attachment.getQuantityATU()));
        if (includeAssetInfo) {
            putAssetInfo(json, attachment.getAssetId());
        }
        putExpectedTransaction(json, transaction);
        return json;
    }

    @Deprecated
    public static JSONObject assetDividend(AssetDividend assetDividend) {
        JSONObject json = new JSONObject();
        json.put("assetDividend", Long.toUnsignedString(assetDividend.getId()));
        json.put("asset", Long.toUnsignedString(assetDividend.getAssetId()));
        json.put("amountATMPerATU", String.valueOf(assetDividend.getAmountATMPerATU()));
        json.put("totalDividend", String.valueOf(assetDividend.getTotalDividend()));
        json.put("dividendHeight", assetDividend.getDividendHeight());
        json.put("numberOfAccounts", assetDividend.getNumAccounts());
        json.put("height", assetDividend.getHeight());
        json.put("timestamp", assetDividend.getTimestamp());
        return json;
    }

    public static JSONObject currencyTransfer(CurrencyTransfer transfer, boolean includeCurrencyInfo) {
        JSONObject json = new JSONObject();
        json.put("transfer", Long.toUnsignedString(transfer.getId()));
        json.put("currency", Long.toUnsignedString(transfer.getCurrencyId()));
        putAccount(json, "sender", transfer.getSenderId());
        putAccount(json, "recipient", transfer.getRecipientId());
        json.put("units", String.valueOf(transfer.getUnits()));
        json.put("height", transfer.getHeight());
        json.put("timestamp", transfer.getTimestamp());
        if (includeCurrencyInfo) {
            putCurrencyInfo(json, transfer.getCurrencyId());
        }
        return json;
    }

    public static JSONObject expectedCurrencyTransfer(Transaction transaction, boolean includeCurrencyInfo) {
        JSONObject json = new JSONObject();
        MSCurrencyTransferAttachment attachment = (MSCurrencyTransferAttachment) transaction.getAttachment();
        json.put("transfer", transaction.getStringId());
        json.put("currency", Long.toUnsignedString(attachment.getCurrencyId()));
        putAccount(json, "sender", transaction.getSenderId());
        putAccount(json, "recipient", transaction.getRecipientId());
        json.put("units", String.valueOf(attachment.getUnits()));
        if (includeCurrencyInfo) {
            putCurrencyInfo(json, attachment.getCurrencyId());
        }
        putExpectedTransaction(json, transaction);
        return json;
    }

    public static JSONObject exchange(Exchange exchange, boolean includeCurrencyInfo) {
        JSONObject json = new JSONObject();
        json.put("transaction", Long.toUnsignedString(exchange.getTransactionId()));
        json.put("timestamp", exchange.getTimestamp());
        json.put("units", String.valueOf(exchange.getUnits()));
        json.put("rateATM", String.valueOf(exchange.getRate()));
        json.put("currency", Long.toUnsignedString(exchange.getCurrencyId()));
        json.put("offer", Long.toUnsignedString(exchange.getOfferId()));
        putAccount(json, "seller", exchange.getSellerId());
        putAccount(json, "buyer", exchange.getBuyerId());
        json.put("block", Long.toUnsignedString(exchange.getBlockId()));
        json.put("height", exchange.getHeight());
        if (includeCurrencyInfo) {
            putCurrencyInfo(json, exchange.getCurrencyId());
        }
        return json;
    }

    public static JSONObject exchangeRequest(ExchangeRequest exchangeRequest, boolean includeCurrencyInfo) {
        JSONObject json = new JSONObject();
        json.put("transaction", Long.toUnsignedString(exchangeRequest.getId()));
        json.put("subtype", exchangeRequest.isBuy() ? TransactionTypes.TransactionTypeSpec.MS_EXCHANGE_BUY.getSubtype() :TransactionTypes.TransactionTypeSpec.MS_EXCHANGE_SELL);
        json.put("timestamp", exchangeRequest.getTimestamp());
        json.put("units", String.valueOf(exchangeRequest.getUnits()));
        json.put("rateATM", String.valueOf(exchangeRequest.getRate()));
        json.put("height", exchangeRequest.getHeight());
        if (includeCurrencyInfo) {
            putCurrencyInfo(json, exchangeRequest.getCurrencyId());
        }
        return json;
    }

    public static JSONObject expectedExchangeRequest(Transaction transaction, boolean includeCurrencyInfo) {
        JSONObject json = new JSONObject();
        json.put("transaction", transaction.getStringId());
        json.put("subtype", transaction.getType().getSpec().getSubtype());
        MonetarySystemExchangeAttachment attachment = (MonetarySystemExchangeAttachment) transaction.getAttachment();
        json.put("units", String.valueOf(attachment.getUnits()));
        json.put("rateATM", String.valueOf(attachment.getRateATM()));
        if (includeCurrencyInfo) {
            putCurrencyInfo(json, attachment.getCurrencyId());
        }
        putExpectedTransaction(json, transaction);
        return json;
    }

    public static JSONObject unconfirmedTransaction(Transaction transaction) {
        return unconfirmedTransaction(transaction, null, false);
    }

    static JSONObject unconfirmedTransaction(Transaction transaction, Filter<Appendix> filter, boolean isPrivate) {

        JSONObject json = new JSONObject();
        json.put("type", transaction.getType().getSpec().getType());
        json.put("subtype", transaction.getType().getSpec().getSubtype());
        json.put("phased", transaction.getPhasing() != null);
        json.put("timestamp", transaction.getTimestamp());
        json.put("deadline", transaction.getDeadline());
        json.put("senderPublicKey", Convert.toHexString(transaction.getSenderPublicKey()));
        if (transaction.getRecipientId() != 0) {
            if (transaction.getType().getSpec() == TransactionTypes.TransactionTypeSpec.PRIVATE_PAYMENT && isPrivate) {
                putPrivateAccount(json, "recipient", transaction.getRecipientId());
            } else {
                putAccount(json, "recipient", transaction.getRecipientId());
            }
        }
        if (transaction.getType().getSpec() == TransactionTypes.TransactionTypeSpec.PRIVATE_PAYMENT && isPrivate) {
            Random random = new Random();
            json.put("amountATM", String.valueOf((long) 100_000_000 * (random.nextInt(10_000_000) + 1)));
        } else {
            json.put("amountATM", String.valueOf(transaction.getAmountATM()));
        }
        json.put("feeATM", String.valueOf(transaction.getFeeATM()));
        String referencedTransactionFullHash = transaction.getReferencedTransactionFullHash();
        if (referencedTransactionFullHash != null) {
            json.put("referencedTransactionFullHash", referencedTransactionFullHash);
        }
        if (transaction.getSignature() != null) {
            json.put("signature", Convert.toHexString(transaction.getSignature().bytes()));
            json.put("signatureHash", Convert.toHexString(Crypto.sha256().digest(transaction.getSignature().bytes())));
            json.put("fullHash", transaction.getFullHashString());
            json.put("transaction", transaction.getStringId());
        }
        JSONObject attachmentJSON = new JSONObject();
        if (filter == null) {

            for (Appendix appendage : transaction.getAppendages()) {
                prunableLoadingService.loadPrunable(transaction, appendage, true);
                attachmentJSON.putAll(appendage.getJSONObject());
            }
        } else {
            for (Appendix appendage : transaction.getAppendages()) {
                if (filter.test(appendage)) {
                    prunableLoadingService.loadPrunable(transaction, appendage, true);
                    attachmentJSON.putAll(appendage.getJSONObject());
                }
            }
        }
        if (!attachmentJSON.isEmpty()) {
            for (Map.Entry entry : (Iterable<Map.Entry>) attachmentJSON.entrySet()) {
                if (entry.getValue() instanceof Long) {
                    entry.setValue(String.valueOf(entry.getValue()));
                }
            }
            json.put("attachment", attachmentJSON);
        }
        if (transaction.getType().getSpec() == TransactionTypes.TransactionTypeSpec.PRIVATE_PAYMENT && isPrivate) {
            putPrivateAccount(json, "sender", transaction.getSenderId());
        } else {
            putAccount(json, "sender", transaction.getSenderId());
        }
        json.put("height", transaction.getHeight());
        json.put("version", transaction.getVersion());
        json.put("ecBlockId", Long.toUnsignedString(transaction.getECBlockId()));
        json.put("ecBlockHeight", transaction.getECBlockHeight());

        return json;
    }

    public static JSONObject transaction(boolean isPrivate, Transaction transaction) {
        return transaction(transaction, false, isPrivate);
    }

    public static JSONObject transaction(Transaction transaction, boolean includePhasingResult, boolean isPrivate) {
        JSONObject json = transaction(transaction, null, isPrivate);
        if (includePhasingResult && transaction.getPhasing() != null) {
            PhasingPollResult phasingPollResult = phasingPollService.getResult(transaction.getId());
            if (phasingPollResult != null) {
                json.put("approved", phasingPollResult.isApproved());
                json.put("result", String.valueOf(phasingPollResult.getResult()));
                json.put("executionHeight", phasingPollResult.getHeight());
            }
        }
        return json;
    }

    /**
     * Use {@link com.apollocurrency.aplwallet.apl.core.rest.converter.BlockConverter#addTransactions(BlockDTO, Block)}
     */
    @Deprecated
    public static JSONObject transaction(Transaction transaction, Filter<Appendix> filter, boolean isPrivate) {
        JSONObject json = unconfirmedTransaction(transaction, filter, isPrivate);
        json.put("block", Long.toUnsignedString(transaction.getBlockId()));
        json.put("confirmations", blockchain.getHeight() - transaction.getHeight());
        json.put("blockTimestamp", transaction.getBlockTimestamp());
        json.put("transactionIndex", transaction.getIndex());
        json.put("errorMessage", transaction.getErrorMessage().orElse(null));
        return json;
    }

    public static JSONObject generator(GeneratorMemoryEntity generator, int elapsedTime) {
        JSONObject response = new JSONObject();
        long deadline = generator.getDeadline();
        putAccount(response, "account", generator.getAccountId());
        response.put("deadline", deadline);
        response.put("hitTime", generator.getHitTime());
        response.put("remaining", Math.max(deadline - elapsedTime, 0));
        return response;
    }

    public static JSONObject accountMonitor(FundingMonitorInstance monitor, boolean includeMonitoredAccounts) {
        JSONObject json = new JSONObject();
        json.put("holdingType", monitor.getHoldingType().getCode());
        json.put("account", Long.toUnsignedString(monitor.getAccountId()));
        json.put("accountRS", monitor.getAccountName());
        json.put("holding", Long.toUnsignedString(monitor.getHoldingId()));
        json.put("property", monitor.getProperty());
        json.put("amount", String.valueOf(monitor.getAmount()));
        json.put("threshold", String.valueOf(monitor.getThreshold()));
        json.put("interval", monitor.getInterval());
        if (includeMonitoredAccounts) {
            JSONArray jsonAccounts = new JSONArray();
            List<MonitoredAccount> accountList = fundingMonitorService.getMonitoredAccounts(monitor);
            accountList.forEach(account -> jsonAccounts.add(JSONData.monitoredAccount(account)));
            json.put("monitoredAccounts", jsonAccounts);
        }
        return json;
    }

    static JSONObject monitoredAccount(MonitoredAccount account) {
        JSONObject json = new JSONObject();
        json.put("account", Long.toUnsignedString(account.getAccountId()));
        json.put("accountRS", account.getAccountName());
        json.put("amount", String.valueOf(account.getAmount()));
        json.put("threshold", String.valueOf(account.getThreshold()));
        json.put("interval", account.getInterval());
        return json;
    }

    public static JSONObject prunableMessage(PrunableMessageService prunableMessageService, PrunableMessage prunableMessage, byte[] keySeed, byte[] sharedKey) {
        JSONObject json = new JSONObject();
        json.put("transaction", Long.toUnsignedString(prunableMessage.getId()));
        if (prunableMessage.getMessage() == null || prunableMessage.getEncryptedData() == null) {
            json.put("isText", prunableMessage.getMessage() != null ? prunableMessage.messageIsText() : prunableMessage.encryptedMessageIsText());
        }
        putAccount(json, "sender", prunableMessage.getSenderId());
        if (prunableMessage.getRecipientId() != 0) {
            putAccount(json, "recipient", prunableMessage.getRecipientId());
        }
        json.put("transactionTimestamp", prunableMessage.getTransactionTimestamp());
        json.put("blockTimestamp", prunableMessage.getBlockTimestamp());
        EncryptedData encryptedData = prunableMessage.getEncryptedData();
        if (encryptedData != null) {
            json.put("encryptedMessage", encryptedData(prunableMessage.getEncryptedData()));
            json.put("encryptedMessageIsText", prunableMessage.encryptedMessageIsText());
            byte[] decrypted = null;
            try {
                if (keySeed != null) {
                    decrypted = prunableMessageService.decryptUsingKeySeed(prunableMessage, keySeed);
                } else if (sharedKey != null && sharedKey.length > 0) {
                    decrypted = prunableMessageService.decryptUsingSharedKey(prunableMessage, sharedKey);
                }
                if (decrypted != null) {
                    json.put("decryptedMessage", Convert.toString(decrypted, prunableMessage.encryptedMessageIsText()));
                }
            } catch (RuntimeException e) {
                putException(json, e, "Decryption failed");
            }
            json.put("isCompressed", prunableMessage.isCompressed());
        }
        if (prunableMessage.getMessage() != null) {
            json.put("message", Convert.toString(prunableMessage.getMessage(), prunableMessage.messageIsText()));
            json.put("messageIsText", prunableMessage.messageIsText());
        }
        return json;
    }


    public static JSONObject taggedData(TaggedData taggedData, boolean includeData) {
        JSONObject json = new JSONObject();
        json.put("transaction", Long.toUnsignedString(taggedData.getId()));
        putAccount(json, "account", taggedData.getAccountId());
        json.put("name", taggedData.getName());
        json.put("description", taggedData.getDescription());
        json.put("tags", taggedData.getTags());
        JSONArray tagsJSON = new JSONArray();
        Collections.addAll(tagsJSON, taggedData.getParsedTags());
        json.put("parsedTags", tagsJSON);
        json.put("type", taggedData.getType());
        json.put("channel", taggedData.getChannel());
        json.put("filename", taggedData.getFilename());
        json.put("isText", taggedData.isText());
        if (includeData) {
            json.put("data", taggedData.isText() ? Convert.toString(taggedData.getData()) : Convert.toHexString(taggedData.getData()));
        }
        json.put("transactionTimestamp", taggedData.getTransactionTimestamp());
        json.put("blockTimestamp", taggedData.getBlockTimestamp());
        return json;
    }

    public static JSONObject dataTag(DataTag dataTag) {
        JSONObject json = new JSONObject();
        json.put("tag", dataTag.getTag());
        json.put("count", dataTag.getCount());
        return json;
    }

    public static JSONObject apiRequestHandler(AbstractAPIRequestHandler handler) {
        JSONObject json = new JSONObject();
        json.put("allowRequiredBlockParameters", handler.allowRequiredBlockParameters());
        if (handler.getFileParameter() != null) {
            json.put("fileParameter", handler.getFileParameter());
        }
        json.put("requireBlockchain", handler.requireBlockchain());
        json.put("requirePost", handler.requirePost());
        json.put("requirePassword", handler.requirePassword());
        json.put("requireFullClient", handler.requireFullClient());
        return json;
    }

    public static void putPrunableAttachment(JSONObject json, Transaction transaction) {
        JSONObject prunableAttachment = transactionJsonSerializer.getPrunableAttachmentJSON(transaction);
        if (prunableAttachment != null) {
            json.put("prunableAttachmentJSON", prunableAttachment);
        }
    }

    public static void putException(JSONObject json, Exception e) {
        putException(json, e, "");
    }

    public static void putException(JSONObject json, Exception e, String error) {
        json.put("errorCode", 4);
        if (error.length() > 0) {
            error += ": ";
        }
        json.put("error", e.toString());
        json.put("errorDescription", error + e.getMessage());
    }

    /**
     * Use {@link com.apollocurrency.aplwallet.apl.core.rest.converter.AccountConverter#apply(Account)}
     */
    @Deprecated
    static void putAccount(JSONObject json, String name, long accountId, boolean isPrivate) {
        if (isPrivate) {
            Random random = new Random();
            accountId = random.nextLong();

            if (name.equals("sender"))
            {
                byte[] b = new byte[32];
                random.nextBytes(b);
                json.put(name +"PublicKey", Convert.toHexString(b));
            }

        }
        json.put(name, Long.toUnsignedString(accountId));
        json.put(name + "RS", Convert2.rsAccount(accountId));
    }

    /**
     * Use {@link com.apollocurrency.aplwallet.apl.core.rest.converter.AccountConverter#convert(Object)}
     */
    @Deprecated
    public static void putAccount(JSONObject json, String name, long accountId) {
        putAccount(json, name, accountId, false);
    }

    static void putPrivateAccount(JSONObject json, String name, long accountId) {
        putAccount(json, name, accountId, true);
    }

    /**
     * Use {@link com.apollocurrency.aplwallet.apl.core.rest.converter.AccountCurrencyConverter#addCurrency(AccountCurrencyDTO, Currency)}
     */
    @Deprecated
    private static void putCurrencyInfo(JSONObject json, long currencyId) {
        Currency currency = currencyService.getCurrency(currencyId);
        if (currency == null) {
            return;
        }
        json.put("name", currency.getName());
        json.put("code", currency.getCode());
        json.put("type", currency.getType());
        json.put("decimals", currency.getDecimals());
        json.put("issuanceHeight", currency.getIssuanceHeight());
        putAccount(json, "issuerAccount", currency.getAccountId());
    }

    /**
     * Use {@link com.apollocurrency.aplwallet.apl.core.rest.converter.AccountAssetConverter#apply(AccountAsset)}
     */
    @Deprecated
    private static void putAssetInfo(JSONObject json, long assetId) {
        Asset asset = assetService.getAsset(assetId);
        if (asset != null) {
            json.put("name", asset.getName());
            json.put("decimals", asset.getDecimals());
        } else {
            LOG.error("Can not get Asset with id: {}", assetId);
        }
    }

    private static void putExpectedTransaction(JSONObject json, Transaction transaction) {
        json.put("height", blockchain.getHeight() + 1);
        json.put("phased", transaction.getPhasing() != null);
        if (transaction.getBlockId() != 0) { // those values may be wrong for unconfirmed transactions
            json.put("transactionHeight", transaction.getHeight());
            json.put("confirmations", blockchain.getHeight() - transaction.getHeight());
        }
    }

    public static void ledgerEntry(JSONObject json, LedgerEntry entry, boolean includeTransactions, boolean includeHoldingInfo) {
        putAccount(json, "account", entry.getAccountId());
        json.put("ledgerId", Long.toUnsignedString(entry.getLedgerId()));
        json.put("block", Long.toUnsignedString(entry.getBlockId()));
        json.put("height", entry.getHeight());
        json.put("timestamp", entry.getTimestamp());
        json.put("eventType", entry.getEvent().name());
        json.put("event", Long.toUnsignedString(entry.getEventId()));
        json.put("isTransactionEvent", entry.getEvent().isTransaction());
        json.put("change", String.valueOf(entry.getChange()));
        json.put("balance", String.valueOf(entry.getBalance()));
        LedgerHolding ledgerHolding = entry.getHolding();
        if (ledgerHolding != null) {
            json.put("holdingType", ledgerHolding.name());
            if (entry.getHoldingId() != null) {
                json.put("holding", Long.toUnsignedString(entry.getHoldingId()));
            }
            if (includeHoldingInfo) {
                JSONObject holdingJson = null;
                if (ledgerHolding == LedgerHolding.ASSET_BALANCE
                    || ledgerHolding == LedgerHolding.UNCONFIRMED_ASSET_BALANCE) {
                    holdingJson = new JSONObject();
                    putAssetInfo(holdingJson, entry.getHoldingId());
                } else if (ledgerHolding == LedgerHolding.CURRENCY_BALANCE
                    || ledgerHolding == LedgerHolding.UNCONFIRMED_CURRENCY_BALANCE) {
                    holdingJson = new JSONObject();
                    putCurrencyInfo(holdingJson, entry.getHoldingId());
                }
                if (holdingJson != null) {
                    json.put("holdingInfo", holdingJson);
                }
            }
        }
        if (includeTransactions && entry.getEvent().isTransaction()) {
            Transaction transaction = blockchain.getTransaction(entry.getEventId());
            json.put("transaction", JSONData.transaction(false, transaction));
        }
    }

    public static JSONObject encryptedTransaction(Transaction transaction, byte[] sharedKey) {
        JSONObject encryptedTransaction = new JSONObject();
        JSONObject transactionJson = JSONData.transaction(false, transaction);

        byte[] encrypted = prepareToAesDecryption(Crypto.aesEncrypt(transactionJson.toJSONString().getBytes(), sharedKey));

        encryptedTransaction.put("encryptedTransaction", Convert.toHexString(encrypted));
        return encryptedTransaction;
    }


    public static JSONObject encryptedLedgerEntry(JSONObject ledgerEntryJson, byte[] sharedKey) {
        JSONObject encryptedLedgerEntry = new JSONObject();
        byte[] encrypted = prepareToAesDecryption(Crypto.aesEncrypt(ledgerEntryJson.toJSONString().getBytes(), sharedKey));
        encryptedLedgerEntry.put("encryptedLedgerEntry", Convert.toHexString(encrypted));
        return encryptedLedgerEntry;
    }

    private static byte[] prepareToAesDecryption(byte[] encryptedData) {
        if (encryptedData.length % 16 != 0) {
            return Arrays.copyOf(encryptedData, encryptedData.length + (16 - encryptedData.length % 16));
        }
        return encryptedData;
    }

    public static JSONObject encryptedUnconfirmedTransaction(Transaction transaction, byte[] sharedKey) {
        JSONObject encryptedUnconfirmedTransaction = new JSONObject();
        byte[] encrypted = prepareToAesDecryption(Crypto.aesEncrypt(unconfirmedTransaction(transaction).toJSONString().getBytes(), sharedKey));
        encryptedUnconfirmedTransaction.put("encryptedUnconfirmedTransaction", Convert.toHexString(encrypted));
        return encryptedUnconfirmedTransaction;
    }

    public interface VoteWeighter {
        long calcWeight(long voterId);
    }

}
