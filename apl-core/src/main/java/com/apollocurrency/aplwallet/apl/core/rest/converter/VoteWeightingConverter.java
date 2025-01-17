/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.account.VoteWeightingDTO;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;

public class VoteWeightingConverter implements Converter<VoteWeighting, VoteWeightingDTO> {

    @Override
    public VoteWeightingDTO apply(VoteWeighting model) {
        VoteWeightingDTO dto = new VoteWeightingDTO();
        dto.setHoldingId(Long.toUnsignedString(model.getHoldingId()));
        dto.setMinBalance(model.getMinBalance());
        dto.setMinBalanceModel(model.getMinBalanceModel().getCode());
        dto.setVotingModel(model.getVotingModel().getCode());
        return dto;
    }
}
