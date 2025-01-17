/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.service;

public interface PassphraseGenerator {
    /**
     * Generate string(passphrase) which consist of random words separated by space
     *
     * @return generated passphrase
     */
    String generate();
}
