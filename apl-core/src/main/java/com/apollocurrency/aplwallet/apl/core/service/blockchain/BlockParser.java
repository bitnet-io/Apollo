/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.BlockImpl;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

public interface BlockParser {

    BlockImpl parseBlock(JSONObject blockData, long baseTarget) throws AplException.NotValidException, AplException.NotCurrentlyValidException;

}
