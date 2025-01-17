/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.service.mapping;

import com.apollocurrency.aplwallet.apl.smc.service.SmcContractStorageService;
import com.apollocurrency.smc.blockchain.storage.AddressJsonConverter;
import com.apollocurrency.smc.blockchain.storage.BigIntegerJsonConverter;
import com.apollocurrency.smc.blockchain.storage.BigNumJsonConverter;
import com.apollocurrency.smc.blockchain.storage.CachedMappingRepository;
import com.apollocurrency.smc.blockchain.storage.ContractMappingRepository;
import com.apollocurrency.smc.blockchain.storage.ContractMappingRepositoryFactory;
import com.apollocurrency.smc.blockchain.storage.ReadonlyMappingRepository;
import com.apollocurrency.smc.blockchain.storage.StringJsonConverter;
import com.apollocurrency.smc.blockchain.storage.UnsignedBigNumJsonConverter;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.data.type.BigNum;
import com.apollocurrency.smc.data.type.UnsignedBigNum;
import lombok.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.util.Set;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class SmcMappingRepositoryClassFactory {
    private final SmcContractStorageService smcContractStorageService;

    @Inject
    public SmcMappingRepositoryClassFactory(SmcContractStorageService smcContractStorageService) {
        this.smcContractStorageService = smcContractStorageService;
    }

    public ContractMappingRepositoryFactory createMappingFactory(final Address contract) {
        return new PersistentMappingRepositoryFactory(contract, smcContractStorageService);
    }

    public ContractMappingRepositoryFactory createReadonlyMappingFactory(final Address contract) {
        final var persistentRepositoryFactory = createMappingFactory(contract);
        return new ReadOnlyMappingRepositoryFactory(persistentRepositoryFactory);
    }

    public ContractMappingRepositoryFactory createCachedMappingFactory(final Address contract, final Set<CachedMappingRepository<?>> mappingRepositories) {
        final var persistentRepositoryFactory = createMappingFactory(contract);
        return new CachedMappingRepositoryFactory(persistentRepositoryFactory, mappingRepositories);
    }

    private static class PersistentMappingRepositoryFactory implements ContractMappingRepositoryFactory {
        private final SmcContractStorageService smcContractStorageService;
        private final Address contract;

        public PersistentMappingRepositoryFactory(@NonNull Address contract, @NonNull SmcContractStorageService smcContractStorageService) {
            this.contract = contract;
            this.smcContractStorageService = smcContractStorageService;
        }

        @Override
        public boolean hasMapping(String mappingName) {
            return smcContractStorageService.isMappingExist(contract, mappingName);
        }

        @Override
        public ContractMappingRepository<Address> addressRepository(String mappingName) {
            return new PersistentMappingRepository<>(smcContractStorageService, contract, mappingName, new AddressJsonConverter());
        }

        @Override
        public ContractMappingRepository<BigNum> bigNumRepository(String mappingName) {
            return new PersistentMappingRepository<>(smcContractStorageService, contract, mappingName, new BigNumJsonConverter());
        }

        @Override
        public ContractMappingRepository<UnsignedBigNum> unsignedBigNumRepository(String mappingName) {
            return new PersistentMappingRepository<>(smcContractStorageService, contract, mappingName, new UnsignedBigNumJsonConverter());
        }

        @Override
        public ContractMappingRepository<BigInteger> bigIntegerRepository(String mappingName) {
            return new PersistentMappingRepository<>(smcContractStorageService, contract, mappingName, new BigIntegerJsonConverter());
        }

        @Override
        public ContractMappingRepository<String> stringRepository(String mappingName) {
            return new PersistentMappingRepository<>(smcContractStorageService, contract, mappingName, new StringJsonConverter());
        }

        @Override
        public void clear() {
            //nothing to do
        }
    }

    private static class ReadOnlyMappingRepositoryFactory implements ContractMappingRepositoryFactory {
        private final ContractMappingRepositoryFactory persistentRepositoryFactory;

        public ReadOnlyMappingRepositoryFactory(ContractMappingRepositoryFactory persistentRepositoryFactory) {
            this.persistentRepositoryFactory = persistentRepositoryFactory;
        }

        @Override
        public boolean hasMapping(String mappingName) {
            return persistentRepositoryFactory.hasMapping(mappingName);
        }

        @Override
        public ContractMappingRepository<Address> addressRepository(String mappingName) {
            return new ReadonlyMappingRepository<>(persistentRepositoryFactory.addressRepository(mappingName));
        }

        @Override
        public ContractMappingRepository<BigNum> bigNumRepository(String mappingName) {
            return new ReadonlyMappingRepository<>(persistentRepositoryFactory.bigNumRepository(mappingName));
        }

        @Override
        public ContractMappingRepository<UnsignedBigNum> unsignedBigNumRepository(String mappingName) {
            return new ReadonlyMappingRepository<>(persistentRepositoryFactory.unsignedBigNumRepository(mappingName));
        }

        @Override
        public ContractMappingRepository<BigInteger> bigIntegerRepository(String mappingName) {
            return new ReadonlyMappingRepository<>(persistentRepositoryFactory.bigIntegerRepository(mappingName));
        }

        @Override
        public ContractMappingRepository<String> stringRepository(String mappingName) {
            return new ReadonlyMappingRepository<>(persistentRepositoryFactory.stringRepository(mappingName));
        }

        @Override
        public void clear() {
            //nothing to do
        }
    }

    private static class CachedMappingRepositoryFactory implements ContractMappingRepositoryFactory {
        private final ContractMappingRepositoryFactory persistentRepositoryFactory;
        private final Set<CachedMappingRepository<?>> mappingRepositories;

        public CachedMappingRepositoryFactory(ContractMappingRepositoryFactory persistentRepositoryFactory, Set<CachedMappingRepository<?>> mappingRepositories) {
            this.persistentRepositoryFactory = persistentRepositoryFactory;
            this.mappingRepositories = mappingRepositories;
        }

        @Override
        public boolean hasMapping(String mappingName) {
            return persistentRepositoryFactory.hasMapping(mappingName);
        }

        @Override
        public ContractMappingRepository<Address> addressRepository(String mappingName) {
            var repo = new CachedMappingRepository<>(persistentRepositoryFactory.addressRepository(mappingName));
            mappingRepositories.add(repo);
            return repo;
        }

        @Override
        public ContractMappingRepository<BigNum> bigNumRepository(String mappingName) {
            var repo = new CachedMappingRepository<>(persistentRepositoryFactory.bigNumRepository(mappingName));
            mappingRepositories.add(repo);
            return repo;
        }

        @Override
        public ContractMappingRepository<UnsignedBigNum> unsignedBigNumRepository(String mappingName) {
            var repo = new CachedMappingRepository<>(persistentRepositoryFactory.unsignedBigNumRepository(mappingName));
            mappingRepositories.add(repo);
            return repo;
        }

        @Override
        public ContractMappingRepository<BigInteger> bigIntegerRepository(String mappingName) {
            var repo = new CachedMappingRepository<>(persistentRepositoryFactory.bigIntegerRepository(mappingName));
            mappingRepositories.add(repo);
            return repo;
        }

        @Override
        public ContractMappingRepository<String> stringRepository(String mappingName) {
            var repo = new CachedMappingRepository<>(persistentRepositoryFactory.stringRepository(mappingName));
            mappingRepositories.add(repo);
            return repo;
        }

        @Override
        public void clear() {
            mappingRepositories.clear();
        }
    }
}
