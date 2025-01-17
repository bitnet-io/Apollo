/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.api.dto.DurableTaskInfo;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.model.BlockImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfigUpdater;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountTableInterface;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.utils.FilterCarriageReturnCharacterInputStream;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.util.env.config.ResourceLocator;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;

@Slf4j
@Singleton
public class GenesisImporter {
    static final String PUBLIC_KEY_NUMBER_TOTAL_PROPERTY_NAME = "apl.genesisAccounts.publicKeyNumberTotal";
    static final String BALANCE_NUMBER_TOTAL_PROPERTY_NAME = "apl.genesisAccounts.balanceNumberTotal";
    private static final String LOADING_STRING_PUB_KEYS = "Loading public keys %d / %d...";
    private static final String LOADING_STRING_GENESIS_BALANCE = "Loading genesis amounts %d / %d...";
    private static final String BALANCES_JSON_FIELD_NAME = "balances";
    private static final String GENESIS_PUBLIC_KEY_JSON_FIELD_NAME = "genesisPublicKey";
    private static final String EPOCH_BEGINNING_JSON_FIELD_NAME = "epochBeginning";
    public static long CREATOR_ID;
    public static long EPOCH_BEGINNING;
    public String GENESIS_PARAMS_JSON = "data" + File.separator + "genesisParameters.json";
    public String GENESIS_ACCOUNTS_JSON = "data" + File.separator + "genesisAccounts.json";
    private final ApplicationJsonFactory jsonFactory;
    /**
     * Represents a total number of public keys in a genesisAccounts.json file.
     * Has a hardcoded value because of the immutability of this file.
     */
    private final int publicKeyNumberTotal;
    /**
     * Represents a total number of balances in a genesisAccounts.json file.
     * Has a hardcoded value because of the immutability of this file.
     */
    private final int balanceNumberTotal;
    private final BlockchainConfigUpdater blockchainConfigUpdater;
    private final BlockchainConfig blockchainConfig;
    private final AplAppStatus aplAppStatus;
    private final AccountService accountService;
    private final AccountPublicKeyService accountPublicKeyService;
    private byte[] CREATOR_PUBLIC_KEY;
    private String genesisTaskId;
    private byte[] computedDigest;
    private final AccountGuaranteedBalanceTable accountGuaranteedBalanceTable;
    private final AccountTableInterface accountTable;
    private final ResourceLocator resourceLocator;

    @Inject
    public GenesisImporter(
        BlockchainConfig blockchainConfig,
        BlockchainConfigUpdater blockchainConfigUpdater,
        AplAppStatus aplAppStatus,
        AccountGuaranteedBalanceTable accountGuaranteedBalanceTable,
        AccountTableInterface accountTable,
        ApplicationJsonFactory jsonFactory,
        PropertiesHolder propertiesHolder,
        AccountService accountService,
        AccountPublicKeyService accountPublicKeyService,
        ResourceLocator resourceLocator
    ) {
        this.blockchainConfig =
            Objects.requireNonNull(blockchainConfig, "blockchainConfig is NULL");
        this.blockchainConfigUpdater =
            Objects.requireNonNull(blockchainConfigUpdater, "blockchainConfigUpdater is NULL");
        this.aplAppStatus = Objects.requireNonNull(aplAppStatus, "aplAppStatus is NULL");
        this.jsonFactory = Objects.requireNonNull(jsonFactory, "jsonFactory is NULL");
        this.accountService = Objects.requireNonNull(accountService, "accountService is NULL");
        this.accountPublicKeyService = Objects.requireNonNull(accountPublicKeyService, "accountPublicKeyService is NULL");
        Objects.requireNonNull(propertiesHolder, "propertiesHolder is NULL");
        this.publicKeyNumberTotal =
            propertiesHolder.getIntProperty(PUBLIC_KEY_NUMBER_TOTAL_PROPERTY_NAME, 230730);
        this.balanceNumberTotal =
            propertiesHolder.getIntProperty(BALANCE_NUMBER_TOTAL_PROPERTY_NAME, 84832);
        this.accountGuaranteedBalanceTable = Objects.requireNonNull(accountGuaranteedBalanceTable, "accountGuaranteedBalanceTable is NULL");
        this.accountTable = Objects.requireNonNull(accountTable, "accountTable is NULL");
        this.resourceLocator = Objects.requireNonNull(resourceLocator);

    }


