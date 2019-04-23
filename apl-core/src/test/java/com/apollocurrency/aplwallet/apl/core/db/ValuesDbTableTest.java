/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apollocurrency.aplwallet.apl.core.db.derived.ValuesDbTable;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class ValuesDbTableTest<T extends DerivedEntity> extends DerivedDbTableTest<T> {
    private DbKey INCORRECT_DB_KEY = new DbKey() {
        @Override
        public int setPK(PreparedStatement pstmt) throws SQLException {
            return setPK(pstmt, 1);
        }

        @Override
        public int setPK(PreparedStatement pstmt, int index) throws SQLException {
            pstmt.setLong(index, Long.MIN_VALUE);
            return index + 1;
        }
    };

    public ValuesDbTableTest(Class<T> clazz) {
        super(clazz);

    }

    public ValuesDbTable<T> getTable() {
        return (ValuesDbTable<T>) getDerivedDbTable();
    }

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        table = getTable();
        assertNotNull(getEntryWithListOfSize(2));
        assertNotNull(getEntryWithListOfSize(3));
    }

    ValuesDbTable<T> table ;

    @Test
    public void testGetByDbKey() {
        Map.Entry<DbKey, List<T>> entry = getEntryWithListOfSize(3);
        List<T> values = entry.getValue();
        DbKey dbKey = entry.getKey();
        List<T> result = table.get(dbKey);
        assertEquals(values, result);
    }

    @Test
    public void testGetByUnknownDbKey() {
        List<T> actual = table.get(INCORRECT_DB_KEY);
        assertTrue(actual.isEmpty(), "No records should be found at dbkey -1");

    }

    @Override
    public void testInsert() {
        List<T> toInsert = dataToInsert();
        DbUtils.inTransaction(extension, (con) -> {
            table.insert(toInsert);
            //check cache in transaction
            assertInCache(table.getDbKeyFactory(), toInsert);
        });
        //check db
        assertNotInCache(table.getDbKeyFactory(), toInsert);
        List<T> retrievedData = table.get(table.getDbKeyFactory().newKey(toInsert.get(0)));
        assertEquals(toInsert, retrievedData);
    }

    @Test
    public void testGetInCached() {
        Map.Entry<DbKey, List<T>> entry = getEntryWithListOfSize(2);
        List<T> values = entry.getValue();
        DbKey dbKey = entry.getKey();
        DbUtils.inTransaction(extension, con -> {
            List<T> actual = table.get(dbKey);
            assertInCache(table.getDbKeyFactory(), values);
            assertEquals(values, actual);
        });
        assertNotInCache(table.getDbKeyFactory(), values);
    }

    @Test
    public void testGetFromDeletedCache() {
        Map.Entry<DbKey, List<T>> entry = getEntryWithListOfSize(2);
        List<T> values = entry.getValue();
        DbKey dbKey = entry.getKey();
        KeyFactory<T> dbKeyFactory = table.getDbKeyFactory();
        DbUtils.inTransaction(extension, con -> {
            List<T> actual = table.get(dbKey);
            assertInCache(dbKeyFactory, values);
            assertEquals(values, actual);
            removeFromCache(dbKeyFactory, values);
            assertNotInCache(dbKeyFactory, values);
        });
        assertNotInCache(dbKeyFactory, values);
    }


    @Test
    public void testInsertNotInTransaction() {
        assertThrows(IllegalStateException.class, () -> table.insert(Collections.emptyList()));
    }

    @Test
    public void testInsertWithDifferentDbKeys() {
        List<T> dataToInsert = dataToInsert();
        T t = dataToInsert.get(0);
        t.setDbKey(INCORRECT_DB_KEY);
        assertThrows(IllegalArgumentException.class, () -> DbUtils.inTransaction(extension, (con) -> table.insert(dataToInsert)));
    }

    @Test
    public void testTrimNothing() throws SQLException {
        DbUtils.inTransaction(extension, (con) -> table.trim(0));
        List<T> allValues = table.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        assertEquals(getAllExpectedData(), allValues);
    }

    @Override
    @Test
    public void testTrim() throws SQLException {
        if (table.isMultiversion()) {
            int trimHeight = getTrimHeight();
            DbUtils.inTransaction(extension, con-> table.trim(trimHeight));
            List<T> allValues = table.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
            assertEquals(getAllAfterTrim(), allValues);
        } else {
            super.testTrim();
        }

    }

    protected Map.Entry<DbKey, List<T>> getEntryWithListOfSize(int i) {
        return groupByDbKey().entrySet().stream().filter(entry -> entry.getValue().size() == i).findFirst().get();
    }

    protected Map<DbKey, List<T>> getDuplicates() {
        return groupByDbKey()
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue()
                        .stream()
                        .map(DerivedEntity::getHeight)
                        .distinct()
                        .count() > 1)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    protected int getTrimHeight() {
        Map<DbKey, List<T>> duplicates = getDuplicates();
        Integer maxDuplicatesHeight = duplicates.values()
                .stream()
                .map(l -> l
                        .stream()
                        .map(DerivedEntity::getHeight)
                        .max(Comparator.naturalOrder())
                        .get())
                .max(Comparator.naturalOrder())
                .get();

        return maxDuplicatesHeight + 1;
    }

    protected List<T> getAllAfterTrim() {
        Map<DbKey, List<T>> duplicates = getDuplicates();
        List<T> removed = new ArrayList<>();
                duplicates.values()
                        .forEach(l ->
                        {
                            Integer maxHeight = l.stream().map(DerivedEntity::getHeight).max(Comparator.naturalOrder()).get();
                            l.stream().filter(e-> e.getHeight() < maxHeight).forEach(removed::add);
                        });
        List<T> allExpectedData = new ArrayList<>(getAllExpectedData());
        removed.forEach(allExpectedData::remove);
        return allExpectedData;
    }


    public Map<DbKey, List<T>> groupByDbKey() {
        List<T> allExpectedData = getAllExpectedData();
        return allExpectedData
                .stream()
                .collect(Collectors.groupingBy(table.getDbKeyFactory()::newKey));
    }

    protected abstract List<T> dataToInsert();
}
