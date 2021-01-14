/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.appdata;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.FileUtils;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.vault.KeyStoreService;
import com.apollocurrency.aplwallet.vault.VaultKeyStoreServiceImpl;
import com.apollocurrency.aplwallet.vault.model.EncryptedSecretBytesDetails;
import com.apollocurrency.aplwallet.vault.model.SecretBytesDetails;
import com.apollocurrency.aplwallet.vault.service.auth.Account2FAService;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
//TODO implement tests.

@EnableWeld
@ExtendWith(MockitoExtension.class)
public class KeyStoreServiceTest {

    private static final String PASSPHRASE = "random passphrase generated by passphrase generator";
    private static final String ACCOUNT1 = "APL-299N-Y6F7-TZ8A-GYAB8";
    private static final String ACCOUNT2 = "APL-Z6D2-YTAB-L6BV-AAEAY";
    private static final String encryptedKeyJSON =
        "{\n" +
            "  \"encryptedSecretBytes\" : \"8qWMzLfNJt4wT0q2n7YuyMouj08hbfzx9z9HuIBZf2tGHqajPXfHpwzV6EwKYTWMDa2j3copDxujx2SLmFXwdA==\",\n" +
            "  \"accountRS\" : \"APL-299N-Y6F7-TZ8A-GYAB8\",\n" +
            "  \"account\" : -2079221632084206348,\n" +
            "  \"version\" : 0,\n" +
            "  \"nonce\" : \"PET2LeUQDMfgrCIvM0j0tA==\",\n" +
            "  \"timestamp\" : 1539036932840\n" +
            "}";
    private static final String SECRET_BYTES_1 = "44a2868161a651682bdf938b16c485f359443a2c53bd3e752046edef20d11567";
    private static final String SECRET_BYTES_2 = "146c55cbdc5f33390d207d6d08030c3dd4012c3f775ed700937a893786393dbf";
    private NtpTime time = mock(NtpTime.class);
    private Account2FAService account2FAService = mock(Account2FAService.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from()
        .addBeans(MockBean.of(time, NtpTime.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .build();
    private Path tempDirectory;
    private VaultKeyStoreServiceImpl keyStore;


    @BeforeEach
    void setUp() throws Exception {
        tempDirectory = Files.createTempDirectory("keystore-test");
        keyStore = new VaultKeyStoreServiceImpl(tempDirectory, time, account2FAService);
        Files.write(tempDirectory.resolve("---" + ACCOUNT1), encryptedKeyJSON.getBytes());
        Convert2.init("APL", 1739068987193023818L);
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Stream<Path> pathStream = Files.list(tempDirectory)) {
            pathStream.forEach(tempFilePath -> {
                try {
                    Files.delete(tempFilePath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            Files.delete(tempDirectory);
        }
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    public void testSaveKey() throws Exception {
        VaultKeyStoreServiceImpl keyStoreSpy = spy(keyStore);

        KeyStoreService.Status status = keyStoreSpy.saveSecretBytes(PASSPHRASE, Convert.parseHexString(SECRET_BYTES_2));
        assertEquals(KeyStoreService.Status.OK, status);
        verify(keyStoreSpy, times(1)).storeJSONSecretBytes(any(Path.class), any(EncryptedSecretBytesDetails.class));
        verify(keyStoreSpy, times(1)).findKeyStorePathWithLatestVersion(anyLong());

        assertEquals(2, FileUtils.countElementsOfDirectory(tempDirectory));

        String rsAcc = Convert2.defaultRsAccount(Convert.getId(Crypto.getPublicKey(Crypto.getKeySeed(Convert.parseHexString(SECRET_BYTES_2)))));

        try (Stream<Path> paths = Files.list(tempDirectory)) {
            Path encryptedKeyPath = paths.filter(path -> path.getFileName().toString().endsWith(rsAcc)).findFirst().orElseThrow(() -> new RuntimeException("No encrypted key found for " + rsAcc + " account"));

            EncryptedSecretBytesDetails KeyDetails = JSON.getMapper().readValue(encryptedKeyPath.toFile(), EncryptedSecretBytesDetails.class);

            byte[] actualKey = Crypto.aesDecrypt(KeyDetails.getEncryptedSecretBytes(), Crypto.getKeySeed(PASSPHRASE,
                KeyDetails.getNonce(), Convert.longToBytes(KeyDetails.getTimestamp())));

            assertEquals(SECRET_BYTES_2, Convert.toHexString(actualKey));
            assertEquals(ACCOUNT2, rsAcc);
        }
    }


    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    public void testGetKey() throws Exception {
        VaultKeyStoreServiceImpl keyStoreSpy = spy(keyStore);

        long accountId = Convert.parseAccountId(ACCOUNT1);
        SecretBytesDetails secretBytes = keyStoreSpy.getSecretBytesV0(PASSPHRASE, accountId);
        byte[] actualKey = secretBytes.getSecretBytes();
        assertEquals(KeyStoreService.Status.OK, secretBytes.getExtractStatus());
        String rsAcc = Convert2.defaultRsAccount(accountId);

        verify(keyStoreSpy, times(1)).findKeyStorePathWithLatestVersion(accountId);

        assertEquals(1, FileUtils.countElementsOfDirectory(tempDirectory));

        try (Stream<Path> pathStream = Files.list(tempDirectory)) {
            Path encryptedKeyPath = pathStream.findFirst().get();
            assertTrue(encryptedKeyPath.getFileName().toString().endsWith(rsAcc));
        }

        assertEquals(SECRET_BYTES_1, Convert.toHexString(actualKey));

    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    public void testGetKeyUsingIncorrectPassphrase() {
        long accountId = Convert.parseAccountId(ACCOUNT1);
        SecretBytesDetails secretBytesDetails = keyStore.getSecretBytesV0("pass", accountId);
        assertNull(secretBytesDetails.getSecretBytes());
        assertEquals(KeyStoreService.Status.DECRYPTION_ERROR, secretBytesDetails.getExtractStatus());
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    public void testGetKeyUsingIncorrectAccount() throws Exception {
        long accountId = 0;
        SecretBytesDetails secretBytesDetails = keyStore.getSecretBytesV0(PASSPHRASE, accountId);
        assertNull(secretBytesDetails.getSecretBytes());
        assertEquals(KeyStoreService.Status.NOT_FOUND, secretBytesDetails.getExtractStatus());
    }

    //    @Test
    public void testSaveDuplicateKey() throws IOException {
        KeyStoreService.Status status = keyStore.saveSecretBytes(PASSPHRASE, Convert.parseHexString(SECRET_BYTES_1));
        assertEquals(KeyStoreService.Status.DUPLICATE_FOUND, status);
    }

    //    @Test
    public void testDeleteKey() throws ParameterException {
        KeyStoreService.Status status = keyStore.deleteKeyStore(PASSPHRASE, Convert.parseAccountId(ACCOUNT1));
        assertEquals(KeyStoreService.Status.OK, status);
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    public void testDeleteNotFound() throws ParameterException {
        KeyStoreService.Status status = keyStore.deleteKeyStore(PASSPHRASE, Convert.parseAccountId(ACCOUNT2));
        assertEquals(KeyStoreService.Status.BAD_CREDENTIALS, status);
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    public void testDeleteIncorrectPassphrase() throws ParameterException {
        KeyStoreService.Status status = keyStore.deleteKeyStore(PASSPHRASE + "0", Convert.parseAccountId(ACCOUNT1));
        assertEquals(KeyStoreService.Status.BAD_CREDENTIALS, status);
    }

    //    @Test
    public void testDeleteIOError() throws IOException, ParameterException {
        VaultKeyStoreServiceImpl spiedKeyStore = Mockito.spy(keyStore);
        doThrow(new IOException()).when(spiedKeyStore).deleteFile(any(Path.class));
        KeyStoreService.Status status = spiedKeyStore.deleteKeyStore(PASSPHRASE, Convert.parseAccountId(ACCOUNT1));
        assertEquals(KeyStoreService.Status.DELETE_ERROR, status);
        verify(spiedKeyStore, times(1)).deleteFile(any(Path.class));
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    public void testDeleteNotAvailable() throws IOException, ParameterException {
        Path path = tempDirectory.resolve(".local");
        try {
            Files.createFile(path);
            KeyStoreService.Status status = keyStore.deleteKeyStore(PASSPHRASE, Convert.parseAccountId(ACCOUNT1));
            assertEquals(KeyStoreService.Status.NOT_AVAILABLE, status);

        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    public void testSaveNotAvailable() throws IOException {
        Path path = tempDirectory.resolve(".local");
        try {
            Files.createFile(path);
            KeyStoreService.Status status = keyStore.saveSecretBytes(PASSPHRASE, Convert.parseHexString(SECRET_BYTES_2));
            assertEquals(KeyStoreService.Status.NOT_AVAILABLE, status);

        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    public void testGetNotAvailable() throws IOException {
        Path path = tempDirectory.resolve(".local");
        try {
            Files.createFile(path);
            SecretBytesDetails secretBytes = keyStore.getSecretBytesV0(PASSPHRASE, Convert.parseAccountId(ACCOUNT1));
            assertEquals(KeyStoreService.Status.OK, secretBytes.getExtractStatus());
            assertEquals(SECRET_BYTES_1, Convert.toHexString(secretBytes.getSecretBytes()));

        } finally {
            Files.deleteIfExists(path);
        }
    }
}

