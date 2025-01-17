/*
 *  Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.cc;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.order.OrderMatchService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCAskOrderPlacementAttachment;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

/**
 * @author al
 */
@Singleton
public class CCAskOrderPlacementTransactionType extends CCOrderPlacementTransactionType {
    private final OrderMatchService orderMatchService;
    private final AccountAssetService accountAssetService;

    @Inject
    public CCAskOrderPlacementTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, AssetService assetService, OrderMatchService orderMatchService, AccountAssetService accountAssetService) {
        super(blockchainConfig, accountService, assetService);
        this.orderMatchService = orderMatchService;
        this.accountAssetService = accountAssetService;
    }


    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.CC_ASK_ORDER_PLACEMENT;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.ASSET_ASK_ORDER_PLACEMENT;
    }

    @Override
    public String getName() {
        return "AskOrderPlacement";
    }

    @Override
    public CCAskOrderPlacementAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new CCAskOrderPlacementAttachment(buffer);
    }

    @Override
    public CCAskOrderPlacementAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new CCAskOrderPlacementAttachment(attachmentData);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        CCAskOrderPlacementAttachment attachment = (CCAskOrderPlacementAttachment) transaction.getAttachment();
        long unconfirmedAssetBalance = accountAssetService.getUnconfirmedAssetBalanceATU(senderAccount.getId(), attachment.getAssetId());
        if (unconfirmedAssetBalance >= 0 && unconfirmedAssetBalance >= attachment.getQuantityATU()) {
            accountAssetService.addToUnconfirmedAssetBalanceATU(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getAssetId(), -attachment.getQuantityATU());
            return true;
        }
        return false;
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        super.doStateDependentValidation(transaction);
        CCAskOrderPlacementAttachment attachment = (CCAskOrderPlacementAttachment) transaction.getAttachment();
        long unconfirmedAssetBalance = accountAssetService.getUnconfirmedAssetBalanceATU(transaction.getSenderId(), attachment.getAssetId());
        if (unconfirmedAssetBalance < attachment.getQuantityATU()) {
            throw new AplException.NotCurrentlyValidException("Account " + Long.toUnsignedString(transaction.getSenderId())
                + " has not enough " + Long.toUnsignedString(attachment.getAssetId()) + " asset balance to place ASK order, required: "
                + attachment.getQuantityATU() + ", but only has " + unconfirmedAssetBalance);
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        CCAskOrderPlacementAttachment attachment = (CCAskOrderPlacementAttachment) transaction.getAttachment();
        orderMatchService.addAskOrder(transaction, attachment);
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        CCAskOrderPlacementAttachment attachment = (CCAskOrderPlacementAttachment) transaction.getAttachment();
        accountAssetService.addToUnconfirmedAssetBalanceATU(senderAccount, getLedgerEvent(), transaction.getId(), attachment.getAssetId(), attachment.getQuantityATU());
    }

}
