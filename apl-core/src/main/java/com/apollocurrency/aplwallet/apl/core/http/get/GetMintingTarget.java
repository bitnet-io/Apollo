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

import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;

/**
 * Currency miners can use this API to obtain their target hash value for minting currency units
 * <p>
 * Parameters
 * <ul>
 * <li>currency - currency id
 * <li>account - miner account id
 * <li>units - number of currency units the miner is trying to mint
 * </ul>
 */
@Vetoed
public final class GetMintingTarget extends AbstractAPIRequestHandler {

    public GetMintingTarget() {
        super(new APITag[]{APITag.MS}, "currency", "account", "units");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        Currency currency = HttpParameterParserUtil.getCurrencyWithSupply(req);
        JSONObject json = new JSONObject();
        json.put("currency", Long.toUnsignedString(currency.getId()));
        long units = HttpParameterParserUtil.getLong(req, "units", 1, currency.getMaxSupply() - currency.getReserveSupply(), true);
        BigInteger numericTarget = lookupMonetaryCurrencyMintingService().getNumericTarget(currency, units);
        json.put("difficulty", String.valueOf(BigInteger.ZERO.equals(numericTarget) ? -1 : BigInteger.valueOf(2).pow(256).subtract(BigInteger.ONE).divide(numericTarget)));
        json.put("targetBytes", Convert.toHexString(lookupMonetaryCurrencyMintingService().getTarget(numericTarget)));
        json.put("counter", lookupCurrencyService().getMintCounter(
            currency.getId(), HttpParameterParserUtil.getAccountId(req, true)));
        return json;
    }

}
