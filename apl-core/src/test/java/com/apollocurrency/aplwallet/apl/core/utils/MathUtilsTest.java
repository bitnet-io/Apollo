/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.utils;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class MathUtilsTest {

    @Test
    void safeMultiplyWithTxOk() throws AplException.NotValidException {
        Transaction tx = createMockTx();

        long result = MathUtils.safeMultiply(15L, -10L, tx);

        assertEquals(-150L, result, "Multiplication result should be -150 (15 * -10)");
    }


    @Test
    void safeMultiplyWithTxFailed() {
        Transaction tx = createMockTx();

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> MathUtils.safeMultiply(2L, Long.MIN_VALUE, tx));

        assertEquals("Result of multiplying x=2, y=-9223372036854775808 exceeds the allowed range " +
            "[-9223372036854775808;9223372036854775807], transaction='test_tx_id', type='ORDINARY_PAYMENT', sender='0'", ex.getMessage());
    }

    @Test
    void safeMultiplyWithMessageOk() throws AplException.NotValidException {
        long result = MathUtils.safeMultiply(-1, -1, "no errors");

        assertEquals(1, result);
    }

    @Test
    void safeMultiplyWithMessageFailed() {
        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> MathUtils.safeMultiply(Long.MAX_VALUE, Long.MIN_VALUE, "test error info"));

        assertEquals("Result of multiplying x=9223372036854775807, y=-9223372036854775808 exceeds the allowed range " +
            "[-9223372036854775808;9223372036854775807], test error info", ex.getMessage());
    }



    private Transaction createMockTx() {
        Transaction tx = mock(Transaction.class);
        TransactionType txType = mock(TransactionType.class);
        doReturn(TransactionTypes.TransactionTypeSpec.ORDINARY_PAYMENT).when(txType).getSpec();
        doReturn(txType).when(tx).getType();
        doReturn("test_tx_id").when(tx).getStringId();
        return tx;
    }
}