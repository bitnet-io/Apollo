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

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONArray;

import java.util.Collection;
import java.util.List;

public interface TransactionProcessor {

    void broadcastWhenConfirmed(Transaction transaction, Transaction uncTransaction);

//    void broadcast(Collection<Transaction> transactions);

    void clearUnconfirmedTransactions();

    void rebroadcastAllUnconfirmedTransactions();

    void removeUnconfirmedTransaction(Transaction transaction);

    void broadcast(Transaction transaction);

    void processDelayedTxs(int number);

    void processLater(Collection<Transaction> transactions);

    void processPeerTransactions(List<Transaction> transactions);

    boolean isSufficientlyValidTransaction(Transaction tx);

    List<Transaction> restorePrunableData(JSONArray transactions) throws AplException.NotValidException;

}
