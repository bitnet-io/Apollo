/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.blockchain;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.PrunableTxRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.TxReceiptRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ChatInfo;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionEntity;
import com.apollocurrency.aplwallet.apl.core.model.Sort;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.TransactionDbInfo;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.transaction.PrunableTransaction;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_0_ID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Slf4j
@Tag("slow")
@EnableWeld
class TransactionDaoTest extends DbContainerBaseTest {

    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();
    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer);
    BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    Chain chain = mock(Chain.class);

    {
        doReturn(chain).when(blockchainConfig).getChain();
    }

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from()
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(mock(Blockchain.class), Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(mock(PropertiesHolder.class), PropertiesHolder.class))
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class))
        .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
        .addBeans(MockBean.of(mock(TimeService.class), TimeService.class))
        .build();

    private TransactionModelToEntityConverter toEntityConverter;
    private TransactionEntityToModelConverter toModelConverter;

    private TransactionDao dao;
    private TransactionTestData td;

    @BeforeEach
    void setUp() {
        td = new TransactionTestData();

        dao = new TransactionDaoImpl(
            new TxReceiptRowMapper(td.getTransactionTypeFactory()),
            new TransactionEntityRowMapper(),
            new PrunableTxRowMapper(td.getTransactionTypeFactory()),
            extension.getDatabaseManager());

        toEntityConverter = new TransactionModelToEntityConverter();
        toModelConverter = new TransactionEntityToModelConverter(td.getTransactionTypeFactory(),
            new TransactionBuilderFactory(td.getTransactionTypeFactory(), blockchainConfig));
    }


    @Test
    void findByBlockId() {
        List<TransactionEntity> transactions = dao.findBlockTransactions(BLOCK_0_ID, extension.getDatabaseManager().getDataSource());
        assertNotNull(transactions);
        assertEquals(2, transactions.size());
    }

    @Test
    void findTransactionId() {
        TransactionEntity transaction = dao.findTransaction(td.TRANSACTION_0.getId(), extension.getDatabaseManager().getDataSource());
        assertNotNull(transaction);
        assertEquals(td.TRANSACTION_0.getId(), transaction.getId());
    }

    @Test
    void findTransactionIdHeight() {
        TransactionEntity transaction = dao.findTransaction(td.TRANSACTION_1.getId(), td.TRANSACTION_1.getHeight(), extension.getDatabaseManager().getDataSource());
        assertNotNull(transaction);
        assertEquals(td.TRANSACTION_1.getId(), transaction.getId());
    }

    @Test
    void findTransactionByFullHash() {
        TransactionEntity transaction = dao.findTransactionByFullHash(td.TRANSACTION_5.getFullHash(), td.TRANSACTION_5.getHeight(), extension.getDatabaseManager().getDataSource());
        assertNotNull(transaction);
        assertEquals(td.TRANSACTION_5.getId(), transaction.getId());
    }

    @Test
    void testFindTransactionByFullHashNotExist() {
        TransactionEntity tx = dao.findTransactionByFullHash(new byte[32], Integer.MAX_VALUE, extension.getDatabaseManager().getDataSource());

        assertNull(tx, "Transaction with zero hash should not exist");
    }

    @Test
    void testFindTransactionByIdNotExist() {
        TransactionEntity tx = dao.findTransaction(Integer.MIN_VALUE, Integer.MAX_VALUE, extension.getDatabaseManager().getDataSource());

        assertNull(tx, "Transaction with Integer.MIN_VALUE id should not exist");
    }

    @Test
    void testFindFailedTransactionById() {
        TransactionEntity tx = dao.findTransaction(td.TRANSACTION_10.getId(), extension.getDatabaseManager().getDataSource());

        assertNotNull(tx, "Transaction with id " + td.TRANSACTION_10.getId() + " should be present");
        assertEquals(td.TRANSACTION_10.getId(), tx.getId());
        assertEquals(td.TRANSACTION_10.getErrorMessage().orElseThrow(()->
            new IllegalStateException("Test data inconsistency, transaction #10 should have error message")), tx.getErrorMessage());
    }

    @Test
    void hasTransactionBy() {
        boolean isFound = dao.hasTransaction(td.TRANSACTION_5.getId(), td.TRANSACTION_5.getHeight(), extension.getDatabaseManager().getDataSource());
        assertTrue(isFound);
    }

    @Test
    void hasTransactionByFullHash() {
        boolean isFound = dao.hasTransactionByFullHash(td.TRANSACTION_5.getFullHash(), td.TRANSACTION_5.getHeight(), extension.getDatabaseManager().getDataSource());
        assertTrue(isFound);
    }

    @Test
    void getFullHash() {
        byte[] fullHash = dao.getFullHash(td.TRANSACTION_5.getId(), extension.getDatabaseManager().getDataSource());
        assertNotNull(fullHash);
        assertArrayEquals(td.TRANSACTION_5.getFullHash(), fullHash);
    }

    @Test
    void getTransactionCount() {
        int count = dao.getTransactionCount();
        assertEquals(15, count);

        long countLong = dao.getTransactionCount(null, 0, 8000);
        assertEquals(7, countLong);
    }

    @Test
    void getTransactionsFromDbToDb() {
        List<TransactionEntity> result = dao.getTransactions((int) td.DB_ID_0, (int) td.DB_ID_9);
        assertNotNull(result);
        assertEquals(9, result.size());
    }

    @Test
    void getTransactionsFromAccount() {
        int count = dao.getTransactionCount(9211698109297098287L, (byte) 0, (byte) 0);
        assertEquals(8, count);
    }

    @Test
    void testFindTransactionByFullHashWithDataSource() {
        TransactionEntity tx = dao.findTransactionByFullHash(td.TRANSACTION_6.getFullHash(), extension.getDatabaseManager().getDataSource());

        assertArrayEquals(td.TRANSACTION_6.getFullHash(), tx.getFullHash());
    }

    @Test
    void testHasTransactionByIdWithDataSource() {
        boolean hasTransaction = dao.hasTransaction(td.TRANSACTION_3.getId(), extension.getDatabaseManager().getDataSource());

        assertTrue(hasTransaction, "Transaction should exist");
    }

    @Test
    void testHasTransactionByFullHashWithDataSource() {
        boolean hasTransaction = dao.hasTransactionByFullHash(td.TRANSACTION_5.getFullHash(), extension.getDatabaseManager().getDataSource());

        assertTrue(hasTransaction, "Transaction should exist");
    }

    @Test
    void testFindPrunableTransactions() {
        List<Long> expectedIds = List.of(td.TRANSACTION_6.getId(), td.TRANSACTION_13.getId(), td.TRANSACTION_14.getId());

        DbUtils.inTransaction(extension, (con) -> {
            List<PrunableTransaction> prunableTransactions = dao.findPrunableTransactions(0, Integer.MAX_VALUE);
            assertEquals(expectedIds.size(), prunableTransactions.size());
            for (int i = 0; i < prunableTransactions.size(); i++) {
                assertEquals(expectedIds.get(i), prunableTransactions.get(i).getId());
            }
        });
    }

    @Test
    void testFindPrunableTransactionsWithTimestampOuterLimit() {
        List<Long> expectedIds = List.of(td.TRANSACTION_6.getId(), td.TRANSACTION_13.getId(), td.TRANSACTION_14.getId());

        DbUtils.inTransaction(extension, (con) -> {
            List<PrunableTransaction> prunableTransactions = dao.findPrunableTransactions(td.TRANSACTION_6.getTimestamp(), td.TRANSACTION_14.getTimestamp());
            assertEquals(expectedIds.size(), prunableTransactions.size());
            for (int i = 0; i < prunableTransactions.size(); i++) {
                assertEquals(expectedIds.get(i), prunableTransactions.get(i).getId());
            }
        });
    }

    @Test
    void testFindPrunableTransactionsWithTimestampInnerLimit() {
        DbUtils.inTransaction(extension, (con) -> {
            List<PrunableTransaction> prunableTransactions = dao.findPrunableTransactions(td.TRANSACTION_6.getTimestamp() + 1, td.TRANSACTION_14.getTimestamp() - 1);
            assertEquals(1, prunableTransactions.size());
            assertEquals(td.TRANSACTION_13.getId(), prunableTransactions.get(0).getId());
        });
    }

    @Test
    void testSaveTransactions() {
        DbUtils.inTransaction(extension, (con) -> dao.saveTransactions(toEntityConverter.convert(List.of(td.NEW_TRANSACTION_1, td.NEW_TRANSACTION_0))));

        List<TransactionEntity> blockTransactions = dao.findBlockTransactions(td.NEW_TRANSACTION_0.getBlockId(), extension.getDatabaseManager().getDataSource());
        List<Transaction> converted = toModelConverter.convert(blockTransactions);
        assertEquals(List.of(td.NEW_TRANSACTION_1, td.NEW_TRANSACTION_0), converted);
        Optional<String> newTxErrorMessageOpt = converted.get(1).getErrorMessage();
        assertTrue(newTxErrorMessageOpt.isPresent(), "Error message should be present for new transaction #0");
        assertEquals("New transaction #1 error message", newTxErrorMessageOpt.get());
    }

    @Test
    void testUpdateTransactions() {
        td.TRANSACTION_0.setFeeATM(1L);
        td.TRANSACTION_0.fail("Test error for update");
        DbUtils.inTransaction(extension, (con) -> dao.updateTransaction(toEntityConverter.convert(td.TRANSACTION_0)));

        TransactionEntity transactionEntity = dao.findTransaction(td.TRANSACTION_0.getId(), extension.getDatabaseManager().getDataSource());

        assertEquals(transactionEntity.getId(), td.TRANSACTION_0.getId());
        assertEquals(1L, transactionEntity.getFeeATM());
        assertEquals("Test error for update", transactionEntity.getErrorMessage());
    }

    @Test
    void testGetTransactionsByAccountId() {
        List<TransactionEntity> transactions = dao.getTransactions(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_1.getSenderId(), (byte) 8, (byte) -1, 0, false, false, false, 0, Integer.MAX_VALUE, false, true, Integer.MAX_VALUE, 0, false, false, Sort.desc());
        assertEquals(List.of(td.TRANSACTION_12, td.TRANSACTION_11, td.TRANSACTION_7), toModelConverter.convert(transactions));
    }

    @Test
    void testGetTransactionsWithPhasingOnlyAndNonPhasedOnly() {
        assertThrows(IllegalArgumentException.class, () -> dao.getTransactions(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_1.getSenderId(), (byte) 8, (byte) -1, 0, false, true, true, 0, Integer.MAX_VALUE, false, true, Integer.MAX_VALUE, 0, false, false, Sort.desc()));
    }

    @Test
    void testGetPrivateTransactionsWhenIncludePrivateIsFalse() {
        assertThrows(RuntimeException.class, () -> dao.getTransactions(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_1.getSenderId(),  (byte) 0, (byte) 1, 0, false, true, false, 0, Integer.MAX_VALUE, false, false, Integer.MAX_VALUE, 0, false, false, Sort.desc()));
    }

    @Test
    void testGetPhasedTransactions() {
        List<TransactionEntity> transactions = dao.getTransactions(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_1.getSenderId(), (byte) 0, (byte) 0, 0, false, true, false, 0, Integer.MAX_VALUE, false, true, Integer.MAX_VALUE, 0, false, false, Sort.desc());
        assertEquals(List.of(td.TRANSACTION_13), toModelConverter.convert(transactions));
    }

    @Test
    void testGetAllNotPhasedTransactionsWithPagination() {
        List<TransactionEntity> transactions = dao.getTransactions(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_1.getSenderId(),  (byte) 0, (byte) 0, 0, false, false, true, 1, 3, false, true, td.TRANSACTION_7.getHeight() - 1, 0, false, false, Sort.desc());
        assertEquals(List.of(td.TRANSACTION_5, td.TRANSACTION_4, td.TRANSACTION_3), toModelConverter.convert(transactions));
    }

    @Test
    void testGetExecutedOnlyTransactions() {
        List<TransactionEntity> transactions = dao.getTransactions(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_1.getSenderId(), (byte) 0, (byte) 0, td.TRANSACTION_3.getBlockTimestamp() + 1, false, false, false, 0, Integer.MAX_VALUE, true, false, Integer.MAX_VALUE, 0, false, false, Sort.desc());

        assertEquals(List.of(td.TRANSACTION_9, td.TRANSACTION_8, td.TRANSACTION_6, td.TRANSACTION_5, td.TRANSACTION_4), toModelConverter.convert(transactions));
    }

    @Test
    void testGetTransactionsWithMessage() {
        List<TransactionEntity> transactions = dao.getTransactions(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_1.getSenderId(), (byte) 0, (byte) 0, 0, true, false, false, 0, Integer.MAX_VALUE, false, true, Integer.MAX_VALUE, 0, false, false, Sort.desc());
        assertEquals(List.of(td.TRANSACTION_13), toModelConverter.convert(transactions));
        transactions = dao.getTransactions(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_14.getSenderId(), (byte) -1, (byte) -1, 0, true, false, false, 0, Integer.MAX_VALUE, false, false, Integer.MAX_VALUE, 0, false, false, Sort.desc());
        assertEquals(List.of(td.TRANSACTION_14), toModelConverter.convert(transactions));
    }

    @Test
    void testGetFailedOnlyTransactions() {
        List<TransactionEntity> transactions = dao.getTransactions(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_1.getSenderId(), (byte) -1, (byte) -1, 0, false, false, false, 0, Integer.MAX_VALUE, false, true, Integer.MAX_VALUE, 0, true, false, Sort.desc());

        assertEquals(List.of(td.TRANSACTION_11), toModelConverter.convert(transactions));


        transactions = dao.getTransactions(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_10.getSenderId(), (byte) -1, (byte) -1, 0, false, false, false, 0, Integer.MAX_VALUE, false, false, Integer.MAX_VALUE, 0, true, false, Sort.desc());

        assertEquals(List.of(td.TRANSACTION_10), toModelConverter.convert(transactions));
    }

    @Test
    void testGetNotFailedOnlyTransactions() {
        List<TransactionEntity> transactions = dao.getTransactions(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_1.getSenderId(), (byte) -1, (byte) -1, 0, false, false, false, 0, Integer.MAX_VALUE, false, true, Integer.MAX_VALUE, 0, false, true, Sort.desc());

        List<Transaction> expected = new ArrayList<>(List.of(td.TRANSACTION_0, td.TRANSACTION_1, td.TRANSACTION_2, td.TRANSACTION_3,
            td.TRANSACTION_4, td.TRANSACTION_5, td.TRANSACTION_6, td.TRANSACTION_7, td.TRANSACTION_8, td.TRANSACTION_9, td.TRANSACTION_12, td.TRANSACTION_13));
        Collections.reverse(expected);
        assertEquals(expected, toModelConverter.convert(transactions));


        transactions = dao.getTransactions(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_10.getSenderId(), (byte) -1, (byte) -1, 0, false, false, false, 0, Integer.MAX_VALUE, false, false, Integer.MAX_VALUE, 0, false, true, Sort.desc());

        assertEquals(List.of(), toModelConverter.convert(transactions));
    }

    @Test
    void testGetTransactions_withASCSort() {
        List<TransactionEntity> transactions = dao.getTransactions(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_2.getSenderId(), (byte) -1, (byte) -1, 0, false, false, false, 0, Integer.MAX_VALUE, false, true, Integer.MAX_VALUE, 0, false, false, Sort.asc());

        List<Transaction> expected = new ArrayList<>(List.of(td.TRANSACTION_0, td.TRANSACTION_1, td.TRANSACTION_2, td.TRANSACTION_3,
            td.TRANSACTION_4, td.TRANSACTION_5, td.TRANSACTION_6, td.TRANSACTION_7, td.TRANSACTION_8, td.TRANSACTION_9, td.TRANSACTION_11, td.TRANSACTION_12, td.TRANSACTION_13));
        assertEquals(expected, toModelConverter.convert(transactions));


        transactions = dao.getTransactions(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_2.getSenderId(), (byte) -1, (byte) -1, 0, false, false, false, 5, 7, false, false, Integer.MAX_VALUE, 0, false, false, Sort.asc());

        assertEquals(List.of(td.TRANSACTION_5, td.TRANSACTION_6, td.TRANSACTION_7), toModelConverter.convert(transactions));
    }

    @Test
    void testGetNotFailedOnlyAndFailedOnlyTransactions() {
        assertThrows(IllegalArgumentException.class, () -> dao.getTransactions(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_1.getSenderId(), (byte) -1, (byte) -1, 0, false, false, false, 0, Integer.MAX_VALUE, false, true, Integer.MAX_VALUE, 0, true, true, Sort.desc()));
    }

    @Test
    void testGetTransactionsWithPagination() {
        extension.cleanAndPopulateDb();
        List<TransactionEntity> transactions = dao.getTransactions((byte) -1, (byte) -1, 2, 4);
        assertEquals(List.of(td.TRANSACTION_12, td.TRANSACTION_11, td.TRANSACTION_10), toModelConverter.convert(transactions));
    }

    @Test
    void testGetTransactionsByType() {
        List<TransactionEntity> transactions = dao.getTransactions((byte) 8, (byte) -1, 0, Integer.MAX_VALUE);
        assertEquals(List.of(td.TRANSACTION_12, td.TRANSACTION_11, td.TRANSACTION_7), toModelConverter.convert(transactions));
    }

    @Test
    void testGetTransactionsByTypeAndSubtypeWithPagination() {
        List<TransactionEntity> transactions = dao.getTransactions((byte) 0, (byte) 0, 3, 5);
        assertEquals(List.of(td.TRANSACTION_8, td.TRANSACTION_6, td.TRANSACTION_5), toModelConverter.convert(transactions));
    }

    @Test
    void testGetTransactionCountForAccountInDataSource() {
        int count = dao.getTransactionCountByFilter(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_1.getSenderId(), (byte) 0, (byte) 0, td.TRANSACTION_3.getBlockTimestamp() + 1, false, false, false, true, false, Integer.MAX_VALUE, 0, false, false);

        assertEquals(5, count);
    }

    @Test
    void testGetTransactionsBeforeHeight() {
        List<TransactionDbInfo> result = dao.getTransactionsBeforeHeight(td.TRANSACTION_6.getHeight());
        List<TransactionDbInfo> expected = List.of(new TransactionDbInfo(td.DB_ID_0, td.TRANSACTION_0.getId()), new TransactionDbInfo(td.DB_ID_1, td.TRANSACTION_1.getId()), new TransactionDbInfo(td.DB_ID_2, td.TRANSACTION_2.getId()), new TransactionDbInfo(td.DB_ID_3, td.TRANSACTION_3.getId()));
        assertEquals(expected, result);
    }

    @Test
    void testGetTransactionsBeforeZeroHeight() {
        List<TransactionDbInfo> result = dao.getTransactionsBeforeHeight(0);
        assertEquals(List.of(), result);
    }

    @Test
    void testGetTransactionsByPreparedStatementOnConnection() {
        DbUtils.checkAndRunInTransaction(extension, (con) -> {
            try (PreparedStatement pstm = con.prepareStatement("select * from transaction where id = ?")) {
                pstm.setLong(1, td.TRANSACTION_10.getId());
                List<TransactionEntity> transactions = dao.getTransactions(con, pstm);
                assertEquals(List.of(td.TRANSACTION_10).stream().map(Transaction::getId).collect(Collectors.toList()),
                    transactions.stream().map(TransactionEntity::getId).collect(Collectors.toList()));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testGetChats() {
        try {
            insertMockMessageTx(-1, 100, 200, 900);
            insertMockMessageTx(-2, 200, 100, 999);
            insertMockMessageTx(-3, 300, 100, 999);
            insertMockMessageTx(-4, 300, 0, 999);
            insertMockMessageTx(-5, 200, 100, 1000);

            List<ChatInfo> chats = dao.getChatAccounts(100, 0, -1);

            assertEquals(List.of(new ChatInfo(200, 1000), new ChatInfo(300, 999)), chats);

            List<ChatInfo> paginatedChats = dao.getChatAccounts(100, 0, 0);

            assertEquals(List.of(new ChatInfo(200, 1000)), paginatedChats);
        } finally {
            removeTransactions(List.of(-1L, -2L, -3L, -4L, -5L));
        }
    }

    @Test
    void testGetTransactionsChatHistory() {
        try {
            insertMockMessageTx(-1, 100, 200, 900);
            insertMockMessageTx(-2, 200, 100, 999);
            insertMockMessageTx(-3, 300, 100, 999);
            insertMockMessageTx(-4, 300, 0, 999);
            insertMockMessageTx(-5, 200, 100, 1000);

            List<TransactionEntity> txs = dao.getTransactionsChatHistory(100, 200, 0, -1);
            List<Long> txIds = txs.stream().map(TransactionEntity::getId).collect(Collectors.toList());

            assertEquals(List.of(-5L, -2L, -1L), txIds);

            List<TransactionEntity> paginatedChatTxs = dao.getTransactionsChatHistory(100, 200, 1, 1);
            List<Long> paginatedIds = paginatedChatTxs.stream().map(TransactionEntity::getId).collect(Collectors.toList());

            assertEquals(List.of(-2L), paginatedIds);

        } finally {
            removeTransactions(List.of(-1L, -2L, -3L, -4L, -5L));
        }
    }

    private void removeTransactions(List<Long> ids) {
        DbUtils.inTransaction(extension, (con)-> {
            for (Long id : ids) {
                try {
                    con.createStatement().executeUpdate("delete from transaction where id = " + id);
                } catch (SQLException e) {
                    fail(e);
                }

            }
        });
    }

    private void insertMockMessageTx(long id, long sender, long recipient, int timestamp) {
        TransactionEntity transactionEntity = new TransactionEntity(0L, id, (short) 12, recipient, (short) 0, 0L, 100_000_000, new byte[32], 100000, 1L, 100000, 1, new byte[64], timestamp, (byte) 1, (byte) 0, sender, new byte[32], 650, new byte[32], (byte) 1, false, false, false, false, false, false, false, false, null, new byte[0]);
        dao.saveTransactions(List.of(transactionEntity));
    }

}