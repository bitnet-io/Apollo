/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MessageAppendixValidator extends AbstractAppendixValidator<MessageAppendix> {
    private final BlockchainConfig config;

    @Inject
    public MessageAppendixValidator(BlockchainConfig config) {
        this.config = config;
    }

    @Override
    public void validateStateDependent(Transaction transaction, MessageAppendix appendix, int validationHeight) throws AplException.ValidationException {
    }

    @Override
    public void validateStateIndependent(Transaction transaction, MessageAppendix appendix, int validationHeight) throws AplException.ValidationException {
        int length = appendix.getMessage().length;
        int maxLength = config.getCurrentConfig().getMaxArbitraryMessageLength();
        if (length > maxLength) {
            throw new AplException.NotValidException("Invalid arbitrary message length: " + length + ", max allowed length is " + maxLength);
        }
    }

    @Override
    public Class<MessageAppendix> forClass() {
        return MessageAppendix.class;
    }
}
