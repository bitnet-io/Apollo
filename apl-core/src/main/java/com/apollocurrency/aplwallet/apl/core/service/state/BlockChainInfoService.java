/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state;

import com.apollocurrency.aplwallet.apl.core.model.Block;

import java.util.List;

/**
 * @author silaev-firstbridge on 1/31/2020
 */
public interface BlockChainInfoService {
    boolean doesNotExceed(int height);

    void checkAvailable(int height, boolean isMultiVersion);

    void checkAvailable(int height);

    int getMinRollbackHeight();

    int getHeight();

    Block getLastBlock();

    List<Block> getBlocksByAccount(long accountId, int from, int to, int timestamp);

    int getLastBlockTimestamp();

    boolean isTrimming();
}
