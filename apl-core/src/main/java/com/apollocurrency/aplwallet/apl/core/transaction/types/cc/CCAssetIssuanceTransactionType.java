/*
 *  Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.cc;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetIssuance;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.annotation.FeeMarker;
import com.apollocurrency.aplwallet.apl.util.annotation.TransactionFee;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @author al
 */
@Singleton
public class CCAssetIssuanceTransactionType extends CCTransactionType {

    private final AssetService assetService;
    private final AccountAssetService accountAssetService;

    @Inject
    public CCAssetIssuanceTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, AssetService assetService, AccountAssetService accountAssetService) {
        super(blockchainConfig, accountService);
        this.assetService = assetService;
        this.accountAssetService = accountAssetService;
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.CC_ASSET_ISSUANCE;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.ASSET_ISSUANCE;
    }

    @Override
    public String getName() {
        return "AssetIssuance";
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        final long oneAPL = getBlockchainConfig().getOneAPL();
        BigDecimal[] singletonAssetFees = getBlockchainConfig().getCurrentConfig().getAdditionalFees(getSpec(), new BigDecimal[]{BigDecimal.ONE, BigDecimal.ONE});
        // do not change/adjust singleton asset fee
        return getFeeFactory().createCustom(
            (tx, appendage) -> {
                if (isSingletonIssuance(tx)) {
                    return new Fee.SizeBasedFee(singletonAssetFees[0].multiply(BigDecimal.valueOf(oneAPL)).longValueExact(),
                        singletonAssetFees[1].multiply(BigDecimal.valueOf(oneAPL)).longValueExact(), 32) {
                        @Override
                        public int getSize(Transaction transaction, Appendix appendage) {
                            ColoredCoinsAssetIssuance attachment = (ColoredCoinsAssetIssuance) transaction.getAttachment();
                            return attachment.getDescription().length();
                        }
                    }.getFee(tx, appendage);
                } else {
                    return getFeeFactory().createFixed(BigDecimal.valueOf(1000)).getFee(tx, appendage);
                }
            });

    }

    @Override
    @TransactionFee({FeeMarker.FEE, FeeMarker.BACK_FEE})
    public long[] getBackFees(Transaction transaction) {
        if (isSingletonIssuance(transaction)) {
            return Convert.EMPTY_LONG;
        }
        long feeATM = transaction.getFeeATM();
        return new long[]{feeATM * 3 / 10, feeATM * 2 / 10, feeATM / 10};
    }

    @Override
    public ColoredCoinsAssetIssuance parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new ColoredCoinsAssetIssuance(buffer);
    }

    @Override
    public ColoredCoinsAssetIssuance parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new ColoredCoinsAssetIssuance(attachmentData);
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        ColoredCoinsAssetIssuance attachment = (ColoredCoinsAssetIssuance) transaction.getAttachment();
        long assetId = transaction.getId();
        assetService.addAsset(transaction, attachment);
        accountAssetService.addToAssetAndUnconfirmedAssetBalanceATU(senderAccount, getLedgerEvent(), assetId, assetId, attachment.getQuantityATU());
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) {
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        ColoredCoinsAssetIssuance attachment = (ColoredCoinsAssetIssuance) transaction.getAttachment();
        if (attachment.getName().length() < Constants.MIN_ASSET_NAME_LENGTH
            || attachment.getName().length() > Constants.MAX_ASSET_NAME_LENGTH
            || attachment.getDescription().length() > Constants.MAX_ASSET_DESCRIPTION_LENGTH
            || attachment.getDecimals() < 0 || attachment.getDecimals() > getBlockchainConfig().getDecimals()
            || attachment.getQuantityATU() <= 0
            || attachment.getQuantityATU() > Math.multiplyExact(getBlockchainConfig().getInitialSupply(), getBlockchainConfig().getOneAPL())) {
            throw new AplException.NotValidException("Invalid asset issuance: " + attachment.getJSONObject());
        }
        String normalizedName = attachment.getName().toLowerCase();
        for (int i = 0; i < normalizedName.length(); i++) {
            if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
                throw new AplException.NotValidException("Invalid asset name: " + normalizedName);
            }
        }
    }

    @Override
    public boolean isBlockDuplicate(final Transaction transaction, final Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        return !isSingletonIssuance(transaction)
            && isDuplicate(TransactionTypes.TransactionTypeSpec.CC_ASSET_ISSUANCE, getName(), duplicates, true);
    }

    @Override
    public boolean canHaveRecipient() {
        return false;
    }

    @Override
    public boolean isPhasingSafe() {
        return true;
    }

    private boolean isSingletonIssuance(Transaction transaction) {
        ColoredCoinsAssetIssuance attachment = (ColoredCoinsAssetIssuance) transaction.getAttachment();
        return attachment.getQuantityATU() == 1
            && attachment.getDecimals() == 0
            && attachment.getDescription().length() <= Constants.MAX_SINGLETON_ASSET_DESCRIPTION_LENGTH;
    }

}
