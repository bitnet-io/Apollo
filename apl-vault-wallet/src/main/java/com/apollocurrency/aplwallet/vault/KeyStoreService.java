/*
 * Copyright © 2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.vault;

import com.apollocurrency.aplwallet.vault.model.ApolloFbWallet;
import com.apollocurrency.aplwallet.vault.model.KMSResponseStatus;
import com.apollocurrency.aplwallet.vault.model.SecretBytesDetails;
import com.apollocurrency.aplwallet.vault.model.WalletKeysInfo;
import io.firstbridge.cryptolib.container.FbWallet;

import java.io.File;

public interface KeyStoreService {

    /**
     * Save encrypted by passphrase secretStore in the json format.
     *
     * @param passphrase - string, which consist of random words for encryption
     * @param fbWallet   - secret array of bytes which will be stored into keystore
     * @return OK - if secretBytes were saved successfully, otherwise returned status hold error cause
     */
    KMSResponseStatus saveSecretKeyStore(String passphrase, ApolloFbWallet fbWallet);

    /**
     * Save encrypted by passphrase secretStore in the json format.
     *
     * @param passphrase - string, which consist of random words for encryption
     * @param fbWallet   - secret array of bytes which will be stored into keystore
     * @return OK - if secretBytes were saved successfully, otherwise returned status hold error cause
     */
    KMSResponseStatus saveSecretKeyStore(String passphrase, Long accountId, FbWallet fbWallet);

    /**
     * Return secret bytes if key exists for accountId and can be decrypted by passphrase
     *
     * @param passphrase - string, which consist of random words for keySeed decryption
     * @param accountId  - id of account, which keySeed should be decrypted
     */
    ApolloFbWallet getSecretStore(String passphrase, long accountId);

    /**
     * Return currency addresses of the user. (apl, eth, pax)
     *
     * @param passphrase - string, which consist of random words for keySeed decryption
     * @param accountId  - id of account, which keySeed should be decrypted
     */
    WalletKeysInfo getWalletKeysInfo(String passphrase, long accountId);

    /**
     * Return secret bytes if key exists for accountId and can be decrypted by passphrase
     *
     * @param passphrase - string, which consist of random words for keySeed decryption
     * @param accountId  - id of account, which keySeed should be decrypted
     * @return decrypted secret bytes with status OK or null with fail status
     */
    @Deprecated
    SecretBytesDetails getSecretBytesV0(String passphrase, long accountId);


    /**
     * Migrate old keyStore to the New
     */
    boolean migrateOldKeyStorageToTheNew(String passphrase, long accountId);

    /**
     * Check if new version of keyStore exist.
     *
     * @param accountId
     * @return true if new version of keystore for account exist
     */
    boolean isNewVersionOfKeyStoreForAccountExist(long accountId);

    /**
     * Check if keyStore exist.
     *
     * @param accountId
     * @return true if key store for account is exist
     */
    boolean isKeyStoreForAccountExist(long accountId);

    /**
     * Remove secret bytes from keystore if secret bytes exist for accountId and can be decrypted by passphrase
     *
     * @param passphrase - string, which consist of random words for secret bytes decryption
     * @param accountId  - id of account, which secretBytes should be deleted
     * @return status of deletion
     */
    KMSResponseStatus deleteKeyStore(String passphrase, long accountId);

    /**
     * Get Key Store as a file.
     *
     * @param accountId
     * @param passphrase
     * @return file.
     */
    File getSecretStoreFile(Long accountId, String passphrase);

}
