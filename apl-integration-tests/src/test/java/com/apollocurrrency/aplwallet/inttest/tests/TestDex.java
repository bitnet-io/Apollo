package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.DexOrderDto;
import com.apollocurrency.aplwallet.api.dto.TradingDataOutputDTO;
import com.apollocurrency.aplwallet.api.response.Account2FAResponse;
import com.apollocurrency.aplwallet.api.response.CreateDexOrderResponse;
import com.apollocurrency.aplwallet.api.response.EthGasInfoResponse;
import com.apollocurrency.aplwallet.api.response.WithdrawResponse;
import com.apollocurrrency.aplwallet.inttest.helper.DexPreconditionExtension;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseNew;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extensions;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;


import java.lang.annotation.ElementType;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

@DisplayName("Dex")
@Epic(value = "Dex")
@ExtendWith(DexPreconditionExtension.class)
@Execution(CONCURRENT)
public class TestDex extends TestBaseNew {

    @DisplayName("Get dex orders")
    @Test
    @Execution(SAME_THREAD)
    public void getExchangeOrders() {
        List<DexOrderDto> orders = getDexOrders();
        assertNotNull(orders);
    }

    @DisplayName("Get trading history (closed orders) for certain account with param")
    @Test
    @Execution(SAME_THREAD)
    public void getTradeHistory() {
        List<DexOrderDto> orders = getDexHistory(TestConfiguration.getTestConfiguration().getVaultWallet().getAccountId(), true, true);
        assertNotNull(orders);
    }

    @DisplayName("Get trading history (closed orders) for certain account")
    @Test
    @Execution(SAME_THREAD)
    public void getAllTradeHistoryByAccount() {
        List<DexOrderDto> orders = getDexHistory(TestConfiguration.getTestConfiguration().getVaultWallet().getAccountId());
        assertNotNull(orders);
    }

    @DisplayName("Get gas prices for different tx speed")
    @Test
    @Execution(SAME_THREAD)
    public void getEthGasPrice() {
        EthGasInfoResponse gasPrice = getEthGasInfo();
        assertTrue(Float.valueOf(gasPrice.getFast()) >= Float.valueOf(gasPrice.getAverage()));
        assertTrue(Float.valueOf(gasPrice.getAverage()) >= Float.valueOf(gasPrice.getSafeLow()));
        assertTrue(Float.valueOf(gasPrice.getSafeLow()) > 0);
    }

    @DisplayName("Obtaining ETH trading information for the given period (10 days) with certain resolution")
    @Test
    @Execution(SAME_THREAD)
    //@ParameterizedTest
    //@ValueSource(strings = {"D", "15", "60", "240"})
    public void getDexTradeInfoETH() {
        TradingDataOutputDTO dexTrades = getDexTradeInfo(true, "D");
        assertNotNull(dexTrades);
    }

    @DisplayName("Obtaining PAX trading information for the given period (10 days) with certain resolution")
    @Test
    @Execution(SAME_THREAD)
    public void getDexTradeInfoPAX() {
        TradingDataOutputDTO dexTrades = getDexTradeInfo(false, "15");
        assertNotNull(dexTrades);
    }

