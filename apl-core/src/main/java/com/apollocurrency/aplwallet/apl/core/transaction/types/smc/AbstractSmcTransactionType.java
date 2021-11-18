/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.smc;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.exception.AplTransactionExecutionException;
import com.apollocurrency.aplwallet.apl.core.exception.AplUnacceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractToolService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.PostponedContractService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcBlockchainIntegratorFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractSmcAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.smc.service.SmcContractTxProcessor;
import com.apollocurrency.aplwallet.apl.util.annotation.FeeMarker;
import com.apollocurrency.aplwallet.apl.util.annotation.TransactionFee;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.fuel.Fuel;
import com.apollocurrency.smc.contract.fuel.FuelCalculator;
import com.apollocurrency.smc.contract.fuel.FuelValidator;
import com.apollocurrency.smc.contract.vm.ExecutionLog;
import com.apollocurrency.smc.polyglot.JSAssertionException;
import com.apollocurrency.smc.polyglot.JSRequirementException;
import com.apollocurrency.smc.polyglot.JSRevertException;
import com.apollocurrency.smc.polyglot.PolyglotException;
import com.apollocurrency.smc.polyglot.engine.InternalNotRecoverableException;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public abstract class AbstractSmcTransactionType extends TransactionType {
    protected PostponedContractService contractService;
    protected ContractToolService contractToolService;
    protected final FuelValidator fuelMinMaxValidator;
    protected final SmcBlockchainIntegratorFactory integratorFactory;
    protected final Blockchain blockchain;
    protected final SmcConfig smcConfig;

    AbstractSmcTransactionType(BlockchainConfig blockchainConfig, Blockchain blockchain,
                               AccountService accountService,
                               PostponedContractService contractService,
                               ContractToolService contractToolService,
                               FuelValidator fuelMinMaxValidator,
                               SmcBlockchainIntegratorFactory integratorFactory,
                               SmcConfig smcConfig) {
        super(blockchainConfig, accountService);
        this.contractService = contractService;
        this.contractToolService = contractToolService;
        this.fuelMinMaxValidator = fuelMinMaxValidator;
        this.integratorFactory = integratorFactory;
        this.blockchain = blockchain;
        this.smcConfig = smcConfig;
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        //TODO calculate the required fuel value by executing the contract
        //currently use: fee = fuelPrice * fuelLimit
        AbstractSmcAttachment attachment = (AbstractSmcAttachment) transaction.getAttachment();
        var fee = attachment.getFuelPrice().multiply(attachment.getFuelLimit()).longValueExact();
        return new Fee.ConstantFee(fee);
    }

    @Override
    public boolean canFailDuringExecution() {
        return true;
    }

    @Override
    public String getName() {
        return getSpec().getCompatibleName();
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    @Override
    public boolean isPhasingSafe() {
        return false;
    }

    @Override
    public boolean isPhasable() {
        return false;
    }

    @Override
    public boolean canHaveRecipient() {
        return true;
    }

    @Override
    public final void doStateIndependentValidation(Transaction transaction) throws AplUnacceptableTransactionValidationException {
        checkPrecondition(transaction);
        AbstractSmcAttachment attachment = (AbstractSmcAttachment) transaction.getAttachment();
        if (!fuelMinMaxValidator.validateLimitValue(attachment.getFuelLimit())) {
            throw new AplUnacceptableTransactionValidationException("Fuel limit value doesn't correspond to the MIN or MAX values.", transaction);
        }
        if (!fuelMinMaxValidator.validatePriceValue(attachment.getFuelPrice())) {
            throw new AplUnacceptableTransactionValidationException("Fuel price value doesn't correspond to the MIN or MAX values.", transaction);
        }

        executeStateIndependentValidation(transaction, attachment);
    }

    public abstract void executeStateIndependentValidation(Transaction transaction, AbstractSmcAttachment abstractSmcAttachment) throws AplUnacceptableTransactionValidationException;

    private void checkPrecondition(Transaction smcTransaction) {
        smcTransaction.getAttachment().getTransactionTypeSpec();
        if (smcTransaction.getAttachment().getTransactionTypeSpec() != getSpec()) {
            log.error("Invalid transaction attachment, txType={} txId={}", smcTransaction.getType(), smcTransaction.getId());
            throw new AplUnacceptableTransactionValidationException("Invalid transaction attachment: " + smcTransaction.getAttachment().getTransactionTypeSpec(), smcTransaction);
        }
    }

    protected void refundRemaining(Transaction transaction, Account senderAccount, Fuel fuel) {
        if (fuel.hasRemaining()) {
            //refund remaining fuel
            log.debug("fuel={}, refunded fee={}, account={}", fuel, fuel.refundedFee().longValueExact(), senderAccount.getId());
            getAccountService().addToBalanceAndUnconfirmedBalanceATM(senderAccount, LedgerEvent.SMC_REFUNDED_FEE, transaction.getId(), fuel.refundedFee().longValueExact());
        }
    }

    protected void executeContract(Transaction transaction, Account senderAccount, SmartContract smartContract, SmcContractTxProcessor processor) {
        var executionLog = new ExecutionLog();
        try {

            processor.process(executionLog);

        } catch (JSRevertException | JSRequirementException e) {
            Fuel fuel = smartContract.getFuel();
            log.info("Refundable exception {} Contract={} Fuel={}", e.getClass().getSimpleName(), smartContract.getAddress(), fuel);
            refundRemaining(transaction, senderAccount, fuel);
            throw new AplTransactionExecutionException(e.getMessage(), e, transaction);
        } catch (JSAssertionException e) {
            log.info("Assertion exception Contract={}, charged all fee={}", smartContract.getAddress(), smartContract.getFuel().fee());
            throw new AplTransactionExecutionException(e.getMessage(), e, transaction);
        } catch (InternalNotRecoverableException e) {
            log.error(executionLog.toJsonString());
            log.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n", e);
            e.printStackTrace();
            System.exit(1);
        } catch (PolyglotException e) {
            log.error(executionLog.toJsonString());
            throw new AplTransactionExecutionException(e.getMessage(), e, transaction);
        }
        log.debug("Commit the contract state changes...");
        processor.commit();
        @TransactionFee({FeeMarker.BACK_FEE, FeeMarker.FUEL})
        Fuel fuel = smartContract.getFuel();
        refundRemaining(transaction, senderAccount, fuel);
    }

    static class SmcFuelBasedFee extends Fee.FuelBasedFee {
        public SmcFuelBasedFee(FuelCalculator fuelCalculator) {
            super(fuelCalculator);
        }

        @Override
        public int getSize(Transaction transaction, Appendix appendage) {
            return ((AbstractSmcAttachment) transaction.getAttachment()).getPayableSize();
        }

        @Override
        public BigInteger getFuelPrice(Transaction transaction, Appendix appendage) {
            return ((AbstractSmcAttachment) appendage).getFuelPrice();
        }
    }

}
