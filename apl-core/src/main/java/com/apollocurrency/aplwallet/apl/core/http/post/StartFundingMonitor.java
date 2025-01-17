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
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.model.HoldingType;
import com.apollocurrency.aplwallet.apl.core.service.appdata.funding.FundingMonitorService;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MONITOR_ALREADY_STARTED;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.UNKNOWN_ACCOUNT;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.incorrect;

/**
 * Start a funding monitor
 * <p>
 * A funding monitor will transfer APL, ASSET or CURRENCY from the funding account
 * to a recipient account when the amount held by the recipient account drops below
 * the threshold.  The transfer will not be done until the current block
 * height is greater than equal to the block height of the last transfer plus the
 * interval. Holding type codes are listed in getConstants. The asset or currency is
 * specified by the holding identifier.
 * <p>
 * The funding account is identified by the secret phrase.  The secret phrase must
 * be specified since the funding monitor needs to sign the transactions that it submits.
 * <p>
 * The recipient accounts are identified by the specified account property.  Each account
 * that has this property set by the funding account will be monitored for changes.
 * The property value can be omitted or it can consist of a JSON string containing one or more
 * values in the format: {"amount":long,"threshold":long,"interval":integer}
 * <p>
 * The long values can be specified as numeric values or as strings.
 * <p>
 * For example, {"amount":25,"threshold":10,"interval":1440}.  The specified values will
 * override the default values specified when the account monitor is started.
 * <p>
 * APL amounts are specified with 8 decimal places.  Asset and Currency decimal places
 * are determined by the asset or currency definition.
 */
@Vetoed
public final class StartFundingMonitor extends AbstractAPIRequestHandler {

    public StartFundingMonitor() {
        super(new APITag[]{APITag.ACCOUNTS}, "holdingType", "holding", "property", "amount", "threshold",
            "interval", "secretPhrase", "account", "passphrase");
    }

    /**
     * Process the request
     *
     * @param req Client request
     * @return Client response
     * @throws AplException Unable to process request
     */
    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        long accountId = HttpParameterParserUtil.getAccountId(req, false);
        HoldingType holdingType = HttpParameterParserUtil.getHoldingType(req);
        long holdingId = HttpParameterParserUtil.getHoldingId(req, holdingType);
        String property = HttpParameterParserUtil.getAccountProperty(req, true);
        long amount = HttpParameterParserUtil.getLong(req, "amount", 0, Long.MAX_VALUE, true);
        if (amount < FundingMonitorService.MIN_FUND_AMOUNT) {
            throw new ParameterException(incorrect("amount", "Minimum funding amount is " + FundingMonitorService.MIN_FUND_AMOUNT));
        }
        long threshold = HttpParameterParserUtil.getLong(req, "threshold", 0, Long.MAX_VALUE, true);
        if (threshold < FundingMonitorService.MIN_FUND_THRESHOLD) {
            throw new ParameterException(incorrect("threshold", "Minimum funding threshold is " + FundingMonitorService.MIN_FUND_THRESHOLD));
        }
        int interval = HttpParameterParserUtil.getInt(req, "interval", FundingMonitorService.MIN_FUND_INTERVAL, Integer.MAX_VALUE, true);
        byte[] keySeed = HttpParameterParserUtil.getKeySeed(req, accountId, true);
//        AssetService assetService = CDI.current().select(AssetService.class).get();
        switch (holdingType) {
            case ASSET:
                Asset asset = assetService.getAsset(holdingId);
                if (asset == null) {
                    throw new ParameterException(JSONResponses.UNKNOWN_ASSET);
                }
                break;
            case CURRENCY:
                Currency currency = lookupCurrencyService().getCurrency(holdingId);
                if (currency == null) {
                    throw new ParameterException(JSONResponses.UNKNOWN_CURRENCY);
                }
                break;
        }
        Account account = lookupAccountService().getAccount(Crypto.getPublicKey(keySeed));
        if (account == null) {
            throw new ParameterException(UNKNOWN_ACCOUNT);
        }
        if (lookupFundingMonitorService().startMonitor(holdingType, holdingId, property, amount, threshold, interval, keySeed)) {
            JSONObject response = new JSONObject();
            response.put("started", true);
            return response;
        } else {
            return MONITOR_ALREADY_STARTED;
        }
    }

    @Override
    protected boolean requirePost() {
        return true;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireFullClient() {
        return true;
    }

}
