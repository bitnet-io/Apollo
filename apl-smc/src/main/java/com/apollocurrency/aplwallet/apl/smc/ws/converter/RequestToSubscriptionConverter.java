/*
 * Copyright (c)  2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws.converter;

import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventSubscriptionRequest;
import com.apollocurrency.aplwallet.apl.smc.ws.subscription.Subscription;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import com.apollocurrency.smc.data.expr.TrueTerm;

import javax.inject.Singleton;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class RequestToSubscriptionConverter implements Converter<SmcEventSubscriptionRequest, Subscription> {

    @Override
    public Subscription apply(SmcEventSubscriptionRequest model) {
        var event = model.getEvents().get(0);
        return Subscription.builder()
            .requestId(model.getRequestId())
            .name(event.getName())
            .filter(event.getFilter() == null ? new TrueTerm() : event.getFilter())
            .fromBlock(event.getFromBlock() == null ? 0 : event.getFromBlock())
            .signature(event.getSignature())
            .subscriptionId(event.getSubscriptionId())
            .build();
    }
}
