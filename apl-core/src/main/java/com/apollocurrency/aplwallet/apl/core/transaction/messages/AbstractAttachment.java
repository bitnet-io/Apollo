/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.util.annotation.FeeMarker;
import com.apollocurrency.aplwallet.apl.util.annotation.TransactionFee;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractAttachment extends AbstractAppendix implements Attachment {
    private TransactionType transactionType;

    public AbstractAttachment(ByteBuffer buffer) {
        super(buffer);
    }

    public AbstractAttachment(JSONObject attachmentData) {
        super(attachmentData);
    }

    public AbstractAttachment(int version) {
        super(version);
    }

    public AbstractAttachment() {
    }

    @Override
    public void bindTransactionType(@NonNull TransactionType transactionType) {
        if (transactionType.getSpec() != getTransactionTypeSpec()) {
            throw new IllegalArgumentException("Required tx type " + getTransactionTypeSpec() + " but got " + transactionType.getSpec());
        }
        this.transactionType = transactionType;
    }

    @Override
    public String getAppendixName() {
        return getTransactionTypeSpec().getCompatibleName();
    }

    @Override
    public void performStateDependentValidation(Transaction transaction, int blockHeight) throws AplException.ValidationException {
        transactionType().validateStateDependent(transaction);
    }

    @Override
    public void performStateIndependentValidation(Transaction transaction, int blockHeight) throws AplException.ValidationException {
        transactionType().validateStateIndependent(transaction);
    }

    @Override
    public void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
        transactionType().apply(transaction, senderAccount, recipientAccount);
    }

    @TransactionFee(FeeMarker.BASE_FEE)
    @Override
    public final Fee getBaselineFee(Transaction transaction, long oneAPL) {
        return transactionType().getBaselineFee(transaction);
    }

    @Override
    public void validateAtFinish(Transaction transaction, int blockHeight) throws AplException.ValidationException {
        if (!isPhased(transaction)) {
            return;
        }
        transactionType().validateStateIndependent(transaction);
        transactionType().validateStateDependentAtFinish(transaction);
    }

    @Override
    public boolean isPhasable() {
        return !(this instanceof Prunable) && transactionType().isPhasable();
    }

    @Override
    public String toString() {
        return "Attachment[" + getClass().getSimpleName() + ", type = " + getTransactionTypeSpec()  + "]";
    }

    @Override
    public void undo(Transaction transaction, Account senderAccount, Account recipientAccount) {
        transactionType().undoApply(transaction, senderAccount, recipientAccount);
        transactionType().undoUnconfirmed(transaction, senderAccount);
    }

    private TransactionType transactionType() {
        if (this.transactionType == null) {
            throw new IllegalStateException("Transaction type was not set");
        }
        return this.transactionType;
    }
}
