/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.dao.appdata.ShardDao;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.PrunableDbTable;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CopyDataCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CreateShardSchemaCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CsvExportCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation;
import com.apollocurrency.aplwallet.apl.core.shard.commands.DeleteCopiedDataCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.FinishShardingCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.UpdateSecondaryIndexCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.ZipArchiveCommand;
import com.apollocurrency.aplwallet.apl.core.shard.hash.ShardHashCalculator;
import com.apollocurrency.aplwallet.apl.core.shard.model.ExcludeInfo;
import com.apollocurrency.aplwallet.apl.core.shard.model.PrevBlockData;
import com.apollocurrency.aplwallet.apl.core.shard.model.TableInfo;
import com.apollocurrency.aplwallet.apl.core.shard.observer.events.ShardChangeStateEvent;
import com.apollocurrency.aplwallet.apl.core.shard.observer.events.ShardChangeStateEventBinding;
import com.apollocurrency.aplwallet.apl.db.updater.ShardAllScriptsDBUpdater;
import com.apollocurrency.aplwallet.apl.db.updater.ShardInitDBUpdater;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;
import org.slf4j.Logger;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Component for starting sharding process which contains several steps/states.
 *
 * @author yuriy.larin
 */
@Singleton
public class ShardMigrationExecutor {
    public static final String ACCOUNT_LEDGER = "account_ledger";
    private static final Logger log = getLogger(ShardMigrationExecutor.class);
    private final List<DataMigrateOperation> dataMigrateOperations = new ArrayList<>();

    private final javax.enterprise.event.Event<MigrateState> migrateStateEvent;
    private final ShardEngine shardEngine;
    private final ShardHashCalculator shardHashCalculator;
    private final ShardDao shardDao;
    private final ExcludedTransactionDbIdExtractor excludedTransactionDbIdExtractor;
    private final DerivedTablesRegistry derivedTablesRegistry;
    private final PrevBlockInfoExtractor prevBlockInfoExtractor;
    private volatile boolean backupDb;

    @Inject
    public ShardMigrationExecutor(ShardEngine shardEngine,
                                  javax.enterprise.event.Event<MigrateState> migrateStateEvent,
                                  ShardHashCalculator shardHashCalculator,
                                  ShardDao shardDao,
                                  ExcludedTransactionDbIdExtractor excludedTransactionDbIdExtractor,
                                  PrevBlockInfoExtractor prevBlockInfoExtractor,
                                  DerivedTablesRegistry registry,
                                  @Property(value = "apl.sharding.backupDb", defaultValue = "false") boolean backupDb) {
        this.shardEngine = Objects.requireNonNull(shardEngine, "managementReceiver is NULL");
        this.migrateStateEvent = Objects.requireNonNull(migrateStateEvent, "migrateStateEvent is NULL");
        this.shardHashCalculator = Objects.requireNonNull(shardHashCalculator, "sharding hash calculator is NULL");
        this.shardDao = Objects.requireNonNull(shardDao, "shardDao is NULL");
        this.excludedTransactionDbIdExtractor = Objects.requireNonNull(excludedTransactionDbIdExtractor, "exluded transaction db_id extractor is NULL");
        this.derivedTablesRegistry = Objects.requireNonNull(registry, "derived table registry is null");
        this.backupDb = backupDb;
        this.prevBlockInfoExtractor = Objects.requireNonNull(prevBlockInfoExtractor);
    }

    public boolean backupDb() {
        return backupDb;
    }

    public void setBackupDb(boolean backupDb) {
        this.backupDb = backupDb;
    }

    private void addCreateSchemaCommand(long shardId) {
        CreateShardSchemaCommand createShardSchemaCommand = new CreateShardSchemaCommand(shardId, shardEngine,
            new ShardInitDBUpdater(), /*hash should be null here*/ null, null);
        this.addOperation(createShardSchemaCommand);
    }

