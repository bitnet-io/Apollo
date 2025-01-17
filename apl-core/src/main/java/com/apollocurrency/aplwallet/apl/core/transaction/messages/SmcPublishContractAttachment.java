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
public class SmcPublishContractAttachment extends AbstractSmcAttachment {
    private static final String CONTRACT_NAME_FIELD = "name";
    private static final String BASE_CONTRACT_FIELD = "baseContract";
    private static final String CONTRACT_SOURCE_FIELD = "source";
    private static final String CONSTRUCTOR_PARAMS_FIELD = "params";
    private static final String LANGUAGE_FIELD = "language";
    private static final String LANGUAGE_VERSION_FIELD = "languageVersion";

    private final String contractName;//contract name is a constructor name
    private final String baseContract;
    private final String contractSource;
    private final String constructorParams;//coma separated string of values
    private final String languageName;
    private final String languageVersion;

    @Builder
    public SmcPublishContractAttachment(String contractName, String baseContract,
                                        String contractSource, String constructorParams,
                                        String languageName, String languageVersion,
                                        BigInteger fuelLimit, BigInteger fuelPrice) {
        super(fuelLimit, fuelPrice);
        this.contractName = Objects.requireNonNull(contractName, "contractName");
        this.baseContract = Objects.requireNonNull(baseContract, "baseContract");
        this.contractSource = Objects.requireNonNull(contractSource, "constructorSource");
        this.constructorParams = constructorParams;
        this.languageName = Objects.requireNonNull(languageName, "languageName");
        this.languageVersion = Objects.requireNonNull(languageVersion, "languageVersion");
    }

    public SmcPublishContractAttachment(ByteBuffer buffer) throws AplCoreContractViolationException {
        super(buffer);
        try {
            this.contractName = Convert.readString(buffer);
            this.baseContract = Convert.readString(buffer);
            this.contractSource = Convert.readString(buffer);
            this.constructorParams = Convert.readString(buffer);
            this.languageName = Convert.readString(buffer);
            this.languageVersion = Convert.readString(buffer);
        } catch (NotValidException ex) {
            throw new AplCoreContractViolationException(ex.getMessage());
        }
    }

    public SmcPublishContractAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.contractName = String.valueOf(attachmentData.get(CONTRACT_NAME_FIELD));
        this.baseContract = String.valueOf(attachmentData.get(BASE_CONTRACT_FIELD));
        this.contractSource = String.valueOf(attachmentData.get(CONTRACT_SOURCE_FIELD));
        this.constructorParams = String.valueOf(attachmentData.get(CONSTRUCTOR_PARAMS_FIELD));
        this.languageName = String.valueOf(attachmentData.get(LANGUAGE_FIELD));
        this.languageVersion = String.valueOf(attachmentData.get(LANGUAGE_VERSION_FIELD));
    }

    @Override
    public int getPayableSize() {
        int size = this.getContractSource().length();
        if (this.getConstructorParams() != null) {
            size += this.getConstructorParams().length();
        }
        return size;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.SMC_PUBLISH;
    }

    @Override
    public void putMyJSON(JSONObject json) {
        super.putMyJSON(json);
        json.put(CONTRACT_NAME_FIELD, this.contractName);
        json.put(BASE_CONTRACT_FIELD, this.baseContract);
        json.put(CONTRACT_SOURCE_FIELD, this.contractSource);
        json.put(CONSTRUCTOR_PARAMS_FIELD, this.constructorParams);
        json.put(LANGUAGE_FIELD, this.languageName);
        json.put(LANGUAGE_VERSION_FIELD, this.languageVersion);
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        super.putMyBytes(buffer);
        Convert.writeString(buffer, contractName);
        Convert.writeString(buffer, baseContract);
        Convert.writeString(buffer, contractSource);
        Convert.writeString(buffer, constructorParams);
        Convert.writeString(buffer, languageName);
        Convert.writeString(buffer, languageVersion);
    }

    @Override
    public int getMySize() {
        return Long.BYTES * 2 + Integer.BYTES * 6
            + contractName.getBytes(StandardCharsets.UTF_8).length
            + baseContract.getBytes(StandardCharsets.UTF_8).length
            + contractSource.getBytes(StandardCharsets.UTF_8).length
            + (constructorParams != null ? constructorParams.getBytes(StandardCharsets.UTF_8).length : 0)
            + languageName.getBytes(StandardCharsets.UTF_8).length
            + languageVersion.getBytes(StandardCharsets.UTF_8).length;
    }

}
