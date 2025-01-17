/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.vault.model.AplWalletKey;
import com.apollocurrency.aplwallet.vault.model.EthWalletKey;
import com.apollocurrency.aplwallet.vault.util.AccountGeneratorUtil;
import com.apollocurrency.aplwallet.vault.util.AccountHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AccountGeneratorImplTest {
    private static final String MESSAGE = "Test message";
    private AccountGeneratorUtil accountGenerator = new AccountGeneratorUtil();

    @Test
    public void testGenerateAccountApl() {
        AplWalletKey actualAcc = accountGenerator.generateApl();
        byte[] keySeed = Crypto.getKeySeed(actualAcc.getSecretBytes());
        assertArrayEquals(actualAcc.getPrivateKey(), Crypto.getPrivateKey(keySeed));
        assertArrayEquals(actualAcc.getPublicKey(), Crypto.getPublicKey(keySeed));
        byte[] signature = Crypto.sign(MESSAGE.getBytes(), actualAcc.getPrivateKey());
        assertTrue(Crypto.verify(signature, MESSAGE.getBytes(), actualAcc.getPublicKey()));
//        assertEquals(PASSPHRASE, actualAcc.getPassphrase());
        assertEquals(actualAcc.getId(), Convert.getId(actualAcc.getPublicKey()));
    }
//    @Test
//    public void testGenerateAccountNullPassphrase() {
//        assertThrows(RuntimeException.class, () -> accountGenerator.generateApl());
//    }

    @Test
    public void testGenerateAccountEth() throws Exception {
        EthWalletKey actualAcc = AccountHelper.generateNewEthAccount();

        assertNotNull(actualAcc);
        assertNotNull(actualAcc.getCredentials());
        assertNotNull(actualAcc.getCredentials().getAddress());
        assertNotNull(actualAcc.getCredentials().getEcKeyPair());
        assertNotNull(actualAcc.getCredentials().getEcKeyPair().getPrivateKey());
        assertNotNull(actualAcc.getCredentials().getEcKeyPair().getPublicKey());
    }


}
