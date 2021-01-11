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
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.blockchain;

import com.apollocurrency.aplwallet.api.v2.model.TxReceipt;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.converter.db.PrunableTxRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TxReceiptRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionEntity;
import com.apollocurrency.aplwallet.apl.core.model.TransactionDbInfo;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.transaction.PrunableTransaction;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.ARBITRARY_MESSAGE;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.PRIVATE_PAYMENT;

@Slf4j
@Singleton
public class TransactionDaoImpl implements TransactionDao {
    private final TxReceiptRowMapper txReceiptRowMapper;
    private final TransactionEntityRowMapper entityRowMapper;
    private final PrunableTxRowMapper prunableTxRowMapper;
    private final DatabaseManager databaseManager;

    @Inject
    public TransactionDaoImpl(TxReceiptRowMapper txReceiptRowMapper, TransactionEntityRowMapper entityRowMapper, PrunableTxRowMapper prunableTxRowMapper, DatabaseManager databaseManager) {
        this.txReceiptRowMapper = txReceiptRowMapper;
        this.entityRowMapper = entityRowMapper;
        this.prunableTxRowMapper = prunableTxRowMapper;
        this.databaseManager = databaseManager;
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionEntity findTransaction(long transactionId, TransactionalDataSource dataSource) {
        return findTransaction(transactionId, Integer.MAX_VALUE, dataSource);
    }

    @Transactional(readOnly = true)
    @Override
    public TransactionEntity findTransaction(long transactionId, int height, TransactionalDataSource dataSource) {
        // Check the block cache
        // Search the database
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction WHERE id = ?")) {
            pstmt.setLong(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getInt("height") <= height) {
                    return entityRowMapper.mapWithException(rs, null);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } catch (AplException.ValidationException e) {
            throw new RuntimeException("Transaction already in database, id = " + transactionId + ", does not pass validation!", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionEntity findTransactionByFullHash(byte[] fullHash, TransactionalDataSource dataSource) {
        return findTransactionByFullHash(fullHash, Integer.MAX_VALUE, dataSource);
    }

    @Transactional(readOnly = true)
    @Override
    public TransactionEntity findTransactionByFullHash(byte[] fullHash, int height, TransactionalDataSource dataSource) {
        long transactionId = Convert.fullHashToId(fullHash);
        // Check the cache
        // Search the database
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction WHERE id = ?")) {
            pstmt.setLong(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && Arrays.equals(rs.getBytes("full_hash"), fullHash) && rs.getInt("height") <= height) {
                    return entityRowMapper.mapWithException(rs, null);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } catch (AplException.ValidationException e) {
            throw new RuntimeException("Transaction already in database, full_hash = " + Convert.toHexString(fullHash)
                + ", does not pass validation!", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasTransaction(long transactionId, TransactionalDataSource dataSource) {
        return hasTransaction(transactionId, Integer.MAX_VALUE, dataSource);
    }

    @Transactional(readOnly = true)
    @Override
    public boolean hasTransaction(long transactionId, int height, TransactionalDataSource dataSource) {
        // Check the block cache
        // Search the database
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT height FROM transaction WHERE id = ?")) {
            pstmt.setLong(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt("height") <= height;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasTransactionByFullHash(byte[] fullHash, TransactionalDataSource dataSource) {
        return Arrays.equals(fullHash, getFullHash(Convert.fullHashToId(fullHash), dataSource));
    }

    @Transactional(readOnly = true)
    @Override
    public boolean hasTransactionByFullHash(byte[] fullHash, int height, TransactionalDataSource dataSource) {
        long transactionId = Convert.fullHashToId(fullHash);
        // Check the block cache
        // Search the database
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT full_hash, height FROM transaction WHERE id = ?")) {
            pstmt.setLong(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && Arrays.equals(rs.getBytes("full_hash"), fullHash) && rs.getInt("height") <= height;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public byte[] getFullHash(long transactionId, TransactionalDataSource dataSource) {
        // Check the block cache
        // Search the database
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT full_hash FROM transaction WHERE id = ?")) {
            pstmt.setLong(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getBytes("full_hash") : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionEntity> findBlockTransactions(long blockId, TransactionalDataSource dataSource) {
        // Check the block cache
        // Search the database
        try (Connection con = dataSource.getConnection()) {
            return findBlockTransactions(con, blockId);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getBlockTransactionsCount(long blockId, TransactionalDataSource dataSource) {
        // Check the block cache
        // Search the database
        long transactionCount = 0L;
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT count(*) as transactionCount FROM transaction WHERE block_id = ?")) {
            pstmt.setLong(1, blockId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    transactionCount = rs.getLong("transactionCount");
                }
                return transactionCount;
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private List<TransactionEntity> findBlockTransactions(Connection con, long blockId) {
        try (PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction WHERE block_id = ? ORDER BY transaction_index")) {
            pstmt.setLong(1, blockId);
            pstmt.setFetchSize(50);
            try (ResultSet rs = pstmt.executeQuery()) {
                List<TransactionEntity> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(entityRowMapper.mapWithException(rs, null));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } catch (AplException.ValidationException e) {
            throw new RuntimeException("Transaction already in database for block_id = " + Long.toUnsignedString(blockId)
                + " does not pass validation!", e);
        }
    }

    @Override
    public List<PrunableTransaction> findPrunableTransactions(int minTimestamp, int maxTimestamp) {
        List<PrunableTransaction> result = new ArrayList<>();
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT id, `type`, subtype, "
                 + "has_prunable_attachment AS prunable_attachment, "
                 + "has_prunable_message AS prunable_plain_message, "
                 + "has_prunable_encrypted_message AS prunable_encrypted_message "
                 + "FROM transaction WHERE (`timestamp` BETWEEN ? AND ?) AND "
                 + "(has_prunable_attachment = TRUE OR has_prunable_message = TRUE OR has_prunable_encrypted_message = TRUE)")) {
            pstmt.setInt(1, minTimestamp);
            pstmt.setInt(2, maxTimestamp);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(prunableTxRowMapper.map(rs, null));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return result;
    }

    @Override
    public void saveTransactions(List<TransactionEntity> transactions) {
        try {
            TransactionalDataSource dataSource = databaseManager.getDataSource();
            short index = 0;
            for (TransactionEntity transaction : transactions) {
                try (Connection con = dataSource.getConnection();
                     PreparedStatement pstmt = con.prepareStatement("INSERT INTO transaction (id, deadline, "
                         + "recipient_id, amount, fee, referenced_transaction_full_hash, height, "
                         + "block_id, signature, `timestamp`, type, subtype, sender_id, sender_public_key, attachment_bytes, "
                         + "block_timestamp, full_hash, version, has_message, has_encrypted_message, has_public_key_announcement, "
                         + "has_encrypttoself_message, phased, has_prunable_message, has_prunable_encrypted_message, "
                         + "has_prunable_attachment, ec_block_height, ec_block_id, transaction_index) "
                         + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    int i = 0;
                    pstmt.setLong(++i, transaction.getId());
                    pstmt.setShort(++i, transaction.getDeadline());
                    DbUtils.setLongZeroToNull(pstmt, ++i, transaction.getRecipientId());
                    pstmt.setLong(++i, transaction.getAmountATM());
                    pstmt.setLong(++i, transaction.getFeeATM());
                    DbUtils.setBytes(pstmt, ++i, transaction.getReferencedTransactionFullHash());
                    pstmt.setInt(++i, transaction.getHeight());
                    pstmt.setLong(++i, transaction.getBlockId());
                    pstmt.setBytes(++i, transaction.getSignatureBytes());
                    pstmt.setInt(++i, transaction.getTimestamp());
                    pstmt.setByte(++i, transaction.getType());
                    pstmt.setByte(++i, transaction.getSubtype());
                    pstmt.setLong(++i, transaction.getSenderId());
                    pstmt.setBytes(++i, transaction.getSenderPublicKey());
                    pstmt.setBytes(++i, transaction.getAttachmentBytes());
                    pstmt.setInt(++i, transaction.getBlockTimestamp());
                    pstmt.setBytes(++i, transaction.getFullHash());
                    pstmt.setByte(++i, transaction.getVersion());
                    pstmt.setBoolean(++i, transaction.isHasMessage());
                    pstmt.setBoolean(++i, transaction.isHasEncryptedMessage());
                    pstmt.setBoolean(++i, transaction.isHasPublicKeyAnnouncement());
                    pstmt.setBoolean(++i, transaction.isHasEncryptToSelfMessage());
                    pstmt.setBoolean(++i, transaction.isPhased());
                    pstmt.setBoolean(++i, transaction.isHasPrunableMessage());
                    pstmt.setBoolean(++i, transaction.isHasPrunableEencryptedMessage());
                    pstmt.setBoolean(++i, transaction.isHasPrunableAttachment());
                    pstmt.setInt(++i, transaction.getEcBlockHeight());
                    DbUtils.setLongZeroToNull(pstmt, ++i, transaction.getEcBlockId());
                    pstmt.setShort(++i, index++);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public synchronized int getTransactionCount() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM transaction");
             ResultSet rs = pstmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public synchronized Long getTransactionCount(TransactionalDataSource dataSource, int from, int to) {
        if (dataSource == null) {
            // select from main db
            dataSource = databaseManager.getDataSource();
        }
        try (Connection con = dataSource.getConnection()) {
            PreparedStatement pstmt = con.prepareStatement("SELECT count(*) FROM transaction WHERE height >= ? AND height < ?");
            pstmt.setInt(1, from);
            pstmt.setInt(2, to);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return 0L;
    }

    @Override
    public synchronized List<TransactionEntity> getTransactions(
        TransactionalDataSource dataSource,
        long accountId, int numberOfConfirmations, byte type, byte subtype,
        int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
        int from, int to, boolean includeExpiredPrunable, boolean executedOnly, boolean includePrivate,
        int height, int prunableExpiration) {
        validatePhaseAndNonPhasedTransactions(phasedOnly, nonPhasedOnly);

        StringBuilder buf = new StringBuilder();
        buf.append("SELECT transaction.* FROM transaction ");
        createTransactionSelectSqlWithOrder(buf, "transaction.*", type, subtype,
            blockTimestamp, withMessage, phasedOnly, nonPhasedOnly, executedOnly, includePrivate, height);
        buf.append(DbUtils.limitsClause(from, to)); // append 'limit offset' clause
        try (Connection con = dataSource.getConnection()) {
            String sql = buf.toString();
            log.trace("getTx sql = {}\naccountId={}, from={}, to={}", sql, accountId, from, to);
            PreparedStatement pstmt = con.prepareStatement(sql);
            int i = setStatement(pstmt, accountId, numberOfConfirmations, type, subtype, blockTimestamp,
                withMessage, phasedOnly, nonPhasedOnly, includeExpiredPrunable, executedOnly, includePrivate, height, prunableExpiration);
            DbUtils.setLimits(++i, pstmt, from, to); // // append 'limit offset' clauese values
            return getTransactions(con, pstmt);
        } catch (SQLException e) {
            log.error("ERROR on DataSource = {}", dataSource.getDbIdentity());
            throw new RuntimeException(e.toString(), e);
        }
    }

    private StringBuilder createTransactionSelectSqlNoOrder(StringBuilder buf, String selectString, byte type, byte subtype, int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly, boolean executedOnly, boolean includePrivate, int height) {
        if (executedOnly && !nonPhasedOnly) {
            buf.append(" LEFT JOIN phasing_poll_result ON transaction.id = phasing_poll_result.id ");
        }
        buf.append("WHERE recipient_id = ? AND sender_id <> ? ");
        if (blockTimestamp > 0) {
            buf.append("AND block_timestamp >= ? ");
        }
        if (!includePrivate && type == PRIVATE_PAYMENT.getType() && subtype == PRIVATE_PAYMENT.getSubtype()) {
            throw new RuntimeException("None of private transactions should be retrieved!");
        }
        if (type >= 0) {
            buf.append("AND `type` = ? ");
            if (subtype >= 0) {
                buf.append("AND subtype = ? ");
            }
        }
        if (!includePrivate) {
            buf.append("AND (`type` <> ? ");
            buf.append("OR subtype <> ? ) ");
        }
        if (height < Integer.MAX_VALUE) {
            buf.append("AND transaction.height <= ? ");
        }
        if (withMessage) {
            buf.append("AND (has_message = TRUE OR has_encrypted_message = TRUE ");
            buf.append("OR ((has_prunable_message = TRUE OR has_prunable_encrypted_message = TRUE) AND `timestamp` > ?)) ");
        }
        if (phasedOnly) {
            buf.append("AND phased = TRUE ");
        } else if (nonPhasedOnly) {
            buf.append("AND phased = FALSE ");
        }
        if (executedOnly && !nonPhasedOnly) {
            buf.append("AND (phased = FALSE OR approved = TRUE) ");
        }
        buf.append("UNION ALL SELECT ").append(selectString).append(" FROM transaction ");
        if (executedOnly && !nonPhasedOnly) {
            buf.append(" LEFT JOIN phasing_poll_result ON transaction.id = phasing_poll_result.id ");
        }
        buf.append("WHERE sender_id = ? ");
        if (blockTimestamp > 0) {
            buf.append("AND block_timestamp >= ? ");
        }
        if (type >= 0) {
            buf.append("AND `type` = ? ");
            if (subtype >= 0) {
                buf.append("AND subtype = ? ");
            }
        }
        if (!includePrivate) {
            buf.append("AND (`type` <> ? ");
            buf.append("OR subtype <> ? ) ");
        }
        if (height < Integer.MAX_VALUE) {
            buf.append("AND transaction.height <= ? ");
        }
        if (withMessage) {
            buf.append("AND (has_message = TRUE OR has_encrypted_message = TRUE OR has_encrypttoself_message = TRUE ");
            buf.append("OR ((has_prunable_message = TRUE OR has_prunable_encrypted_message = TRUE) AND `timestamp` > ?)) ");
        }
        if (phasedOnly) {
            buf.append("AND phased = TRUE ");
        } else if (nonPhasedOnly) {
            buf.append("AND phased = FALSE ");
        }
        if (executedOnly && !nonPhasedOnly) {
            buf.append("AND (phased = FALSE OR approved = TRUE) ");
        }
        return buf;
    }

    private StringBuilder createTransactionSelectSqlWithOrder(StringBuilder buf, String selectString, byte type, byte subtype, int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly, boolean executedOnly, boolean includePrivate, int height) {
        createTransactionSelectSqlNoOrder(buf, selectString, type, subtype, blockTimestamp, withMessage, phasedOnly, nonPhasedOnly, executedOnly, includePrivate, height);
        buf.append("ORDER BY block_timestamp DESC, transaction_index DESC");
        return buf;
    }

    private void validatePhaseAndNonPhasedTransactions(boolean phasedOnly, boolean nonPhasedOnly) {
        if (phasedOnly && nonPhasedOnly) {
            throw new IllegalArgumentException("At least one of phasedOnly or nonPhasedOnly must be false");
        }
    }

    @Override
    public synchronized int getTransactionCountByFilter(
        TransactionalDataSource dataSource, long accountId,
        int numberOfConfirmations, byte type, byte subtype, int blockTimestamp, boolean withMessage, boolean phasedOnly,
        boolean nonPhasedOnly, boolean includeExpiredPrunable, boolean executedOnly,
        boolean includePrivate, int height, int prunableExpiration) {
        validatePhaseAndNonPhasedTransactions(phasedOnly, nonPhasedOnly);
        @DatabaseSpecificDml(DmlMarker.NAMED_SUB_SELECT)
        StringBuilder buf = new StringBuilder();
        buf.append("SELECT count(*) FROM (SELECT transaction.id FROM transaction ");
        createTransactionSelectSqlNoOrder(buf, "transaction.id", type, subtype, blockTimestamp, withMessage, phasedOnly, nonPhasedOnly, executedOnly, includePrivate, height);
        buf.append(") AS tr_id_count");
        String sql = buf.toString();
        log.trace(sql);
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            log.trace("getTxCount sql = {}\naccountId={}, dataSource={}", sql, accountId, dataSource.getDbIdentity());
            setStatement(pstmt, accountId, numberOfConfirmations, type, subtype, blockTimestamp, withMessage, phasedOnly, nonPhasedOnly, includeExpiredPrunable, executedOnly, includePrivate, height, prunableExpiration);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("sql: " + sql + ", " + e.toString(), e);
        }
    }

    private int setStatement(PreparedStatement pstmt, long accountId, int numberOfConfirmations, byte type, byte subtype, int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly, boolean includeExpiredPrunable, boolean executedOnly, boolean includePrivate, int height, int prunableExpiration) throws SQLException {
        int i = 0;
        pstmt.setLong(++i, accountId);
        pstmt.setLong(++i, accountId);
        if (blockTimestamp > 0) {
            pstmt.setInt(++i, blockTimestamp);
        }
        if (type >= 0) {
            pstmt.setByte(++i, type);
            if (subtype >= 0) {
                pstmt.setByte(++i, subtype);
            }
        }
        if (!includePrivate) {
            pstmt.setByte(++i, PRIVATE_PAYMENT.getType());
            pstmt.setByte(++i, PRIVATE_PAYMENT.getSubtype());
        }
        if (height < Integer.MAX_VALUE) {
            pstmt.setInt(++i, height);
        }
        if (withMessage) {
            pstmt.setInt(++i, prunableExpiration);
        }
        // bind all the same parameters doe second part sql after 'UNION ALL'
        pstmt.setLong(++i, accountId);
        if (blockTimestamp > 0) {
            pstmt.setInt(++i, blockTimestamp);
        }
        if (type >= 0) {
            pstmt.setByte(++i, type);
            if (subtype >= 0) {
                pstmt.setByte(++i, subtype);
            }
        }
        if (!includePrivate) {
            pstmt.setByte(++i, PRIVATE_PAYMENT.getType());
            pstmt.setByte(++i, PRIVATE_PAYMENT.getSubtype());
        }
        if (height < Integer.MAX_VALUE) {
            pstmt.setInt(++i, height);
        }
        if (withMessage) {
            pstmt.setInt(++i, prunableExpiration);
        }
        return i;
    }

    @Override
    public synchronized List<TransactionEntity> getTransactions(byte type, byte subtype, int from, int to) {
        StringBuilder sqlQuery = new StringBuilder("SELECT * FROM transaction WHERE (type <> ? OR subtype <> ?) ");
        if (type >= 0) {
            sqlQuery.append("AND type = ? ");
            if (subtype >= 0) {
                sqlQuery.append("AND subtype = ? ");
            }
        }
        sqlQuery.append("ORDER BY block_timestamp DESC, transaction_index DESC ");
        sqlQuery.append(DbUtils.limitsClause(from, to));
        TransactionalDataSource dataSource = databaseManager.getDataSource();

        try (Connection con = dataSource.getConnection();
            PreparedStatement statement = con.prepareStatement(sqlQuery.toString())) {
            int i = 0;
            statement.setByte(++i, PRIVATE_PAYMENT.getType());
            statement.setByte(++i, PRIVATE_PAYMENT.getSubtype());
            if (type >= 0) {
                statement.setByte(++i, type);
                if (subtype >= 0) {
                    statement.setByte(++i, subtype);
                }
            }
            DbUtils.setLimits(++i, statement, from, to);
            return getTransactions(con, statement);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public List<TransactionEntity> getTransactionsChatHistory(long account1, long account2, int from, int to) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(
            "SELECT * from transaction "
                + "where type = ? and subtype = ? and ((sender_id =? and recipient_id = ?) or  (sender_id =? and recipient_id = ?)) "
                + "order by timestamp desc"
                + DbUtils.limitsClause(from, to))) {
            int i = 0;
            stmt.setByte(++i, ARBITRARY_MESSAGE.getType());
            stmt.setByte(++i, ARBITRARY_MESSAGE.getSubtype());
            stmt.setLong(++i, account1);
            stmt.setLong(++i, account2);
            stmt.setLong(++i, account2);
            stmt.setLong(++i, account1);
            DbUtils.setLimits(++i, stmt, from, to);
            return getTransactions(conn, stmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public synchronized List<TransactionEntity> getTransactions(int fromDbId, int toDbId) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM transaction where DB_ID >= ? and DB_ID < ? order by height asc, transaction_index asc")) {
            pstmt.setLong(1, fromDbId);
            pstmt.setLong(2, toDbId);
            return getTransactions(conn, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public synchronized List<TransactionDbInfo> getTransactionsBeforeHeight(int height) {
        List<TransactionDbInfo> result = new ArrayList<>();
        try (Connection con = databaseManager.getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT db_id, id FROM transaction WHERE height < ? ORDER BY db_id")) {
            pstmt.setInt(1, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new TransactionDbInfo(rs.getLong("db_id"), rs.getLong("id")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return result;
    }


    @Override
    public synchronized int getTransactionCount(long accountId, byte type, byte subtype) {
        StringBuilder sqlQuery = new StringBuilder("SELECT COUNT(*) FROM transaction WHERE (type <> ? OR subtype <> ?) AND (sender_id = ? OR recipient_id = ?) ");
        if (type >= 0) {
            sqlQuery.append("AND type = ? ");
            if (subtype >= 0) {
                sqlQuery.append("AND subtype = ? ");
            }
        }
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement statement = con.prepareStatement(sqlQuery.toString())) {
            int i = 0;
            statement.setByte(++i, PRIVATE_PAYMENT.getType());
            statement.setByte(++i, PRIVATE_PAYMENT.getSubtype());
            statement.setLong(++i, accountId);
            statement.setLong(++i, accountId);
            if (type >= 0) {
                statement.setByte(++i, type);
                if (subtype >= 0) {
                    statement.setByte(++i, subtype);
                }
            }
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public List<TransactionEntity> getTransactions(Connection con, PreparedStatement pstmt) {
        List<TransactionEntity> transactions = new ArrayList<>();
        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                TransactionEntity transaction = entityRowMapper.mapWithException(rs, null);
                transactions.add(transaction);
            }
        } catch (SQLException | AplException.NotValidException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return transactions;
    }

    @Override
    public synchronized int getTransactionsCount(List<Long> accounts, byte type, byte subtype,
                                                 int startTime, int endTime,
                                                 int fromHeight, int toHeight,
                                                 String sortOrder,
                                                 int from, int to) {
        StringBuilder sqlQuery = new StringBuilder("SELECT COUNT(*) FROM transaction tx ");
        sqlQuery.append("WHERE 1=1 ");

        createSelectTransactionQuery(sqlQuery, type, subtype, startTime, endTime, fromHeight, toHeight);

        addAccountsFilter(sqlQuery, accounts);

        log.trace("getTransactionsCount sql=[{}]", sqlQuery.toString());

        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement statement = con.prepareStatement(sqlQuery.toString())) {
            int i = setSelectTransactionQueryParams(statement, type, subtype, startTime, endTime, fromHeight, toHeight);
            ResultSet rs = statement.executeQuery();
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public synchronized List<TxReceipt> getTransactions(List<Long> accounts, byte type, byte subtype,
                                                        int startTime, int endTime,
                                                        int fromHeight, int toHeight,
                                                        String sortOrder,
                                                        int from, int to) {
        List<TxReceipt> result = new ArrayList<>();
        StringBuilder sqlQuery = new StringBuilder("SELECT version, type, subtype, id, sender_id, recipient_id, " +
            "signature, `timestamp`, amount, fee, height, block_id, block_timestamp, transaction_index, " +
            "attachment_bytes, has_message " +
            "FROM transaction ");
        sqlQuery.append("WHERE 1=1 ");
        createSelectTransactionQuery(sqlQuery, type, subtype, startTime, endTime, fromHeight, toHeight);

        addAccountsFilter(sqlQuery, accounts);

        sqlQuery.append("ORDER BY block_timestamp " + sortOrder + ", transaction_index " + sortOrder + " ");

        sqlQuery.append(DbUtils.limitsClause(from, to));

        log.trace("getTransactions sql=[{}]", sqlQuery.toString());

        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement statement = con.prepareStatement(sqlQuery.toString())) {
            int i = setSelectTransactionQueryParams(statement, type, subtype, startTime, endTime, fromHeight, toHeight);
            DbUtils.setLimits(++i, statement, from, to);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    TxReceipt receipt = txReceiptRowMapper.map(rs, null);
                    result.add(receipt);
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private StringBuilder createSelectTransactionQuery(StringBuilder buf, byte type, byte subtype,
                                                       int startTime, int endTime,
                                                       int fromHeight, int toHeight) {
        if (type >= 0) {
            buf.append("AND type = ? ");
            if (subtype >= 0) {
                buf.append("AND subtype = ? ");
            }
        }
        if (startTime > 0) {
            buf.append("AND block_timestamp >= ? ");
        }
        if (endTime > 0) {
            buf.append("AND block_timestamp <= ? ");
        }
        if (fromHeight > 0) {
            buf.append("AND height >= ? ");
        }
        if (toHeight > 0) {
            buf.append("AND height <= ? ");
        }
        return buf;
    }

    private StringBuilder addAccountsFilter(StringBuilder buf, List<Long> accounts) {
        if (accounts != null && !accounts.isEmpty()) {
            if (accounts.size() > 1) {
                StringBuilder accBuf = new StringBuilder("[");
                boolean first = true;
                for (Long accountId : accounts) {
                    if (accountId != 0) {
                        if (!first) {
                            accBuf.append(",");
                        } else {
                            first = false;
                        }
                        accBuf.append(accountId);
                    }
                }
                accBuf.append("]");
                buf.append("AND ( sender_id IN ").append(accBuf).append(" OR recipient_id IN ").append(accBuf).append(")");
            } else {
                buf.append("AND sender_id=").append(accounts.get(0)).append(" OR recipient_id=").append(accounts.get(0));
            }
        }
        return buf;
    }

    private int setSelectTransactionQueryParams(PreparedStatement pstmt, byte type, byte subtype,
                                                int startTime, int endTime,
                                                int fromHeight, int toHeight) throws SQLException {
        int i = 0;
        if (type >= 0) {
            pstmt.setByte(++i, type);
            if (subtype >= 0) {
                pstmt.setByte(++i, subtype);
            }
        }
        if (startTime > 0) {
            pstmt.setInt(++i, startTime);
        }
        if (endTime > 0) {
            pstmt.setInt(++i, endTime);
        }
        if (fromHeight > 0) {
            pstmt.setInt(++i, fromHeight);
        }
        if (toHeight > 0) {
            pstmt.setInt(++i, toHeight);
        }
        return i;
    }
}
