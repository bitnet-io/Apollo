/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.order;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCAskOrderPlacementAttachment;
import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author silaev-firstbridge on 4/8/2020
 */
@ToString(callSuper = true)
public class AskOrder extends Order {

    public AskOrder(Transaction transaction, CCAskOrderPlacementAttachment attachment, int height) {
        super(transaction, attachment, height);
    }

    public AskOrder(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs, dbKey);
    }
}
