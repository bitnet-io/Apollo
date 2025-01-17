/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.smc;

import com.apollocurrency.aplwallet.apl.core.app.GenesisAccounts;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.JdbiConfiguration;
import com.apollocurrency.aplwallet.apl.core.config.NtpTimeConfig;
import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractEventLogModelToLogEntryConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractEventModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractModelToStateEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.SmcContractDetailsRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.impl.ReferencedTransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.publickey.PublicKeyTableProducer;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractEventLogTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractEventTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractMappingTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractStateTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractTable;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.model.EcBlockData;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.smc.SmcTxData;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.rest.service.ServerInfoService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.GeneratorService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockSerializer;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.DefaultBlockValidator;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.ReferencedTransactionService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionSigner;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionSignerImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountControlPhasingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.PublicKeyDao;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountControlPhasingServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountPublicKeyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractToolService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractRepository;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcFuelValidator;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.event.SmcContractEventManagerClassFactory;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcBlockchainIntegratorFactory;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcContractEventServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcContractRepositoryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcContractServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcContractStorageServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcContractToolServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcPostponedContractServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.txlog.SmcTxLogProcessor;
import com.apollocurrency.aplwallet.apl.core.signature.MultiSigCredential;
import com.apollocurrency.aplwallet.apl.core.transaction.CachedTransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.FeeCalculator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionApplier;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionVersionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractSmcAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixApplierRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixApplierRegistryInitializer;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixValidatorRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendixApplier;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.smc.service.mapping.SmcMappingRepositoryClassFactory;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * @author andrew.zinchenko@gmail.com
 */
