/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.data.type.Key;


/**
 * @author andrew.zinchenko@gmail.com
 */
public interface SmcContractEventService {
    /**
     * Save the contract event info in persistent storage
     *
     * @param address    the contract address
     * @param key        the mapping key
     * @param name       the mapping name
     * @param jsonObject the serialized object
     */
    void saveEventEntry(Address address, Key key, String name, String jsonObject);

    /**
     * Save the serialized object in persistent mapping storage
     *
     * @param address    the contract address
     * @param key        the mapping key
     * @param name       the mapping name
     * @param jsonObject the serialized object
     */
    void saveEventLogEntry(Address address, Key key, String name, String jsonObject);

    /**
     * Load the serialized object by the given address or null if the given key doesn't match the value
     *
     * @param address the contract address
     * @param key     the mapping key
     * @param name    the mapping name
     * @return the serialized object
     */
    String loadEntry(Address address, Key key, String name);

    /**
     * Delete the serialized object by the given address from storage
     *
     * @param address the contract address
     * @param key     the mapping key
     * @param name    the mapping name
     * @return true if entry was deleted
     */
    boolean deleteEntry(Address address, Key key, String name);

    /**
     * Checks if mapping exists
     *
     * @param address given contract address
     * @param name    given mapping name
     * @return true if specified mapping exists
     */
    boolean isEventExist(Address address, String name);

}
