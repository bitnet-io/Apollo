/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.apl.util.exception;

//TODO Move to api module.
public enum ApiErrors implements ApiErrorInfo {

    INTERNAL_SERVER_EXCEPTION(-1, 100, "Internal error, root cause: %s"),
    BLOCKCHAIN_NOT_INITIALIZED(-1, 101, "Blockchain is not initialized"),
    UNCONFIRMED_TRANSACTION_CACHE_IS_FULL(-1, 102, "Blockchain is busy, the unconfirmed transaction cache is full"),

    JSON_SERIALIZATION_EXCEPTION(1, 1001, "Exception encountered during generating JSON content, root cause: %s"),

    CONSTRAINT_VIOLATION(4, 2001, "Constraint violation: %s"),
    MISSING_PARAM_LIST(3, 2002, "At least one of [%s] must be specified"),
    MISSING_PARAM(3, 2003, "The mandatory parameter ''{0}'' is not specified"),
    INCORRECT_VALUE(4, 2004, "Incorrect ''{0}'' value, [{1}] is not defined or wrong"),
    UNKNOWN_VALUE(5, 2005, "Unknown ''{0}'' : {1}"),
    OUT_OF_RANGE(4, 2006, "{0} is not in range [{1}..{2}]"),
    PEER_NOT_CONNECTED(5, 2007, "Peer not connected."),
    PEER_NOT_OPEN_API(5, 2008, "Peer is not providing open API."),
    FAILED_TO_ADD_PEER(8, 2009, "Failed to add peer %s"),
    ACCOUNT_GENERATION_ERROR(6, 2010, "Error occurred during account generation"),
    ONLY_ONE_OF_PARAM_LIST(6, 2011, "Not more than one of [%s] can be specified"),
    INCORRECT_PARAM_VALUE(4, 2012, "Incorrect ''{0}''"),
    INCORRECT_PARAM(4, 2013, "Incorrect {0}, {1}"),
    ACCOUNT_2FA_ERROR(22, 2014, "%s"),
    NO_PASSWORD_IN_CONFIG(8, 2015, "Administrator's password is not configured. Please set apl.adminPassword"),
    UNKNOWN_SERVER_ERROR(1, 2016, "Unknown server error: '%s', see stacktrace for details"),
    WEB3J_CRYPTO_ERROR(-1, 2017, "Web3j crypto error: '%s', see stacktrace for details"),
    ETH_NODE_ERROR(-1, 2018, "Unable to query eth node: '%s'"),
    PARAM_GREATER_OR_EQUAL_ERROR(4, 2019, "''{0}'' is greater or equal to ''{1}''"),
    DEX_NOT_ENOUGH_AMOUNT(6, 2020, "Not enough %s"),
    DEX_NOT_ENOUGH_FEE(6, 2021, "Not enough {0} for a fee. Min value is {1} {0}"),
    OUT_OF_RANGE_NAME_VALUE(4, 2022, "param ''{0}'' with value ''{1}'' is not within range [{2}..{3}]"),
    REST_API_SERVER_ERROR(1, 2023, "REST API error: ''%s'', see server's log for details"),
    OVERFLOW(11, 2024, "Overflow for value ''{0}''"),
    OVERFLOW_PARAM(11, 2025, "Overflow in param name ''{0}'' for value ''{1}''"),
    BAD_CREDENTIALS(4, 2026, "Unable to extract valid account credentials, ''{0}''"),
    FEATURE_NOT_ENABLED(9, 2027, "Feature not available, ''{0}''"),
    NOT_ENOUGH_FUNDS(6, 2028, "Not enough %s funds"),
    TX_VALIDATION_FAILED(-1, 2029, "Failed to validate tx: %s"),
    CUSTOM_ERROR_MESSAGE(8, 2030, "{0}"),
    EXCEPTION_MESSAGE(4, 2050, "{0}"),

    //KMS
    EXPORT_KEY_READ_WALLET(-1, 2201, "Can't read wallet."),
    NOT_FOUND_WALLET(-1, 2202, "Incorrect account id or passphrase"),
    NOT_FOUND_ETH_ACCOUNT(-1, 2203, "Incorrect ethereum address"),

    //SMC
    CONTRACT_PROCESSING_ERROR(-1, 3101, "Contract processing error: {0}."),
    CONTRACT_VALIDATION_ERROR(-1, 3102, "Contract validation error: {0}."),
    CONTRACT_METHOD_VALIDATION_ERROR(-1, 3103, "Contract method validation error: {0}."),
    CONTRACT_NOT_FOUND(-1, 3104, "Contract {0} not found."),
    CONTRACTS_NOT_FOUND(-1, 3105, "Contracts not found."),
    CONTRACT_READ_METHOD_ERROR(-1, 3106, "Call @view method error: {0}."),
    CONTRACT_SYNTAX_VALIDATION_ERROR(-1, 3107, "Contract syntax validation error: {0}."),
    CONTRACT_SPEC_NOT_FOUND(-1, 3108, "Contract specification for ASR module {0} not found."),
    CONTRACT_EVENT_FILTER_ERROR(-1, 3109, "Event filter parsing error: {0}."),
    ;
    private final int oldErrorCode;
    private final int errorCode;
    private final String errorDescription;

    ApiErrors(int oldErrorCode, int errorCode, String errorDescription) {
        this.oldErrorCode = oldErrorCode;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    @Override
    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public int getOldErrorCode() {
        return oldErrorCode;
    }

    @Override
    public String getErrorDescription() {
        return errorDescription;
    }

    public static String format(ApiErrorInfo error, Object... args) {
        return Messages.format(error.getErrorDescription(), args);
    }
}
