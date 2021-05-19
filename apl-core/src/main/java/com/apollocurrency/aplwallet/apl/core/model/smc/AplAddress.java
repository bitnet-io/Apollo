/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.model.smc;

import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.util.AddressHelper;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Smart contract address
 *
 * @author andrew.zinchenko@gmail.com
 */
public class AplAddress implements Address {
    @JsonProperty
    long id;

    public AplAddress(@JsonProperty long id) {
        this.id = id;
    }

    public AplAddress(Address address) {
        this.id = new BigInteger(address.get()).longValueExact();
    }

    public AplAddress(String address) {
        byte[] bytes = AddressHelper.parseHex(address);
        this.id = new BigInteger(bytes).longValueExact();
    }

    public long getLongId() {
        return id;
    }

    public String toRS() {
        return Convert2.rsAccount(id);
    }

    @Override
    public byte[] get() {
        return BigInteger.valueOf(id).toByteArray();
    }

    @Override
    public String toString() {
        return id + "(" + getHex() + "," + toRS() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AplAddress that = (AplAddress) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
