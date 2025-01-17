/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.cache;

import com.apollocurrency.aplwallet.apl.dex.core.model.OrderFreezing;
import com.apollocurrency.aplwallet.apl.util.cache.CacheConfiguration;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheConfigurator;
import com.google.common.cache.CacheLoader;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
@Slf4j
public class AplCacheConfig implements InMemoryCacheConfigurator {

    private static final int ADDRESSABLE_MEM_PERCENT_FOR_CACHE = 30; //30 percent of Available memory;

    private CacheConfiguration[] cacheConfigurations;

    @Inject
    public AplCacheConfig(CacheLoader<Long, OrderFreezing> orderFreezingCacheLoader) {
        this.cacheConfigurations = new CacheConfiguration[]{
            new PublicKeyCacheConfig(60),
            new BlockIndexCacheConfig(60),
            new DexOrderFreezingCacheConfig(15, orderFreezingCacheLoader),
            new RemovedTxsCacheConfig(5),
            new AccountCacheConfig(30)
        };
    }

    @PostConstruct
    public void setUp() {
        log.debug("Runtime: maMemory={} totalMemory={}", Runtime.getRuntime().maxMemory(), Runtime.getRuntime().totalMemory());
        log.debug("Available for cache={}", getAvailableMemory());
    }

    @Override
    public long getAvailableMemory() {
        long mem = Math.min(Runtime.getRuntime().maxMemory(), Runtime.getRuntime().totalMemory()) / 100;

        mem = mem * ADDRESSABLE_MEM_PERCENT_FOR_CACHE;

        return mem;
    }

    @Override
    public List<CacheConfiguration> getConfiguredCaches() {
        return List.of(cacheConfigurations);
    }
}
