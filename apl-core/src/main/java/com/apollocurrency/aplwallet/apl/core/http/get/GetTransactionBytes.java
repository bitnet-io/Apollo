/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionWrapperHelper;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_TRANSACTION;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_TRANSACTION;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.UNKNOWN_TRANSACTION;

@Vetoed
public final class GetTransactionBytes extends AbstractAPIRequestHandler {

    public GetTransactionBytes() {
        super(new APITag[]{APITag.TRANSACTIONS}, "transaction");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {

        String transactionValue = req.getParameter("transaction");
        if (transactionValue == null) {
            return MISSING_TRANSACTION;
        }

        long transactionId;
        Transaction transaction;
        try {
            transactionId = Convert.parseUnsignedLong(transactionValue);
        } catch (RuntimeException e) {
            return INCORRECT_TRANSACTION;
        }

        Blockchain blockchain = lookupBlockchain();
        transaction = blockchain.getTransaction(transactionId);

        Result unsignedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion())
            .serialize(TransactionWrapperHelper.createUnsignedTransaction(transaction), unsignedTxBytes);

        Result signedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion()).serialize(transaction, signedTxBytes);

        JSONObject response = new JSONObject();
        if (transaction == null) {
            transaction = lookupMemPool().get(transactionId);
            if (transaction == null) {
                return UNKNOWN_TRANSACTION;
            }
        } else {
            response.put("confirmations", blockchain.getHeight() - transaction.getHeight());
        }
        response.put("transactionBytes", Convert.toHexString(signedTxBytes.array()));
        response.put("unsignedTransactionBytes", Convert.toHexString(unsignedTxBytes.array()));
        JSONData.putPrunableAttachment(response, transaction);
        return response;

    }

}
