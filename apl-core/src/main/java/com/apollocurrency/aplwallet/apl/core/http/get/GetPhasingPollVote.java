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

import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingVote;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public class GetPhasingPollVote extends AbstractAPIRequestHandler {

    private static PhasingPollService phasingPollService = CDI.current().select(PhasingPollService.class).get();

    public GetPhasingPollVote() {
        super(new APITag[]{APITag.PHASING}, "transaction", "account");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        long transactionId = HttpParameterParserUtil.getUnsignedLong(req, "transaction", true);
        long accountId = HttpParameterParserUtil.getAccountId(req, true);

        PhasingVote phasingVote = phasingPollService.getVote(transactionId, accountId);
        if (phasingVote != null) {
            return JSONData.phasingPollVote(phasingVote);
        }
        return JSON.emptyJSON;
    }
}
