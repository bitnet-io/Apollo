/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.model.HoldingType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.EqualsAndHashCode;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
@EqualsAndHashCode(callSuper = true)
public final class ShufflingCreationAttachment extends AbstractAttachment {

    final long holdingId;
    final HoldingType holdingType;
    final long amount;
    final byte participantCount;
    final short registrationPeriod;

    public ShufflingCreationAttachment(ByteBuffer buffer) {
        super(buffer);
        this.holdingId = buffer.getLong();
        this.holdingType = HoldingType.get(buffer.get());
        this.amount = buffer.getLong();
        this.participantCount = buffer.get();
        this.registrationPeriod = buffer.getShort();
    }

    public ShufflingCreationAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.holdingId = Convert.parseUnsignedLong((String) attachmentData.get("holding"));
        this.holdingType = HoldingType.get(((Number) attachmentData.get("holdingType")).byteValue());
        this.amount = Convert.parseLong(attachmentData.get("amount"));
        this.participantCount = ((Number) attachmentData.get("participantCount")).byteValue();
        this.registrationPeriod = ((Number) attachmentData.get("registrationPeriod")).shortValue();
    }

    public ShufflingCreationAttachment(long holdingId, HoldingType holdingType, long amount, byte participantCount, short registrationPeriod) {
        this.holdingId = holdingId;
        this.holdingType = holdingType;
        this.amount = amount;
        this.participantCount = participantCount;
        this.registrationPeriod = registrationPeriod;
    }

    @Override
    public int getMySize() {
        return 8 + 1 + 8 + 1 + 2;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(holdingId);
        buffer.put(holdingType.getCode());
        buffer.putLong(amount);
        buffer.put(participantCount);
        buffer.putShort(registrationPeriod);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("holding", Long.toUnsignedString(holdingId));
        attachment.put("holdingType", holdingType.getCode());
        attachment.put("amount", amount);
        attachment.put("participantCount", participantCount);
        attachment.put("registrationPeriod", registrationPeriod);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.SHUFFLING_CREATION;
    }

    public long getHoldingId() {
        return holdingId;
    }

    public HoldingType getHoldingType() {
        return holdingType;
    }

    public long getAmount() {
        return amount;
    }

    public byte getParticipantCount() {
        return participantCount;
    }

    public short getRegistrationPeriod() {
        return registrationPeriod;
    }

}
