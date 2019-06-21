/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.tagged.model;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class DataTag extends VersionedDerivedEntity {

    private String tag;
    private int count;

    public DataTag(String tag, int height, int count) {
        super(null, height);
        this.tag = tag;
        this.count = count;
    }

    public DataTag(String tag, int height) {
        super(null, height);
        this.tag = tag;
    }

    public DataTag(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.tag = rs.getString("tag");
        this.count = rs.getInt("tag_count");
        setDbKey(dbKey);
    }

    public DataTag(ResultSet rs) throws SQLException {
        super(rs);
        this.tag = rs.getString("tag");
        this.count = rs.getInt("tag_count");
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }


    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataTag)) return false;
        if (!super.equals(o)) return false;
        DataTag dataTag = (DataTag) o;
        return count == dataTag.count &&
                Objects.equals(tag, dataTag.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tag, count);
    }
}
