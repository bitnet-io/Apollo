/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.exception.AplCoreContractViolationException;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.NotValidException;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@EqualsAndHashCode(callSuper = true)
@Getter
public class SmcCallMethodAttachment extends AbstractSmcAttachment {
    private static final String METHOD_NAME_FIELD = "contractMethod";
    private static final String METHOD_PARAMS_FIELD = "params";

    private final String methodName;// method or constructor name
    private final String methodParams; //coma separated values

    @Builder
    public SmcCallMethodAttachment(String methodName, String methodParams, BigInteger fuelLimit, BigInteger fuelPrice) {
        super(fuelLimit, fuelPrice);
        this.methodName = Objects.requireNonNull(methodName);
        this.methodParams = methodParams;
    }

    public SmcCallMethodAttachment(ByteBuffer buffer) throws AplCoreContractViolationException {
        super(buffer);
        try {
            this.methodName = Convert.readString(buffer);
            this.methodParams = Convert.readString(buffer);
        } catch (NotValidException ex) {
            throw new AplCoreContractViolationException(ex.getMessage());
        }
    }

    public SmcCallMethodAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.methodName = String.valueOf(attachmentData.get(METHOD_NAME_FIELD));
        this.methodParams = String.valueOf(attachmentData.get(METHOD_PARAMS_FIELD));
    }

    @Override
    public int getPayableSize() {
        int size = this.getMethodName().length();
        if (this.getMethodParams() != null) {
            size += this.getMethodParams().length();
        }
        return size;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.SMC_CALL_METHOD;
    }

    @Override
    public void putMyJSON(JSONObject json) {
        super.putMyJSON(json);
        json.put(METHOD_NAME_FIELD, this.methodName);
        json.put(METHOD_PARAMS_FIELD, this.methodParams);
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        super.putMyBytes(buffer);
        Convert.writeString(buffer, methodName);
        Convert.writeString(buffer, methodParams);

    }

    @Override
    public int getMySize() {
        return Long.BYTES*2 + Integer.BYTES*2
            + methodName.getBytes(StandardCharsets.UTF_8).length
            + (methodParams!=null?methodParams.getBytes(StandardCharsets.UTF_8).length:0);
    }

}
