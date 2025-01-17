/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.EqualsAndHashCode;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
@EqualsAndHashCode(callSuper = true)
public final class MonetarySystemReserveIncreaseAttachment extends AbstractAttachment implements MonetarySystemAttachment {

    final long currencyId;
    final long amountPerUnitATM;

    public MonetarySystemReserveIncreaseAttachment(ByteBuffer buffer) {
        super(buffer);
        this.currencyId = buffer.getLong();
        this.amountPerUnitATM = buffer.getLong();
    }

    public MonetarySystemReserveIncreaseAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.currencyId = Convert.parseUnsignedLong((String) attachmentData.get("currency"));
        this.amountPerUnitATM = Convert.parseLong(attachmentData.get("amountPerUnitATM"));
    }

    public MonetarySystemReserveIncreaseAttachment(long currencyId, long amountPerUnitATM) {
        this.currencyId = currencyId;
        this.amountPerUnitATM = amountPerUnitATM;
    }

    @Override
    public int getMySize() {
        return 8 + 8;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(currencyId);
        buffer.putLong(amountPerUnitATM);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("currency", Long.toUnsignedString(currencyId));
        attachment.put("amountPerUnitATM", amountPerUnitATM);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.MS_RESERVE_INCREASE;
    }

    @Override
    public long getCurrencyId() {
        return currencyId;
    }

    public long getAmountPerUnitATM() {
        return amountPerUnitATM;
    }

}
