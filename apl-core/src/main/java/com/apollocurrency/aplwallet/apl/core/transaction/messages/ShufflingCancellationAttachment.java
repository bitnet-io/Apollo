/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.ToString;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.List;

/**
 * @author al
 */
@ToString(callSuper = true)
public final class ShufflingCancellationAttachment extends AbstractShufflingAttachment {

    final byte[][] blameData;
    final byte[][] keySeeds;
    final long cancellingAccountId;

    public ShufflingCancellationAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        int count = buffer.get();
        if (count > Constants.MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS || count <= 0) {
            throw new AplException.NotValidException("Invalid data count " + count);
        }
        this.blameData = new byte[count][];
        for (int i = 0; i < count; i++) {
            int size = buffer.getInt();
            this.blameData[i] = new byte[size];
            buffer.get(this.blameData[i]);
        }
        count = buffer.get();
        this.keySeeds = new byte[count][];
        for (int i = 0; i < count; i++) {
            this.keySeeds[i] = new byte[32];
            buffer.get(this.keySeeds[i]);
        }
        this.cancellingAccountId = buffer.getLong();
    }

    public ShufflingCancellationAttachment(JSONObject attachmentData) {
        super(attachmentData);
        List<?> jsonArray = (List<?>) attachmentData.get("blameData");
        this.blameData = new byte[jsonArray.size()][];
        for (int i = 0; i < this.blameData.length; i++) {
            this.blameData[i] = Convert.parseHexString((String) jsonArray.get(i));
        }
        jsonArray = (List<?>) attachmentData.get("keySeeds");
        this.keySeeds = new byte[jsonArray.size()][];
        for (int i = 0; i < this.keySeeds.length; i++) {
            this.keySeeds[i] = Convert.parseHexString((String) jsonArray.get(i));
        }
        this.cancellingAccountId = Convert.parseUnsignedLong((String) attachmentData.get("cancellingAccount"));
    }

    public ShufflingCancellationAttachment(long shufflingId, byte[][] blameData, byte[][] keySeeds, byte[] shufflingStateHash, long cancellingAccountId) {
        super(shufflingId, shufflingStateHash);
        this.blameData = blameData;
        this.keySeeds = keySeeds;
        this.cancellingAccountId = cancellingAccountId;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.SHUFFLING_CANCELLATION;
    }

    @Override
    public int getMySize() {
        int size = super.getMySize();
        size += 1;
        for (byte[] bytes : blameData) {
            size += 4;
            size += bytes.length;
        }
        size += 1;
        size += 32 * keySeeds.length;
        size += 8;
        return size;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        super.putMyBytes(buffer);
        buffer.put((byte) blameData.length);
        for (byte[] bytes : blameData) {
            buffer.putInt(bytes.length);
            buffer.put(bytes);
        }
        buffer.put((byte) keySeeds.length);
        for (byte[] bytes : keySeeds) {
            buffer.put(bytes);
        }
        buffer.putLong(cancellingAccountId);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        super.putMyJSON(attachment);
        JSONArray jsonArray = new JSONArray();
        attachment.put("blameData", jsonArray);
        for (byte[] bytes : blameData) {
            jsonArray.add(Convert.toHexString(bytes));
        }
        jsonArray = new JSONArray();
        attachment.put("keySeeds", jsonArray);
        for (byte[] bytes : keySeeds) {
            jsonArray.add(Convert.toHexString(bytes));
        }
        if (cancellingAccountId != 0) {
            attachment.put("cancellingAccount", Long.toUnsignedString(cancellingAccountId));
        }
    }

    public byte[][] getBlameData() {
        return blameData;
    }

    public byte[][] getKeySeeds() {
        return keySeeds;
    }

    public long getCancellingAccountId() {
        return cancellingAccountId;
    }

    public byte[] getHash() {
        MessageDigest digest = Crypto.sha256();
        for (byte[] bytes : blameData) {
            digest.update(bytes);
        }
        return digest.digest();
    }

}
