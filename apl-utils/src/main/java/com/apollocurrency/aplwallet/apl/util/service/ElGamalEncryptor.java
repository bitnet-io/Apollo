/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.service;

import com.apollocurrency.aplwallet.apl.crypto.AplElGamalCrypto;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import io.firstbridge.cryptolib.ElGamalKeyPair;

/**
 * @author alukin@gmail.com
 */
public class ElGamalEncryptor {

    private byte[] privateKey;
    private byte[] publicKey;
    private ElGamalKeyPair elGamalKeyPair;
    private final TaskDispatchManager taskDispatchManager;

    public ElGamalEncryptor(TaskDispatchManager dispatchManager) {
        taskDispatchManager = dispatchManager;
    }

    public final void init() {
        taskDispatchManager.newBackgroundDispatcher("KeyGenerator")
            .schedule(Task.builder()
                .name("KeyGenerationTask")
                .delay(15 * 60 * 1000) // 15 min
                .task(this::generateKeys)
                .build());
    }

    private synchronized void generateKeys() {
        byte[] keyBytes = new byte[32];
        Crypto.getSecureRandom().nextBytes(keyBytes);
        byte[] keySeed = Crypto.getKeySeed(keyBytes);
        privateKey = Crypto.getPrivateKey(keySeed);
        publicKey = Crypto.getPublicKey(keySeed);
        elGamalKeyPair = Crypto.getElGamalKeyPair();
    }

    public synchronized byte[] getServerPublicKey() {
        return publicKey;
    }

    public synchronized byte[] getServerPrivateKey() {
        return privateKey;
    }

    public synchronized ElGamalKeyPair getServerElGamalPublicKey() {
        return elGamalKeyPair;
    }

    public synchronized String elGamalDecrypt(String cryptogramm) {
        return AplElGamalCrypto.decrypt(cryptogramm, elGamalKeyPair);
    }

}
