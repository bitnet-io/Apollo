/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.rest.service.PhasingAppendixFactory;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureParser;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureToolFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptToSelfMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingProcessingAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.TaggedDataExtendAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.TaggedDataUploadAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

@Singleton
@Slf4j
public class TransactionBuilder {
    private final TransactionTypeFactory factory;

    @Inject
    public TransactionBuilder(TransactionTypeFactory factory) {
        this.factory = factory;
    }

    public Transaction.Builder newTransactionBuilder(byte[] senderPublicKey, long amountATM, long feeATM, short deadline, Attachment attachment, int timestamp) {
        return newTransactionBuilder((byte) 1, senderPublicKey, amountATM, feeATM, deadline, attachment, timestamp);
    }

    public Transaction.Builder newTransactionBuilder(int version, byte[] senderPublicKey, long amountATM, long feeATM, short deadline, Attachment attachment, int timestamp) {
        TransactionTypes.TransactionTypeSpec spec = attachment.getTransactionTypeSpec();
        TransactionType transactionType = factory.findTransactionType(spec.getType(), spec.getSubtype());
        attachment.bindTransactionType(transactionType);
        return new TransactionImpl.BuilderImpl((byte) version, senderPublicKey, amountATM, feeATM, deadline, (AbstractAttachment) attachment, timestamp, transactionType);
    }

    public Transaction.Builder newTransactionBuilder(String chainId, int version, byte[] senderPublicKey, BigInteger nonce, BigInteger amount, BigInteger fuelLimit, BigInteger fuelPrice, int deadline, long timestamp, Attachment attachment) {
        TransactionTypes.TransactionTypeSpec spec = attachment.getTransactionTypeSpec();
        TransactionType transactionType = factory.findTransactionType(spec.getType(), spec.getSubtype());
        attachment.bindTransactionType(transactionType);
        return new TransactionImpl.BuilderImpl(chainId, transactionType, (byte) version, senderPublicKey,nonce, amount, fuelLimit, fuelPrice, deadline, timestamp, (AbstractAttachment) attachment);
    }

