/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.dto.utils;

import com.apollocurrency.aplwallet.api.dto.BaseDTO;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class QrEncodeDto extends BaseDTO {
    public String qrCodeBase64;
}