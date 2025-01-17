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

package com.apollocurrency.aplwallet.apl.core.http;

import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONStreamAware;

@Deprecated
public final class ParameterException extends AplException {

    private final JSONStreamAware errorResponse;

    public ParameterException(JSONStreamAware errorResponse) {
        this.errorResponse = errorResponse;
    }

    public ParameterException(String message, Throwable cause, JSONStreamAware errorResponse) {
        super(message, cause);
        this.errorResponse = errorResponse;
    }

    public JSONStreamAware getErrorResponse() {
        return errorResponse;
    }
}
