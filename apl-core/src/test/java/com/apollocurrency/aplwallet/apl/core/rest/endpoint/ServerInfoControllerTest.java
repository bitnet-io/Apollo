/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

import javax.ws.rs.core.MediaType;

import java.io.IOException;
import java.net.URISyntaxException;

import com.apollocurrency.aplwallet.api.dto.info.AccountEffectiveBalanceDto;
import com.apollocurrency.aplwallet.api.dto.info.AccountsCountDto;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.rest.service.ServerInfoService;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.Dispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

@Slf4j
class ServerInfoControllerTest {
    @Mock
    private BlockchainConfig blockchainConfig = Mockito.mock(BlockchainConfig.class);
    @Mock
    private ServerInfoService serverInfoService = Mockito.mock(ServerInfoService.class);

    private static ObjectMapper mapper = new ObjectMapper();
    private static Dispatcher dispatcher;

    private static String accountCountUri = "/server/info/count";

    @BeforeEach
    void setup(){
        dispatcher = MockDispatcherFactory.createDispatcher();
        doReturn("APL").when(blockchainConfig).getAccountPrefix();
    }

    @Test
    void counts_SUCCESS() throws URISyntaxException, IOException {
        // prepare data
        AccountsCountDto dto = new AccountsCountDto(112L, 113L, 1, 124L);
        AccountEffectiveBalanceDto balanceDto = new AccountEffectiveBalanceDto(
            100L, 200L, 100L, 200L, 200L, "123", "RS-ADVB");
        dto.topHolders.add(balanceDto);
        doReturn(dto).when(serverInfoService).getAccountsStatistic(Constants.MIN_TOP_ACCOUNTS_NUMBER);
        // init mocks
        ServerInfoController controller = new ServerInfoController(serverInfoService);
        dispatcher.getRegistry().addSingletonResource(controller);
        // call
        String uri = accountCountUri + "?numberOfAccounts=" + Constants.MIN_TOP_ACCOUNTS_NUMBER;
        MockHttpRequest request = MockHttpRequest.get(uri);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        // check
        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        AccountsCountDto dtoResult = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(dtoResult.topHolders);
        assertEquals(112L, dtoResult.totalSupply);
        assertEquals(1, dtoResult.topHolders.size());

        // call 2
        uri = accountCountUri + "?numberOfAccounts="; // empty value
        request = MockHttpRequest.get(uri);
        response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        // check
        assertEquals(200, response.getStatus());
        respondJson = response.getContentAsString();
        dtoResult = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(dtoResult.topHolders);
        assertEquals(112L, dtoResult.totalSupply);
        assertEquals(1, dtoResult.topHolders.size());
    }


}