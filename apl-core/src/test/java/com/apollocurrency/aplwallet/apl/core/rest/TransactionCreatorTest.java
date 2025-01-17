package com.apollocurrency.aplwallet.apl.core.rest;

import com.apollocurrency.aplwallet.api.v2.model.TransactionCreationResponse;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.exception.AplAcceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.exception.AplTransactionFeatureNotEnabledException;
import com.apollocurrency.aplwallet.apl.core.exception.AplUnacceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.model.EcBlockData;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionSigner;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.transaction.CachedTransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.FeeCalculator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EmptyAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.exception.RestParameterException;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionCreatorTest {
    public static final long TEST_LOCAL_ONE_APL = 100000000L;
    BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    Chain chain = mock(Chain.class);

    {
        doReturn(chain).when(blockchainConfig).getChain();
    }

    @Mock
    TransactionValidator validator;
    @Mock
    TransactionSigner transactionSigner;
    Blockchain blockchain = mock(Blockchain.class);
    @Mock
    TimeService timeService;
    @Mock
    TransactionProcessor processor;
    @Mock
    PropertiesHolder propertiesHolder;
    @Mock
    FeeCalculator calculator;

    TransactionBuilderFactory transactionBuilderFactory;

    TransactionTypeFactory transactionTypeFactory;

    @Mock
    AccountService accountService;
    Account sender;
    TransactionCreator txCreator;
    private String accountRS = "APL-XR8C-K97J-QDZC-3YXHE";
    private String publicKey = "d52a07dc6fdf9f5c6b547ccb11444ce7bba73a99014eb9ac647b6971bee9263c";
    private String secretPhrase = "here we go again";
    private CustomTransactionType transactionType;

    @BeforeEach
    void setUp() {
        sender = new Account(Convert.parseAccountId(accountRS), 1000 * TEST_LOCAL_ONE_APL, 100 * TEST_LOCAL_ONE_APL, 0L, 0L, 0);
        transactionType = new CustomTransactionType(blockchainConfig, accountService);
        transactionTypeFactory = new CachedTransactionTypeFactory(List.of(transactionType));
        transactionBuilderFactory = new TransactionBuilderFactory(transactionTypeFactory, blockchainConfig);
        txCreator = new TransactionCreator(validator, propertiesHolder, timeService, calculator, blockchain, processor, transactionTypeFactory, transactionBuilderFactory, transactionSigner, blockchainConfig);
    }

    @Test
    void testCreateTransactionSuccessful() throws AplException.ValidationException {
        EcBlockData ecBlockData = new EcBlockData(121, 100_000);
        doReturn(ecBlockData).when(blockchain).getECBlock(300);
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .deadlineValue("1440")
            .feeATM(blockchainConfig.getOneAPL())
            .attachment(new CustomAttachment())
            .timestamp(300)
            .keySeed(Crypto.getKeySeed(secretPhrase))
            .broadcast(true)
            .build();
//        doAnswer(invocation-> {
//            ((TransactionImpl) invocation.getArgument(0)).sign(new Signature() {
//                @Override
//                public byte[] bytes() {
//                    return new byte[] {1};
//                }
//
//                @Override
//                public String getHexString() {
//                    return "00";
//                }
//
//                @Override
//                public boolean isVerified() {
//                    return false;
//                }
//            }, new PayloadResult(new WriteByteBuffer(ByteBuffer.allocate(10))));
//            return null;
//        }).when(transactionSigner).sign(any(Transaction.class), any(byte[].class));
        mockSigning();


        Transaction tx = txCreator.createTransactionThrowingException(request);

        assertSame(transactionType, tx.getType());
        assertTrue(tx.getAttachment() instanceof EmptyAttachment);
        assertEquals(300, tx.getTimestamp());
        verify(processor).broadcast(tx);
    }

    @Test
    void testCreateTransaction_API_V2_signed() throws AplException.ValidationException {
        EcBlockData ecBlockData = new EcBlockData(121, 100_000);
        doReturn(ecBlockData).when(blockchain).getECBlock(300);
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .deadlineValue("1440")
            .feeATM(blockchainConfig.getOneAPL())
            .attachment(new CustomAttachment())
            .timestamp(300)
            .keySeed(Crypto.getKeySeed(secretPhrase))
            .broadcast(true)
            .build();
        mockSigning();

        TransactionCreationResponse apiV2TransactionResponse = txCreator.createApiV2Transaction(request);

        assertNotNull(apiV2TransactionResponse.getFullHash());
        assertNotNull(apiV2TransactionResponse.getSignature());
        assertTrue(apiV2TransactionResponse.isBroadcasted());
        assertEquals("6821601105394672790", apiV2TransactionResponse.getId());
        assertNotNull(apiV2TransactionResponse.getUnsignedTransactionBytes());
        verify(processor).broadcast(any(Transaction.class));
    }

    @Test
    void testCreateTransaction_API_V2_unsigned() throws AplException.ValidationException {
        EcBlockData ecBlockData = new EcBlockData(121, 100_000);
        doReturn(ecBlockData).when(blockchain).getECBlock(300);
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .deadlineValue("1440")
            .feeATM(blockchainConfig.getOneAPL())
            .attachment(new CustomAttachment())
            .timestamp(300)
            .keySeed(Crypto.getKeySeed(secretPhrase))
            .broadcast(true)
            .build();

        TransactionCreationResponse apiV2TransactionResponse = txCreator.createApiV2Transaction(request);

        assertNull(apiV2TransactionResponse.getFullHash());
        assertNull(apiV2TransactionResponse.getSignature());
        assertFalse(apiV2TransactionResponse.isBroadcasted());
        assertNull(apiV2TransactionResponse.getId());
        assertNotNull(apiV2TransactionResponse.getUnsignedTransactionBytes());
        verify(processor, never()).broadcast(any(Transaction.class));
    }

    @Test
    void testCreateTransaction_setEcBlock() throws AplException.ValidationException {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .deadlineValue("1440")
            .feeATM(blockchainConfig.getOneAPL())
            .attachment(new CustomAttachment())
            .keySeed(Crypto.getKeySeed(secretPhrase))
            .broadcast(true)
            .ecBlockId(1)
            .ecBlockHeight(100)
            .build();
        doReturn(1L).when(blockchain).getBlockIdAtHeight(100);
//        doAnswer(invocation-> {
//            ((Transaction) invocation.getArgument(0)).sign(mock(Signature.class));
//            return null;
//        }).when(transactionSigner).sign(any(Transaction.class), any(byte[].class));
        mockSigning();

        Transaction tx = txCreator.createTransactionThrowingException(request);

        assertSame(transactionType, tx.getType());
        assertTrue(tx.getAttachment() instanceof EmptyAttachment);
        assertEquals(1, tx.getECBlockId());

        verify(processor).broadcast(tx);
    }

    @Test
    void testCreateTransaction_setEcBlock_usingHeight() throws AplException.ValidationException {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .deadlineValue("1440")
            .feeATM(blockchainConfig.getOneAPL())
            .attachment(new CustomAttachment())
            .keySeed(Crypto.getKeySeed(secretPhrase))
            .broadcast(true)
            .ecBlockHeight(100)
            .build();
        doReturn(2L).when(blockchain).getBlockIdAtHeight(100);
//        doAnswer(invocation-> {
//            ((Transaction) invocation.getArgument(0)).sign(mock(Signature.class));
//            return null;
//        }).when(transactionSigner).sign(any(Transaction.class), any(byte[].class));
        mockSigning();

        Transaction tx = txCreator.createTransactionThrowingException(request);

        assertSame(transactionType, tx.getType());
        assertTrue(tx.getAttachment() instanceof EmptyAttachment);
        assertEquals(2, tx.getECBlockId());

        verify(processor).broadcast(tx);
    }

    @Test
    void createTransaction_missingPassphrase() throws AplException.ValidationException {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .deadlineValue("1440")
            .attachment(new CustomAttachment())
            .feeATM(blockchainConfig.getOneAPL())
            .build();
        assertThrows(RestParameterException.class, () -> txCreator.createTransactionThrowingException(request));

        TransactionCreator.TransactionCreationData data = txCreator.createTransaction(request);
        assertEquals(TransactionCreator.TransactionCreationData.ErrorType.MISSING_SECRET_PHRASE, data.getErrorType());
        verify(processor, never()).broadcast(any(Transaction.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1440", "0", "100.0", "5f"})
    void createTransaction_incorrectDeadline(String deadline) throws AplException.ValidationException {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .keySeed(Crypto.getKeySeed(secretPhrase))
            .deadlineValue(deadline)
            .attachment(new CustomAttachment())
            .feeATM(blockchainConfig.getOneAPL())
            .build();
        assertThrows(RestParameterException.class, () -> txCreator.createTransactionThrowingException(request));

        TransactionCreator.TransactionCreationData data = txCreator.createTransaction(request);
        assertEquals(TransactionCreator.TransactionCreationData.ErrorType.INCORRECT_DEADLINE, data.getErrorType());
        verify(processor, never()).broadcast(any(Transaction.class));
    }

    @ParameterizedTest
    @ValueSource(longs = {100_000_000L * 100, Long.MAX_VALUE})
    void createTransaction_notEnoughFunds(long amount) throws AplException.ValidationException {
        EcBlockData ecBlockData = new EcBlockData(121, 100_000);
        doReturn(ecBlockData).when(blockchain).getECBlock(0);
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .keySeed(Crypto.getKeySeed(secretPhrase))
            .amountATM(amount)
            .deadlineValue("1")
            .attachment(new CustomAttachment())
            .feeATM(1)
            .build();
        assertThrows(RestParameterException.class, () -> txCreator.createTransactionThrowingException(request));

        TransactionCreator.TransactionCreationData data = txCreator.createTransaction(request);
        assertEquals(TransactionCreator.TransactionCreationData.ErrorType.NOT_ENOUGH_APL, data.getErrorType());
        verify(processor, never()).broadcast(any(Transaction.class));
    }

    @Test
    void createTransaction_missingDeadline() throws AplException.ValidationException {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .keySeed(Crypto.getKeySeed(secretPhrase))
            .attachment(new CustomAttachment())
            .feeATM(blockchainConfig.getOneAPL())
            .build();
        assertThrows(RestParameterException.class, () -> txCreator.createTransactionThrowingException(request));

        TransactionCreator.TransactionCreationData data = txCreator.createTransaction(request);
        assertEquals(TransactionCreator.TransactionCreationData.ErrorType.MISSING_DEADLINE, data.getErrorType());
        verify(processor, never()).broadcast(any(Transaction.class));
    }


    @Test
    void createTransaction_correctFee() throws AplException.ValidationException {
        EcBlockData ecBlockData = new EcBlockData(121, 100_000);
        doReturn(ecBlockData).when(blockchain).getECBlock(0);
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .keySeed(Crypto.getKeySeed(secretPhrase))
            .deadlineValue("1")
            .attachment(new CustomAttachment())
            .feeATM(0)
            .broadcast(true)
            .build();
        doReturn(200_000_000L).when(calculator).getMinimumFeeATM(any(Transaction.class), anyInt());
//        doAnswer(invocation-> {
//            ((Transaction) invocation.getArgument(0)).sign(mock(Signature.class));
//            return null;
//        }).when(transactionSigner).sign(any(Transaction.class), any(byte[].class));
        mockSigning();

        Transaction tx = txCreator.createTransactionThrowingException(request);

        assertSame(transactionType, tx.getType());
        assertEquals(200_000_000, tx.getFeeATM());
        assertTrue(tx.getAttachment() instanceof EmptyAttachment);

        verify(processor).broadcast(tx);
    }

    @Test
    void createTransaction_correctFee_when_correction_enabled() throws AplException.ValidationException {
        EcBlockData ecBlockData = new EcBlockData(121, 100_000);
        doReturn(ecBlockData).when(blockchain).getECBlock(0);
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .publicKey(Convert.parseHexString(publicKey))
            .deadlineValue("1")
            .attachment(new CustomAttachment())
            .feeATM(1)
            .validate(true)
            .build();
        doReturn(200_000_000L).when(calculator).getMinimumFeeATM(any(Transaction.class), anyInt());
        doReturn(true).when(propertiesHolder).correctInvalidFees();
        Transaction tx = txCreator.createTransactionThrowingException(request);

        assertSame(transactionType, tx.getType());
        assertEquals(200_000_000, tx.getFeeATM());
        assertTrue(tx.getAttachment() instanceof EmptyAttachment);

        verify(processor, never()).broadcast(tx);
        verify(validator ).validateFully(tx);
    }

    @Test
    void testCreateTransaction_not_valid_on_unconfirmed() throws AplException.NotValidException {
        EcBlockData ecBlockData = new EcBlockData(121, 100_000);
        doReturn(ecBlockData).when(blockchain).getECBlock(0);
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .keySeed(Crypto.getKeySeed(publicKey))
            .deadlineValue("1")
            .attachment(new CustomAttachment())
            .feeATM(1000000)
            .broadcast(true)
            .build();
        doThrow(new AplAcceptableTransactionValidationException("Test. Not enough funds", mock(Transaction.class))).when(processor).broadcast(any(Transaction.class));
        mockSigning();
        assertThrows(RestParameterException.class, () -> txCreator.createTransactionThrowingException(request));

        TransactionCreator.TransactionCreationData data = txCreator.createTransaction(request);
        assertEquals(TransactionCreator.TransactionCreationData.ErrorType.VALIDATION_FAILED, data.getErrorType());
    }

    @Test
    void testCreateTransaction_featureNotEnabled() throws AplException.NotValidException {
        EcBlockData ecBlockData = new EcBlockData(121, 100_000);
        doReturn(ecBlockData).when(blockchain).getECBlock(0);
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .keySeed(Crypto.getKeySeed(publicKey))
            .deadlineValue("1")
            .attachment(new CustomAttachment())
            .feeATM(1000000)
            .broadcast(true)
            .build();
        Transaction tx = mock(Transaction.class);
        TransactionType type = mock(TransactionType.class);
        doReturn(type).when(tx).getType();
        doReturn(TransactionTypes.TransactionTypeSpec.ORDINARY_PAYMENT).when(type).getSpec();
        doThrow(new AplTransactionFeatureNotEnabledException("Test. Not enabled", tx)).when(processor).broadcast(any(Transaction.class));
        mockSigning();

        assertThrows(RestParameterException.class, () -> txCreator.createTransactionThrowingException(request));

        TransactionCreator.TransactionCreationData data = txCreator.createTransaction(request);
        assertEquals(TransactionCreator.TransactionCreationData.ErrorType.FEATURE_NOT_AVAILABLE, data.getErrorType());
    }

    @Test
    void testCreateTransaction_general_validation_failed() throws AplException.ValidationException {
        EcBlockData ecBlockData = new EcBlockData(121, 100_000);
        doReturn(ecBlockData).when(blockchain).getECBlock(0);
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .keySeed(Crypto.getKeySeed(publicKey))
            .deadlineValue("1")
            .attachment(new CustomAttachment())
            .feeATM(1000000)
            .broadcast(true)
            .build();
        doThrow(new AplUnacceptableTransactionValidationException("Test. Not valid", mock(Transaction.class))).when(processor).broadcast(any(Transaction.class));
//        doAnswer(invocation-> {
//            ((Transaction) invocation.getArgument(0)).sign(mock(Signature.class));
//            return null;
//        }).when(transactionSigner).sign(any(Transaction.class), any(byte[].class));
        mockSigning();

        assertThrows(RestParameterException.class, () -> txCreator.createTransactionThrowingException(request));

        TransactionCreator.TransactionCreationData data = txCreator.createTransaction(request);
        assertEquals(TransactionCreator.TransactionCreationData.ErrorType.VALIDATION_FAILED, data.getErrorType());
        assertEquals("Test. Not valid", data.getError());
    }

    @Test
    void testCreateTransaction_ecBlock_notValid() throws AplException.ValidationException {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .keySeed(Crypto.getKeySeed(publicKey))
            .deadlineValue("1")
            .ecBlockHeight(100)
            .ecBlockId(2)
            .attachment(new CustomAttachment())
            .feeATM(1000000)
            .broadcast(true)
            .build();
        doReturn(3L).when(blockchain).getBlockIdAtHeight(100);

        assertThrows(RestParameterException.class, () -> txCreator.createTransactionThrowingException(request));

        TransactionCreator.TransactionCreationData data = txCreator.createTransaction(request);
        assertEquals(TransactionCreator.TransactionCreationData.ErrorType.INCORRECT_EC_BLOCK, data.getErrorType());

        verify(processor, never()).broadcast(any(Transaction.class));
        verify(validator, never()).validateFully(any(Transaction.class));
    }


    private static class CustomAttachment extends EmptyAttachment {

        @Override
        public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
            return TransactionTypes.TransactionTypeSpec.ORDINARY_PAYMENT;
        }
    }

    private static class CustomTransactionType extends TransactionType {

        public CustomTransactionType(BlockchainConfig blockchainConfig, AccountService accountService) {
            super(blockchainConfig, accountService);
        }

        @Override
        public TransactionTypes.TransactionTypeSpec getSpec() {
            return TransactionTypes.TransactionTypeSpec.ORDINARY_PAYMENT;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.ORDINARY_PAYMENT;
        }

        @Override
        public AbstractAttachment parseAttachment(ByteBuffer buffer) {
            return null;
        }

        @Override
        public AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return null;
        }

        @Override
        public void doStateDependentValidation(Transaction transaction) {

        }

        @Override
        public void doStateIndependentValidation(Transaction transaction) {

        }

        @Override
        public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {

        }

        @Override
        public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {

        }

        @Override
        public boolean canHaveRecipient() {
            return true;
        }

        @Override
        public boolean isPhasingSafe() {
            return false;
        }

        @Override
        public String getName() {
            return "CustomTestType";
        }
    }

    private void mockSigning() throws AplException.NotValidException {
        String transactionUnsignedBytes = "6f4b6612125fb3a0daecd2799dfd6c9c299424fd920f9b308110a2c1fbd8f4436f4b6612125fb3a0daecd2799dfd6c9c299424fd920f9b308110a2c1fbd8f4436f4b6612125fb3a0daecd2799dfd6c9c299424fd920f9b308110a2c1fbd8f443";
        PayloadResult signedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
        signedTxBytes.getBuffer().write(Convert.parseHexString(transactionUnsignedBytes));
        doAnswer(invocation-> {
            Signature sig = mock(Signature.class);
            doReturn(Convert.parseHexString("6f4b6612125fb3a0daecd2799dfd6c9c299424fd920f9b308110a2c1fbd8f443")).when(sig).bytes();
            lenient().doReturn("6f4b6612125fb3a0daecd2799dfd6c9c299424fd920f9b308110a2c1fbd8f443").when(sig).getHexString();
            ((TransactionImpl) invocation.getArgument(0)).sign(sig, signedTxBytes);
            return null;
        }).when(transactionSigner).sign(any(Transaction.class), any(byte[].class));
    }
}