/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.chainid;

import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Extract  chains from config file and provide current active chain
 * @see Chain
 * @see BlockchainConfig
 */
@Singleton
public class ChainIdServiceImpl implements ChainIdService {
    private static final String DEFAULT_CONFIG_LOCATION = "conf/chains.json";
    private final String chainsConfigFileLocations;

    public ChainIdServiceImpl(String chainsConfigFileLocation) {
        this.chainsConfigFileLocations = chainsConfigFileLocation;
    }

    public ChainIdServiceImpl() {
        this(DEFAULT_CONFIG_LOCATION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Chain> getAll() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        InputStream is = getClass().getClassLoader().getResourceAsStream(chainsConfigFileLocations);
        if (is == null) {
            is = Files.newInputStream(Paths.get(chainsConfigFileLocations));
        }
        return mapper.readValue(is, new TypeReference<List<Chain>>() {});
    }

    /**
     * {@inheritDoc}
     * @throws RuntimeException when no active chain available
     * @throws RuntimeException when multiple active chains available
     */
    @Override
    public Chain getActiveChain() throws IOException {
        List<Chain> chains = getAll();
        List<Chain> activeChains = chains.stream().filter(Chain::isActive).collect(Collectors.toList());
        if (activeChains.size() == 0) {
            throw new RuntimeException("No active chain specified!");
        } else if (activeChains.size() > 1) {
            throw new RuntimeException("Only one chain can be active at the moment!");
        }
        return activeChains.get(0);
    }
}
