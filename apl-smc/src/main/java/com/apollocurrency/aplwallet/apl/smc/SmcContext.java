/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc;

import com.apollocurrency.smc.blockchain.BlockchainIntegrator;
import com.apollocurrency.smc.polyglot.engine.ExecutionEnv;
import com.apollocurrency.smc.polyglot.language.LanguageContext;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface SmcContext {
    BlockchainIntegrator getIntegrator();

    ExecutionEnv getExecutionEnv();

    LanguageContext getLanguageContext();
}
