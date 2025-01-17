/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionUtils;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UnconfirmedTransactionCreator {
    private final TimeService timeService;
    private final TxBContext txBContext;

    @Inject
    public UnconfirmedTransactionCreator(BlockchainConfig blockchainConfig, TimeService timeService) {
        this.timeService = timeService;
        this.txBContext = TxBContext.newInstance(blockchainConfig.getChain());
    }

    public UnconfirmedTransaction from(Transaction transaction) {
        return from(transaction, timeService.getEpochTime());
    }

    public UnconfirmedTransaction from(Transaction transaction, long arrivalTimestamp) {
        Result byteArrayTx = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion()).serialize(transaction, byteArrayTx);
        int fullSize = TransactionUtils.calculateFullSize(transaction, byteArrayTx.size());

        return new UnconfirmedTransaction(transaction, arrivalTimestamp, transaction.getFeeATM() / fullSize, fullSize);
    }
}
