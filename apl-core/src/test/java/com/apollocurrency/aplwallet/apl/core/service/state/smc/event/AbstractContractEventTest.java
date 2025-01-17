/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.event;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
import com.apollocurrency.aplwallet.apl.smc.model.AplContractEvent;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.smc.blockchain.crypt.DigestWrapper;
import com.apollocurrency.smc.blockchain.crypt.HashSumProvider;
import com.apollocurrency.smc.contract.vm.event.SmcContractEvent;
import com.apollocurrency.smc.data.type.ContractEventType;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.when;

/**
 * @author andrew.zinchenko@gmail.com
 */
@ExtendWith(MockitoExtension.class)
class AbstractContractEventTest {

    static {
        Convert2.init("APL", 1739068987193023818L);
    }

    @Mock
    HashSumProvider hashSumProvider;
    int height = 10000;
    AplAddress contractAddress = new AplAddress(Convert.parseAccountId("APL-632K-TWX3-2ALQ-973CU"));
    AplAddress transactionAddress = new AplAddress(123L);

    final ContractEventType eventType = ContractEventType.builder()
        .spec("Transfer:from,to,amount")
        .indexedFieldsCount(2)
        .anonymous(false)
        .build();
    final String signatureStr = eventType.getSpec();
    final byte[] signature = {1, 2, 3, 4, 5, 6, 7, 8};

    AplContractEventManager manager;

    SmcContractEvent createEvent(String state) {
        return SmcContractEvent.builder()
            .contract(contractAddress)
            .transaction(transactionAddress)
            .height(height)
            .eventType(eventType)
            .txIdx(0)
            .signature(signature)
            .state(state != null ? state : "{}")
            .build();
    }

    AplContractEvent createAplEvent(long id) {
        return createAplEvent(id, null);
    }

    AplContractEvent createAplEvent(long id, String state) {
        var event = createEvent(state);
        var aplEvent = AplContractEvent.builder()
            .event(event)
            .id(id)
            .build();

        return aplEvent;
    }

    AplContractEvent mockAplEvent(String state) {
        var id = 987654321L;
        var aplEvent = createAplEvent(id, state);

        when(hashSumProvider.sha256(signatureStr.getBytes(StandardCharsets.UTF_8))).thenReturn(signature);
        when(hashSumProvider.sha256()).thenReturn(new DigestWrapper(Crypto.sha256()));
        when(manager.generateId(0, signature)).thenReturn(id);
        return aplEvent;
    }

}