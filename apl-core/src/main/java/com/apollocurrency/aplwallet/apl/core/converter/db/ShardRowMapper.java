/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.util.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ShardState;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JDBI row mapper for {@link Shard}
 */
public class ShardRowMapper implements RowMapper<Shard> {

    @Override
    public Shard map(ResultSet rs, StatementContext ctx) throws SQLException {

        Long[] generatorIds = DbUtils.getArray(rs, "generator_ids", Long[].class, new Long[]{});
        Integer[] blockTimeouts = DbUtils.getArray(rs, "block_timeouts", Integer[].class, new Integer[]{});
        Integer[] blockTimestamps = DbUtils.getArray(rs, "block_timestamps", Integer[].class, new Integer[]{});
        return Shard.builder()
            .id(rs.getLong("shard_id"))
            .shardHash(rs.getBytes("shard_hash"))
            .shardState(ShardState.getType(rs.getLong("shard_state")))
            .shardHeight(rs.getInt("shard_height"))
            .coreZipHash(rs.getBytes("zip_hash_crc"))
            .generatorIds(generatorIds == null ? null : Convert.toArray(generatorIds)) // should not be empty
            .blockTimeouts(blockTimeouts == null ? null : Convert.toArrayInt(blockTimeouts)) // should not be empty
            .blockTimestamps(blockTimestamps == null ? null : Convert.toArrayInt(blockTimestamps)) // should not be empty
            .prunableZipHash(rs.getBytes("prunable_zip_hash"))
            .build();
    }
}
