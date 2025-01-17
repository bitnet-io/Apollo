/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.p2p.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

@Getter
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetCumulativeDifficultyRequest extends BaseP2PRequest {
    private static final String REQUEST_TYPE = "getCumulativeDifficulty";

    public GetCumulativeDifficultyRequest(UUID chainId) {
        super(REQUEST_TYPE, chainId);
    }

}
