/*
 * Copyright © 2018-2022 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.MinMaxValue;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.PrunableDbTable;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.shard.ShardConstants;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvEscaper;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvExportException;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvWriter;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvWriterImpl;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InvalidClassException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.core.shard.ShardConstants.BLOCK_INDEX_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.ShardConstants.BLOCK_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.ShardConstants.SHARD_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.ShardConstants.TRANSACTION_INDEX_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.ShardConstants.TRANSACTION_TABLE_NAME;

/**
 * {@inheritDoc}
 */
@Slf4j
@Singleton
public class CsvExporterImpl implements CsvExporter {

    private static final Set<String> DEFAULT_EXCLUDED_COLUMNS = Set.of("db_id", "latest", "deleted");
    private static final String EXPORT_TABLE_EXCEPTION_MESSAGE = "Exporting table exception %s";
    private final CsvEscaper translator;
    private Path dataExportPath; // path to folder with CSV files
    private DatabaseManager databaseManager;
    private Set<String> excludeTables; // skipped tables

    @Inject
    public CsvExporterImpl(DatabaseManager databaseManager, @Named("dataExportDir") Path dataExportPath, CsvEscaper translator) {
        Objects.requireNonNull(dataExportPath, "exportDirProducer 'data Path' is NULL");
        this.translator = Objects.requireNonNull(translator, "Csv escaper is NULL.");
        this.dataExportPath = dataExportPath;
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.excludeTables = Set.of(ShardConstants.GENESIS_PK_TABLE_NAME, ShardConstants.DATA_TAG_TABLE_NAME, ShardConstants.UNCONFIRMED_TX_TABLE_NAME);
    }

