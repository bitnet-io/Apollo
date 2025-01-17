/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws.subscription;

import com.apollocurrency.aplwallet.apl.smc.ws.SmcEventSocket;
import com.apollocurrency.aplwallet.apl.smc.ws.converter.RequestToSubscriptionConverter;
import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventMessage;
import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventSubscriptionRequest;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import com.apollocurrency.smc.contract.vm.event.EventArguments;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.data.type.ContractEvent;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.WebSocketException;

import static com.apollocurrency.smc.util.HexUtils.toHex;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class SubscriptionManager {
    static final int MAX_SIZE = 200;
    private final RegisteredSocketContainer registeredSockets;

    private final Converter<SmcEventSubscriptionRequest, Subscription> converter;

    public SubscriptionManager() {
        this(new RegisteredSocketContainer());
    }

    public SubscriptionManager(RegisteredSocketContainer registeredSockets) {
        this.registeredSockets = registeredSockets;
        this.converter = new RequestToSubscriptionConverter();
    }

    public int getRegisteredSocketsCount() {
        return registeredSockets.size();
    }

    /**
     * Registers the listener for any events from the contract address.
     *
     * @param address the contract address
     * @param socket  the incoming socket object
     * @return true if listener registered successfully.
     */
    public boolean register(Address address, SmcEventSocket socket) {
        if (registeredSockets.size() < MAX_SIZE) {
            if (!registeredSockets.register(address, socket)) {
                throw new WebSocketException("The socket is already registered.");
            } else {
                return true;
            }
        } else {
            throw new WebSocketException("The queue size exceeds the MAX value.");
        }
    }

    /**
     * Registers the listener for any events from the contract address.
     *
     * @param address the contract address
     * @param socket  the incoming socket object
     * @param request the incoming subscription request
     * @return true if subscription added successfully.
     */
    public boolean addSubscription(Address address, SmcEventSocket socket, SmcEventSubscriptionRequest request) {
        if (registeredSockets.isRegistered(address, socket)) {
            if (!request.getEvents().isEmpty()) {
                return registeredSockets.addSubscription(address, socket, converter.convert(request));
            }
            return true;
        } else {
            log.debug("The socket is not registered.");
            return false;
        }
    }

    public void remove(Address address, SmcEventSocket session) {
        registeredSockets.remove(address, session);
    }

    public boolean removeSubscription(Address address, SmcEventSocket socket, SmcEventSubscriptionRequest request) {
        if (registeredSockets.isRegistered(address, socket)) {
            if (!request.getEvents().isEmpty()) {
                var event = request.getEvents().get(0);
                return registeredSockets.removeSubscription(address, socket, event.getSignature());
            }
            return true;
        } else {
            log.debug("The socket is not registered.");
            return false;
        }
    }

    /**
     * Broadcast given event to all subscribers.
     *
     * @param contractEvent the fired contract event
     */
    public void fire(ContractEvent contractEvent, EventArguments params) {
        var signature = toHex(contractEvent.getSignature());
        if (log.isDebugEnabled()) {
            log.debug("Fired event, contract={} signature={} params={}", contractEvent.getContract().getHex(), signature, params);
        }
        var sockets = registeredSockets.getSubscriptionSockets(contractEvent.getContract(), signature);
        if (!sockets.isEmpty()) {
            log.debug("found {} subscriptions", sockets.size());
            var response = toMessage(contractEvent);
            sockets.forEach(socket -> {
                var subscription = socket.getSubscription();
                log.debug("Check subscription={}", subscription);
                if (checkSubscription(subscription, contractEvent, params)) {
                    log.debug("is matched, send response to remote={}", socket.getSocket().getRemote());
                    response.setSubscriptionId(subscription.getSubscriptionId());
                    response.setParsedParams(params.getMap());
                    socket.getSocket().sendWebSocket(response);
                }
            });
        }
    }

    private static boolean checkSubscription(Subscription subscription, ContractEvent contractEvent, EventArguments params) {
        return subscription.getFromBlock() <= contractEvent.getHeight()
            && subscription.getFilter().test(params.getMap());
    }

    private static SmcEventMessage toMessage(ContractEvent contractEvent) {
        return SmcEventMessage.builder()
            .name(contractEvent.getSpec())
            .address(contractEvent.getContract().getHex())
            .signature(toHex(contractEvent.getSignature()))
            .transactionIndex(contractEvent.getTxIdx())
            .transaction(contractEvent.getTransaction().getHex())
            .data(contractEvent.getState())
            .build();
    }
}
