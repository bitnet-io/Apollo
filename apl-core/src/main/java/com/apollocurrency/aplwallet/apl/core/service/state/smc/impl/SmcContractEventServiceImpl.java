/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractEventLogModelToLogEntryConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractEventModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractEventLogTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractEventTable;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplContractEvent;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractEventService;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import com.apollocurrency.smc.blockchain.crypt.HashSumProvider;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class SmcContractEventServiceImpl implements SmcContractEventService {

    private final Blockchain blockchain;
    private final SmcContractEventTable contractEventTable;
    private final SmcContractEventLogTable contractEventLogTable;
    private final ContractEventLogModelToLogEntryConverter logEntryConverter;
    private final ContractEventModelToEntityConverter entityConverter;
    private final HashSumProvider hashSumProvider;

    @Inject
    public SmcContractEventServiceImpl(Blockchain blockchain, SmcContractEventTable contractEventTable, SmcContractEventLogTable contractEventLogTable, ContractEventLogModelToLogEntryConverter contractEventLogModelToLogEntryConverter, ContractEventModelToEntityConverter contractEventModelToEntityConverter, HashSumProvider hashSumProvider) {
        this.blockchain = blockchain;
        this.contractEventTable = contractEventTable;
        this.contractEventLogTable = contractEventLogTable;
        this.logEntryConverter = contractEventLogModelToLogEntryConverter;
        this.entityConverter = contractEventModelToEntityConverter;
        this.hashSumProvider = hashSumProvider;
    }

    @Override
    @Transactional
    public void saveEvent(AplContractEvent event) {
        var eventEntity = entityConverter.convert(event);
        eventEntity.setHeight(blockchain.getHeight());

        var logEntry = logEntryConverter.convert(event);
        logEntry.setHeight(blockchain.getHeight());

        log.debug("Save the contract event at height {}, event={}, log={}", eventEntity.getHeight(), eventEntity, logEntry);

        var oldEvent = contractEventTable.get(contractEventTable.getDbKeyFactory().newKey(eventEntity));
        if (oldEvent == null || oldEvent.isNew()) {
            //save the new contract event type
            contractEventTable.insert(eventEntity);
        }
        contractEventLogTable.insert(logEntry);
    }
}
