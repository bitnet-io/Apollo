/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.db.updater.DBUpdater;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Interface for database shard management.
 *
 * @author yuriy.larin
 */
public interface ShardManagement {

    long TEMP_DB_IDENTITY = -1L;

    /**
     * Number of shard data sources totally keeped in cache
     */
    long MAX_CACHED_SHARDS_NUMBER = 6;

    /**
     * Total time after latest cache entry access before it gets evicted from cache.
     */
    long SHARD_EVICTION_TIME = 15;


    void initFullShards(Collection<Long> ids);

    void addFullShard(Long shard);

    /**
     * That is preferred way to retrieve cached shard data source or create it fully initialized
     *
     * @param shardId shard Id to be added, can be NULL then an next shardId is selected from 'SHARD' table
     * @return shard database connection pool instance is put into internal cache
     */
    TransactionalDataSource getOrCreateShardDataSourceById(Long shardId);

    /**
     * That is preferred way to retrieve cached shard data source or create it fully or partially initialized.
     * The initialization schema is specified by dbUpdater implementation class.
     *
     * @param shardId   shard Id to be added, can be NULL then an next shardId is selected from 'SHARD' table
     * @param dbUpdater 'partial' or 'full' kind of 'schema script' implementation class can be supplied
     * @return shard database connection pool instance is put into internal cache
     */
    TransactionalDataSource getOrCreateShardDataSourceById(Long shardId, DBUpdater dbUpdater);

    /**
     * Return already initialized or init and then return transactional datasource for full shard specified by shardId
     *
     * @param shardId id of the full shard
     * @return datasource for full shard if exist, otherwise - null
     */
    TransactionalDataSource getOrInitFullShardDataSourceById(long shardId);

    /**
     * @param shardId id of shard datasorce
     * @return Return datasource for shard by id if exists, otherwise - null
     */
    TransactionalDataSource getShardDataSourceById(long shardId);

    /**
     * Method gives ability to create new 'shard database' file with partially initialized internal schema.
     * It opens existing shard file and adds it into cached shard data source list.
     * Partial schema is specified by dbVersion implementation
     *
     * @param shardId   shard Id to be added, can be NULL then an next shardId is selected from 'SHARD' table
     * @param dbUpdater 'partial' or 'full' kind of 'schema script' implementation class can be supplied
     * @return shard database connection pool instance is put into internal cache
     */
    TransactionalDataSource createOrUpdateShard(Long shardId, DBUpdater dbUpdater);


    /**
     * Return list of data sources with state = FULL. Each datasource point to not empty shard db, which store blocks and transactions for specific shard
     *
     * @return list of full shard data sources
     */
    List<TransactionalDataSource> getAllFullDataSources(Long numberOfShards);

    /**
     * Return Iterator of data sources with state = FULL. Each datasource point to not empty shard db, which store blocks and transactions for specific shard
     *
     * @return list of full shard data sources
     */
    Iterator<TransactionalDataSource> getAllFullDataSourcesIterator();


    /**
     * Return Iterator for all data sources, including full shard data sources with the current data source, sorted using
     * specified comparator.
     * <p>Default order is from earlier shards to recent, last is current datasource</p>
     * @return all data sources iterator sorted using specified comparator
     */
    Iterator<TransactionalDataSource> getAllSortedDataSourcesIterator(Comparator<TransactionalDataSource> comparator);

    /**
     * Close all datasources related to shards, this method will close all opened datasources excluding current main datasource
     *
     * @return number of closed datasources
     */
    long closeAllShardDataSources();

}
