/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_TRANSACTION;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_SECRET_PHRASE_AND_PUBLIC_KEY;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_TRANSACTION;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.UNKNOWN_TRANSACTION;

@Vetoed
public final class GetPrivateTransaction extends AbstractAPIRequestHandler {

    public GetPrivateTransaction() {
        super(new APITag[]{APITag.TRANSACTIONS}, "transaction", "fullHash", "secretPhrase", "publicKey");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        HttpParameterParserUtil.PrivateTransactionsAPIData data = HttpParameterParserUtil.parsePrivateTransactionRequest(req);
        if (data == null) {
            return MISSING_SECRET_PHRASE_AND_PUBLIC_KEY;
        }
        String transactionIdString = Convert.emptyToNull(req.getParameter("transaction"));
        String transactionFullHash = Convert.emptyToNull(req.getParameter("fullHash"));
        if (transactionIdString == null && transactionFullHash == null) {
            return MISSING_TRANSACTION;
        }
        long accountId = data.getAccountId();
        long transactionId = 0;
        Transaction transaction;
        try {
            Blockchain blockchain = lookupBlockchain();
            if (transactionIdString != null) {
                transactionId = Convert.parseUnsignedLong(transactionIdString);
                transaction = blockchain.getTransaction(transactionId);
            } else {
                transaction = blockchain.getTransactionByFullHash(transactionFullHash);
                if (transaction == null) {
                    return UNKNOWN_TRANSACTION;
                }
            }
        } catch (RuntimeException e) {
            return INCORRECT_TRANSACTION;
        }
        JSONObject response;
        if (transaction == null) {
            transaction = lookupMemPool().get(transactionId);
            if (transaction == null || transaction.getType().getSpec() != TransactionTypes.TransactionTypeSpec.PRIVATE_PAYMENT || transaction.getType().getSpec() == TransactionTypes.TransactionTypeSpec.PRIVATE_PAYMENT && (transaction.getSenderId() != accountId && transaction.getRecipientId() != accountId)) {
                return UNKNOWN_TRANSACTION;
            }
            if (data.isEncrypt()) {
                response = JSONData.encryptedUnconfirmedTransaction(transaction, data.getSharedKey());
            } else {
                response = JSONData.unconfirmedTransaction(transaction);
            }
        } else {
            if (transaction.getType().getSpec() != TransactionTypes.TransactionTypeSpec.PRIVATE_PAYMENT || transaction.getType().getSpec() == TransactionTypes.TransactionTypeSpec.PRIVATE_PAYMENT && (transaction.getSenderId() != accountId && transaction.getRecipientId() != accountId)) {
                return UNKNOWN_TRANSACTION;
            }
            if (data.isEncrypt()) {
                response = JSONData.encryptedTransaction(transaction, data.getSharedKey());
            } else {
                response = JSONData.transaction(transaction, false, false);
            }
        }
        response.put("serverPublicKey", Convert.toHexString(elGamal.getServerPublicKey()));
        return response;
    }

    @Override
    protected String vaultAccountName() {
        return "account";
    }
}

