/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.service.tx;

import com.apollocurrency.aplwallet.apl.smc.SmcContext;
import com.apollocurrency.smc.contract.ContractStatus;
import com.apollocurrency.smc.contract.SmartContract;
import com.apollocurrency.smc.contract.vm.ResultValue;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.apollocurrency.aplwallet.apl.util.exception.ApiErrors.CONTRACT_VALIDATION_ERROR;

/**
 * Validate the smart contract - create and initialize the smart contract and manipulate balances in sandbox.
 * This validation process doesn't change the blockchain state.
 * This processor should be used during the state independent validation routine of the transaction
 *
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class PublishContractTxValidator extends AbstractSmcContractTxProcessor {

    public PublishContractTxValidator(SmartContract smartContract, SmcContext context) {
        super(smartContract, context);
    }

    @Override
    public ResultValue process() {
        validateStatus(ContractStatus.CREATED);
        var result = ResultValue.from(getSmartContract());
        var isValid = smcMachine.validateContract(getSmartContract());
        if (!isValid) {
            getExecutionLog().setErrorCode(CONTRACT_VALIDATION_ERROR.getErrorCode());
            result.setErrorCode(CONTRACT_VALIDATION_ERROR.getErrorCode());
            result.setOutput(List.of(false));
            result.setErrorDescription(smcMachine.getExecutionLog().toJsonString());
        }
        return result;
    }
}
