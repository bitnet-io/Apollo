/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimEvent;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountGuaranteedBalance;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j

@Tag("slow")
class AccountGuaranteedBalanceTableTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer, DbTestData.getInMemDbProps(), "db/acc-data.sql", "db/schema.sql");
    AccountGuaranteedBalanceTable table;
    AccountTestData testData = new AccountTestData();
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    Event<FullTextOperationData> fullTextOperationDataEvent = mock(Event.class);

    @BeforeEach
    void setUp() {
        when(fullTextOperationDataEvent.select(new AnnotationLiteral<TrimEvent>() {})).thenReturn(fullTextOperationDataEvent);
        table = new AccountGuaranteedBalanceTable(blockchainConfig, propertiesHolder, dbExtension.getDatabaseManager(), fullTextOperationDataEvent);
    }

    @Test
    void trim() throws SQLException {
        doReturn(10).when(propertiesHolder).BATCH_COMMIT_SIZE();
        table = new AccountGuaranteedBalanceTable(blockchainConfig,
            propertiesHolder, dbExtension.getDatabaseManager(), mock(Event.class));

        long sizeAll = table.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues().size();
        assertEquals(testData.ALL_BALANCES.size(), sizeAll);
        DbUtils.inTransaction(dbExtension, con -> table.trim(testData.ACC_GUARANTEE_BALANCE_HEIGHT_MAX));
        long sizeTrim = table.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues().size();
        assertEquals(1, sizeTrim);
    }

    @Test
    void testGetSumOfAdditions() {
        dbExtension.cleanAndPopulateDb();

        long accountId = testData.ACC_BALANCE_1.getAccountId();
        int height1 = testData.ACC_BALANCE_1.getHeight();
        int height2 = height1 + 1000;

        long expectedSum = testData.ALL_BALANCES.stream().
            filter(b -> (b.getAccountId() == accountId && b.getHeight() > height1 && b.getHeight() <= height2)).
            mapToLong(AccountGuaranteedBalance::getAdditions).sum();

        assertEquals(expectedSum, table.getSumOfAdditions(accountId, height1, height2));
    }

    @Test
    void getLessorsAdditions() {
        List<Long> lessorsList = List.of(testData.ACC_BALANCE_1.getAccountId(), testData.ACC_BALANCE_3.getAccountId(), testData.ACC_BALANCE_5.getAccountId());
        doReturn(1440).when(blockchainConfig).getGuaranteedBalanceConfirmations();
        int height = testData.ACC_GUARANTEE_BALANCE_HEIGHT_MAX;

        Map<Long, Long> result = table.getLessorsAdditions(lessorsList, height, height + 1000);
        assertEquals(lessorsList.size(), result.values().size());
        lessorsList.forEach(accountId -> assertEquals(getSumOfAdditionsByAccountId(accountId), result.get(accountId)));
    }

    private long getSumOfAdditionsByAccountId(final long accountId) {
        long expectedSum = testData.ALL_BALANCES.stream().
            filter(balance -> (accountId == balance.getAccountId())).
            mapToLong(AccountGuaranteedBalance::getAdditions).sum();
        return expectedSum;
    }

    @Test
    void addToGuaranteedBalanceATM() {
        dbExtension.cleanAndPopulateDb();

        long amountATM = 10000L;
        long expectedSum = testData.ACC_BALANCE_3.getAdditions() + amountATM;

        table.addToGuaranteedBalanceATM(testData.ACC_BALANCE_3.getAccountId(), amountATM, testData.ACC_BALANCE_3.getHeight() + 1);
        assertEquals(expectedSum, table.getSumOfAdditions(testData.ACC_BALANCE_3.getAccountId(), testData.ACC_BALANCE_3.getHeight() - 1, testData.ACC_BALANCE_3.getHeight() + 1));
    }
}