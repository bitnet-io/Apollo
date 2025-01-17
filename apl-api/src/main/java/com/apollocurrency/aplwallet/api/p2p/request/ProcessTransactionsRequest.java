/*
 *  Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.p2p.request;

import com.apollocurrency.aplwallet.api.dto.UnconfirmedTransactionDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ProcessTransactionsRequest extends BaseP2PRequest {
    public List<UnconfirmedTransactionDTO> transactions;

    public ProcessTransactionsRequest(UUID chainId) {
        super("processTransactions", chainId);
    }

    public ProcessTransactionsRequest(List<UnconfirmedTransactionDTO> transactions, UUID chainId) {
        this(chainId);
        this.transactions = transactions;
    }
}
