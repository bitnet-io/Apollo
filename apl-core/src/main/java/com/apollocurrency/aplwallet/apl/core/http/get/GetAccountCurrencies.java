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

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Deprecated
public final class GetAccountCurrencies extends AbstractAPIRequestHandler {

//    private static class GetAccountCurrenciesHolder {
//        private static final GetAccountCurrencies INSTANCE = new GetAccountCurrencies();
//    }
//
//    public static GetAccountCurrencies getInstance() {
//        return GetAccountCurrenciesHolder.INSTANCE;
//    }

    public GetAccountCurrencies() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.MS}, "account", "currency", "height", "includeCurrencyInfo");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long accountId = HttpParameterParserUtil.getAccountId(req, true);
        int height = HttpParameterParserUtil.getHeight(req);
        long currencyId = HttpParameterParserUtil.getUnsignedLong(req, "currency", false);
        boolean includeCurrencyInfo = "true".equalsIgnoreCase(req.getParameter("includeCurrencyInfo"));

        if (currencyId == 0) {
            JSONObject response = new JSONObject();
            List<AccountCurrency> accountCurrencies = lookupAccountCurrencyService().getByAccount(accountId, height, 0, -1);
            JSONArray currencyJSON = new JSONArray();
            accountCurrencies.forEach(accountCurrency -> currencyJSON.add(JSONData.accountCurrency(accountCurrency, false, includeCurrencyInfo)));
            response.put("accountCurrencies", currencyJSON);
            return response;
        } else {
            AccountCurrency accountCurrency = lookupAccountCurrencyService().getAccountCurrency(accountId, currencyId, height);
            if (accountCurrency != null) {
                return JSONData.accountCurrency(accountCurrency, false, includeCurrencyInfo);
            }
            return JSON.emptyJSON;
        }
    }

}
