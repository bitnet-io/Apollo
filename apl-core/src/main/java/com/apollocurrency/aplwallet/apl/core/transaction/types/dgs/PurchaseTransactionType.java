/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.dgs;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSGoods;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.DGSService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsPurchaseAttachment;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Map;

@Singleton
public class PurchaseTransactionType extends DigitalGoodsTransactionType {

    private final Blockchain blockchain;

    @Inject
    public PurchaseTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, DGSService service, Blockchain blockchain) {
        super(blockchainConfig, accountService, service);
        this.blockchain = blockchain;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.DGS_PURCHASE;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.DIGITAL_GOODS_PURCHASE;
    }

    @Override
    public String getName() {
        return "DigitalGoodsPurchase";
    }

    @Override
    public DigitalGoodsPurchaseAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new DigitalGoodsPurchaseAttachment(buffer);
    }

    @Override
    public DigitalGoodsPurchaseAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new DigitalGoodsPurchaseAttachment(attachmentData);
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        DigitalGoodsPurchaseAttachment attachment = (DigitalGoodsPurchaseAttachment) transaction.getAttachment();
        if (attachment.getQuantity() <= 0
            || attachment.getQuantity() > Constants.MAX_DGS_LISTING_QUANTITY
            || attachment.getPriceATM() <= 0
            ||  Convert2.safeMultiply(attachment.getQuantity(), attachment.getPriceATM(), transaction)
            > getBlockchainConfig().getCurrentConfig().getMaxBalanceATM()) {
            throw new AplException.NotValidException("Invalid digital goods purchase: " + attachment.getJSONObject());
        }
        if (transaction.getEncryptedMessage() != null && !transaction.getEncryptedMessage().isText()) {
            throw new AplException.NotValidException("Only text encrypted messages allowed");
        }
        if (attachment.getDeliveryDeadlineTimestamp() <= blockchain.getLastBlockTimestamp()) {
            throw new AplException.NotCurrentlyValidException("Delivery deadline has already expired: " + attachment.getDeliveryDeadlineTimestamp());
        }
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        DigitalGoodsPurchaseAttachment attachment = (DigitalGoodsPurchaseAttachment) transaction.getAttachment();
        if (senderAccount.getUnconfirmedBalanceATM() >= Math.multiplyExact(attachment.getQuantity(), attachment.getPriceATM())) {
            getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), -Math.multiplyExact(attachment.getQuantity(), attachment.getPriceATM()));
            return true;
        }
        return false;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        DigitalGoodsPurchaseAttachment attachment = (DigitalGoodsPurchaseAttachment) transaction.getAttachment();
        getAccountService().addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), Math.multiplyExact(attachment.getQuantity(), attachment.getPriceATM()));
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        DigitalGoodsPurchaseAttachment attachment = (DigitalGoodsPurchaseAttachment) transaction.getAttachment();
        dgsService.purchase(transaction, attachment);
    }

    @Override
    public void doValidateAttachment(Transaction transaction) throws AplException.ValidationException {
        DigitalGoodsPurchaseAttachment attachment = (DigitalGoodsPurchaseAttachment) transaction.getAttachment();
        DGSGoods goods = dgsService.getGoods(attachment.getGoodsId());
        if (goods != null && goods.getSellerId() != transaction.getRecipientId()) {
            throw new AplException.NotValidException("Invalid digital goods purchase: " + attachment.getJSONObject());
        }
        if (goods == null || goods.isDelisted()) {
            throw new AplException.NotCurrentlyValidException("Goods '" + Long.toUnsignedString(attachment.getGoodsId()) + "' not yet listed or already delisted");
        }
        if (attachment.getQuantity() > goods.getQuantity() || attachment.getPriceATM() != goods.getPriceATM()) {
            throw new AplException.NotCurrentlyValidException("Goods price or quantity changed: " + attachment.getJSONObject());
        }
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        DigitalGoodsPurchaseAttachment attachment = (DigitalGoodsPurchaseAttachment) transaction.getAttachment();
        // not a bug, uniqueness is based on DigitalGoods.DELISTING
        return isDuplicate(TransactionTypes.TransactionTypeSpec.DGS_DELISTING, Long.toUnsignedString(attachment.getGoodsId()), duplicates, false);
    }

    @Override
    public boolean canHaveRecipient() {
        return true;
    }

    @Override
    public boolean isPhasingSafe() {
        return false;
    }
}
