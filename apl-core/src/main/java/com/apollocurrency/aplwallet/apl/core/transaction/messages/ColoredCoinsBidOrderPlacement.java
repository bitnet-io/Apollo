/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.types.cc.ColoredCoinsTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
public final class ColoredCoinsBidOrderPlacement extends ColoredCoinsOrderPlacementAttachment {

    public ColoredCoinsBidOrderPlacement(ByteBuffer buffer) {
        super(buffer);
    }

    public ColoredCoinsBidOrderPlacement(JSONObject attachmentData) {
        super(attachmentData);
    }

    public ColoredCoinsBidOrderPlacement(long assetId, long quantityATU, long priceATM) {
        super(assetId, quantityATU, priceATM);
    }

    @Override
    public TransactionType getTransactionType() {
        return ColoredCoinsTransactionType.BID_ORDER_PLACEMENT;
    }

}
