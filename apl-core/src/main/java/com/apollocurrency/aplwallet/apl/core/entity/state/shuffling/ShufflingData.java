/*
 * Copyright © 2020-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.shuffling;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.db.DbUtils;
import lombok.Getter;
import lombok.Setter;

import java.sql.ResultSet;
import java.sql.SQLException;

@Getter
@Setter
public class ShufflingData extends DerivedEntity {
    private long transactionId;
    private long shufflingId;
    private long accountId;
    private byte[][] data;
    private int transactionTimestamp;

    public ShufflingData(long transactionId, long shufflingId, long accountId, byte[][] data, int transactionTimestamp, int height) {
        super(null, height);
        this.transactionId = transactionId;
        this.shufflingId = shufflingId;
        this.accountId = accountId;
        this.data = data;
        this.transactionTimestamp = transactionTimestamp;
    }

    public ShufflingData(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        setDbKey(dbKey);
        this.transactionId = rs.getLong("transaction_id");
        this.shufflingId = rs.getLong("shuffling_id");
        this.accountId = rs.getLong("account_id");
        this.data = DbUtils.get2dByteArray(rs, "data", Convert.EMPTY_BYTES);
        this.transactionTimestamp = rs.getInt("transaction_timestamp");
    }

    public ShufflingData(Long dbId, Integer height, long transactionId, long shufflingId, long accountId, byte[][] data, int transactionTimestamp) {
        super(dbId, height);
        this.transactionId = transactionId;
        this.shufflingId = shufflingId;
        this.accountId = accountId;
        this.data = data;
        this.transactionTimestamp = transactionTimestamp;
    }

}
