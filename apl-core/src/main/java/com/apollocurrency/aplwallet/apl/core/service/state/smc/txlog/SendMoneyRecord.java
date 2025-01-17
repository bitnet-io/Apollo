/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.txlog;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.txlog.Record;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode
public class SendMoneyRecord implements Record {
    private final Address contract;
    private final long sender;
    private final long recipient;
    private final long value;//amount
    private final LedgerEvent event;
    private final long transaction;

    @Override
    public SmcRecordType type() {
        return SmcRecordType.SEND_MONEY;
    }
}