    @Transactional
    public void createAllCommands(int height, long shardId, MigrateState state) {
        int shardStartHeight = getShardStartHeight();
        log.info("Create commands for shard '{}' between heights[{},{}]", shardId, shardStartHeight, height);
        List<TableInfo> tableInfoList = null;
        ExcludeInfo excludeInfo = null;
        switch (state) {
            case INIT:
                if (backupDb) { // there is not backup-ing in mariadb using SQL command
//                    log.info("Will backup db before sharding");
//                    BackupDbBeforeShardCommand beforeShardCommand = new BackupDbBeforeShardCommand(shardEngine);
//                    this.addOperation(beforeShardCommand);
                } else {
                    addCreateSchemaCommand(shardId);
                }
            case MAIN_DB_BACKUPED:
                addCreateSchemaCommand(shardId); //should happen only when enabled backup
            case SHARD_SCHEMA_CREATED:
            case DATA_COPY_TO_SHARD_STARTED:
                excludeInfo = getOrInitExcludeInfo(excludeInfo, shardStartHeight, height);
                CopyDataCommand copyDataCommand = new CopyDataCommand(shardId, shardEngine, height, excludeInfo);
                this.addOperation(copyDataCommand);
            case DATA_COPY_TO_SHARD_FINISHED:
                byte[] hash = calculateHash(height);
                if (hash == null || hash.length <= 0) {
                    throw new IllegalStateException("Cannot calculate shard hash");
                }
                log.debug("SHARD HASH = {}", hash.length);
                PrevBlockData prevBlockData = prevBlockInfoExtractor.extractPrevBlockData(height, 3);
                CreateShardSchemaCommand createShardConstraintsCommand = new CreateShardSchemaCommand(shardId, shardEngine,
                    new ShardAllScriptsDBUpdater(), /*hash should be correct value*/ hash, prevBlockData);
                this.addOperation(createShardConstraintsCommand);
            case SHARD_SCHEMA_FULL:
            case SECONDARY_INDEX_STARTED:
                excludeInfo = getOrInitExcludeInfo(excludeInfo, shardStartHeight, height);
                UpdateSecondaryIndexCommand updateSecondaryIndexCommand = new UpdateSecondaryIndexCommand
                    (shardEngine, height, excludeInfo);
                this.addOperation(updateSecondaryIndexCommand);
            case SECONDARY_INDEX_FINISHED:
            case CSV_EXPORT_STARTED:
                excludeInfo = getOrInitExcludeInfo(excludeInfo, shardStartHeight, height);
                tableInfoList = getOrInitTableList(tableInfoList);
                CsvExportCommand csvExportCommand = new CsvExportCommand(shardEngine, ShardConstants.DEFAULT_COMMIT_BATCH_SIZE, height, tableInfoList, excludeInfo);

                this.addOperation(csvExportCommand);
            case CSV_EXPORT_FINISHED:
            case ZIP_ARCHIVE_STARTED:
                tableInfoList = getOrInitTableList(tableInfoList);
                ZipArchiveCommand zipArchiveCommand = new ZipArchiveCommand(shardId, tableInfoList, shardEngine);
                this.addOperation(zipArchiveCommand);
            case ZIP_ARCHIVE_FINISHED:
            case DATA_REMOVE_STARTED:
                excludeInfo = getOrInitExcludeInfo(excludeInfo, shardStartHeight, height);
                DeleteCopiedDataCommand deleteCopiedDataCommand =
                    new DeleteCopiedDataCommand(shardEngine, ShardConstants.DEFAULT_COMMIT_BATCH_SIZE, height, excludeInfo);
                this.addOperation(deleteCopiedDataCommand);
            case DATA_REMOVED_FROM_MAIN:
                FinishShardingCommand finishShardingCommand = new FinishShardingCommand(shardEngine, shardId);
                this.addOperation(finishShardingCommand);
                break;
            default:
                throw new IllegalArgumentException("Unable to create commands for state " + state);
        }
    }

    private List<TableInfo> getOrInitTableList(List<TableInfo> existingList) {
        if (existingList != null) {
            return existingList;
        }
        List<TableInfo> tableInfoList = derivedTablesRegistry.getDerivedTables()
            .stream()
            .filter(t -> !t.getName().equalsIgnoreCase(ACCOUNT_LEDGER))
            .map(t -> new TableInfo(t.getName(), t instanceof PrunableDbTable))
            .collect(Collectors.toList());
        List<TableInfo> coreTableInfoList = List.of(
            new TableInfo(ShardConstants.BLOCK_TABLE_NAME), new TableInfo(ShardConstants.TRANSACTION_TABLE_NAME),
            new TableInfo(ShardConstants.BLOCK_INDEX_TABLE_NAME), new TableInfo(ShardConstants.TRANSACTION_INDEX_TABLE_NAME),
            new TableInfo(ShardConstants.SHARD_TABLE_NAME));
        tableInfoList.addAll(coreTableInfoList);
        return tableInfoList;
    }

    private byte[] calculateHash(int height) {
        int lastShardHeight = getShardStartHeight();
        return shardHashCalculator.calculateHash(lastShardHeight, height);
    }

    private int getShardStartHeight() {
        Shard lastCompletedShard = shardDao.getLastCompletedOrArchivedShard(); // last shard is missing on the first time
        return lastCompletedShard != null ? lastCompletedShard.getShardHeight() : 0;
    }

    @Transactional
    public void prepare() {
        dataMigrateOperations.clear();
        shardEngine.prepare();
    }

    public void addOperation(DataMigrateOperation shardOperation) {
        Objects.requireNonNull(shardOperation, "operation is NULL");
        log.debug("Add {}", shardOperation);
        dataMigrateOperations.add(shardOperation);
    }

    public MigrateState executeAllOperations() {
        long start = System.currentTimeMillis();
        log.debug("START SHARDING...");
        MigrateState state = MigrateState.INIT;
        for (DataMigrateOperation dataMigrateOperation : dataMigrateOperations) {
            log.debug("Before execute {}", dataMigrateOperation);
            state = dataMigrateOperation.execute();
            log.debug("After execute step {} = '{}' before Fire Event...", dataMigrateOperation, state.name());
            migrateStateEvent.select(literal(state)).fire(state);
            if (state == MigrateState.FAILED) {
                log.warn("{} FAILED sharding...", dataMigrateOperation);
                break;
            }
        }
        log.debug("FINISHED SHARDING ----- '{}' in {} ms", state, System.currentTimeMillis() - start);
        return state;
    }

    public MigrateState executeOperation(DataMigrateOperation shardOperation) {
        dataMigrateOperations.add(shardOperation);
        return shardOperation.execute();
    }

    private AnnotationLiteral<ShardChangeStateEvent> literal(MigrateState migrateState) {
        return new ShardChangeStateEventBinding() {
            @Override
            public MigrateState value() {
                return migrateState;
            }
        };
    }

    private ExcludeInfo getOrInitExcludeInfo(ExcludeInfo existing, int startHeight, int finishHeight) {
        if (existing != null) {
            return existing;
        }
        return excludedTransactionDbIdExtractor.getExcludeInfo(startHeight, finishHeight);
    }

}
