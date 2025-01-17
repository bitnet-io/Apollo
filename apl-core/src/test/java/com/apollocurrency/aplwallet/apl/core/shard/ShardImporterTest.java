/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ShardDao;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.DataTagDao;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ShardState;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.DataTag;
import com.apollocurrency.aplwallet.apl.core.files.DownloadableFilesManager;
import com.apollocurrency.aplwallet.apl.core.files.shards.ShardPresentData;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.shard.helper.CsvImporter;
import com.apollocurrency.aplwallet.apl.core.shard.helper.CsvImporterImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.ValueParserImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvEscaper;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvEscaperImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.ValueParser;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.ChunkedFileOps;
import com.apollocurrency.aplwallet.apl.util.Zip;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@Slf4j
@Tag("slow")
@EnableWeld
@ExtendWith(MockitoExtension.class)
class ShardImporterTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer);
    @RegisterExtension
    TemporaryFolderExtension folder = new TemporaryFolderExtension();

    BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);
    @Mock
    private ShardDao shardDao;
    @Mock
    private Blockchain blockchain;
    private DerivedTablesRegistry derivedTablesRegistry = mock(DerivedTablesRegistry.class);
    @Mock
    private BlockchainConfig blockchainConfig;
    @Mock
    private DownloadableFilesManager downloadableFilesManager;
    @Mock
    private AplAppStatus aplAppStatus;
    @Mock
    private Zip zipComponent;
    @Mock
    private ChunkedFileOps fopsComponent;
    @Mock
    private GenesisImporter genesisImporter;
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(DataTagDao.class, FullTextConfigImpl.class,
        ValueParserImpl.class, CsvEscaperImpl.class)
        .addBeans(
            MockBean.of(derivedTablesRegistry, DerivedTablesRegistry.class),
            MockBean.of(blockchainProcessor, BlockchainProcessor.class),
            MockBean.of(extension.getDatabaseManager(), DatabaseManager.class),
            MockBean.of(genesisImporter, GenesisImporter.class))
        .build();

    @Inject
    private DataTagDao dataTagDao;
    @Inject
    private ValueParser parser;
    @Inject
    private CsvEscaper translator;
    private CsvImporter csvImporter;
    private ShardImporter shardImporter;


    private DataTag dataTag_1 = new DataTag(4L, 3500, "tag2", 3);
    private DataTag dataTag_2 = new DataTag(5L, 3500, "tag3", 3);
    private DataTag dataTag_3 = new DataTag(10L, 8000, "iambatman", 1);
    private DataTag dataTag_4 = new DataTag(7L, 3500, "newtag", 1);
    private DataTag dataTag_5 = new DataTag(1L, 2000, "tag1", 1);
    private DataTag dataTag_6 = new DataTag(6L, 3500, "tag4", 1);
    private UUID chainId = UUID.fromString("2f2b6149-d29e-41ca-8c0d-f3343f5540c6");

    @BeforeEach
    void setUp() {
        csvImporter = new CsvImporterImpl(folder.newFolder("csv-import").toPath(), extension.getDatabaseManager(), aplAppStatus, parser, translator);
        shardImporter = spy(new ShardImporter(shardDao, blockchainConfig, genesisImporter,
            blockchain, derivedTablesRegistry, csvImporter, zipComponent, dataTagDao, downloadableFilesManager, aplAppStatus));
    }

    @Test
    void importShardByFileIdFaile() {
        assertThrows(NullPointerException.class, () -> shardImporter.importShardByFileId(
            new ShardPresentData(null, "fileId", List.of()))
        );
    }

    @Test
    void importLastShardFailed() {
        assertThrows(NullPointerException.class, () -> shardImporter.importLastShard(-1));
    }

    @Test
    void testCanImportForZeroHeight() {
        boolean result = shardImporter.canImport(0);
        assertTrue(result);
    }

    @Test
    void testCanImportForDisabledSharding() {
        HeightConfig heightConfig = mock(HeightConfig.class);
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        boolean result = shardImporter.canImport(1);
        assertTrue(result);
    }

    @Test
    void testCanImportForEnabledShardingWithoutStoredShads() {
        HeightConfig heightConfig = mock(HeightConfig.class);
        doReturn(true).when(heightConfig).isShardingEnabled();
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        boolean result = shardImporter.canImport(1);
        assertTrue(result);
    }

    @Test
    void testCanImportForEnableShardingWithFullLastShard() {
        HeightConfig heightConfig = mock(HeightConfig.class);
        doReturn(true).when(heightConfig).isShardingEnabled();
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        Shard lastShard = new Shard();
        lastShard.setShardState(ShardState.FULL);
        doReturn(lastShard).when(shardDao).getLastShard();
        boolean result = shardImporter.canImport(1);
        assertTrue(result);
    }

    @Test
    void testCanImportForEnabledShardingWithArchiveLastShard() {
        HeightConfig heightConfig = mock(HeightConfig.class);
        doReturn(true).when(heightConfig).isShardingEnabled();
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        Shard lastShard = new Shard();
        lastShard.setShardState(ShardState.CREATED_BY_ARCHIVE);
        doReturn(lastShard).when(shardDao).getLastShard();
        boolean result = shardImporter.canImport(1);
        assertTrue(result);
    }

    @Test
    void testCanImportForEnabledShardingWithInitStateForLastShard() {
        HeightConfig heightConfig = mock(HeightConfig.class);
        doReturn(true).when(heightConfig).isShardingEnabled();
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        Shard lastShard = new Shard();
        lastShard.setShardState(ShardState.INIT);
        doReturn(lastShard).when(shardDao).getLastShard();
        boolean result = shardImporter.canImport(1);
        assertFalse(result);
    }


    @Test
    void testImportShardWhenZipCorrupted() {
        doReturn(Paths.get("")).when(downloadableFilesManager).mapFileIdToLocalPath("fileId");
        assertThrows(ShardArchiveProcessingException.class, () -> shardImporter.importShard(
            new ShardPresentData(null, "fileId", List.of()), List.of(), true)
        );
        verify(aplAppStatus).durableTaskFinished(any(), anyBoolean(), anyString());
    }

    @Test
    void testImportShardWhenLastShardWasNotSaved() throws Exception {
        doReturn(Paths.get("")).when(downloadableFilesManager).mapFileIdToLocalPath("fileId");
        doReturn(true).when(zipComponent).extract(Paths.get("").toAbsolutePath().toString(), csvImporter.getDataExportPath().toAbsolutePath().toString(), true);
        doNothing().when(genesisImporter).importGenesisJson(true);

        assertThrows(IllegalStateException.class, () -> shardImporter.importShard(
            new ShardPresentData(null, "fileId", List.of()), List.of("block_index"), true)
        );
        verify(aplAppStatus, times(4)).durableTaskUpdate(any(), anyString(), anyDouble());
        verify(aplAppStatus).durableTaskFinished(any(), anyBoolean(), anyString());
    }

    @Test
    void testImportShardWhenExceptionOccurredDuringImport() {
        doReturn(Paths.get("")).when(downloadableFilesManager).mapFileIdToLocalPath("fileId");
        doReturn(true).when(zipComponent).extract(Paths.get("").toAbsolutePath().toString(), csvImporter.getDataExportPath().toAbsolutePath().toString(), true);
        doNothing().when(genesisImporter).importGenesisJson(true);
        doReturn(null).when(aplAppStatus).durableTaskUpdate(null, 50.0, "Public keys were imported");
        doThrow(new IllegalArgumentException()).when(aplAppStatus).durableTaskUpdate(null, "Loading 'shard'", 0.6);

        assertThrows(RuntimeException.class, () -> shardImporter.importShard(
            new ShardPresentData(null, "fileId", List.of()), List.of(), true)
        );
        verify(aplAppStatus).durableTaskFinished(any(), anyBoolean(), anyString());
    }

    @Test
    void testImportShardWhenShardTableWasExcluded() {
        doReturn(Paths.get("")).when(downloadableFilesManager).mapFileIdToLocalPath("fileId");
        doReturn(true).when(zipComponent).extract(Paths.get("").toAbsolutePath().toString(), csvImporter.getDataExportPath().toAbsolutePath().toString(), true);
        doNothing().when(genesisImporter).importGenesisJson(true);
        doReturn(List.of()).when(derivedTablesRegistry).getDerivedTables();

        shardImporter.importShard(
            new ShardPresentData(null, "fileId", List.of()), List.of(ShardConstants.SHARD_TABLE_NAME, ShardConstants.TRANSACTION_INDEX_TABLE_NAME), true);
        verify(aplAppStatus, times(3)).durableTaskUpdate(any(), anyString(), anyDouble());
        verify(aplAppStatus).durableTaskFinished(null, false, "Shard data import"); //success
    }

    @Test
    void testImportShardWhenLastShardExist() {
        doReturn(Paths.get("")).when(downloadableFilesManager).mapFileIdToLocalPath("fileId");
        doReturn(true).when(zipComponent).extract(Paths.get("").toAbsolutePath().toString(), csvImporter.getDataExportPath().toAbsolutePath().toString(), true);
        doNothing().when(genesisImporter).importGenesisJson(true);
        doReturn(List.of()).when(derivedTablesRegistry).getDerivedTableNames();
        Shard lastShard = new Shard();
        lastShard.setShardState(ShardState.INIT);
        lastShard.setShardId(1L);
        doReturn(lastShard).when(shardDao).getLastShard();

        shardImporter.importShard(
            new ShardPresentData(lastShard.getShardId(), "fileId", List.of()), List.of(ShardConstants.TRANSACTION_INDEX_TABLE_NAME), true);

        verify(shardDao).updateShard(lastShard);
        assertEquals(ShardState.CREATED_BY_ARCHIVE, lastShard.getShardState());
        byte[] hash = lastShard.getCoreZipHash();
        assertEquals(null, hash);
        verify(aplAppStatus, times(4)).durableTaskUpdate(any(), anyString(), anyDouble());
        verify(aplAppStatus).durableTaskFinished(null, false, "Shard data import"); //success
    }

    @Test
    void testImportAccountTaggedDataWithDataTags() throws IOException {
        doReturn(Paths.get("")).when(downloadableFilesManager).mapFileIdToLocalPath("fileId");
        doReturn(true).when(zipComponent).extract(Paths.get("").toAbsolutePath().toString(), csvImporter.getDataExportPath().toAbsolutePath().toString(), true);
        doNothing().when(genesisImporter).importGenesisJson(true);
        doReturn(List.of(ShardConstants.GOODS_TABLE_NAME, ShardConstants.ACCOUNT_TABLE_NAME, ShardConstants.TAGGED_DATA_TABLE_NAME)).when(derivedTablesRegistry).getDerivedTableNames();

        DatabaseManager databaseManager = extension.getDatabaseManager();
        TransactionalDataSource dataSource = databaseManager.getDataSource();

        DbUtils.inTransaction(dataSource, (con) -> dataTagDao.truncate());

        Block block = mock(Block.class);
        doReturn(1000).when(block).getHeight();
        doReturn(block).when(blockchain).findFirstBlock();
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("tagged_data.csv");
        assertNotNull(resourceAsStream);
        Files.copy(resourceAsStream, csvImporter.getDataExportPath().resolve("tagged_data.csv"));
        InputStream resourceAsStreamAccount = getClass().getClassLoader().getResourceAsStream("account.csv");
        assertNotNull(resourceAsStreamAccount);
        Files.copy(resourceAsStreamAccount, csvImporter.getDataExportPath().resolve("account.csv"));

        DbUtils.inTransaction(dataSource, (con) -> {
            shardImporter.importShard(
                new ShardPresentData(null, "fileId", List.of()), List.of(ShardConstants.SHARD_TABLE_NAME), true);
            dataSource.commit(false);
        });

        List<DataTag> allTags = CollectionUtil.toList(dataTagDao.getAllTags(0, Integer.MAX_VALUE));
        assertEquals(6, allTags.size());
        List<DataTag> expected = List.of(this.dataTag_1, dataTag_2, dataTag_3, dataTag_4, dataTag_5, dataTag_6);
        assertEquals(expected, allTags);

        DbUtils.inTransaction(extension, (con) -> {
            try {
                ResultSet rs = con.createStatement().executeQuery("select avg(height) from account");
                rs.next();
                assertEquals(1000.0, rs.getDouble(1), 0.00001);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        verify(aplAppStatus, times(7)).durableTaskUpdate(any(), anyString(), anyDouble());
        verify(aplAppStatus).durableTaskFinished(null, false, "Shard data import"); //success
    }

    @Test
    void testImportShardDerivedTablesWithException() {
        doReturn(Paths.get("")).when(downloadableFilesManager).mapFileIdToLocalPath("fileId");
        doReturn(true).when(zipComponent).extract(Paths.get("").toAbsolutePath().toString(), csvImporter.getDataExportPath().toAbsolutePath().toString(), true);
        doNothing().when(genesisImporter).importGenesisJson(true);
        ArrayList<String> derivedTableNames = new ArrayList<>();
        derivedTableNames.add(null);
        doReturn(derivedTableNames).when(derivedTablesRegistry).getDerivedTableNames();

        assertThrows(RuntimeException.class, () -> shardImporter.importShard(
            new ShardPresentData(null, "fileId", List.of()), List.of(ShardConstants.SHARD_TABLE_NAME, ShardConstants.TRANSACTION_INDEX_TABLE_NAME), true)
        );

        verify(aplAppStatus).durableTaskFinished(null, true, "Shard data import");
    }

    @Test
    void testImportByFileId() {
        doReturn(Paths.get("")).when(downloadableFilesManager).mapFileIdToLocalPath("fileId");
        doReturn(true).when(zipComponent).extract(Paths.get("").toAbsolutePath().toString(), csvImporter.getDataExportPath().toAbsolutePath().toString(), true);
        doNothing().when(genesisImporter).importGenesisJson(true);
        doReturn(List.of()).when(derivedTablesRegistry).getDerivedTableNames();
        doReturn(mock(Shard.class)).when(shardDao).getLastShard();

        shardImporter.importShardByFileId(new ShardPresentData(null, "fileId", List.of()));

        verify(blockchain).update();
        verify(blockchainProcessor).resumeBlockchainDownloading();
        verify(aplAppStatus).durableTaskFinished(null, false, "Shard data import");
    }

    @Test
    void testImportLastShardForZeroHeight() {
        doNothing().when(genesisImporter).importGenesisJson(false);

        shardImporter.importLastShard(0);

        verify(genesisImporter).importGenesisJson(false);
        verifyNoInteractions(aplAppStatus, zipComponent, downloadableFilesManager, blockchain, blockchainConfig, blockchainProcessor);
    }

    @Test
    void testImportLastShard() {
        doReturn(Paths.get("")).when(downloadableFilesManager).mapFileIdToLocalPath("chain::" + chainId + ";shard::1");
        doReturn(true).when(zipComponent).extract(Paths.get("").toAbsolutePath().toString(), csvImporter.getDataExportPath().toAbsolutePath().toString(), true);
        doNothing().when(genesisImporter).importGenesisJson(true);
        doReturn(List.of(ShardConstants.GOODS_TABLE_NAME, ShardConstants.PHASING_POLL_TABLE_NAME, ShardConstants.TAGGED_DATA_TABLE_NAME)).when(derivedTablesRegistry).getDerivedTableNames();
        Shard lastShard = new Shard(1, 100);
        doReturn(lastShard).when(shardDao).getLastCompletedOrArchivedShard();
        doReturn(lastShard).when(shardDao).getLastShard();
        Chain chain = mock(Chain.class);
        doReturn(chainId).when(chain).getChainId();
        doReturn(chain).when(blockchainConfig).getChain();

        shardImporter.importLastShard(1);
        verify(aplAppStatus, times(3)).durableTaskUpdate(any(), anyString(), anyDouble()); // number of tables imported
        verify(aplAppStatus).durableTaskFinished(null, false, "Shard data import");
    }
}