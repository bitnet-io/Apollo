/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class ConsoleSecurityAlertSender implements SecurityAlertSender {
    private static final Logger LOG = getLogger(ConsoleSecurityAlertSender.class);

    @Override
    public void send(Transaction invalidUpdateTransaction) {
        LOG.info("Transaction: " + invalidUpdateTransaction.getId() + " is invalid");
    }

    @Override
    public void send(String message) {
        LOG.warn(message);
    }
}