    public TransactionImpl.BuilderImpl newTransactionBuilder(byte[] bytes) throws AplException.NotValidException {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            byte type = buffer.get();
            byte subtype = buffer.get();
            byte version = (byte) ((subtype & 0xF0) >> 4);
            Signature signature = null;
            SignatureParser signatureParser = SignatureToolFactory.selectParser(version).orElseThrow(UnsupportedTransactionVersion::new);
            subtype = (byte) (subtype & 0x0F);
            int timestamp = buffer.getInt();
            short deadline = buffer.getShort();
            byte[] senderPublicKey = new byte[32];
            buffer.get(senderPublicKey);
            long recipientId = buffer.getLong();
            long amountATM = buffer.getLong();
            long feeATM = buffer.getLong();
            byte[] referencedTransactionFullHash = new byte[32];
            buffer.get(referencedTransactionFullHash);
            referencedTransactionFullHash = Convert.emptyToNull(referencedTransactionFullHash);
            if (version < 2) {
                signature = signatureParser.parse(buffer);
            }
            int flags = 0;
            int ecBlockHeight = 0;
            long ecBlockId = 0;
            if (version > 0) {
                flags = buffer.getInt();
                ecBlockHeight = buffer.getInt();
                ecBlockId = buffer.getLong();
            }
            TransactionType transactionType = factory.findTransactionType(type, subtype);

            AbstractAttachment attachment = transactionType.parseAttachment(buffer);
            attachment.bindTransactionType(transactionType);
            TransactionImpl.BuilderImpl builder = new TransactionImpl.BuilderImpl(version, senderPublicKey, amountATM, feeATM,
                deadline, attachment, timestamp, transactionType)
                .referencedTransactionFullHash(referencedTransactionFullHash)
                .signature(signature)
                .ecBlockHeight(ecBlockHeight)
                .ecBlockId(ecBlockId);
            if (transactionType.canHaveRecipient()) {
                builder.recipientId(recipientId);
            }
            int position = 1;
            if ((flags & position) != 0 || (version == 0 && transactionType.getSpec() == TransactionTypes.TransactionTypeSpec.ARBITRARY_MESSAGE)) {
                builder.appendix(new MessageAppendix(buffer));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(new EncryptedMessageAppendix(buffer));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(new PublicKeyAnnouncementAppendix(buffer));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(new EncryptToSelfMessageAppendix(buffer));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(PhasingAppendixFactory.build(buffer));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(new PrunablePlainMessageAppendix(buffer));
            }
            position <<= 1;
            if ((flags & position) != 0) {
                builder.appendix(new PrunableEncryptedMessageAppendix(buffer));
            }
            if (version >= 2) {
                //read transaction multi-signature V2
                signature = signatureParser.parse(buffer);
            }
            builder.signature(signature);
            if (buffer.hasRemaining()) {
                throw new AplException.NotValidException("Transaction bytes too long, " + buffer.remaining() + " extra bytes");
            }
            return builder;
        } catch (RuntimeException e) {
            log.debug("Failed to parse transaction bytes: " + Convert.toHexString(bytes));
            throw e;
        }
    }

    public TransactionImpl.BuilderImpl newTransactionBuilder(byte[] bytes, JSONObject prunableAttachments) throws AplException.NotValidException {
        TransactionImpl.BuilderImpl builder = newTransactionBuilder(bytes);
        if (prunableAttachments != null) {
            ShufflingProcessingAttachment shufflingProcessing = ShufflingProcessingAttachment.parse(prunableAttachments);
            if (shufflingProcessing != null) {
                TransactionType transactionType = factory.findTransactionTypeBySpec(shufflingProcessing.getTransactionTypeSpec());
                builder.appendix(shufflingProcessing);
                shufflingProcessing.bindTransactionType(transactionType);
            }
            TaggedDataUploadAttachment taggedDataUploadAttachment = TaggedDataUploadAttachment.parse(prunableAttachments);
            if (taggedDataUploadAttachment != null) {
                TransactionType transactionType = factory.findTransactionTypeBySpec(taggedDataUploadAttachment.getTransactionTypeSpec());
                taggedDataUploadAttachment.bindTransactionType(transactionType);
                builder.appendix(taggedDataUploadAttachment);
            }
            TaggedDataExtendAttachment taggedDataExtendAttachment = TaggedDataExtendAttachment.parse(prunableAttachments);
            if (taggedDataExtendAttachment != null) {
                TransactionType transactionType = factory.findTransactionTypeBySpec(taggedDataExtendAttachment.getTransactionTypeSpec());
                taggedDataExtendAttachment.bindTransactionType(transactionType);
                builder.appendix(taggedDataExtendAttachment);
            }
            PrunablePlainMessageAppendix prunablePlainMessage = PrunablePlainMessageAppendix.parse(prunableAttachments);
            if (prunablePlainMessage != null) {
                builder.appendix(prunablePlainMessage);
            }
            PrunableEncryptedMessageAppendix prunableEncryptedMessage = PrunableEncryptedMessageAppendix.parse(prunableAttachments);
            if (prunableEncryptedMessage != null) {
                builder.appendix(prunableEncryptedMessage);
            }
        }
        return builder;
    }

    public TransactionImpl.BuilderImpl newTransactionBuilder(RlpReader reader) throws AplException.NotValidException {
        MessageAppendix message=null;
        EncryptedMessageAppendix encryptedMessage=null;
        EncryptToSelfMessageAppendix encryptToSelfMessage=null;
        PublicKeyAnnouncementAppendix publicKeyAnnouncement=null;
        PhasingAppendix phasing=null;
        PrunablePlainMessageAppendix prunablePlainMessage=null;
        PrunableEncryptedMessageAppendix prunableEncryptedMessage=null;

        try {
            //header
            byte type = reader.readByte();
            byte subtype = reader.readByte();
            byte version = (byte) ((subtype & 0xF0) >> 4);
            Signature signature = null;
            SignatureParser signatureParser = SignatureToolFactory.selectParser(version).orElseThrow(UnsupportedTransactionVersion::new);
            subtype = (byte) (subtype & 0x0F);

            String chainId = reader.readString();
            int deadline = reader.readInt();
            long timestamp = reader.readLong();
            int ecBlockHeight = reader.readInt();
            long ecBlockId = reader.readLong();
            BigInteger nonce = reader.readBigInteger();
            byte[] senderPublicKey = reader.read();
            long recipientId = reader.readLong();
            BigInteger amount = reader.readBigInteger();
            BigInteger fuelPrice = reader.readBigInteger();
            BigInteger fuelLimit = reader.readBigInteger();

            //data part
            byte[] referencedTransactionFullHash = reader.read();
            referencedTransactionFullHash = Convert.emptyToNull(referencedTransactionFullHash);

            TransactionType transactionType = factory.findTransactionType(type, subtype);
            if(transactionType == null){
                throw new AplException.NotValidException("Wrong transaction spec: type="+type+", subtype="+subtype+", version="+version);
            }

            //attachments
            AbstractAttachment attachment=null;
            List<AbstractAppendix> appendages = new ArrayList<>();
            RlpReader attachmentsListReader = reader.readListReader();
            while (attachmentsListReader.hasNext()){
                RlpReader attachmentReader = attachmentsListReader.readListReader();
                int appendixFlag = attachmentReader.readInt();
                if (appendixFlag == 0 ){//transaction attachment
                    attachment = transactionType.parseAttachment(reader);
                    attachment.bindTransactionType(transactionType);
                }else{//transaction appendages
                    AbstractAppendix appendix = readAppendix(appendixFlag, attachmentReader);
                    if(appendix!=null){
                        appendages.add(appendix);
                    }
                }
                if(attachmentReader.hasNext()){
                    throw new AplException.NotValidException("Wrong transaction structure: type="+type+", subtype="+subtype+", version="+version);
                }
            }

            TransactionImpl.BuilderImpl builder = new TransactionImpl.BuilderImpl(chainId, transactionType, version,
                senderPublicKey, nonce,amount, fuelLimit, fuelPrice,deadline, timestamp, attachment);

            appendages.forEach(builder::appendix);

            builder.referencedTransactionFullHash(referencedTransactionFullHash)
                .ecBlockHeight(ecBlockHeight)
                .ecBlockId(ecBlockId);

            if (transactionType.canHaveRecipient()) {
                builder.recipientId(recipientId);
            }

            if (version >= 2) {
                //read transaction multi-signature V2
                signature = signatureParser.parse(reader);
            }
            builder.signature(signature);
            if (reader.hasNext()) {
                throw new AplException.NotValidException("Transaction bytes too long");
            }
            return builder;
        } catch (RuntimeException e) {
            log.debug("Failed to parse transaction RLP reader: " + reader);
            throw e;
        }
    }

    private AbstractAppendix readAppendix(int flag, RlpReader reader) throws AplException.NotValidException {
        switch (flag){
            case 1:
                return new MessageAppendix(reader);
            case 2:
                return new EncryptedMessageAppendix(reader);
            case 4:
                return new PublicKeyAnnouncementAppendix(reader);
            case 8:
                return new EncryptToSelfMessageAppendix(reader);
            case 16:
                //TODO: should be implemented
                //PhasingAppendixFactory.build(reader));
                break;
            case 32:
                //TODO: should be implemented
                //new PrunablePlainMessageAppendix(reader));
                break;
            case 64:
                //TODO: should be implemented
                //new PrunableEncryptedMessageAppendix(reader));
                break;
            default:
                throw new AplException.NotValidException("Unexpected value: " + flag);
        }
        return null;
    }

    /**
     * @deprecated Use com.apollocurrency.aplwallet.apl.core.rest.converter.TransactionDTOConverter
     */
    @Deprecated
    public TransactionImpl.BuilderImpl newTransactionBuilder(JSONObject transactionData) throws AplException.NotValidException {

        try {
            byte type = ((Long) transactionData.get("type")).byteValue();
            byte subtype = ((Long) transactionData.get("subtype")).byteValue();
            int timestamp = ((Long) transactionData.get("timestamp")).intValue();
            short deadline = ((Long) transactionData.get("deadline")).shortValue();
            byte[] senderPublicKey = Convert.parseHexString((String) transactionData.get("senderPublicKey"));
            long amountATM = transactionData.containsKey("amountATM") ? Convert.parseLong(transactionData.get("amountATM")) : Convert.parseLong(transactionData.get("amountNQT"));
            long feeATM = transactionData.containsKey("feeATM") ? Convert.parseLong(transactionData.get("feeATM")) : Convert.parseLong(transactionData.get("feeNQT"));
            String referencedTransactionFullHash = (String) transactionData.get("referencedTransactionFullHash");
            Long versionValue = (Long) transactionData.get("version");
            byte version = versionValue == null ? 0 : versionValue.byteValue();

            SignatureParser signatureParser = SignatureToolFactory.selectParser(version).orElseThrow(UnsupportedTransactionVersion::new);
            ByteBuffer signatureBuffer = ByteBuffer.wrap(Convert.parseHexString((String) transactionData.get("signature")));
            Signature signature = signatureParser.parse(signatureBuffer);

            JSONObject attachmentData = (JSONObject) transactionData.get("attachment");
            int ecBlockHeight = 0;
            long ecBlockId = 0;
            if (version > 0) {
                ecBlockHeight = ((Long) transactionData.get("ecBlockHeight")).intValue();
                ecBlockId = Convert.parseUnsignedLong((String) transactionData.get("ecBlockId"));
            }

            TransactionType transactionType = factory.findTransactionType(type, subtype);
            if (transactionType == null) {
                throw new AplException.NotValidException("Invalid transaction type: " + type + ", " + subtype);
            }
            AbstractAttachment attachment = transactionType.parseAttachment(attachmentData);
            attachment.bindTransactionType(transactionType);
            TransactionImpl.BuilderImpl builder = new TransactionImpl.BuilderImpl(version, senderPublicKey,
                amountATM, feeATM, deadline,
                attachment, timestamp, transactionType)
                .referencedTransactionFullHash(referencedTransactionFullHash)
                .signature(signature)
                .ecBlockHeight(ecBlockHeight)
                .ecBlockId(ecBlockId);
            if (transactionType.canHaveRecipient()) {
                long recipientId = Convert.parseUnsignedLong((String) transactionData.get("recipient"));
                builder.recipientId(recipientId);
            }
            if (attachmentData != null) {
                builder.appendix(MessageAppendix.parse(attachmentData));
                builder.appendix(EncryptedMessageAppendix.parse(attachmentData));
                builder.appendix(PublicKeyAnnouncementAppendix.parse(attachmentData));
                builder.appendix(EncryptToSelfMessageAppendix.parse(attachmentData));
                builder.appendix(PhasingAppendixFactory.parse(attachmentData));
                builder.appendix(PrunablePlainMessageAppendix.parse(attachmentData));
                builder.appendix(PrunableEncryptedMessageAppendix.parse(attachmentData));
            }
            builder.signature(signature);
            return builder;
        } catch (RuntimeException e) {
            log.debug("Failed to parse transaction: " + transactionData.toJSONString());
            throw e;
        }
    }
}
