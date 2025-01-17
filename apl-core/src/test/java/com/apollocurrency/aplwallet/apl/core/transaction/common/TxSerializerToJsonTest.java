/*
 * Copyright (c)  2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.common;

import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionJsonSerializer;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionJsonSerializerImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionWrapperHelper;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.io.JsonBuffer;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class TxSerializerToJsonTest {
    @Mock
    PrunableLoadingService prunableLoadingService;
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    Chain chain;
    TxBContext context;
    TransactionJsonSerializer jsonSerializer;
    LegacyJsonSerializer legacyJsonSerializer;

    TransactionTestData td = new TransactionTestData();

    @BeforeAll
    static void beforeAll() {
        GenesisImporter.CREATOR_ID = 1739068987193023818L;
    }

    @BeforeEach
    void setUp() {
        context = TxBContext.newInstance(chain);
        doReturn(chain).when(blockchainConfig).getChain();
        jsonSerializer = new TransactionJsonSerializerImpl(prunableLoadingService, blockchainConfig);
        legacyJsonSerializer = new LegacyJsonSerializer(prunableLoadingService);

    }

    @SneakyThrows
    @Test
    void serializeV1toJsonCompatibleWithLegacyFormat() {
        //GIVEN
        TxSerializer serializer = context.createSerializer(2);
        Transaction t1 = td.TRANSACTION_14;
        PayloadResult result = PayloadResult.createJsonResult();

        //WHEN
        serializer.serialize(t1, result);

        //THEN
        assertNotNull(result.array());
        assertNotNull(result.getBuffer());
        JSONObject json = ((JsonBuffer) result.getBuffer()).getJsonObject();
        assertEquals(t1.getVersion(), json.get("version"));
        assertEquals(t1.getId(), Long.parseUnsignedLong((String) json.get("id")));
        assertEquals(Convert.toHexString(t1.getSenderPublicKey()), json.get("senderPublicKey"));
        assertEquals(t1.getType().getSpec().getType(), json.get("type"));
        assertEquals(t1.getType().getSpec().getSubtype(), json.get("subtype"));
        assertEquals(t1.getTimestamp(), json.get("timestamp"));
        assertEquals(t1.getDeadline(), json.get("deadline"));

        String jsonObjectString = ((JsonBuffer) result.getBuffer()).getJsonObject().toJSONString();
        JSONObject jsonObject2 = (JSONObject) new JSONParser().parse(jsonObjectString);

        TransactionBuilderFactory transactionBuilderFactory = new TransactionBuilderFactory(td.getTransactionTypeFactory(), blockchainConfig);
        Transaction txFromJson = transactionBuilderFactory.newTransaction(jsonObject2);
        assertEquals(t1.getId(), txFromJson.getId());
        assertArrayEquals(t1.getSignature().bytes(), txFromJson.getSignature().bytes());

        JSONObject jsonLegacy = legacyJsonSerializer.toLegacyJsonFormat(txFromJson);

        assertNotNull(jsonLegacy);
        assertEquals(jsonLegacy, ((JsonBuffer) result.getBuffer()).getJsonObject());
    }

    @SneakyThrows
    @Test
    void toJsonLegacyFormat() {
        //GIVEN
        String t14UnsignedHex = "0310397a8002a00539dc2e813bb45ff063a376e316b10cd0addd7306555ca0dd2890194d379601524add89a5076a2218000000000000000000ca9a3b00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200000009d6e08009a520333578e293d010c00546573742070726f647563741500546573742070726f6475637420666f722073616c650c007461672074657374646174610200000000e40b540200000001b9dd15475e2f8da755f1b63933051dede676b223c86e70f54c7182b976d2f86d";

        String t14FullHash = "76774424416d4fdf2d218a8705a72fd313b7dd52db7ab34229d23e4125e02a00";
        Transaction t1 = td.TRANSACTION_14;

        //WHEN
        JSONObject jsonLegacy = legacyJsonSerializer.toLegacyJsonFormat(t1);
        String jsonObjectString = jsonLegacy.toJSONString();
        JSONObject jsonObject2 = (JSONObject) new JSONParser().parse(jsonObjectString);

        TransactionBuilderFactory transactionBuilderFactory = new TransactionBuilderFactory(td.getTransactionTypeFactory(), blockchainConfig);
        Transaction txFromJson = transactionBuilderFactory.newTransaction(jsonObject2);

        //THEN
        assertNotNull(jsonLegacy);

        assertEquals(t1.getVersion(), jsonLegacy.get("version"));
        assertEquals(t1.getId(), Long.parseUnsignedLong((String) jsonLegacy.get("id")));
        assertEquals(Convert.toHexString(t1.getSenderPublicKey()), jsonLegacy.get("senderPublicKey"));
        assertEquals(t1.getType().getSpec().getType(), jsonLegacy.get("type"));
        assertEquals(t1.getType().getSpec().getSubtype(), jsonLegacy.get("subtype"));
        assertEquals(t1.getTimestamp(), jsonLegacy.get("timestamp"));
        assertEquals(t1.getDeadline(), jsonLegacy.get("deadline"));

        TxSerializer serializer = context.createSerializer(2);
        PayloadResult result = PayloadResult.createLittleEndianByteArrayResult();
        serializer.serialize(TransactionWrapperHelper.createUnsignedTransaction(t1), result);
        log.info("t_14 unsigned={}", Convert.toHexString(result.array()));
        assertEquals(t14UnsignedHex, Convert.toHexString(result.array()));
        PayloadResult result2 = PayloadResult.createLittleEndianByteArrayResult();
        serializer.serialize(TransactionWrapperHelper.createUnsignedTransaction(txFromJson), result2);
        log.info("fromJson    u={}", Convert.toHexString(result2.array()));
        assertEquals(t14UnsignedHex, Convert.toHexString(result2.array()));

        assertEquals(t1.getId(), txFromJson.getId());
        assertArrayEquals(t1.getSignature().bytes(), txFromJson.getSignature().bytes());
    }

}
