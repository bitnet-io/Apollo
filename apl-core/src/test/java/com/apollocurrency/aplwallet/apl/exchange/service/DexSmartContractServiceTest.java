package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.dex.core.dao.DexTransactionDao;
import com.apollocurrency.aplwallet.apl.dex.core.model.DepositedOrderDetails;
import com.apollocurrency.aplwallet.apl.dex.core.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.dex.core.model.DexTransaction;
import com.apollocurrency.aplwallet.apl.dex.core.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.dex.core.model.OrderType;
import com.apollocurrency.aplwallet.apl.dex.eth.contracts.DexContract;
import com.apollocurrency.aplwallet.apl.dex.eth.model.EthChainGasInfoImpl;
import com.apollocurrency.aplwallet.apl.dex.eth.model.EthGasInfo;
import com.apollocurrency.aplwallet.apl.dex.eth.model.EthStationGasInfo;
import com.apollocurrency.aplwallet.apl.dex.eth.service.DexBeanProducer;
import com.apollocurrency.aplwallet.apl.dex.eth.service.DexEthService;
import com.apollocurrency.aplwallet.apl.dex.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.dex.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.dex.eth.web3j.ChainId;
import com.apollocurrency.aplwallet.apl.dex.eth.web3j.ComparableStaticGasProvider;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.vault.model.EthWalletKey;
import com.apollocurrency.aplwallet.vault.service.KMSService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.response.TransactionReceiptProcessor;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DexSmartContractServiceTest {
    private static final long ALICE_ID = 100;
    private static final String ALICE_PASS = "PASS";
    private static final String PAX_ETH_ADDRESS = "0xc3188f569Ec3fD52335B8BcDB4A57A3cc377c221";
    private static final String SWAP_ETH_ADDRESS = "0x64A2759A779d0928A00082621c0BB4b8050144f9";
    private static final String ALICE_ETH_ADDRESS = "0xeb751ae27f31d0cecc3d11b3a654851fbe72bb9c";
    private static final String ALICE_PRIV_KEY = "f47759941904a9bf6f89736c4541d850107c9be6ec619e7e65cf80a14ff7e8e4";
    private static final EthWalletKey ETH_WALLET_KEY = new EthWalletKey(Convert.parseHexString(ALICE_PRIV_KEY));
    @Mock
    private Web3j web3j;
    @Mock
    private DexEthService dexEthService;
    @Mock
    private DexContract dexContract;
    @Mock
    private EthereumWalletService ethereumWalletService;
    @Mock
    private DexTransactionDao dexTransactionDao;
    @Mock
    private TransactionReceiptProcessor receiptProcessor;
    @Mock
    private DexBeanProducer dexBeanProducer;
    @Mock
    private KMSService KMSService;
    @Mock
    private ChainId chainId;
    private DexSmartContractService service;
    private Credentials walletCredentials;
    private EthGasInfo gasInfo;
    private byte[] secretHash = new byte[32];
    private String empty32EncodedBytes = Numeric.toHexString(secretHash);
    private DexOrder order = new DexOrder(2L, 100L, "from-address", "to-address", OrderType.BUY, OrderStatus.OPEN, DexCurrency.APL, 127_000_000L, DexCurrency.ETH, BigDecimal.valueOf(0.0001), 500);

    @BeforeEach
    void setUp() {
        Properties props = new Properties();
        props.setProperty("apl.eth.swap.proxy.contract.address", SWAP_ETH_ADDRESS);
        props.setProperty("apl.eth.pax.contract.address", PAX_ETH_ADDRESS);
        PropertiesHolder holder =  new PropertiesHolder(props);
        service = spy(new DexSmartContractService(holder, dexEthService, ethereumWalletService, dexTransactionDao,
            dexBeanProducer, null, KMSService, chainId));
        walletCredentials = Credentials.create(ECKeyPair.create(Convert.parseHexString(ALICE_PRIV_KEY)));
        gasInfo = new EthChainGasInfoImpl(100L, 82L, 53L);
    }


    @Test
    void testHasFrozenMoneyForSellOrder() {
        order.setType(OrderType.SELL);

        boolean result = service.hasFrozenMoney(order);

        assertTrue(result);
    }

    @Test
    void testHasFrozenMoneyForBuyOrder() throws AplException.ExecutiveProcessException {
        DepositedOrderDetails depositedOrderDetails = new DepositedOrderDetails(true, null, new BigDecimal("0.0001270"), false);
        doReturn(depositedOrderDetails).when(service).getDepositedOrderDetails(order.getFromAddress(), order.getId());

        boolean result = service.hasFrozenMoney(order);

        assertTrue(result);
    }

    @Test
    void testHasFrozenMoneyForBuyOrderWithoutUserDeposits() throws AplException.ExecutiveProcessException {
        DepositedOrderDetails depositedOrderDetails = new DepositedOrderDetails(false, null, null, false);
        doReturn(depositedOrderDetails).when(service).getDepositedOrderDetails(order.getFromAddress(), order.getId());

        boolean result = service.hasFrozenMoney(order);

        assertFalse(result);
    }


    @Test
    void testDepositEth() throws ExecutionException, AplException.ExecutiveProcessException {
        doReturn(new EthStationGasInfo(25.0, 20.0, 18.0)).when(dexEthService).getEthPriceInfo();
        doReturn(ETH_WALLET_KEY).when(KMSService).getEthWallet(ALICE_ID, ALICE_PASS, ALICE_ETH_ADDRESS);
        doReturn(dexContract).when(service).createDexContract(new ComparableStaticGasProvider(BigInteger.valueOf(25_000_000_000L), BigInteger.valueOf(400_000)), new DexTransaction(null, null, null, DexTransaction.Op.DEPOSIT, "100", ALICE_ETH_ADDRESS, 0), walletCredentials);
        doReturn("hash").when(dexContract).deposit(BigInteger.valueOf(100), BigInteger.TEN);

        String hash = service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, BigInteger.TEN, null, DexCurrency.ETH);

        assertEquals("hash", hash);
    }

    @Test
    void testDepositEthWithException() throws ExecutionException, AplException.ExecutiveProcessException {
        doReturn(new EthStationGasInfo(25.0, 20.0, 18.0)).when(dexEthService).getEthPriceInfo();
        doReturn(ETH_WALLET_KEY).when(KMSService).getEthWallet(ALICE_ID, ALICE_PASS, ALICE_ETH_ADDRESS);
        doReturn(dexContract).when(service).createDexContract(new ComparableStaticGasProvider(BigInteger.valueOf(25_000_000_000L), BigInteger.valueOf(400_000)), new DexTransaction(null, null, null, DexTransaction.Op.DEPOSIT, "100", ALICE_ETH_ADDRESS, 0), walletCredentials);
        doThrow(new RuntimeException()).when(dexContract).deposit(BigInteger.valueOf(100), BigInteger.TEN);

        String hash = service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, BigInteger.TEN, null, DexCurrency.ETH);

        assertNull(hash);
    }

    @Test
    void testDepositPaxWithoutAllowance() throws ExecutionException, AplException.ExecutiveProcessException, IOException, TransactionException {
        BigInteger amount = EthUtil.etherToWei(BigDecimal.ONE);
        doReturn(new EthStationGasInfo(25.0, 20.0, 18.0)).when(dexEthService).getEthPriceInfo();
        doReturn(ETH_WALLET_KEY).when(KMSService).getEthWallet(ALICE_ID, ALICE_PASS, ALICE_ETH_ADDRESS);
        doReturn(dexContract).when(service).createDexContract(new ComparableStaticGasProvider(BigInteger.valueOf(25_000_000_000L), BigInteger.valueOf(400_000)), new DexTransaction(null, null, null, DexTransaction.Op.DEPOSIT, "100", ALICE_ETH_ADDRESS, 0), walletCredentials);
        doReturn(BigInteger.ZERO).when(ethereumWalletService).getAllowance(SWAP_ETH_ADDRESS, ALICE_ETH_ADDRESS, PAX_ETH_ADDRESS);
        doReturn("approve-hash").when(ethereumWalletService).sendApproveTransaction(walletCredentials, SWAP_ETH_ADDRESS, Constants.ETH_MAX_POS_INT);
        doReturn("hash").when(dexContract).deposit(BigInteger.valueOf(100), amount, PAX_ETH_ADDRESS);

        TransactionReceiptProcessor transactionReceiptProcessor = Mockito.mock(TransactionReceiptProcessor.class);
        doReturn(new TransactionReceipt()).when(transactionReceiptProcessor).waitForTransactionReceipt(any());
        doReturn(transactionReceiptProcessor).when(dexBeanProducer).receiptProcessor();

        String hash = service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, amount, null, DexCurrency.PAX);

        assertEquals("hash", hash);
    }

    @Test
    void testDepositPaxWithAllowance() throws ExecutionException, AplException.ExecutiveProcessException, IOException {
        BigInteger amount = EthUtil.etherToWei(BigDecimal.ONE);
        doReturn(new EthStationGasInfo(25.0, 20.0, 18.0)).when(dexEthService).getEthPriceInfo();
        doReturn(ETH_WALLET_KEY).when(KMSService).getEthWallet(ALICE_ID, ALICE_PASS, ALICE_ETH_ADDRESS);
        doReturn(dexContract).when(service).createDexContract(new ComparableStaticGasProvider(BigInteger.valueOf(25_000_000_000L), BigInteger.valueOf(400_000)), new DexTransaction(null, null, null, DexTransaction.Op.DEPOSIT, "100", ALICE_ETH_ADDRESS, 0), walletCredentials);
        doReturn(amount).when(ethereumWalletService).getAllowance(SWAP_ETH_ADDRESS, ALICE_ETH_ADDRESS, PAX_ETH_ADDRESS);
        doReturn("hash").when(dexContract).deposit(BigInteger.valueOf(100), amount, PAX_ETH_ADDRESS);

        String hash = service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, amount, null, DexCurrency.PAX);

        assertEquals("hash", hash);
        verify(ethereumWalletService, never()).sendApproveTransaction(walletCredentials, SWAP_ETH_ADDRESS, Constants.ETH_MAX_POS_INT);
    }

    @Test
    void testDepositEthWithoutGasPrice() throws ExecutionException, AplException.ExecutiveProcessException, IOException {
        BigInteger amount = BigInteger.valueOf(150_000_000_000_000L);
        doReturn(ETH_WALLET_KEY).when(KMSService).getEthWallet(ALICE_ID, ALICE_PASS, ALICE_ETH_ADDRESS);
        doReturn(dexContract).when(service).createDexContract(new ComparableStaticGasProvider(BigInteger.valueOf(27_000_000_000L), BigInteger.valueOf(400_000)), new DexTransaction(null, null, null, DexTransaction.Op.DEPOSIT, "100", ALICE_ETH_ADDRESS, 0), walletCredentials);
        doReturn("hash").when(dexContract).deposit(BigInteger.valueOf(100), amount);

        String hash = service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, amount, 27L, DexCurrency.ETH);

        assertEquals("hash", hash);
        verifyNoChainId();
    }

    @Test
    void testDepositOnExceptionDuringAllowance() throws IOException, AplException.ExecutiveProcessException {
        BigInteger amount = EthUtil.etherToWei(BigDecimal.ONE);
        doReturn(ETH_WALLET_KEY).when(KMSService).getEthWallet(ALICE_ID, ALICE_PASS, ALICE_ETH_ADDRESS);
        doThrow(new IOException()).when(ethereumWalletService).getAllowance(SWAP_ETH_ADDRESS, ALICE_ETH_ADDRESS, PAX_ETH_ADDRESS);


        assertThrows(RuntimeException.class, () -> service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, amount, 10L, DexCurrency.PAX));

        verifyNoInteractions(dexContract);
        verify(ethereumWalletService, never()).sendApproveTransaction(walletCredentials, SWAP_ETH_ADDRESS, Constants.ETH_MAX_POS_INT);
    }

    @Test
    void testDepositNotSupportedCurrency() throws AplException.ExecutiveProcessException {
        doReturn(ETH_WALLET_KEY).when(KMSService).getEthWallet(ALICE_ID, ALICE_PASS, ALICE_ETH_ADDRESS);

        assertThrows(UnsupportedOperationException.class, () -> service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, BigInteger.TEN, 10L, DexCurrency.APL));

        verifyNoInteractions(dexContract);
        verify(ethereumWalletService, never()).sendApproveTransaction(walletCredentials, SWAP_ETH_ADDRESS, Constants.ETH_MAX_POS_INT);
    }

    @Test
    void testDepositWithExceptionDuringApproving() throws IOException {
        BigInteger amount = EthUtil.etherToWei(BigDecimal.ONE);
        doReturn(ETH_WALLET_KEY).when(KMSService).getEthWallet(ALICE_ID, ALICE_PASS, ALICE_ETH_ADDRESS);
        doReturn(BigInteger.ZERO).when(ethereumWalletService).getAllowance(SWAP_ETH_ADDRESS, ALICE_ETH_ADDRESS, PAX_ETH_ADDRESS);

        assertThrows(AplException.ExecutiveProcessException.class, () -> service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, amount, 10L, DexCurrency.PAX));
    }

    @Test
    void testDepositWhenUncTransactionWasSentBefore() throws AplException.ExecutiveProcessException, IOException {
        DexTransaction tx = new DexTransaction(1L, new byte[32], new byte[32], DexTransaction.Op.DEPOSIT, "100", ALICE_ETH_ADDRESS, 150);
        doReturn(ETH_WALLET_KEY).when(KMSService).getEthWallet(ALICE_ID, ALICE_PASS, ALICE_ETH_ADDRESS);
        doReturn(tx).when(dexTransactionDao).get(tx.getParams(), tx.getAccount(), tx.getOperation());
        Transaction responseTx = mock(Transaction.class);
        doReturn(Optional.ofNullable(responseTx)).when(service).getTxByHash(Numeric.toHexString(new byte[32]));

        String hash = service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, BigInteger.TEN, 10L, DexCurrency.ETH);

        assertEquals(Numeric.toHexString(new byte[32]), hash);
    }


    @Test
    void testDepositWhenConfirmedTxWasSent() throws AplException.ExecutiveProcessException, IOException {
        DexTransaction tx = new DexTransaction(1L, new byte[32], new byte[32], DexTransaction.Op.DEPOSIT, "100", ALICE_ETH_ADDRESS, 150);
        doReturn(ETH_WALLET_KEY).when(KMSService).getEthWallet(ALICE_ID, ALICE_PASS, ALICE_ETH_ADDRESS);
        doReturn(tx).when(dexTransactionDao).get(tx.getParams(), tx.getAccount(), tx.getOperation());
        Transaction responseTx = mock(Transaction.class);
        doReturn(Optional.ofNullable(responseTx)).when(service).getTxByHash(Numeric.toHexString(new byte[32]));
        doReturn("1").when(responseTx).getBlockNumberRaw();
        TransactionReceipt responseReceipt = mock(TransactionReceipt.class);
        doReturn(Optional.ofNullable(responseReceipt)).when(service).getTxReceipt(Numeric.toHexString(new byte[32]));
        doReturn("0x1").when(responseReceipt).getStatus();

        String hash = service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, BigInteger.TEN, 10L, DexCurrency.ETH);

        assertEquals(Numeric.toHexString(new byte[32]), hash);

    }

    @Test
    void testDepositWhenConfirmedFailedTxWasSent() throws AplException.ExecutiveProcessException, IOException {
        DexTransaction tx = new DexTransaction(1L, new byte[32], new byte[32], DexTransaction.Op.DEPOSIT, "100", ALICE_ETH_ADDRESS, 150);
        doReturn(ETH_WALLET_KEY).when(KMSService).getEthWallet(ALICE_ID, ALICE_PASS, ALICE_ETH_ADDRESS);
        doReturn(tx).when(dexTransactionDao).get(tx.getParams(), tx.getAccount(), tx.getOperation());
        Transaction responseTx = mock(Transaction.class);
        doReturn(Optional.ofNullable(responseTx)).when(service).getTxByHash(Numeric.toHexString(new byte[32]));
        doReturn("1").when(responseTx).getBlockNumberRaw();
        TransactionReceipt responseReceipt = mock(TransactionReceipt.class);
        doReturn(Optional.ofNullable(responseReceipt)).when(service).getTxReceipt(Numeric.toHexString(new byte[32]));
        doReturn("0x0").when(responseReceipt).getStatus();

        doReturn(dexContract).when(service).createDexContract(new ComparableStaticGasProvider(BigInteger.valueOf(27_000_000_000L), BigInteger.valueOf(400_000)), new DexTransaction(null, null, null, DexTransaction.Op.DEPOSIT, "100", ALICE_ETH_ADDRESS, 0), walletCredentials);
        doReturn("hash").when(dexContract).deposit(BigInteger.valueOf(100), BigInteger.TEN);

        String hash = service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, BigInteger.TEN, 27L, DexCurrency.ETH);

        assertEquals("hash", hash);
        verify(dexTransactionDao).delete(1L);
    }

    @Test
    void testDepositWhenConfirmedReceiptIsNull() throws AplException.ExecutiveProcessException, IOException {
        DexTransaction tx = new DexTransaction(1L, new byte[32], new byte[32], DexTransaction.Op.DEPOSIT, "100", ALICE_ETH_ADDRESS, 150);
        doReturn(ETH_WALLET_KEY).when(KMSService).getEthWallet(ALICE_ID, ALICE_PASS, ALICE_ETH_ADDRESS);
        doReturn(tx).when(dexTransactionDao).get(tx.getParams(), tx.getAccount(), tx.getOperation());
        Transaction responseTx = mock(Transaction.class);
        doReturn(Optional.ofNullable(responseTx)).when(service).getTxByHash(Numeric.toHexString(new byte[32]));
        doReturn("1").when(responseTx).getBlockNumberRaw();
        doReturn(Optional.empty()).when(service).getTxReceipt(Numeric.toHexString(new byte[32]));

        String hash = service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, BigInteger.TEN, 10L, DexCurrency.ETH);

        assertEquals(Numeric.toHexString(new byte[32]), hash);
    }

    @Test
    void testDepositWhenTxWasNotSent() throws AplException.ExecutiveProcessException, IOException {
        DexTransaction tx = new DexTransaction(1L, new byte[32], new byte[32], DexTransaction.Op.DEPOSIT, "100", ALICE_ETH_ADDRESS, 150);
        doReturn(ETH_WALLET_KEY).when(KMSService).getEthWallet(ALICE_ID, ALICE_PASS, ALICE_ETH_ADDRESS);
        doReturn(tx).when(dexTransactionDao).get(tx.getParams(), tx.getAccount(), tx.getOperation());
        String empty32Bytes = Numeric.toHexString(new byte[32]);
        doReturn(Optional.empty()).when(service).getTxByHash(empty32Bytes);
        mockEthSendTransactionCorrectResponse(empty32EncodedBytes, empty32EncodedBytes);
        when(dexBeanProducer.web3j()).thenReturn(web3j);

        String hash = service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, BigInteger.TEN, 10L, DexCurrency.ETH);

        assertEquals(empty32Bytes, hash);
    }

    @Test
    void testDepositWhenUnableToGetResponseFromNode() throws IOException, AplException.ExecutiveProcessException {
        DexTransaction tx = new DexTransaction(1L, new byte[32], new byte[32], DexTransaction.Op.DEPOSIT, "100", ALICE_ETH_ADDRESS, 150);
        doReturn(ETH_WALLET_KEY).when(KMSService).getEthWallet(ALICE_ID, ALICE_PASS, ALICE_ETH_ADDRESS);
        doReturn(tx).when(dexTransactionDao).get(tx.getParams(), tx.getAccount(), tx.getOperation());
        String empty32Bytes = Numeric.toHexString(new byte[32]);
        doThrow(new IOException()).when(service).getTxByHash(empty32Bytes);

        String hash = service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, BigInteger.TEN, 10L, DexCurrency.ETH);

        assertEquals(empty32Bytes, hash);
    }

    @Test
    void testDepositWhenStoredPreviousTxIsIncorrect() throws IOException, AplException.ExecutiveProcessException {
        DexTransaction tx = new DexTransaction(1L, new byte[32], new byte[32], DexTransaction.Op.DEPOSIT, "100", ALICE_ETH_ADDRESS, 150);
        doReturn(ETH_WALLET_KEY).when(KMSService).getEthWallet(ALICE_ID, ALICE_PASS, ALICE_ETH_ADDRESS);
        doReturn(dexContract).when(service).createDexContract(new ComparableStaticGasProvider(BigInteger.valueOf(10_000_000_000L), BigInteger.valueOf(400_000)), new DexTransaction(null, null, null, DexTransaction.Op.DEPOSIT, "100", ALICE_ETH_ADDRESS, 0), walletCredentials);
        doReturn("hash").when(dexContract).deposit(BigInteger.valueOf(100), BigInteger.TEN);
        doReturn(tx).when(dexTransactionDao).get(tx.getParams(), tx.getAccount(), tx.getOperation());
        String empty32Bytes = Numeric.toHexString(new byte[32]);
        doReturn(Optional.empty()).when(service).getTxByHash(empty32Bytes);
        mockEthSendTransactionWithErrorResponse(empty32EncodedBytes);
        when(dexBeanProducer.web3j()).thenReturn(web3j);

        String hash = service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, BigInteger.TEN, 10L, DexCurrency.ETH);

        assertEquals("hash", hash);
        verify(dexTransactionDao).delete(1);
    }

    @Test
    void testRefund() throws ExecutionException, AplException.ExecutiveProcessException {
        doReturn(ETH_WALLET_KEY).when(KMSService).getEthWallet(ALICE_ID, ALICE_PASS, ALICE_ETH_ADDRESS);
        doReturn(gasInfo).when(dexEthService).getEthPriceInfo();
        doReturn(dexContract).when(service).createDexContract(new ComparableStaticGasProvider(BigInteger.valueOf(100_000_000_000L), BigInteger.valueOf(400_000)), new DexTransaction(null, null, null, DexTransaction.Op.REFUND, empty32EncodedBytes, ALICE_ETH_ADDRESS, 0), walletCredentials);
        doReturn("hash").when(dexContract).refundAndWithdraw(secretHash, true);

        boolean r = service.refundAndWithdraw(secretHash, ALICE_PASS, ALICE_ETH_ADDRESS, ALICE_ID, true) != null;

        assertTrue(r);
        verifyNoChainId();
    }

    @Test
    void testNotSuccessfulRefund() throws ExecutionException, AplException.ExecutiveProcessException {
        doReturn(ETH_WALLET_KEY).when(KMSService).getEthWallet(ALICE_ID, ALICE_PASS, ALICE_ETH_ADDRESS);
        doReturn(gasInfo).when(dexEthService).getEthPriceInfo();
        doReturn(dexContract).when(service).createDexContract(new ComparableStaticGasProvider(BigInteger.valueOf(100_000_000_000L), BigInteger.valueOf(400_000)), new DexTransaction(null, null, null, DexTransaction.Op.REFUND, empty32EncodedBytes, ALICE_ETH_ADDRESS, 0), walletCredentials);

        boolean r = service.refundAndWithdraw(secretHash, ALICE_PASS, ALICE_ETH_ADDRESS, ALICE_ID, true) != null;

        assertFalse(r);
    }

    @Test
    void testRefundSendExistingRawTransactionWithConfirmation() throws AplException.ExecutiveProcessException, IOException, TransactionException {
        when(dexBeanProducer.web3j()).thenReturn(web3j);
        when(dexBeanProducer.receiptProcessor()).thenReturn(receiptProcessor);
        mockExistingTransactionSendingWithReceipt(empty32EncodedBytes);

        boolean r = service.refundAndWithdraw(secretHash, ALICE_PASS, ALICE_ETH_ADDRESS, ALICE_ID, true) != null;

        assertTrue(r);
        verifyNoInteractions(dexContract, dexEthService);
    }


    @Test
    void testRefundSendExistingRawTransactionWithConfirmationWhenHashesNotMatch() throws IOException, TransactionException {
        when(dexBeanProducer.web3j()).thenReturn(web3j);
        when(dexBeanProducer.receiptProcessor()).thenReturn(receiptProcessor);
        mockExistingTransactionSendingWithReceipt("");

        assertThrows(AplException.DEXProcessingException.class, () -> service.refundAndWithdraw(secretHash, ALICE_PASS, ALICE_ETH_ADDRESS, ALICE_ID, true));

        verifyNoInteractions(dexContract, dexEthService);
    }

    @Test
    void testRefundSendExistingRawTransactionWithConfirmationWhenTransactionExceptionThrown() throws IOException, TransactionException {
        when(dexBeanProducer.web3j()).thenReturn(web3j);
        when(dexBeanProducer.receiptProcessor()).thenReturn(receiptProcessor);
        mockExistingTransactionSendingWithoutReceipt(empty32EncodedBytes);
        doThrow(new TransactionException("Test tx exception")).when(receiptProcessor).waitForTransactionReceipt(empty32EncodedBytes);

        assertThrows(AplException.DEXProcessingException.class, () -> service.refundAndWithdraw(secretHash, ALICE_PASS, ALICE_ETH_ADDRESS, ALICE_ID, true));

        verifyNoInteractions(dexContract, dexEthService);
    }

    private void mockExistingTransactionSendingWithoutReceipt(String hash) throws IOException {
        DexTransaction tx = new DexTransaction(1L, new byte[32], new byte[32], DexTransaction.Op.REFUND, empty32EncodedBytes, ALICE_ETH_ADDRESS, 150);
        doReturn(ETH_WALLET_KEY).when(KMSService).getEthWallet(ALICE_ID, ALICE_PASS, ALICE_ETH_ADDRESS);
        doReturn(tx).when(dexTransactionDao).get(tx.getParams(), tx.getAccount(), tx.getOperation());
        doReturn(Optional.empty()).when(service).getTxByHash(Numeric.toHexString(new byte[32]));
        mockEthSendTransactionCorrectResponse(empty32EncodedBytes, hash);
    }

    private void mockExistingTransactionSendingWithReceipt(String hash) throws IOException, TransactionException {
        mockExistingTransactionSendingWithoutReceipt(hash);
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        doReturn(empty32EncodedBytes).when(receipt).getTransactionHash();
        doReturn(receipt).when(receiptProcessor).waitForTransactionReceipt(hash);
    }

    private void mockEthSendTransactionCorrectResponse(String encodedTx, String hash) throws IOException {
        EthSendTransaction response = mock(EthSendTransaction.class);
        doReturn(hash).when(response).getTransactionHash();
        mockEthSendTransactionWithRespnse(encodedTx, response);
    }

    private void mockEthSendTransactionWithRespnse(String encodedTx, EthSendTransaction response) throws IOException {
        Request request = mock(Request.class);
        doReturn(request).when(web3j).ethSendRawTransaction(encodedTx);
        doReturn(response).when(request).send();
    }

    private void mockEthSendTransactionWithErrorResponse(String encodedTx) throws IOException {
        EthSendTransaction response = mock(EthSendTransaction.class);
        doReturn(true).when(response).hasError();
        Response.Error error = new Response.Error();
        error.setData(encodedTx);
        error.setMessage("nonce too low");
        doReturn(error).when(response).getError();
        mockEthSendTransactionWithRespnse(encodedTx, response);
    }

    private void verifyNoChainId() {
        verifyNoInteractions(chainId);
    }
}