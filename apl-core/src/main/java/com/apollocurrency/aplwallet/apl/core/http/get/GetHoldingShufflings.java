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

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingStage;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.incorrect;

@Vetoed
public final class GetHoldingShufflings extends AbstractAPIRequestHandler {

    public GetHoldingShufflings() {
        super(new APITag[]{APITag.SHUFFLING}, "holding", "stage", "includeFinished", "firstIndex", "lastIndex");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        long holdingId = 0;
        String holdingValue = Convert.emptyToNull(req.getParameter("holding"));
        if (holdingValue != null) {
            try {
                holdingId = Convert.parseUnsignedLong(holdingValue);
            } catch (RuntimeException e) {
                return incorrect("holding");
            }
        }
        String stageValue = Convert.emptyToNull(req.getParameter("stage"));
        ShufflingStage stage = null;
        if (stageValue != null) {
            try {
                stage = ShufflingStage.get(Byte.parseByte(stageValue));
            } catch (RuntimeException e) {
                return incorrect("stage");
            }
        }
        boolean includeFinished = "true".equalsIgnoreCase(req.getParameter("includeFinished"));
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        response.put("shufflings", jsonArray);
        List<Shuffling> shufflings = shufflingService.getHoldingShufflings(holdingId, stage, includeFinished, firstIndex, lastIndex);
        for (Shuffling shuffling : shufflings) {
            jsonArray.add(JSONData.shuffling(shuffling, false));
        }
        return response;
    }

}
