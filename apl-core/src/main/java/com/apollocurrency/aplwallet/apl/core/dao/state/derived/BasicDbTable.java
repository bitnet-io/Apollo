/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.derived;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.core.shard.ShardConstants;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provide rollback and trim multiversion implementations and hold common parameters such as multiversion and keyfactory
 */
@Slf4j
public abstract class BasicDbTable<T extends DerivedEntity> extends DerivedDbTable<T> {

    protected KeyFactory<T> keyFactory;
    protected boolean multiversion;

    protected BasicDbTable(String table, KeyFactory<T> keyFactory, boolean multiversion,
                           DatabaseManager databaseManager,
                           Event<FullTextOperationData> fullTextOperationDataEvent,
                           String fullTextSearchColumns) {
        super(table, databaseManager, fullTextOperationDataEvent, fullTextSearchColumns);
        this.keyFactory = keyFactory;
        this.multiversion = multiversion;
    }

    public KeyFactory<T> getDbKeyFactory() {
        return keyFactory;
    }

    public boolean isMultiversion() {
        return multiversion;
    }

    @Override
    public int rollback(int height) {
        if (multiversion) {
            return doMultiversionRollback(height);
        } else {
            return super.rollback(height);
        }
    }

    private int doMultiversionRollback(int height) {
        log.trace("doMultiversionRollback(), height={}", height);
        int deletedRecordsCount = 0; // count for selected db_ids (only Searchable tables)
        int deletedResult; // count for deleted db_ids (any table)

        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (!dataSource.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        long startTime = System.currentTimeMillis();
        String sql = "UPDATE " + table
            + " SET latest = TRUE " + (getDeletedSetStatementIfSupported(false)) + keyFactory.getPKClause() + " AND height ="
            + " (SELECT MAX(height) FROM " + table + keyFactory.getPKClause() + ")";
        log.trace(sql);
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmtSelectToDelete = con.prepareStatement("SELECT DISTINCT " + keyFactory.getPKColumns()
                 + " FROM " + table + " WHERE height > ?");
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table
                 + " WHERE height > ?");
             // select records to deleted in FTS by 'db_id' values
             PreparedStatement pstmtSelectDeletedIds = con.prepareStatement("SELECT DB_ID FROM " + table + " WHERE height > ?");
             PreparedStatement pstmtSetLatest = con.prepareStatement(sql)) {
            pstmtSelectToDelete.setInt(1, height);
            List<DbKey> dbKeys = new ArrayList<>();
            try (ResultSet rs = pstmtSelectToDelete.executeQuery()) {
                while (rs.next()) {
                    dbKeys.add(keyFactory.newKey(rs));
                }
            }
            if (dbKeys.size() > 0) {
                log.trace("Rollback table {} found {} records to update to latest", table, dbKeys.size());
            }

            pstmtSelectDeletedIds.setInt(1, height); // set height to select DB_IDs to be deleted
            pstmtDelete.setInt(1, height); // set height to delete records

            // select deleted DB_IDs and fire FTS events for searchable tables to remove data from FTS
            deletedRecordsCount = super.selectSearchableRecordsSendFtsEvent(deletedRecordsCount, pstmtSelectDeletedIds);
            // deleting actual records for entity
            deletedResult = pstmtDelete.executeUpdate();
            if (this.isSearchable()) {
                assert deletedResult == deletedRecordsCount; // check in debug mode only
            }
            log.trace("Deleted '{}' at height = {}, deletedCount = {}, deletedResult = {}",
                this.table, height, deletedRecordsCount, deletedResult);

            if (deletedResult > 0) {
                log.trace("Rollback table {} deleting {} records", table, deletedRecordsCount);
            }

            boolean supportDelete = supportDelete();
            if (supportDelete) { // do not 'setLatest' for deleted entities ( if last entity below given height was deleted 'deleted=true')
                try (PreparedStatement pstmtSelectDeletedCount = con.prepareStatement("SELECT " + keyFactory.getPKColumns() + " FROM " + table + " WHERE height <= ? AND deleted = true GROUP BY " + keyFactory.getPKColumns() + " HAVING COUNT(DISTINCT HEIGHT) % 2 = 0");
                     PreparedStatement pstmtGetLatestDeleted = con.prepareStatement("SELECT deleted FROM " + table + keyFactory.getPKClause() + " AND height <=? ORDER BY db_id DESC LIMIT 1")) {
                    pstmtSelectDeletedCount.setInt(1, height);
                    try (ResultSet rs = pstmtSelectDeletedCount.executeQuery()) {
                        while (rs.next()) {
                            DbKey candidateToRemove = keyFactory.newKey(rs);
                            int index = candidateToRemove.setPK(pstmtGetLatestDeleted, 1);
                            pstmtGetLatestDeleted.setInt(index, height);
                            try (ResultSet latestDeleted = pstmtGetLatestDeleted.executeQuery()) {
                                if (latestDeleted.next()) {
                                    boolean del = latestDeleted.getBoolean(1);
                                    if (del) {
                                        dbKeys.remove(candidateToRemove);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            for (DbKey dbKey : dbKeys) {
                int i = dbKey.setPK(pstmtSetLatest, 1);
                dbKey.setPK(pstmtSetLatest, i);
                pstmtSetLatest.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("Error: table=" + table + ", " + e.getMessage(), e);
            throw new RuntimeException(e.toString(), e);
        }
        log.trace("Rollback for table {} took {} ms", table, System.currentTimeMillis() - startTime);
        return deletedResult;
    }


    @Override
    public void trim(int height) {
        if (multiversion) {
            doMultiversionTrim(height);
        } else {
            super.trim(height);
        }
    }

    /**
     * <p>Delete old data from db before target height. Leave last actual entry for each entity to allow rollback to target height</p>
     * <p>Also will completely delete blockchain 'deleted' entries with latest=false & deleted=true (applies only for paired 'deleted' records to ensure rollback availability)</p>
     * <p>WARNING! Do not trim to your current blockchain height! It will delete all history data and you will not be able to rollback and switch to another fork</p>
     *
     * @param height target height of blockchain for trimming, should be less or equal to minRollbackHeight to allow rollback to such height
     *               <p>Example:</p>
     *               <pre>{@code
     *                                                               db_id     account   balance    height    latest deleted
     *                                                               10         1          10         0       true   false
     *                                                               11         2          10         1       false  false
     *                                                               11         2          10         2       false  true
     *                                                               15         2           0         3       false  true
     *                                                               30         3          50         3       false  false
     *                                                               40         3           5         4       true   false
     *                                                               42         2          11         4       false  false
     *                                                               44         2          12         5       false  true
     *                                                               50         4         125         5       false  false
     *                                                               70         6           6         5       false  false
     *                                                               80         6           6         6       true   false
     *                                                               81         5           5         6       false  false
     *                                                               82         2           0         6       false   true
     *                                                               90         5          80         7       true   false
     *                                                               100        4         100         7       true   false
     *                                                               }</pre>
     *               <p>
     *               Trim to height 6 will result in
     *               <pre>{@code
     *                                                               db_id     account   balance    height    latest deleted
     *                                                               10         1           10        0       true   false
     *                                                               40         3            5        4       true   false
     *                                                               44         2           12        5       false  true
     *                                                               50         4          125        5       false  false
     *                                                               70         6            6        5       false  false
     *                                                               80         6            6        6       true   false
     *                                                               81         5            5        6       false  false
     *                                                               82         2            0        6       false  true
     *                                                               90         5           80        7       true   false
     *                                                               100        4          100        7       true   false
     *                                                               }</pre>
     *               </p>
     */
    private void doMultiversionTrim(final int height) {
        log.trace("doMultiversionTrim(), height={}", height);
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (!dataSource.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        long startTime = System.currentTimeMillis();
        try (Connection con = dataSource.getConnection();
             //find acc and max_height of last written record (accounts with one record will be omitted)
             PreparedStatement pstmtSelect = con.prepareStatement("SELECT " + keyFactory.getPKColumns() + ", MAX(height) AS max_height"
                 + " FROM " + table + " WHERE height < ? GROUP BY " + keyFactory.getPKColumns() + " HAVING COUNT(DISTINCT height) > 1")) {
            pstmtSelect.setInt(1, height);
            long startDeleteTime, deleted = 0L, deleteStm = 0L, startSelectTime = System.currentTimeMillis();
            try (ResultSet rs = pstmtSelect.executeQuery();
                 PreparedStatement pstmtDeleteById =
                     con.prepareStatement("DELETE FROM " + table + " WHERE db_id = ?");
                 PreparedStatement selectDbIdStatement =
                     con.prepareStatement("SELECT db_id, height " + getDeletedColumnIfSupported() + " FROM " + table + " " + keyFactory.getPKClause())) {
                log.trace("Select 1. {} time: {} ms", table, System.currentTimeMillis() - startSelectTime);

                Set<Long> keysToDelete = new HashSet<>();
                while (rs.next()) {
                    keysToDelete.addAll( selectDbIds(selectDbIdStatement, rs) );
                }
                log.trace("Select 2. {} time: {} ms", table, System.currentTimeMillis() - startSelectTime);

                startDeleteTime = System.currentTimeMillis();
                if (keysToDelete.size() > 0) {
                    for (Long id : keysToDelete) {
                        deleted += deleteByDbId(pstmtDeleteById, id);
                        if (deleted % ShardConstants.DEFAULT_COMMIT_BATCH_SIZE == 0) {
                            dataSource.commit(false);
                        }
                    }
                    log.debug("Delete for table {} took {} ms", table, System.currentTimeMillis() - startDeleteTime);
                    dataSource.commit(false);
                    log.trace("Delete table '{}' in {} ms: deleted=[{}]",
                        table, System.currentTimeMillis() - startDeleteTime, deleted);
                }
            }
            long trimTime = System.currentTimeMillis() - startTime;
            if (trimTime > 10) {
                log.debug("Trim for table {} time {} ms", table, trimTime);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private String getDeletedColumnIfSupported() {
        return supportDelete() ? ", deleted" : "";
    }

    private String getDeletedSetStatementIfSupported(boolean deleted) {
        return supportDelete() ? ", deleted = " + deleted + " " : "";
    }

    private Set<Long> selectDbIds(PreparedStatement selectDbIdStatement, ResultSet rs) throws SQLException {
        DbKey dbKey = keyFactory.newKey(rs);
        dbKey.setPK(selectDbIdStatement);
        Set<Long> outputKeys = new HashSet<>();
        int maxHeight = rs.getInt("max_height");
        boolean lastDeleted = false;
        Set<Integer> deleteHeights = new HashSet<>();
        Set<Long> lastDbIds = new HashSet<>();
        try (ResultSet dbIdsSet = selectDbIdStatement.executeQuery()) {
            while (dbIdsSet.next()) {
                int currentHeight = dbIdsSet.getInt(2);
                boolean entryDeleted = supportDelete() && dbIdsSet.getBoolean(3);
                long dbId = dbIdsSet.getLong(1);
                if (currentHeight == maxHeight) {
                    lastDeleted = entryDeleted;
                    lastDbIds.add(dbId);
                } else if (currentHeight < maxHeight && currentHeight >= 0) {
                    if (entryDeleted) {
                        deleteHeights.add(currentHeight);
                    }
                    outputKeys.add(dbId);
                }
            }
        }
        // last existing record should be 'deleted' and paired with previously deleted records
        if (deleteHeights.size() % 2 != 0 && lastDeleted) {
            outputKeys.addAll(lastDbIds);
        }
        return outputKeys;
    }

    private int deleteByDbId(PreparedStatement pstmtDeleteByDbId, long dbId) throws SQLException {
        pstmtDeleteByDbId.setLong(1, dbId);
        return pstmtDeleteByDbId.executeUpdate();
    }
}
