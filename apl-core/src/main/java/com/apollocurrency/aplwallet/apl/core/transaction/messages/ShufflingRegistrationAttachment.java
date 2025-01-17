/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.SHUFFLING_REGISTRATION;

/**
 * @author al
 */
@EqualsAndHashCode(callSuper = true)
@ToString
public final class ShufflingRegistrationAttachment extends AbstractAttachment implements ShufflingAttachment {

    final byte[] shufflingFullHash;

    public ShufflingRegistrationAttachment(ByteBuffer buffer) {
        super(buffer);
        this.shufflingFullHash = new byte[32];
        buffer.get(this.shufflingFullHash);
    }

    public ShufflingRegistrationAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.shufflingFullHash = Convert.parseHexString((String) attachmentData.get("shufflingFullHash"));
    }

    public ShufflingRegistrationAttachment(byte[] shufflingFullHash) {
        this.shufflingFullHash = shufflingFullHash;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return SHUFFLING_REGISTRATION;
    }

    @Override
    public int getMySize() {
        return 32;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.put(shufflingFullHash);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("shufflingFullHash", Convert.toHexString(shufflingFullHash));
    }

    @Override
    public long getShufflingId() {
        return Convert.transactionFullHashToId(shufflingFullHash);
    }

    @Override
    public byte[] getShufflingStateHash() {
        return shufflingFullHash;
    }

}
