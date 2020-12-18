/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.rlp;

import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;

/**
 * Wrapper to collect values in the list.
 * Not thread-safe.
 *
 * @author andrew.zinchenko@gmail.com
 */
public class RlpListBuilder {
    private List<RlpType> list;

    public RlpListBuilder(List<RlpType> list) {
        this.list = list;
    }

    public RlpListBuilder() {
        this.list = new ArrayList<>();
    }

    public RlpListBuilder add(byte value){
        list.add(RlpString.create(value));
        return this;
    }

    public RlpListBuilder add(byte[] value){
        list.add(RlpString.create(value));
        return this;
    }

    public RlpListBuilder add(long value){
        list.add(RlpString.create(value));
        return this;
    }

    public RlpListBuilder add(String value){
        list.add(RlpString.create(value));
        return this;
    }

    public RlpListBuilder add(BigInteger value){
        list.add(RlpString.create(value));
        return this;
    }

    public RlpListBuilder add(List<RlpType> value){
        list.add(new RlpList(value));
        return this;
    }

    public List<RlpType> build(){
        return list;
    }

    public static List<RlpType> ofString(List<String> values){
        return values.stream().collect(
            Collector.of(
                ArrayList::new,
                (List<RlpType> objects, String e) -> objects.add(RlpString.create(e)),
                (r1, r2) -> {
                    r1.addAll(r2);
                    return r1;
                },
                Collector.Characteristics.IDENTITY_FINISH
            )
        );
    }
}
