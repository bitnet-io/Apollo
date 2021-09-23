/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author andrew.zinchenko@gmail.com
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
public class SmcEventMessage extends SmcEventResponse {
    private String address;//contract address
    private String data;
    private String name;
    private String signature;
    private int transactionIndex;
    private String transaction;//transaction id/address
    private String blockHash;// - ?
    private long blockNumber;// - ?

    @Builder
    public SmcEventMessage(Integer errorCode, String errorDescription, String data, String name, String signature, int transactionIndex, String transaction, String blockHash, long blockNumber, String address) {
        super(errorCode, errorDescription, Type.EVENT);
        this.data = data;
        this.name = name;
        this.signature = signature;
        this.transactionIndex = transactionIndex;
        this.transaction = transaction;
        this.blockHash = blockHash;
        this.blockNumber = blockNumber;
        this.address = address;
    }
}