    @PostConstruct
    private void init() {
        try {
            Files.createDirectories(this.dataExportPath);
        } catch (IOException e) {
            throw new CsvExportException("Unable to create data export directory", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getDataExportPath() {
        return this.dataExportPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long exportDerivedTable(DerivedTableInterface derivedTableInterface, int targetHeight, int batchLimit, Set<String> excludedColumns) {
        return exportDerivedTableByUniqueLongColumnPagination(derivedTableInterface.getName(), derivedTableInterface.getMinMaxValue(targetHeight), batchLimit, excludedColumns);
    }

    @Override
    public long exportDerivedTableByUniqueLongColumnPagination(String table, MinMaxValue minMaxValue, int batchLimit, Set<String> excludedColumns) {
        log.trace("exportDerivedTableByUniqueLongColumnPagination: {}, minMaxValue = {}, batchLimit = {}, excludedColumns = {}",
            table, minMaxValue, batchLimit, excludedColumns);
        return exportTable(table, "WHERE  " + minMaxValue.getColumn() + " BETWEEN ? and ? and height <= ? ORDER BY " + minMaxValue.getColumn() + " LIMIT ?", minMaxValue, excludedColumns, (pstmt, minMaxColumnValue, totalProcessed) -> {
            pstmt.setBigDecimal(1, minMaxColumnValue.getMin());
            pstmt.setBigDecimal(2, minMaxColumnValue.getMax());
            pstmt.setInt(3, minMaxColumnValue.getHeight());
            pstmt.setInt(4, batchLimit);
        });
    }

    @Override
    public long exportDerivedTableCustomSort(DerivedTableInterface derivedTableInterface, int targetHeight, int batchLimit, Set<String> excludedColumns, String sortColumn) {
        log.trace("exportDerivedTableCustomSort: {}, targetHeight = {}, batchLimit = {}, excludedColumns = {}, sortColumn={}",
            derivedTableInterface, targetHeight, batchLimit, excludedColumns, sortColumn);
        return exportTable(derivedTableInterface.getName(), "WHERE height <= ? ORDER BY " + sortColumn + " LIMIT ? OFFSET ?", derivedTableInterface.getMinMaxValue(targetHeight), excludedColumns, (pstmt, minMaxId, totalProcessed) -> {
            pstmt.setInt(1, targetHeight);
            pstmt.setInt(2, batchLimit);
            pstmt.setInt(3, totalProcessed);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long exportDerivedTable(DerivedTableInterface derivedTableInterface, int targetHeight, int batchLimit) {
        return exportDerivedTable(derivedTableInterface, targetHeight, batchLimit, DEFAULT_EXCLUDED_COLUMNS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long exportPrunableDerivedTable(PrunableDbTable table, int targetHeight, int currentTime, int batchLimit) {
        return exportDerivedTableByUniqueLongColumnPagination(table.getName(), table.getMinMaxValue(targetHeight, currentTime), batchLimit, DEFAULT_EXCLUDED_COLUMNS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long exportShardTable(int targetHeight, int batchLimit) {
        int processedCount;
        int totalCount = 0;
        // prepare connection + statement + writer
        TransactionalDataSource dataSource = this.databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                 "SELECT * FROM " +
                     SHARD_TABLE_NAME + " WHERE shard_id > ? AND shard_height <= ? ORDER BY shard_id LIMIT ?");
             PreparedStatement countPstmt = con.prepareStatement(
                 "SELECT count(*) FROM " + SHARD_TABLE_NAME + " WHERE shard_height <= ?");
             CsvWriter csvWriter = new CsvWriterImpl(this.dataExportPath, Set.of("shard_state", "prunable_zip_hash"), translator)
        ) {
            csvWriter.setOptions("fieldDelimiter="); // do not remove! it deletes double quotes  around values in csv
            // select Min, Max DbId + rows count
            countPstmt.setInt(1, targetHeight);
            ResultSet countRs = countPstmt.executeQuery();
            countRs.next();
            int count = countRs.getInt(1);
            log.debug("Table = {}, count - {} at height = {}", SHARD_TABLE_NAME, count, targetHeight);

            // process non empty tables only
            if (count > 0) {
                long from = 0;
                do { // do exporting into csv with pagination
                    pstmt.setLong(1, from);
                    pstmt.setInt(2, targetHeight);
                    pstmt.setInt(3, batchLimit);
                    CsvExportData csvExportData = csvWriter.append(SHARD_TABLE_NAME, pstmt.executeQuery());
                    processedCount = csvExportData.getProcessCount();
                    if (processedCount > 0) {
                        from = (Long) csvExportData.getLastRow().get("shard_id");
                    }
                    totalCount += processedCount;
                } while (processedCount > 0); //keep processing while not found more rows
                log.debug("Table = {}, exported rows = {}", SHARD_TABLE_NAME, totalCount);
            } else {
                // skipped empty table
                log.debug("Skipped exporting Table = {}", SHARD_TABLE_NAME);
            }
        } catch (Exception e) {
            throw new CsvExportException(String.format(EXPORT_TABLE_EXCEPTION_MESSAGE, SHARD_TABLE_NAME), e);
        }
        return totalCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long exportShardTableIgnoringLastZipHashes(int targetHeight, int batchLimit) {
        int totalCount = 0;
        TransactionalDataSource dataSource = this.databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM "
                 + SHARD_TABLE_NAME + " WHERE shard_height <= ? ORDER BY shard_id DESC LIMIT 1")) {
            pstmt.setInt(1, targetHeight);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int height = rs.getInt("shard_height");
                    totalCount += exportShardTable(height - 1, batchLimit);
                    try (CsvWriter csvWriter = new CsvWriterImpl(dataExportPath, Set.of("shard_state", "prunable_zip_hash"), translator)) {
                        csvWriter.setOptions("fieldDelimiter="); // do not remove! it deletes double quotes  around values in csv
                        csvWriter.append(SHARD_TABLE_NAME, pstmt.executeQuery(), Map.of("zip_hash_crc", "null"));
                        totalCount += 1;
                    }
                } else {
                    log.debug("Skipped exporting Table = {}", SHARD_TABLE_NAME);
                }
                log.trace("Table = {}, exported rows = {}", SHARD_TABLE_NAME, totalCount);
            }
        } catch (Exception e) {
            throw new CsvExportException(String.format(EXPORT_TABLE_EXCEPTION_MESSAGE, SHARD_TABLE_NAME), e);
        }
        return totalCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long exportBlockIndex(int targetHeight, int batchLimit) {
        int blockProcessedCount;
        int blockTotalCount = 0;
        // prepare connection + statement + writer
        TransactionalDataSource dataSource = this.databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement blockPstm = con.prepareStatement(
                 "SELECT * FROM " + BLOCK_INDEX_TABLE_NAME + " WHERE block_height >= ? AND block_height < ? ORDER BY block_height limit ?");
             PreparedStatement blockCountPstm = con.prepareStatement("SELECT COUNT(*) FROM " + BLOCK_INDEX_TABLE_NAME);
             CsvWriter blockCsvWriter = new CsvWriterImpl(this.dataExportPath, null, translator)
        ) {
            blockCsvWriter.setOptions("fieldDelimiter="); // do not remove! it deletes double quotes  around values in csv

            // process non empty tables only
            int blockCount;
            try (ResultSet rs = blockCountPstm.executeQuery()) {
                rs.next();
                blockCount = rs.getInt(1);
            }
            if (blockCount > 0) {
                int fromHeight = 0;
                do { // do exporting into csv with pagination
                    blockPstm.setInt(1, fromHeight);
                    blockPstm.setInt(2, targetHeight);
                    blockPstm.setInt(3, batchLimit);
                    CsvExportData csvExportData = blockCsvWriter.append(BLOCK_INDEX_TABLE_NAME,
                        blockPstm.executeQuery());
                    blockProcessedCount = csvExportData.getProcessCount();
                    if (blockProcessedCount > 0) {
                        int lastHeight = (int) csvExportData.getLastRow().get("block_height");
                        fromHeight = lastHeight + 1;
                    }
                    blockTotalCount += blockProcessedCount;
                } while (blockProcessedCount > 0); //keep processing while not found more rows
                log.trace("Table = {}, exported rows = {}", BLOCK_INDEX_TABLE_NAME, blockTotalCount);
            } else {
                // skipped empty table
                log.debug("Skipped exporting Table = {}", BLOCK_INDEX_TABLE_NAME);
            }
        } catch (Exception e) {
            throw new CsvExportException(String.format(EXPORT_TABLE_EXCEPTION_MESSAGE, BLOCK_INDEX_TABLE_NAME), e);
        }
        return blockTotalCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long exportTransactionIndex(int targetHeight, int batchLimit) {
        MinMaxValue minMaxValue = new MinMaxValue(BigDecimal.valueOf(Long.MIN_VALUE), BigDecimal.valueOf(Long.MAX_VALUE), "transaction_id", 1, targetHeight - 1);
        TransactionalDataSource dataSource = this.databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement txCountPstm = con.prepareStatement("SELECT COUNT(*) FROM transaction_shard_index");
        ) {
            try (ResultSet rs = txCountPstm.executeQuery()) {
                rs.next();
                minMaxValue.setCount(rs.getInt(1));
                return exportDerivedTableByUniqueLongColumnPagination(TRANSACTION_INDEX_TABLE_NAME, minMaxValue, batchLimit, Set.of());
            }
        } catch (SQLException e) {
            throw new CsvExportException(String.format(EXPORT_TABLE_EXCEPTION_MESSAGE, TRANSACTION_INDEX_TABLE_NAME), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long exportTransactions(Collection<Long> dbIds, int height) {
        log.debug("Exporting 'transaction' by dbIds=[{}] on height = {}", dbIds != null ? dbIds.size() : -1, height);
        int processCount;
        int totalCount = 0;
        Objects.requireNonNull(dbIds, "dbIds list is NULL");
        // prepare connection + statement + writer
        List<Long> sortedDbIds = dbIds.stream().distinct().sorted(Comparator.naturalOrder()).collect(Collectors.toList());

        TransactionalDataSource dataSource = this.databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             // block related Txs
             PreparedStatement blockTxPstm = con.prepareStatement("SELECT * FROM "
                 + TRANSACTION_TABLE_NAME + " WHERE height = ? ORDER BY transaction_index");
             // phasing related Txs for inclusion
             PreparedStatement txPstm = con.prepareStatement(
                 "SELECT * FROM " + TRANSACTION_TABLE_NAME + " WHERE db_id = ?");
             CsvWriter txCsvWriter = new CsvWriterImpl(this.dataExportPath, Set.of("db_id"), translator)
        ) {
            txCsvWriter.setOptions("fieldDelimiter="); // do not remove! it deletes double quotes  around values in csv

            // transactions by snapshot block
            blockTxPstm.setInt(1, height);
            CsvExportData txExportData = txCsvWriter.append(TRANSACTION_TABLE_NAME, blockTxPstm.executeQuery());
            processCount = txExportData.getProcessCount(); // tx
            totalCount += processCount;

            // process non empty tables only
            if (!sortedDbIds.isEmpty()) {
                log.debug("Additional Export from Table = {} by dbIds = [{}]", TRANSACTION_TABLE_NAME, dbIds.size());
                for (Long dbId : sortedDbIds) {
                    txPstm.setLong(1, dbId);
                    CsvExportData csvExportData = txCsvWriter.append(TRANSACTION_TABLE_NAME,
                        txPstm.executeQuery());
                    processCount = csvExportData.getProcessCount();
                    totalCount += processCount;
                }
            } else {
                // skipped empty table
                log.debug("Nothing to export in Table = {} by sortedDbIds = [{}]", TRANSACTION_TABLE_NAME, sortedDbIds.size());
            }
            log.debug("Exported {}: totalCount = {}, count = {} by height = {}",
                TRANSACTION_TABLE_NAME, totalCount, processCount, height);
        } catch (Exception e) {
            throw new CsvExportException(String.format(EXPORT_TABLE_EXCEPTION_MESSAGE, TRANSACTION_TABLE_NAME), e);
        }
        return totalCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long exportBlock(int height) {
        log.debug("Exporting '{}' on height = {}", BLOCK_TABLE_NAME, height);
        int processCount;
        TransactionalDataSource dataSource = this.databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement blockPstm = con.prepareStatement(
                 "SELECT * FROM " + BLOCK_TABLE_NAME + " WHERE height = ?");
             CsvWriter blockCsvWriter = new CsvWriterImpl(this.dataExportPath, Set.of("db_id"), translator);
        ) {
            blockCsvWriter.setOptions("fieldDelimiter="); // do not remove! it deletes double quotes  around values in csv

            blockPstm.setInt(1, height);
            CsvExportData blockExportData = blockCsvWriter.append(BLOCK_TABLE_NAME, blockPstm.executeQuery());
            if (blockExportData.getProcessCount() != 1) {
                Files.deleteIfExists(dataExportPath.resolve(BLOCK_TABLE_NAME + ".csv"));
                throw new IllegalStateException("Expected one exported block, got " + blockExportData.getProcessCount());
            }
            processCount = blockExportData.getProcessCount(); // block
            log.debug("Exported {}: count = {} by height = {}", BLOCK_TABLE_NAME, processCount, height);
        } catch (SQLException | IOException e) {
            throw new CsvExportException(String.format(EXPORT_TABLE_EXCEPTION_MESSAGE, BLOCK_TABLE_NAME), e);
        }
        return processCount;
    }

    private long exportTable(String table, String condition, MinMaxValue minMaxValue, Set<String> excludedColumns, StatementConfigurator statementConfigurator) {
        Objects.requireNonNull(condition, "Condition sql should not be null");
        Objects.requireNonNull(table, "Table should not be null");
        Objects.requireNonNull(minMaxValue, "MinMaxValue should not be null");
        log.trace("exportTable: {}, = {}, minMaxValue = {}, excludedColumns = {}",
            table, condition, minMaxValue, excludedColumns);
        // skip hard coded table
        if (excludeTables.contains(table.toLowerCase())) {
            // skip not needed table
            log.debug("Skipped excluded Table = {}", table);
            return -1;
        }

        long start = System.currentTimeMillis();
        int processedCount;
        int totalCount = 0;
        // prepare connection + statement + writer
        String sql = "SELECT * FROM " + table + " " + condition;
        TransactionalDataSource dataSource = this.databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql);
             CsvWriter csvWriter = new CsvWriterImpl(this.dataExportPath, excludedColumns, translator)
        ) {
            csvWriter.setOptions("fieldDelimiter="); // do not remove! it deletes double quotes  around values in csv
            // select Min, Max DbId + rows count
            log.debug("Table = {}, Min/Max = {}, sql = {}", table, minMaxValue, sql);

            // process non empty tables only
            if (minMaxValue.getCount() > 0) {
                do { // do exporting into csv with pagination
                    statementConfigurator.configure(pstmt, minMaxValue, totalCount);
                    CsvExportData csvExportData = csvWriter.append(table, pstmt.executeQuery());
                    processedCount = csvExportData.getProcessCount();
                    if (processedCount > 0) {
                        // sometimes DB_ID can have a SQL type mapped to BigDecimal or Long (depends on table)
                        Object rawObjectValue = csvExportData.getLastRow().get(minMaxValue.getColumn().toLowerCase());
                        if (rawObjectValue instanceof Long) {
                            Long longValue = (Long) rawObjectValue;
                            BigDecimal bidDecimalId = new BigDecimal(longValue);
                            bidDecimalId = bidDecimalId.add(BigDecimal.ONE);
                            minMaxValue.setMin(bidDecimalId);
                        } else if (rawObjectValue instanceof BigInteger) {
                            BigDecimal bidDecimalId = new BigDecimal((BigInteger) rawObjectValue);
                            bidDecimalId = bidDecimalId.add(BigDecimal.ONE);
                            minMaxValue.setMin(bidDecimalId);
                        } else {
                            String error = String.format(
                                "BigDecimal or Long should be present for DB_ID column, but here are data:" +
                                    " Java class: '%s', column name: '%s', table value=%d in table=%s",
                                rawObjectValue.getClass(), minMaxValue.getColumn().toLowerCase(), rawObjectValue, table);
                            log.error(error);
                            throw new InvalidClassException(error);
                        }
                    }
                    totalCount += processedCount;
                } while (processedCount > 0); //keep processing while not found more rows
                log.debug("Table = {}, exported rows = {} in {} sec", table, totalCount,
                    (System.currentTimeMillis() - start) / 1000);
            } else {
                // skipped empty table
                log.debug("Skipped exporting Table = {} because min/max count = {}", table, minMaxValue);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new CsvExportException("Exporting derived table exception: " + table, e);
        }

        return totalCount;
    }

    private interface StatementConfigurator {
        void configure(PreparedStatement pstmt, MinMaxValue minMaxValue, int totalProcessed) throws SQLException;
    }
}