    private void cleanUpGenesisData() {
        log.debug("clean Up Incomplete Genesis data...");
        accountPublicKeyService.cleanUpPublicKeys();
        this.accountGuaranteedBalanceTable.truncate();
        this.accountTable.truncate();
    }

    public void loadGenesisDataFromResources() {
        if (CREATOR_PUBLIC_KEY == null) {
            InputStream is = resourceLocator.locate(GENESIS_PARAMS_JSON)
                .orElseThrow(() -> new RuntimeException("Failed to load genesis parameters"));
            loadGenesisDataFromIS(is);
        }
        //TODO Move it somewhere
        Convert2.init(blockchainConfig.getAccountPrefix(), EPOCH_BEGINNING);
    }

    public byte[] getCreatorPublicKey() {
        return CREATOR_PUBLIC_KEY;
    }

    public byte[] getComputedDigest() {
        return computedDigest;
    }

    public void loadGenesisDataFromIS(InputStream is) {
        try (
            final JsonParser jsonParser = jsonFactory.createParser(is)
        ) {
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                final String currentName = jsonParser.getCurrentName();
                final JsonToken currentToken = jsonParser.currentToken();
                if (currentToken == JsonToken.FIELD_NAME) {
                    if (GENESIS_PUBLIC_KEY_JSON_FIELD_NAME.endsWith(currentName)) {
                        jsonParser.nextToken();
                        CREATOR_PUBLIC_KEY = Convert.parseHexString(jsonParser.getText());
                        CREATOR_ID = AccountService.getId(CREATOR_PUBLIC_KEY);
                    } else if (EPOCH_BEGINNING_JSON_FIELD_NAME.endsWith(currentName)) {
                        jsonParser.nextToken();
                        final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
                        EPOCH_BEGINNING = dateFormat.parse(jsonParser.getText()).getTime();
                    }
                }
            }
        } catch (IOException | ParseException e) {
            log.error("genesis Parameters were not loaded = {}", e.getMessage());
            throw new RuntimeException("Failed to load genesis parameters", e);
        }
    }

    private byte[] loadBalancesAccountsComputeDigest() throws GenesisImportException {
        final long start = System.currentTimeMillis();
        createGenesisTaskIdForStatus();

        final String path = GENESIS_ACCOUNTS_JSON;

        log.trace("path = {}", path);
        final List<String> publicKeys = new ArrayList<>();
        final Map<String, Long> balances = new HashMap<>();
        final MessageDigest digest = Crypto.sha256();
        int balanceCount = 0;
        int publicKeyCount = 0;
        try (
            final InputStream filteredIs =
                new FilterCarriageReturnCharacterInputStream(
                    resourceLocator.locate(path)
                        .orElseThrow(() -> new RuntimeException("The resource could not be found, path=" + path))
                );
            final InputStream digestIs = new DigestInputStream(filteredIs, digest);
            final JsonParser jsonParser = jsonFactory.createParser(digestIs)
        ) {
            boolean isBalancesProcessingOn = false;
            boolean isPublicKeysProcessingOn = false;
            while (!jsonParser.isClosed()) {
                //nextToken() is called to calculate digest regardless of a log level
                final JsonToken currentToken = jsonParser.nextToken();
                if (log.isDebugEnabled() || log.isTraceEnabled()) {
                    final String currentName = jsonParser.getCurrentName();
                    if ((currentToken == JsonToken.FIELD_NAME) && (BALANCES_JSON_FIELD_NAME.equals(currentName))) {
                        jsonParser.nextToken();
                        isBalancesProcessingOn = true;
                    } else if ((isBalancesProcessingOn) && (currentToken == JsonToken.END_OBJECT)) {
                        isBalancesProcessingOn = false;
                    } else if (isBalancesProcessingOn) {
                        jsonParser.nextToken();
                        balances.put(currentName, jsonParser.getLongValue());
                        balanceCount++;
                    } else if (currentToken == JsonToken.START_ARRAY) {
                        isPublicKeysProcessingOn = true;
                    } else if (currentToken == JsonToken.END_ARRAY) {
                        break;
                    } else if (isPublicKeysProcessingOn) {
                        publicKeys.add(jsonParser.getText());
                        publicKeyCount++;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to process genesis recipients accounts", e);
        }

        log.debug("balances = [{}]", balanceCount);
        traceDumpData("balances = {}", balances);
        log.debug("publicKeys = [{}]", publicKeyCount);
        traceDumpData("publicKeys = {}", publicKeys);

        if (log.isDebugEnabled() || log.isTraceEnabled()) {
            validateBalanceNumber(balanceCount);
            validatePublicKeyNumber(publicKeyCount);
        }

        this.computedDigest = updateComputedDigest(digest);

        final Long usedBytes = null; //Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory(); // to measure in unit tests
        log.debug("Digest is computed in {} milliSec, used {} Kb", System.currentTimeMillis() - start,
            usedBytes != null ? usedBytes / 1024 : "not calculated");

        return this.computedDigest;
    }

    private void createGenesisTaskIdForStatus() {
        if (genesisTaskId == null) {
            final Optional<DurableTaskInfo> task = aplAppStatus.findTaskByName("Shard data import");
            if (task.isPresent()) {
                genesisTaskId = task.get().getId();
            } else {
                genesisTaskId = aplAppStatus.durableTaskStart("Genesis account load", "Loading and creating Genesis accounts + balances", true);
            }
        }
    }

    private void traceDumpData(String pattern, Object... data) {
        if (log.isTraceEnabled()) {
            log.trace(pattern, data);
        }
    }

    public Block newGenesisBlock() throws GenesisImportException {
        long baseTarget = blockchainConfig.getCurrentConfig().getInitialBaseTarget();
        return new BlockImpl(CREATOR_PUBLIC_KEY, loadBalancesAccountsComputeDigest(), baseTarget);
    }

    @Transactional
    public void importGenesisJson(final boolean loadOnlyPublicKeys) {
        final long start = System.currentTimeMillis();
        createGenesisTaskIdForStatus(); // recreate taskId for task execution status update

        this.blockchainConfigUpdater.reset();

        // Always remove possibly previously 'incomplete genesis import' data
        cleanUpGenesisData(); // clean up previous incomplete genesis import (if any)

        final int publicKeyNumber = saveGenesisPublicKeys();

        if (loadOnlyPublicKeys) {
            log.debug("Public Keys were saved in {} s. The rest of GENESIS is skipped, shard info will be loaded...",
                (System.currentTimeMillis() - start) / 1000);
            return;
        }
        // load 'balances' from JSON only
        final Pair<Long, Integer> balanceStatistics = saveBalances();
        final Integer balanceNumber = balanceStatistics.getRight();
        final long total = balanceStatistics.getLeft();

        final long maxBalanceATM = blockchainConfig.getCurrentConfig().getMaxBalanceATM();
        if (total > maxBalanceATM) {
            throw new RuntimeException("Total balance " + total + " exceeds maximum allowed " + maxBalanceATM);
        }
        final String message = String.format("Total balance %f %s", (double) total / blockchainConfig.getOneAPL(), blockchainConfig.getCoinSymbol());
        final Account creatorAccount = accountService.createAccount(CREATOR_ID, CREATOR_PUBLIC_KEY);
        accountPublicKeyService.apply(creatorAccount, CREATOR_PUBLIC_KEY, true);

        accountService.addToBalanceAndUnconfirmedBalanceATM(creatorAccount, null, 0, -total);
        aplAppStatus.durableTaskFinished(genesisTaskId, false, message);
        log.debug("Public Keys [{}] + Balances [{}] were saved in {} ms", publicKeyNumber, balanceNumber,
            (System.currentTimeMillis() - start) / 1000);
        this.genesisTaskId = null;

        final Long usedBytes = null; //Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory(); // to measure in unit tests
        log.debug("ImportGenesisJson is computed in {} milliSec, used {} Kb", System.currentTimeMillis() - start,
            usedBytes != null ? usedBytes / 1024 : "not calculated");
    }

    @SneakyThrows(value = {JsonParseException.class, IOException.class})
    private int saveGenesisPublicKeys() {
        final long start = System.currentTimeMillis();
        int count = 0;
        final String path = GENESIS_ACCOUNTS_JSON;
        log.trace("Saving public keys from a file: {}", path);
        aplAppStatus.durableTaskUpdate(genesisTaskId, 0.2, "Loading public keys");

        final MessageDigest digest = Crypto.sha256();
        try (
            final InputStream filteredIs =
                new FilterCarriageReturnCharacterInputStream(resourceLocator.locate(path)
                    .orElseThrow(() -> new RuntimeException("The resource could not be found, path=" + path))
                );
            final InputStream digestedIs = new DigestInputStream(filteredIs, digest);
            final JsonParser jsonParser = jsonFactory.createParser(digestedIs)
        ) {
            boolean isPublicKeysProcessingOn = false;
            while (!jsonParser.isClosed()) {
                final JsonToken jsonToken = jsonParser.nextToken();
                if (jsonToken == JsonToken.START_ARRAY) {
                    isPublicKeysProcessingOn = true;
                } else if (jsonToken == JsonToken.END_ARRAY) {
                    break;
                } else if (isPublicKeysProcessingOn) {
                    final String jsonPublicKey = jsonParser.getText();
                    final byte[] publicKey = Convert.parseHexString(jsonPublicKey);
                    final long id = AccountService.getId(publicKey);
                    log.trace("AccountId = '{}' by publicKey string = '{}'", id, jsonPublicKey);
                    final Account account = accountService.createAccount(id, publicKey);
                    accountPublicKeyService.apply(account, publicKey, true);

                    if (++count % 10000 == 0) {
                        final String message = String.format(LOADING_STRING_PUB_KEYS, count, publicKeyNumberTotal);
                        log.debug(message);
                        aplAppStatus.durableTaskUpdate(genesisTaskId, (count * 1.0 / publicKeyNumberTotal * 1.0) * 50, message);
                    }
                }
            }
        }

        this.computedDigest = updateComputedDigest(digest);

        log.debug("Saved public keys = [{}] in {} sec", count, (System.currentTimeMillis() - start) / 1000);

        try {
            validatePublicKeyNumber(count);
        } catch (GenesisImportException e) {
            throw new RuntimeException(e);
        }
        return count;
    }

    /**
     * Updates computed digest.
     * <p>
     * Note that we should leave here '0' to create correct genesis block for already launched the Main net.
     *
     * @param digest to update
     * @return the array of bytes for the resulting hash value.
     */
    private byte[] updateComputedDigest(final MessageDigest digest) {
        digest.update((byte) (0));
        digest.update(Convert.toBytes(EPOCH_BEGINNING));
        return digest.digest();
    }

    @SneakyThrows(value = {JsonParseException.class, IOException.class})
    private Pair<Long, Integer> saveBalances() {
        final String path = GENESIS_ACCOUNTS_JSON;

        final long start = System.currentTimeMillis();
        int count = 0;
        long totalAmount = 0;
        log.trace("Saved public keys, start saving Balances...");
        aplAppStatus.durableTaskUpdate(genesisTaskId, 50 + 0.1, "Loading genesis balance amounts");
        try (
            final InputStream is = resourceLocator.locate(path)
                .orElseThrow(() -> new RuntimeException("The resource could not be found, path=" + path));
            final JsonParser jsonParser = jsonFactory.createParser(is)
        ) {
            boolean isBalancesProcessingStarted = false;
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                final JsonToken currentToken = jsonParser.getCurrentToken();
                final String currentName = jsonParser.getCurrentName();
                if ((currentToken == JsonToken.FIELD_NAME) && (BALANCES_JSON_FIELD_NAME.equals(currentName))) {
                    jsonParser.nextToken();
                    isBalancesProcessingStarted = true;
                } else if (isBalancesProcessingStarted) {
                    jsonParser.nextToken();
                    final long balanceValue = jsonParser.getLongValue();
                    log.trace("Parsed json balance: {} - {}", currentName, balanceValue);
                    final Account account = accountService.createAccount(Long.parseUnsignedLong(currentName));
                    accountService.addToBalanceAndUnconfirmedBalanceATM(account, null, 0, balanceValue);
                    totalAmount += balanceValue;

                    if (++count % 10000 == 0) {
                        final String message = String.format(LOADING_STRING_GENESIS_BALANCE, count, balanceNumberTotal);
                        log.debug(message);
                        aplAppStatus.durableTaskUpdate(genesisTaskId, 50 + (count * 1.0 / balanceNumberTotal * 1.0) * 50, message);
                    }
                }
            }
        }

        log.debug(
            "Saved [{}] balances in {} sec, total balance amount = {}",
            count,
            (System.currentTimeMillis() - start) / 1000, totalAmount
        );
        try {
            validateBalanceNumber(count);
        } catch (GenesisImportException e) {
            throw new RuntimeException(e);
        }

        return Pair.of(totalAmount, count);
    }

    List<Map.Entry<String, Long>> loadGenesisAccounts() throws GenesisImportException {
        final String path = GENESIS_ACCOUNTS_JSON;
        log.debug("Genesis accounts json resource path = " + path);
        final InputStream is = resourceLocator.locate(path).orElseThrow(() -> new RuntimeException("The resource could not be found, path=" + path));

        final Queue<Map.Entry<String, Long>> sortedEntries = loadGenesisAccountsFromIS(is);

        final int balanceNumber = sortedEntries.size();
        validateBalanceNumber(balanceNumber);

        return new ArrayList<>(sortedEntries);
    }

    private Queue<Map.Entry<String, Long>> loadGenesisAccountsFromIS(InputStream is) throws GenesisImportException {
        Objects.requireNonNull(is);
        final Queue<Map.Entry<String, Long>> sortedEntries =
            new PriorityQueue<>((o1, o2) -> Long.compare(o2.getValue(), o1.getValue()));

        try (
            final JsonParser jsonParser = jsonFactory.createParser(is)
        ) {
            boolean isBalancesProcessingStarted = false;
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                final JsonToken currentToken = jsonParser.getCurrentToken();
                if (currentToken == null) {
                    break;
                }
                final String currentName = jsonParser.getCurrentName();
                if ((currentToken == JsonToken.FIELD_NAME) && (BALANCES_JSON_FIELD_NAME.equals(currentName))) {
                    jsonParser.nextToken();
                    isBalancesProcessingStarted = true;
                } else if (isBalancesProcessingStarted) {
                    jsonParser.nextToken();
                    sortedEntries.add(Map.entry(currentName, jsonParser.getLongValue()));
                }
            }
        } catch (IOException e) {
            log.error("Failed to load genesis accounts, cause:{}", e.getMessage());
            throw new GenesisImportException("Failed to load genesis accounts", e);
        }

        return sortedEntries;
    }

    /**
     * Validates the publicKeyNumberTotal against a publicKeyCount.
     *
     * @param publicKeyCount
     */
    private void validatePublicKeyNumber(int publicKeyCount) throws GenesisImportException {
        if (publicKeyNumberTotal != publicKeyCount) {
            throw new GenesisImportException(
                String.format(
                    "A hardcoded public key total number: %d is different to a calculated value: %d",
                    publicKeyNumberTotal, publicKeyCount
                )
            );
        }
    }

    /**
     * Validates the balanceNumberTotal against a balanceCount.
     *
     * @param balanceCount
     */
    private void validateBalanceNumber(int balanceCount) throws GenesisImportException {
        if (balanceNumberTotal != balanceCount) {
            throw new GenesisImportException(
                String.format(
                    "A hardcoded balance total number: %d is different to a calculated value: %d",
                    balanceNumberTotal, balanceCount
                )
            );
        }
    }
}
