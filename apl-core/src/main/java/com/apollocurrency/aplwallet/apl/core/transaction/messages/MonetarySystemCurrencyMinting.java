/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
public final class MonetarySystemCurrencyMinting extends AbstractAttachment implements MonetarySystemAttachment {

    final long nonce;
    final long currencyId;
    final long units;
    final long counter;

    public MonetarySystemCurrencyMinting(ByteBuffer buffer) {
        super(buffer);
        this.nonce = buffer.getLong();
        this.currencyId = buffer.getLong();
        this.units = buffer.getLong();
        this.counter = buffer.getLong();
    }

    public MonetarySystemCurrencyMinting(JSONObject attachmentData) {
        super(attachmentData);
        this.nonce = Convert.parseLong(attachmentData.get("nonce"));
        this.currencyId = Convert.parseUnsignedLong((String) attachmentData.get("currency"));
        this.units = Convert.parseLong(attachmentData.get("units"));
        this.counter = Convert.parseLong(attachmentData.get("counter"));
    }

    public MonetarySystemCurrencyMinting(long nonce, long currencyId, long units, long counter) {
        this.nonce = nonce;
        this.currencyId = currencyId;
        this.units = units;
        this.counter = counter;
    }

    @Override
    public int getMySize() {
        return 8 + 8 + 8 + 8;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(nonce);
        buffer.putLong(currencyId);
        buffer.putLong(units);
        buffer.putLong(counter);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("nonce", nonce);
        attachment.put("currency", Long.toUnsignedString(currencyId));
        attachment.put("units", units);
        attachment.put("counter", counter);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.MS_CURRENCY_MINTING;
    }

    public long getNonce() {
        return nonce;
    }

    @Override
    public long getCurrencyId() {
        return currencyId;
    }

    public long getUnits() {
        return units;
    }

    public long getCounter() {
        return counter;
    }

}
