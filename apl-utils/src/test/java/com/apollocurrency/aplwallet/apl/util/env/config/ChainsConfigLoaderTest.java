/*
 *  Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.env.config;

import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ChainsConfigLoaderTest {

    private static final List<BlockchainProperties> BLOCKCHAIN_PROPERTIES1 = Arrays.asList(
        new BlockchainProperties(0, 255, 160, 1, 60, 67, 53, 30000000000L),
        new BlockchainProperties(2000, 300, 160, 0, 2, 4, 1, 30000000000L,
            new ShardingSettings(false),
            new ConsensusSettings(ConsensusSettings.Type.POS,
                new AdaptiveForgingSettings(true, 60, 0)),
            new TransactionFeeSettings(), new SmcSettings()),
        new BlockchainProperties(42300, 300, 160, 0, 2, 4, 1, 30000000000L,
            new ShardingSettings(true),
            new ConsensusSettings(new AdaptiveForgingSettings(true, 10, 0)),
            new TransactionFeeSettings(), new SmcSettings()),
        new BlockchainProperties(100000, 300, 160, 0, 2, 4, 1, 30000000000L,
            new ShardingSettings(true, 1_000_000),
            new ConsensusSettings(new AdaptiveForgingSettings(true, 10, 0)),
            new TransactionFeeSettings(Map.of((short) 0x0000, new FeeRate((byte) 0, (byte) 0, 22, BigDecimal.TEN, null, null),
                (short) 0x0001, new FeeRate((byte) 0, (byte) 1, 0, null, new BigDecimal("22.1111"), new BigDecimal[]{new BigDecimal("1.2"), new BigDecimal("2.3222"), new BigDecimal("1.111")}),
                (short) 0x0101, new FeeRate((byte) 1, (byte) 1, 100, null, null, new BigDecimal[0]))), new SmcSettings("1234567890")),
        new BlockchainProperties(100100, 300, 160, 0, 5, 7, 2, 30000000000L,
            new ShardingSettings(true, "SHA-512"), new ConsensusSettings(new AdaptiveForgingSettings()), new TransactionFeeSettings(), new SmcSettings("abbc0123"))
    );
    private static final List<BlockchainProperties> BLOCKCHAIN_PROPERTIES2 = Collections.singletonList(
        new BlockchainProperties(0, 2000, 160, 10, 2, 3, 1, (long) 1e8)
    );
    private static final String CONFIG_NAME = "test-chains.json";
    private static final String OLD_CONFIG_NAME = "old-chains.json";
    private static UUID chainId1 = UUID.fromString("3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6");
    private static final Chain CHAIN1 = new Chain(chainId1, true, Collections.emptyList(), Arrays.asList("51.15.250.32",
        "51.15.253.171",
        "51.15.210.116",
        "51.15.242.197",
        "51.15.218.241"),
        Collections.emptyList(),
        "Apollo experimental testnet",
        "NOT STABLE testnet for experiments. Don't use it if you don't know what is it", "Apollo",
        "APL", "Apollo",
        30000000000L, 8,
        BLOCKCHAIN_PROPERTIES1, new FeaturesHeightRequirement(), Set.of(102,103,105), Set.of("1000", "18446744073709551615"));
    private static UUID chainId2 = UUID.fromString("ff3bfa13-3711-4f23-8f7b-4fccaa87c4c1");
    private static final Chain CHAIN2 = new Chain(chainId2, Arrays.asList("51.15.0.1",
        "51.15.1.0"),
        "Gotham",
        "Batman's chain", "BTM",
        "BTM", "I am batman!",
        30000000000L, 8,
        BLOCKCHAIN_PROPERTIES2);
    private static final Chain CHAIN3 = new Chain(chainId2, false, Arrays.asList("51.15.1.1",
        "51.15.0.0"), Collections.emptyList(), Collections.emptyList(), "1", "2", "3", "4", "5",
        30000000000L, 8,
        BLOCKCHAIN_PROPERTIES1.subList(0, 3), new FeaturesHeightRequirement(150, 150, 150, null, null), Set.of(), null);
    private Path tempRootPath;

    @BeforeEach
    public void setUp() throws IOException {
        tempRootPath = Files.createTempDirectory("chains-config-loader-test-root");
    }

    @AfterEach
    public void tearDown() throws IOException {
        Files.walkFileTree(tempRootPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return super.visitFile(file, attrs);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return super.postVisitDirectory(dir, exc);
            }
        });
    }

    @Test
    public void testLoadConfig() {
        ChainsConfigLoader chainsConfigLoader = new ChainsConfigLoader(CONFIG_NAME);
        Map<UUID, Chain> loadedChains = chainsConfigLoader.load();
        assertEquals(2, loadedChains.size());
        Map<UUID, Chain> expectedChains = Map.of(CHAIN1.getChainId(), CHAIN1, CHAIN2.getChainId(), CHAIN2);
        Assertions.assertNotNull(loadedChains);
//        Chain chain = loadedChains.get(chainId1);
//        assertEquals(CHAIN1, chain);
        assertEquals(expectedChains.entrySet(), loadedChains.entrySet());
    }

    @Test
    public void testLoadOldConfig() {
        ChainsConfigLoader chainsConfigLoader = new ChainsConfigLoader(false, null, OLD_CONFIG_NAME);
        Map<UUID, Chain> loadedChains = chainsConfigLoader.load();
        assertEquals(1, loadedChains.size());
        assertEquals(Map.of(CHAIN3.getChainId(), CHAIN3), loadedChains);
    }

    @Test
    void testLoadOldConfigWithNew() throws IOException {
        Path oldConfigFile = tempRootPath.resolve("test-chains.json");
        JSON.getMapper().writerWithDefaultPrettyPrinter().writeValue(oldConfigFile.toFile(), Arrays.asList(CHAIN3));
        ChainsConfigLoader chainsConfigLoader = new ChainsConfigLoader(false, tempRootPath.toAbsolutePath().toString(), CONFIG_NAME);
        Map<UUID, Chain> loadedChains = chainsConfigLoader.load();
        assertEquals(2, loadedChains.size());
        assertEquals(Map.of(CHAIN3.getChainId(), CHAIN3, CHAIN1.getChainId(), CHAIN1), loadedChains);
    }

    @Test
    void testLoadAndSaveConfig() throws IOException {
        ChainsConfigLoader chainsConfigLoader = new ChainsConfigLoader(CONFIG_NAME);
        Map<UUID, Chain> loadedChains = chainsConfigLoader.load();
        assertEquals(2, loadedChains.size());
        Path file = tempRootPath.resolve("new-config");
        JSON.getMapper().writerWithDefaultPrettyPrinter().writeValue(file.toFile(), loadedChains.values());
        chainsConfigLoader = new ChainsConfigLoader(true, tempRootPath.toAbsolutePath().toString(), "new-config");
        Map<UUID, Chain> reloadedChains = chainsConfigLoader.load();
        assertEquals(loadedChains, reloadedChains);
    }

    @Test
    void testLoadResourceAndUserDefinedConfig() throws IOException {
        Chain secondChain = CHAIN1.copy();
        UUID secondChainId = UUID.randomUUID();
        secondChain.setChainId(secondChainId);
        Chain thirdChain = CHAIN1.copy();
        thirdChain.getBlockchainPropertiesList().get(0).setBlockTime(2);
        thirdChain.getBlockchainPropertiesList().get(0).setMaxBalance(0);
        List<Chain> chainsToWrite = Arrays.asList(secondChain, thirdChain);

        Path userConfigFile = tempRootPath.resolve(CONFIG_NAME);
        JSON.getMapper().writerWithDefaultPrettyPrinter().writeValue(userConfigFile.toFile(), chainsToWrite);
        ChainsConfigLoader chainsConfigLoader = new ChainsConfigLoader(false, tempRootPath.toAbsolutePath().toString(), CONFIG_NAME);
        Map<UUID, Chain> actualChains = chainsConfigLoader.load();
        assertEquals(3, actualChains.size());
        Map<UUID, Chain> expectedChains = chainsToWrite.stream().collect(Collectors.toMap(Chain::getChainId, Function.identity()));
        expectedChains.put(CHAIN2.getChainId(), CHAIN2);
        assertEquals(expectedChains, actualChains);
    }

    @Test
    void testLoadFromUserDefinedLocation() throws IOException {
        Chain secondChain = CHAIN1.copy();
        UUID secondChainId = UUID.randomUUID();
        secondChain.setChainId(secondChainId);
        List<Chain> chainsToWrite = Arrays.asList(secondChain);

        Path userConfigFile = tempRootPath.resolve(CONFIG_NAME);
        JSON.getMapper().writerWithDefaultPrettyPrinter().writeValue(userConfigFile.toFile(), chainsToWrite);
        ChainsConfigLoader chainsConfigLoader = new ChainsConfigLoader(true, tempRootPath.toAbsolutePath().toString(), CONFIG_NAME);
        Map<UUID, Chain> actualChains = chainsConfigLoader.load();
        assertEquals(1, actualChains.size());
        Map<UUID, Chain> expectedChains = chainsToWrite.stream().collect(Collectors.toMap(Chain::getChainId, Function.identity()));
        assertEquals(expectedChains, actualChains);
    }

    @Test
    void testLoadConfigWhichWasNotFound() throws IOException {
        String wrongFileName = CONFIG_NAME + ".wrongName";
        ChainsConfigLoader chainsConfigLoader = new ChainsConfigLoader(false, tempRootPath.toAbsolutePath().toString(), wrongFileName);
        Map<UUID, Chain> actualChains = chainsConfigLoader.load();
        Assertions.assertNull(actualChains);
    }

    @Test
    void testLoadConfigUsingConfigDirProvider() throws IOException {
        ConfigDirProvider configDirProvider = Mockito.mock(ConfigDirProvider.class);

        File installationLocation = Files.createTempDirectory(tempRootPath, "installation").toFile();
        File userConfigLocation = Files.createTempDirectory(tempRootPath, "user").toFile();
        File sysConfigLocation = Files.createTempDirectory(tempRootPath, "sys").toFile();

        Files.createDirectory(Paths.get(installationLocation.getAbsolutePath(), "conf"));
        Files.createDirectory(Paths.get(userConfigLocation.getAbsolutePath(), "conf"));
        Files.createDirectory(Paths.get(sysConfigLocation.getAbsolutePath(), "conf"));


        Mockito.doReturn(installationLocation.getPath()).when(configDirProvider).getInstallationConfigLocation();
        Mockito.doReturn(sysConfigLocation.getPath()).when(configDirProvider).getSysConfigLocation();
        Mockito.doReturn(userConfigLocation.getPath()).when(configDirProvider).getUserConfigLocation();
        Mockito.doReturn("conf").when(configDirProvider).getConfigName();

        Chain chain1 = CHAIN1.copy();
        chain1.setChainId(UUID.randomUUID());
        Chain chain2 = CHAIN1.copy();
        chain2.getBlockchainPropertiesList().get(0).setBlockTime(2);
        chain2.getBlockchainPropertiesList().get(0).setMaxBalance(0);
        Chain chain3 = chain2.copy();
        chain3.getBlockchainPropertiesList().get(1).setMaxNumberOfTransactions(400);
        Chain chain4 = chain1.copy();
        chain4.setActive(true);
        Chain chain5 = CHAIN1.copy();
        chain5.setChainId(UUID.randomUUID());
        Chain chain6 = chain5.copy();
        chain6.setDescription("Another description");
        List<Chain> chainsToWriteToUserConfigDir = Arrays.asList(chain6, chain3);
        List<Chain> chainsToWriteToInstallationDir = Arrays.asList(chain5, chain4);
        List<Chain> chainsToWriteToSysConfigDir = Arrays.asList(chain1, chain2);

        Path userConfigFile = userConfigLocation.toPath().resolve(configDirProvider.getConfigName()).resolve(CONFIG_NAME);
        Path sysConfigFile = sysConfigLocation.toPath().resolve(configDirProvider.getConfigName()).resolve(CONFIG_NAME);
        Path installationConfigFile = installationLocation.toPath().resolve(configDirProvider.getConfigName()).resolve(CONFIG_NAME);

        JSON.getMapper().writerWithDefaultPrettyPrinter().writeValue(userConfigFile.toFile(), chainsToWriteToUserConfigDir);
        JSON.getMapper().writerWithDefaultPrettyPrinter().writeValue(sysConfigFile.toFile(), chainsToWriteToSysConfigDir);
        JSON.getMapper().writerWithDefaultPrettyPrinter().writeValue(installationConfigFile.toFile(), chainsToWriteToInstallationDir);

        ChainsConfigLoader chainsConfigLoader = new ChainsConfigLoader(configDirProvider, false, CONFIG_NAME);
        Map<UUID, Chain> actualChains = chainsConfigLoader.load();
        assertEquals(4, actualChains.size());
        Map<UUID, Chain> expectedChains = Stream.of(chain4, chain3, chain6, CHAIN2).collect(Collectors.toMap(Chain::getChainId,
            Function.identity()));
        assertEquals(expectedChains, actualChains);
    }

    @Test
    void testLoadConfigWhenUserConfigAndResourcesIgnored() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ChainsConfigLoader(true, null, CONFIG_NAME));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ChainsConfigLoader(true, null));

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ChainsConfigLoader(null, true));
    }

    @Test
    void testLoadConfigWhenJsonIsIncorrect() throws IOException {

        Path userConfigFile = tempRootPath.resolve(CONFIG_NAME);
        Files.createFile(userConfigFile);
        ChainsConfigLoader chainsConfigLoader = new ChainsConfigLoader(false, userConfigFile.getParent().toString(), CONFIG_NAME);
        Map<UUID, Chain> chains = chainsConfigLoader.load();
        assertEquals(CHAIN1, chains.get(CHAIN1.getChainId()));
    }

}
