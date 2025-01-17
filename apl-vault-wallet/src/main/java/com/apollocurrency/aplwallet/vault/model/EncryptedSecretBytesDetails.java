/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.vault.model;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Convert2;

import java.util.Arrays;
import java.util.Objects;

/**
 * Use FbWallet
 */
@Deprecated
public class EncryptedSecretBytesDetails {
    private byte[] encryptedSecretBytes;
    private String accountRS;
    private long account;
    private Integer version;
    private byte[] nonce;
    private long timestamp;

    public EncryptedSecretBytesDetails() {
    }

    public EncryptedSecretBytesDetails(byte[] encryptedSecretBytes, String accountRS, long account, Integer version, byte[] nonce, long timestamp) {
        this.encryptedSecretBytes = encryptedSecretBytes;
        this.accountRS = accountRS;
        this.account = account;
        this.version = version;
        this.nonce = nonce;
        this.timestamp = timestamp;
    }

    public EncryptedSecretBytesDetails(byte[] encryptedSecretBytes, long account, Integer version, byte[] nonce, long timestamp) {
        this(encryptedSecretBytes, Convert2.defaultRsAccount(account), version, nonce, timestamp);
    }

    public EncryptedSecretBytesDetails(byte[] encryptedSecretBytes, String accountRS, Integer version, byte[] nonce, long timestamp) {
        this(encryptedSecretBytes, accountRS, Convert.parseAccountId(accountRS), version, nonce, timestamp);
    }

    public byte[] getEncryptedSecretBytes() {
        return encryptedSecretBytes;
    }

    public void setEncryptedSecretBytes(byte[] encryptedSecretBytes) {
        this.encryptedSecretBytes = encryptedSecretBytes;
    }

    public String getAccountRS() {
        return accountRS;
    }

    public void setAccountRS(String accountRS) {
        this.accountRS = accountRS;
    }

    public long getAccount() {
        return account;
    }

    public void setAccount(long account) {
        this.account = account;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public void setNonce(byte[] nonce) {
        this.nonce = nonce;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EncryptedSecretBytesDetails)) return false;
        EncryptedSecretBytesDetails that = (EncryptedSecretBytesDetails) o;
        return account == that.account &&
            Objects.equals(version, that.version) &&
            timestamp == that.timestamp &&
            Arrays.equals(encryptedSecretBytes, that.encryptedSecretBytes) &&
            Objects.equals(accountRS, that.accountRS) &&
            Arrays.equals(nonce, that.nonce);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(accountRS, account, version, timestamp);
        result = 31 * result + Arrays.hashCode(encryptedSecretBytes);
        result = 31 * result + Arrays.hashCode(nonce);
        return result;
    }
}
