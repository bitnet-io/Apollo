/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPool;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.UnconfirmedTransactionCreator;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.spi.CDI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Class makes lookup of BlockchainProcessor
 */
@Slf4j
public class RebroadcastTransactionsThread implements Runnable {

    private BlockchainProcessor blockchainProcessor;
    private final TimeService timeService;
    private final MemPool memPool;
    private final PeersService peers;
    private final Blockchain blockchain;
    private final UnconfirmedTransactionCreator unconfirmedTransactionCreator;

    public RebroadcastTransactionsThread(TimeService timeService,
                                         MemPool memPool,
                                         PeersService peers,
                                         Blockchain blockchain,
                                         UnconfirmedTransactionCreator unconfirmedTransactionCreator) {
        this.timeService = Objects.requireNonNull(timeService);
        this.memPool = Objects.requireNonNull(memPool);
        this.peers = Objects.requireNonNull(peers);
        this.blockchain = Objects.requireNonNull(blockchain);
        this.unconfirmedTransactionCreator = Objects.requireNonNull(unconfirmedTransactionCreator);
        log.info("Created 'RebroadcastTransactionsThread' instance");
    }

    @Override
    public void run() {
        try {
            if (lookupBlockchainProcessor().isDownloading()) {
                return;
            }

            rebroadcast();

        } catch (Throwable t) {
            log.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
            t.printStackTrace();
            System.exit(1);
        }
    }

    private void rebroadcast() {
        try {
            List<Transaction> transactionList = new ArrayList<>();
            int curTime = timeService.getEpochTime();
            Collection<Transaction> broadcastedTransactions = memPool.getAllBroadcasted();
            for (Transaction transaction : broadcastedTransactions) {
                if (transaction.getExpiration() < curTime || blockchain.hasTransaction(transaction.getId())) {
                    memPool.removeBroadcasted(transaction);
                } else if (transaction.getTimestamp() < curTime - 30) {
                    transactionList.add(
                        unconfirmedTransactionCreator.from(transaction, Convert2.fromEpochTime(transaction.getTimestamp()))
                    );
                }
            }

            if (!transactionList.isEmpty()) {
                peers.sendToSomePeers(transactionList);
            }

        } catch (Exception e) {
            log.info("Error in transaction re-broadcasting thread", e);
        }
    }

    private BlockchainProcessor lookupBlockchainProcessor() {
        if (blockchainProcessor == null) {
            blockchainProcessor = CDI.current().select(BlockchainProcessor.class).get();
        }
        return blockchainProcessor;
    }
}
