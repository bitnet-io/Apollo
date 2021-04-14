/*
 * Copyright (c)  2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransactionEntity;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionJsonSerializer;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class UnconfirmedTransactionModelToEntityConverter implements Converter<UnconfirmedTransaction, UnconfirmedTransactionEntity> {
    private final TransactionJsonSerializer transactionJsonSerializer;
    private final TxBContext txBContext;

    @Inject
    public UnconfirmedTransactionModelToEntityConverter(BlockchainConfig blockchainConfig, TransactionJsonSerializer transactionJsonSerializer) {
        this.transactionJsonSerializer = transactionJsonSerializer;
        this.txBContext = TxBContext.newInstance(blockchainConfig.getChain());
    }

    @Override
    public UnconfirmedTransactionEntity apply(UnconfirmedTransaction model) {
        Objects.requireNonNull(model);
        UnconfirmedTransactionEntity.UnconfirmedTransactionEntityBuilder builder = UnconfirmedTransactionEntity.builder()
            .id(model.getId())
            .transactionHeight(model.getHeight())
            .arrivalTimestamp(model.getArrivalTimestamp())
            .feePerByte(model.getFeePerByte())
            .expiration(model.getExpiration());

        JSONObject prunableJSON = transactionJsonSerializer.getPrunableAttachmentJSON(model);
        if (prunableJSON != null) {
            builder.prunableAttachmentJsonString(prunableJSON.toJSONString());
        }
        Result signedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
        builder.transactionBytes(signedTxBytes.array());
        return builder.build();
    }

}
