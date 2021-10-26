/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.fuel.Fuel;
import com.apollocurrency.smc.data.type.Address;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface CachedContractService {

    /**
     * Save the published contract
     *
     * @param contract the published contract
     */
    void saveContract(SmartContract contract);

    /**
     * Load the saved contract by the given address or null if the given address doesn't correspond the smart contract
     *
     * @param address      given contract address
     * @param originator   the origin transaction sender
     * @param contractFuel given fuel to execute method calling
     * @return loaded smart contract or null
     */
    SmartContract loadContract(Address address, Address originator, Fuel contractFuel);

    /**
     * Checks if contract already exists
     *
     * @param address given contract address
     * @return true if contract exists at given address
     */
    boolean isContractExist(Address address);

    void updateContractState(SmartContract contract);

}
