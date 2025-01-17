/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.hash;

import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.model.BlockImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.PrunableTxRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.TxReceiptRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ShardDao;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.config.JdbiConfiguration;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.service.state.AliasService;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.PublicKeyDao;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexServiceImpl;
import com.apollocurrency.aplwallet.apl.core.shard.ShardDbExplorerImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import javax.inject.Inject;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Slf4j

@Tag("slow")
@EnableWeld
public class ShardHashCalculatorImplTest extends DbContainerBaseTest {

    static final String SHA_256 = "SHA-256";
    static final byte[] PARTIAL_MERKLE_ROOT_2_6 = Convert.parseHexString("57a86e3f4966f6751d661fbb537780b65d4b0edfc1b01f48780a360c4babdea7");
    static final byte[] PARTIAL_MERKLE_ROOT_7_12 = Convert.parseHexString("da5ad74821dc77fa9fb0f0ddd2e48284fe630fee9bf70f98d7aa38032ddc8f57");
    static final byte[] PARTIAL_MERKLE_ROOT_1_8 = Convert.parseHexString("3987b0f2fb15fdbe3e815cbdd1ff8f9527d4dc18989ae69bc446ca0b40759a6b");

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer);
    BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    Chain chain = mock(Chain.class);
    PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    HeightConfig heightConfig = mock(HeightConfig.class);
    TransactionTestData ttd = new TransactionTestData();
    private final PublicKeyDao publicKeyDao = mock(PublicKeyDao.class);

    {
        doReturn(chain).when(blockchainConfig).getChain();
        doReturn(new PublicKey(1L, new byte[32], 2)).when(publicKeyDao).searchAll(anyLong());
    }

    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from(BlockchainImpl.class, ShardHashCalculatorImpl.class,
        BlockImpl.class, BlockDaoImpl.class,
        BlockEntityRowMapper.class, BlockEntityToModelConverter.class, BlockModelToEntityConverter.class,
        DerivedDbTablesRegistryImpl.class, TimeServiceImpl.class, GlobalSyncImpl.class, TransactionDaoImpl.class,
        DaoConfig.class,
        TransactionServiceImpl.class, ShardDbExplorerImpl.class,
        TransactionEntityRowMapper.class, TransactionEntityRowMapper.class, TxReceiptRowMapper.class, PrunableTxRowMapper.class,
        TransactionModelToEntityConverter.class, TransactionEntityToModelConverter.class,
        TransactionBuilderFactory.class, JdbiHandleFactory.class, JdbiConfiguration.class)
        .addBeans(
            MockBean.of(blockchainConfig, BlockchainConfig.class),
            MockBean.of(propertiesHolder, PropertiesHolder.class),
            MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class),
            MockBean.of(mock(PhasingPollService.class), PhasingPollService.class),
            MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class),
            MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class),
            MockBean.of(mock(NtpTime.class), NtpTime.class),
            MockBean.of(mock(BlockIndexService.class), BlockIndexService.class, BlockIndexServiceImpl.class),
            MockBean.of(mock(AliasService.class), AliasService.class)

        )
        .addBeans(MockBean.of(mock(PrunableLoadingService.class), PrunableLoadingService.class))
        .addBeans(MockBean.of(ttd.getTransactionTypeFactory(), TransactionTypeFactory.class))
        .addBeans(MockBean.of(publicKeyDao, PublicKeyDao.class))
        .build();

    @Inject
    ShardHashCalculator shardHashCalculator;
    BlockTestData td;

    @Inject
    Blockchain blockchain;

    @BeforeEach
    void setUp() {
        Mockito.doReturn(SHA_256).when(heightConfig).getShardingDigestAlgorithm();
        Mockito.doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        td = new BlockTestData();
        blockchain.setLastBlock(td.LAST_BLOCK);
    }

    @Test
    void testCalculateHashForAllBlocks() throws IOException {
        byte[] expectedFullMerkleRoot = hash(td.BLOCKS);

        byte[] merkleRoot1 = shardHashCalculator.calculateHash(td.GENESIS_BLOCK.getHeight(), td.LAST_BLOCK.getHeight() + 1);
        byte[] merkleRoot2 = shardHashCalculator.calculateHash(td.GENESIS_BLOCK.getHeight() - 100, td.LAST_BLOCK.getHeight() + 200);
        byte[] merkleRoot3 = shardHashCalculator.calculateHash(td.GENESIS_BLOCK.getHeight(), td.LAST_BLOCK.getHeight() + 20000);
        assertArrayEquals(expectedFullMerkleRoot, merkleRoot1);
        assertArrayEquals(expectedFullMerkleRoot, merkleRoot2);
        assertArrayEquals(expectedFullMerkleRoot, merkleRoot3);
    }

    private byte[] hash(List<Block> blocks) {
        try {
            MerkleTree merkleTree = new MerkleTree(MessageDigest.getInstance(SHA_256));
            blocks.stream().map(Block::getBlockSignature).forEach(merkleTree::appendLeaf);
            merkleTree.appendLeaf(td.GENESIS_BLOCK.getGenerationSignature());
            return merkleTree.getRoot().getValue();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testCalculateHashWhenNoBlocks() throws IOException {

        byte[] merkleRoot = shardHashCalculator.calculateHash(td.LAST_BLOCK.getHeight() + 1, td.LAST_BLOCK.getHeight() + 100_000);

        Assertions.assertNull(merkleRoot);
    }

    @Test
    void testCalculateHashForMiddleBlocks() throws IOException {
        byte[] merkleRoot = shardHashCalculator.calculateHash(td.BLOCK_1.getHeight(), td.BLOCK_5.getHeight());
        assertArrayEquals(PARTIAL_MERKLE_ROOT_2_6, merkleRoot);
    }

    @Test
    void testCalculateHashForFirstBlocks() throws IOException {

        byte[] merkleRoot = shardHashCalculator.calculateHash(0, td.BLOCK_8.getHeight());
        assertArrayEquals(PARTIAL_MERKLE_ROOT_1_8, merkleRoot);
    }

    @Test
    void testCalculateHashForLastBlocks() throws IOException {
        byte[] merkleRoot = shardHashCalculator.calculateHash(td.BLOCK_6.getHeight(), td.BLOCK_11.getHeight() + 1);
        assertArrayEquals(PARTIAL_MERKLE_ROOT_7_12, merkleRoot);
    }

    @Test
    void testCreateShardingHashCalculatorWithZeroBlockSelectLimit() throws IOException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ShardHashCalculatorImpl(mock(Blockchain.class), mock(BlockchainConfig.class), mock(ShardDao.class), 0));
    }

//    @Test
//    public void testCalculateShardingHashFromMainDb() {
//        DbProperties dbFileProperties = DbTestData.getDbFileProperties(Paths.get("unit-test-db").resolve(Constants.APPLICATION_DIR_NAME).toAbsolutePath().toString());
//        TransactionalDataSource transactionalDataSource = new TransactionalDataSource(dbFileProperties, new PropertiesHolder());
//        transactionalDataSource.init(new DbVersion() {
//            @Override
//            protected int update(int nextUpdate) {return 260;} //do not modify original db!!!
//        });
//        Mockito.doReturn(transactionalDataSource).when(databaseManager).getDataSource();
//        byte[] bytes = shardHashCalculator.calculateHash(0, 2_000_000);
//    }
}
