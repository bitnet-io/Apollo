/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto.account;

import java.util.List;

import com.apollocurrency.aplwallet.api.dto.BaseDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
public class AccountControlPhasingDTO extends BaseDTO {

    private String account;
    private String accountRS;
//    private String publicKey;

    private Long maxFees;
    private Short minDuration;
    private Short maxDuration;
    private Long quorum;

    private List<WhileListEntity> whitelist;

    private PhasingParamsDTO phasingParams;

    @Getter
    @Setter
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WhileListEntity {
        private String whitelisted;
        private String whitelistedRS;
    }

}
