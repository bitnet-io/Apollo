/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.peer.PeerState;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Version;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.slf4j.LoggerFactory.getLogger;

@Vetoed
public final class DumpPeers extends AbstractAPIRequestHandler {

    private static final Logger LOG = getLogger(DumpPeers.class);

    public DumpPeers() {
        super(new APITag[]{APITag.DEBUG}, "version", "weight", "connect", "adminPassword");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        Version version = new Version(Convert.nullToEmpty(req.getParameter("version")));

        int weight =
            HttpParameterParserUtil.getInt(req, "weight", 0, (int) CDI.current().select(BlockchainConfig.class).get().getCurrentConfig().getMaxBalanceAPL(),
                false);
        boolean connect = "true".equalsIgnoreCase(req.getParameter("connect")) && apw.checkPassword(req);
        if (connect) {
            List<Callable<Object>> connects = new ArrayList<>();
            lookupPeersService().getAllPeers().forEach(peer -> connects.add(() -> {
                lookupPeersService().connectPeer(peer);
                return null;
            }));
            ExecutorService service = Executors.newFixedThreadPool(10);
            try {
                service.invokeAll(connects);
            } catch (InterruptedException e) {
                LOG.info(e.toString(), e);
            }
        }
        Set<String> addresses = new HashSet<>();
        lookupPeersService().getAllPeers().forEach(peer -> {
            if (peer.getState() == PeerState.CONNECTED
                && peer.shareAddress()
                && !peer.isBlacklisted()
                && peer.getVersion() != null && peer.getVersion().equals(version)
                && (weight == 0 || peer.getWeight() > weight)) {
                addresses.add(peer.getAnnouncedAddress());
            }
        });
        StringBuilder buf = new StringBuilder();
        for (String address : addresses) {
            buf.append(address).append("; ");
        }
        JSONObject response = new JSONObject();
        response.put("peers", buf.toString());
        response.put("count", addresses.size());
        return response;
    }

    @Override
    protected final boolean requirePost() {
        return true;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

}
