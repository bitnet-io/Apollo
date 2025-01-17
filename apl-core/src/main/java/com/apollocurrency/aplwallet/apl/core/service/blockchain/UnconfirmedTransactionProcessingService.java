/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.util.Constants;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Slf4j
public class UnconfirmedTransactionProcessingService {
    private final TimeService timeService;
    private final Blockchain blockchain;
    private final BlockchainConfig blockchainConfig;
    private final MemPool memPool;
    private final TransactionValidator validator;
    private final AccountService accountService;

    @Inject
    public UnconfirmedTransactionProcessingService(TimeService timeService, Blockchain blockchain, BlockchainConfig blockchainConfig, MemPool memPool, TransactionValidator validator, AccountService accountService) {
        this.timeService = timeService;
        this.blockchain = blockchain;
        this.blockchainConfig = blockchainConfig;
        this.memPool = memPool;
        this.validator = validator;
        this.accountService = accountService;
    }


    public UnconfirmedTxValidationResult validateBeforeProcessing(Transaction transaction) {
        int curTime = timeService.getEpochTime();
        if (transaction.getTimestamp() > curTime + Constants.MAX_TIMEDRIFT || transaction.getExpiration() < curTime) {
            return new UnconfirmedTxValidationResult(100_100, UnconfirmedTxValidationResult.Error.NOT_CURRENTLY_VALID, "Invalid transaction timestamp=" + transaction.getTimestamp() + ", current time=" + curTime);
        }
        if (transaction.getVersion() < 1) {
            return new UnconfirmedTxValidationResult(100_105, UnconfirmedTxValidationResult.Error.NOT_VALID, "Invalid transaction version=" + transaction.getVersion());
        }

        if (transaction.getId() == 0L) {
            return new UnconfirmedTxValidationResult(100_110, UnconfirmedTxValidationResult.Error.NOT_VALID, "Invalid transaction id 0");
        }
        if (blockchain.getHeight() < blockchainConfig.getLastKnownBlock()) {
            return new UnconfirmedTxValidationResult(100_115, UnconfirmedTxValidationResult.Error.NOT_CURRENTLY_VALID, "Blockchain not ready to accept transactions");
        }
        if (memPool.hasSaved(transaction.getId()) || blockchain.hasTransaction(transaction.getId())) {
            return new UnconfirmedTxValidationResult(100_120, UnconfirmedTxValidationResult.Error.ALREADY_PROCESSED, "Transaction already processed");
        }
        if (transaction.getReferencedTransactionFullHash() != null && !memPool.canAcceptReferenced()) {
            return new UnconfirmedTxValidationResult(100_122, UnconfirmedTxValidationResult.Error.NOT_CURRENTLY_VALID, "Unable to accept new referenced transactions");
        }
        if (memPool.isRemoved(transaction)) {
            return new UnconfirmedTxValidationResult(100_124, UnconfirmedTxValidationResult.Error.NOT_CURRENTLY_VALID, "Transaction was recently processed");
        }

        if (!validator.verifySignature(transaction)) {
            if (accountService.getAccount(transaction.getSenderId()) != null) {
                return new UnconfirmedTxValidationResult(100_125, UnconfirmedTxValidationResult.Error.NOT_VALID, "Transaction signature verification failed");
            } else {
                return new UnconfirmedTxValidationResult(100_130, UnconfirmedTxValidationResult.Error.NOT_CURRENTLY_VALID, "Unknown transaction sender");
            }
        }
        return new UnconfirmedTxValidationResult(0, null, "");
    }
}