@EnableWeld
@ExtendWith(MockitoExtension.class)
abstract class AbstractSmcTransactionTypeApplyTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer);

    BlockTestData btd = new BlockTestData();
    ServerInfoService serverInfoService = mock(ServerInfoService.class);
    PublicKeyDao publicKeyDao = mock(PublicKeyDao.class);
    AccountTable accountTable = new AccountTable(extension.getDatabaseManager(), mock(Event.class));
    PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    Chain chain = mock(Chain.class);

    {
        doReturn(chain).when(blockchainConfig).getChain();
    }

    Blockchain blockchain = mock(Blockchain.class);
    FeeCalculator calculator = mock(FeeCalculator.class);
    NtpTimeConfig ntpTimeConfig = new NtpTimeConfig();
    TimeService timeService = new TimeServiceImpl(ntpTimeConfig.time());
    TransactionTestData td = new TransactionTestData();

    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from(
            GlobalSyncImpl.class, DaoConfig.class, JdbiHandleFactory.class, JdbiConfiguration.class,
            AccountGuaranteedBalanceTable.class, PublicKeyTableProducer.class,
            AccountServiceImpl.class, GenesisAccounts.class, BlockChainInfoServiceImpl.class, AccountPublicKeyServiceImpl.class,
            FullTextConfigImpl.class, DerivedDbTablesRegistryImpl.class,
            DefaultBlockValidator.class, ReferencedTransactionService.class,
            PublicKeyAnnouncementAppendixApplier.class, AppendixApplierRegistry.class,
            AppendixValidatorRegistry.class, NtpTime.class,
            ReferencedTransactionDaoImpl.class, TransactionSignerImpl.class,
            TransactionValidator.class, TransactionApplier.class,
            SmcConfig.class, SmcBlockchainIntegratorFactory.class, SmcPostponedContractServiceImpl.class,
            SmcContractTable.class, SmcContractStateTable.class, SmcContractMappingTable.class, SmcContractEventTable.class, SmcContractEventLogTable.class,
            ContractModelToEntityConverter.class, ContractModelToStateEntityConverter.class,
            ContractEventLogModelToLogEntryConverter.class, ContractEventModelToEntityConverter.class,
            SmcContractRepositoryImpl.class, SmcContractServiceImpl.class, SmcContractToolServiceImpl.class, SmcContractStorageServiceImpl.class, SmcContractEventServiceImpl.class,
            SmcTxLogProcessor.class, SmcMappingRepositoryClassFactory.class, SmcContractEventManagerClassFactory.class,
            AppendixApplierRegistryInitializer.class
        )
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(mock(InMemoryCacheManager.class), InMemoryCacheManager.class))
        .addBeans(MockBean.of(mock(TaskDispatchManager.class), TaskDispatchManager.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(ntpTimeConfig, NtpTimeConfig.class))
        .addBeans(MockBean.of(timeService, TimeService.class))
        .addBeans(MockBean.of(accountTable, AccountTableInterface.class))
        .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class, PhasingPollServiceImpl.class))
        .addBeans(MockBean.of(serverInfoService, ServerInfoService.class))
        .addBeans(MockBean.of(mock(TransactionEntityToModelConverter.class), TransactionEntityToModelConverter.class))
        .addBeans(MockBean.of(mock(TransactionEntityRowMapper.class), TransactionEntityRowMapper.class))
        .addBeans(MockBean.of(mock(SmcContractDetailsRowMapper.class), SmcContractDetailsRowMapper.class))
        .addBeans(MockBean.of(mock(PrunableLoadingService.class), PrunableLoadingService.class))
        .addBeans(MockBean.of(mock(GeneratorService.class), GeneratorService.class))
        .addBeans(MockBean.of(publicKeyDao, PublicKeyDao.class))
        .addBeans(MockBean.of(mock(TransactionVersionValidator.class), TransactionVersionValidator.class))
        .addBeans(MockBean.of(mock(BlockSerializer.class), BlockSerializer.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(mock(AccountControlPhasingService.class), AccountControlPhasingService.class, AccountControlPhasingServiceImpl.class))
        .addBeans(MockBean.of(calculator, FeeCalculator.class))
        .build();

    @Inject
    AppendixApplierRegistryInitializer appendixApplierRegistryInitializer;
    @Inject
    TransactionApplier txApplier;
    @Inject
    AccountService accountService;
    @Inject
    SmcContractRepository contractRepository;
    @Inject
    SmcContractService contractService;
    @Inject
    ContractToolService contractToolService;
    @Inject
    SmcBlockchainIntegratorFactory integratorFactory;
    @Inject
    AppendixApplierRegistry registry;
    AccountService spyAccountService;

    @Inject
    TransactionValidator validator;
    TransactionProcessor processor = mock(TransactionProcessor.class);
    @Inject
    TransactionSigner signerService;

    TransactionCreator transactionCreator;

    TxBContext context;
    TransactionTypeFactory transactionTypeFactory;
    TransactionBuilderFactory transactionBuilderFactory;

    Block lastBlock = btd.BLOCK_12;

    SmcFuelValidator fuelValidator = new SmcFuelValidator(blockchainConfig);
    SmcConfig smcConfig = new SmcConfig();

    @BeforeAll
    static void beforeAll() {
        Convert2.init("APL", 0L);
    }

    @BeforeEach
    void setUp() {
        doReturn(chain).when(blockchainConfig).getChain();
        doReturn(lastBlock.getHeight()).when(blockchain).getHeight();

        EcBlockData ecBlockData = new EcBlockData(btd.BLOCK_10.getId(), btd.BLOCK_10.getHeight());
        doReturn(ecBlockData).when(blockchain).getECBlock(any(int.class));

        doReturn(lastBlock).when(blockchain).getLastBlock();

        spyAccountService = spy(accountService);
        context = TxBContext.newInstance(chain);
        transactionTypeFactory = new CachedTransactionTypeFactory(List.of(
            new SmcPublishContractTransactionType(blockchainConfig, blockchain, spyAccountService, contractRepository, contractToolService, fuelValidator, integratorFactory, smcConfig),
            new SmcCallMethodTransactionType(blockchainConfig, blockchain, spyAccountService, contractRepository, contractToolService, fuelValidator, integratorFactory, smcConfig)
        ));
        transactionBuilderFactory = new TransactionBuilderFactory(transactionTypeFactory, blockchainConfig);
        transactionCreator = new TransactionCreator(validator, propertiesHolder, timeService, calculator, blockchain, processor, transactionTypeFactory, transactionBuilderFactory, signerService, blockchainConfig);
    }

    Transaction createTransaction(SmcTxData body, AbstractSmcAttachment attachment, Account senderAccount) {
        return createTransaction(body, attachment, senderAccount, null);
    }

    Transaction createTransaction(SmcTxData body, AbstractSmcAttachment attachment, Account senderAccount, Long recipientId) {
        var txReqBuilder = CreateTransactionRequest.builder()
            .version(2)
            .senderAccount(senderAccount)
            .amountATM(body.getAmountATM())
            .feeATM(body.getFuelLimit() * body.getFuelPrice())
            .secretPhrase(body.getSecret())
            .deadlineValue(String.valueOf(1440))
            .attachment(attachment)
            .credential(new MultiSigCredential(1, Crypto.getKeySeed(body.getSecret())))
            .broadcast(false)
            .validate(false);
        if (recipientId != null) {
            txReqBuilder.recipientId(recipientId);
        }
        CreateTransactionRequest txRequest = txReqBuilder.build();

        Transaction transaction = transactionCreator.createTransactionThrowingException(txRequest);

        return transaction;

    }

}