    @DisplayName("Create 4 types of orders and cancel them")
    @Test
    @Execution(SAME_THREAD)
    public void dexOrders() {
        //Create Sell order ETH
        CreateDexOrderResponse sellOrderEth = createDexOrder("40000", "1000", TestConfiguration.getTestConfiguration().getVaultWallet(), false, true);
        assertNotNull(sellOrderEth, "RESPONSE is not correct/dex offer wasn't created");
        verifyTransactionInBlock(sellOrderEth.getOrder().getId());
        //Create Sell order PAX
        CreateDexOrderResponse sellOrderPax = createDexOrder("40000", "1000", TestConfiguration.getTestConfiguration().getVaultWallet(), false, false);
        assertNotNull(sellOrderPax, "RESPONSE is not correct/dex offer wasn't created");
        verifyTransactionInBlock(sellOrderPax.getOrder().getId());
        //Create Buy order PAX
        CreateDexOrderResponse buyOrderPax = createDexOrder("15000", "1000", TestConfiguration.getTestConfiguration().getVaultWallet(), true, false);
        assertNotNull(buyOrderPax,  "RESPONSE is not correct/dex offer wasn't created");
        assertNotNull(buyOrderPax.getFrozenTx(), "FrozenTx isn't present");
        verifyTransactionInBlock(buyOrderPax.getOrder().getId());
        //Create Buy order ETH
        CreateDexOrderResponse buyOrderEth = createDexOrder("15000", "1000", TestConfiguration.getTestConfiguration().getVaultWallet(), true, true);
        assertNotNull(buyOrderEth,  "RESPONSE is not correct/dex offer wasn't created");
        assertNotNull(buyOrderEth.getFrozenTx(), "FrozenTx isn't present");
        verifyTransactionInBlock(buyOrderEth.getOrder().getId());

        List<DexOrderDto> orders = getDexOrders("0", TestConfiguration.getTestConfiguration().getVaultWallet().getAccountId());
        //TODO: add additional asserts for checking statuses after order was cancelled
        for (DexOrderDto order : orders) {
            if (order.status == 0) {
                System.out.println("order ID = " + order.id + "    status = " + order.status);
                verifyCreatingTransaction(dexCancelOrder(order.id, TestConfiguration.getTestConfiguration().getVaultWallet()));
            }
            else log.info("orders with status OPEN are not present");
        }
    }

    @DisplayName("withdraw ETH/PAX + validation of ETH/PAX balances")
    @Test
    @Execution(SAME_THREAD)
    public void dexWithdrawTransactions() {
        Account2FAResponse balance = getDexBalances(TestConfiguration.getTestConfiguration().getVaultWallet().getEthAddress());
        assertNotNull(balance.getEth().get(0).getBalances().getEth());
        double ethBalance = balance.getEth().get(0).getBalances().getEth();
        assertNotNull(balance.getEth().get(0).getBalances().getPax());
        double paxBalance = balance.getEth().get(0).getBalances().getPax();

        EthGasInfoResponse gasPrice = getEthGasInfo();
        assertTrue(Float.valueOf(gasPrice.getFast()) >= Float.valueOf(gasPrice.getAverage()));
        assertTrue(Float.valueOf(gasPrice.getAverage()) >= Float.valueOf(gasPrice.getSafeLow()));
        assertTrue(Float.valueOf(gasPrice.getSafeLow()) > 0);
        Integer fastGas = Math.round(Float.valueOf(gasPrice.getFast()));
        Integer averageGas = Math.round(Float.valueOf(gasPrice.getAverage()));
        Integer safeLowGas = Math.round(Float.valueOf(gasPrice.getSafeLow()));

        //TODO: add assertion and getEthGasFee to include it into tests and validation on balances
        WithdrawResponse withdrawEth = dexWidthraw(TestConfiguration.getTestConfiguration().getVaultWallet().getEthAddress(),
                TestConfiguration.getTestConfiguration().getVaultWallet(),
                TestConfiguration.getTestConfiguration().getVaultWallet().getEthAddress(),
                "0.5",
                String.valueOf(averageGas),
                true);
        assertNotNull(withdrawEth.transactionAddress);

        //TODO: add transaction validation is accepted in ETH blockchain
        double newEthBalance = ethBalance - (21000 * 0.000000001 * averageGas);


        WithdrawResponse withdrawPax = dexWidthraw(TestConfiguration.getTestConfiguration().getVaultWallet().getEthAddress(),
                TestConfiguration.getTestConfiguration().getVaultWallet(),
                TestConfiguration.getTestConfiguration().getVaultWallet().getEthAddress(),
                "100",
                String.valueOf(fastGas),
                false);
        assertNotNull(withdrawPax.transactionAddress);
        Account2FAResponse balanceValidationPax = getDexBalances(TestConfiguration.getTestConfiguration().getVaultWallet().getEthAddress());

        //PAX balances are the same. All transaction fee is in ETH
        assertEquals(paxBalance, balanceValidationPax.getEth().get(0).getBalances().getPax(), "balances are different");
        double newEthBalanceAfterPax = newEthBalance - (300000 * 0.000000001 * fastGas);

    }


}
