/*
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.EqualsAndHashCode;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;

@EqualsAndHashCode(callSuper = true)
public class MessageAppendix extends AbstractAppendix {

    static final String appendixName = "Message";
    private final byte[] message;
    private final boolean isText;

    public MessageAppendix(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        int messageLength = buffer.getInt();
        this.isText = messageLength < 0; // ugly hack
        if (messageLength < 0) {
            messageLength &= Integer.MAX_VALUE;
        }
        if (messageLength > 1000) {
            throw new AplException.NotValidException("Invalid arbitrary message length: " + messageLength);
        }
        this.message = new byte[messageLength];
        buffer.get(this.message);
        if (isText && !Arrays.equals(message, Convert.toBytes(Convert.toString(message)))) {
            throw new AplException.NotValidException("Message is not UTF-8 text");
        }
    }

    public MessageAppendix(JSONObject attachmentData) {
        super(attachmentData);
        String messageString = (String) attachmentData.get("message");
        this.isText = Boolean.TRUE.equals(attachmentData.get("messageIsText"));
        this.message = isText ? Convert.toBytes(messageString) : Convert.parseHexString(messageString);
    }

    public MessageAppendix(byte[] message) {
        this(message, false);
    }

    public MessageAppendix(String string) {
        this(Convert.toBytes(string), true);
    }

    public MessageAppendix(String string, boolean isText) {
        this(isText ? Convert.toBytes(string) : Convert.parseHexString(string), isText);
    }

    public MessageAppendix(byte[] message, boolean isText) {
        this.message = message;
        this.isText = isText;
    }

    public static MessageAppendix parse(JSONObject attachmentData) {
        if (!Appendix.hasAppendix(appendixName, attachmentData)) {
            return null;
        }
        return new MessageAppendix(attachmentData);
    }

    @Override
    public String getAppendixName() {
        return appendixName;
    }

    @Override
    public int getMySize() {
        return 4 + message.length;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putInt(isText ? (message.length | Integer.MIN_VALUE) : message.length);
        buffer.put(message);
    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put("message", Convert.toString(message, isText));
        json.put("messageIsText", isText);
    }


    @Override
    public Fee getBaselineFee(Transaction transaction, long oneAPL) {
        return new Fee.SizeBasedFee(0, oneAPL, 32) {
            @Override
            public int getSize(Transaction transaction, Appendix appendage) {
                return ((MessageAppendix) appendage).getMessage().length;
            }
        };
    }

    @Override
    public void performStateDependentValidation(Transaction transaction, int blockHeight) throws AplException.ValidationException {
        throw new UnsupportedOperationException("Validation for message appendix is not supported, use separate class");
    }

    @Override
    public void performStateIndependentValidation(Transaction transaction, int blockHeight) throws AplException.ValidationException {
        throw new UnsupportedOperationException("Validation for message appendix is not supported, use separate class");
    }

    @Override
    public void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
    }

    @Override
    public int getAppendixFlag() {
        return 0x01;
    }

    public byte[] getMessage() {
        return message;
    }

    public boolean isText() {
        return isText;
    }

    @Override
    public boolean isPhasable() {
        return false;
    }

    @Override
    public String toString() {
        return "MessageAppendix{" +
            "message=" + new String(message) +
            ", isText=" + isText +
            '}';
    }
}