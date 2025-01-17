/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.UnconfirmedTransactionDTO;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import lombok.Getter;
import lombok.Setter;
import org.json.simple.JSONObject;

import javax.enterprise.inject.Vetoed;
import java.util.Map;

/**
 * Converts Transaction model into a transactionDTO using specific configuration
 * <p><b>ATTENTION!</b> This class should NOT be SINGLETON, since such case may cause configuration issues between classes, sharing same instance.
 * <br>
 * Each caller should instantiate new instance or just use CDI default Dependant Scope
 */
@Vetoed
public class UnconfirmedTransactionConverter implements Converter<Transaction, UnconfirmedTransactionDTO> {
    private final PrunableLoadingService prunableLoadingService;
    @Getter
    @Setter
    private volatile boolean priv = true;

    public UnconfirmedTransactionConverter(PrunableLoadingService prunableLoadingService) {
        this.prunableLoadingService = prunableLoadingService;
    }

    @Override
    public UnconfirmedTransactionDTO apply(Transaction model) {
        UnconfirmedTransactionDTO dto = new UnconfirmedTransactionDTO();
        dto.setType(model.getType().getSpec().getType());
        dto.setSubtype(model.getType().getSpec().getSubtype());
        dto.setPhased(model.getPhasing() != null);
        dto.setTimestamp(model.getTimestamp());
        dto.setDeadline(model.getDeadline());
        dto.setSenderPublicKey(Convert.toHexString(model.getSenderPublicKey()));

        long recipientId;
        long senderId;
        long amountATM;
        String senderPublicKey;

        if (priv && model.getType().getSpec() == TransactionTypes.TransactionTypeSpec.PRIVATE_PAYMENT) {
            recipientId = AccountConverter.anonymizeAccount();
            senderPublicKey = AccountConverter.anonymizePublicKey();
            senderId = AccountService.getId(Convert.parseHexString(senderPublicKey));
            amountATM = AccountConverter.anonymizeBalance();

            dto.setRecipient(Long.toUnsignedString(recipientId));
            dto.setRecipientRS(Convert2.rsAccount(recipientId));

        } else {
            senderPublicKey = Convert.toHexString(model.getSenderPublicKey());
            amountATM = model.getAmountATM();
            senderId = model.getSenderId();
            if (model.getRecipientId() != 0) {
                recipientId = model.getRecipientId();
                dto.setRecipient(Long.toUnsignedString(recipientId));
                dto.setRecipientRS(Convert2.rsAccount(recipientId));
            }
        }

        dto.setSender(Long.toUnsignedString(senderId));
        dto.setSenderRS(Convert2.rsAccount(senderId));
        dto.setSenderPublicKey(senderPublicKey);
        dto.setAmountATM(String.valueOf(amountATM));
        dto.setFeeATM(String.valueOf(model.getFeeATM()));
        dto.setReferencedTransactionFullHash(model.getReferencedTransactionFullHash());
        Signature signature = model.getSignature();
        if (signature != null) {
            dto.setSignature(Convert.toHexString(model.getSignature().bytes()));
            dto.setSignatureHash(Convert.toHexString(Crypto.sha256().digest(model.getSignature().bytes())));
            dto.setFullHash(model.getFullHashString());
            dto.setTransaction(model.getStringId());
        }
        JSONObject attachmentJSON = new JSONObject();
        for (Appendix appendage : model.getAppendages()) {
            prunableLoadingService.loadPrunable(model, appendage, true);
            attachmentJSON.putAll(appendage.getJSONObject());
        }
        if (!attachmentJSON.isEmpty()) {
            for (Map.Entry entry : (Iterable<Map.Entry>) attachmentJSON.entrySet()) {
                if (entry.getValue() instanceof Long) {
                    entry.setValue(String.valueOf(entry.getValue()));
                }
            }
            dto.setAttachment(attachmentJSON);
        }

        dto.setHeight(model.getHeight());
        dto.setVersion(model.getVersion());
        dto.setEcBlockId(Long.toUnsignedString(model.getECBlockId()));
        dto.setEcBlockHeight(model.getECBlockHeight());

        return dto;
    }

}
