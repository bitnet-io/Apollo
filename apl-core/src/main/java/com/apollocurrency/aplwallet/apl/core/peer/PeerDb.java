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
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Singleton
public class PeerDb {

    private final DatabaseManager databaseManager;

    @Inject
    public PeerDb(DatabaseManager databaseManagerParam) {
        databaseManager = databaseManagerParam;
    }

    public List<Entry> loadPeers() {
        List<Entry> peers = new ArrayList<>();
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM peer");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                peers.add(new Entry(rs.getString("address"), rs.getLong("services"), rs.getInt("last_updated")));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return peers;
    }

    public void deletePeer(Entry peer) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection()) {
            PreparedStatement pstmt = con.prepareStatement("DELETE FROM peer WHERE address = ?");
            pstmt.setString(1, peer.getAddress());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void deletePeers(Collection<Entry> peers) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("DELETE FROM peer WHERE address = ?")) {
            for (Entry peer : peers) {
                pstmt.setString(1, peer.getAddress());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void updatePeers(Collection<Entry> peers) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        dataSource.begin();
        try (Connection con = dataSource.getConnection();
             @DatabaseSpecificDml(DmlMarker.MERGE)
             PreparedStatement pstmt = con.prepareStatement("INSERT INTO peer "
                 + "(address, services, last_updated) "
                 + "VALUES(?, ?, ?) "
                 + "ON DUPLICATE KEY UPDATE "
                 + "address = VALUES(address), services = VALUES(services), last_updated = VALUES(last_updated)")
        ) {
            for (Entry peer : peers) {
                pstmt.setString(1, peer.getAddress());
                pstmt.setLong(2, peer.getServices());
                pstmt.setInt(3, peer.getLastUpdated());
                pstmt.executeUpdate();
            }
            dataSource.commit();
        } catch (SQLException e) {
            dataSource.rollback();
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void updatePeer(PeerImpl peer) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        dataSource.begin();
        try (Connection con = dataSource.getConnection();
             @DatabaseSpecificDml(DmlMarker.MERGE)
             PreparedStatement pstmt = con.prepareStatement("INSERT INTO peer "
                 + "(address, services, last_updated) VALUES(?, ?, ?) "
                 + "ON DUPLICATE KEY UPDATE "
                 + "address = VALUES(address), services = VALUES(services), last_updated = VALUES(last_updated)")) {
            pstmt.setString(1, peer.getAnnouncedAddress());
            pstmt.setLong(2, peer.getServices());
            pstmt.setInt(3, peer.getLastUpdated());
            pstmt.executeUpdate();
            dataSource.commit();
        } catch (SQLException e) {
            dataSource.rollback();
            throw new RuntimeException(e.toString(), e);
        }
    }

    static class Entry {
        private final String address;
        private final long services;
        private final int lastUpdated;

        Entry(String address, long services, int lastUpdated) {
            this.address = address;
            this.services = services;
            this.lastUpdated = lastUpdated;
        }

        public String getAddress() {
            return address;
        }

        public long getServices() {
            return services;
        }

        public int getLastUpdated() {
            return lastUpdated;
        }

        @Override
        public int hashCode() {
            return address.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return (obj != null && (obj instanceof Entry) && address.equals(((Entry) obj).address));
        }

        @Override
        public String toString() {
            return "PeerEntry{" +
                "address='" + address + '\'' +
                ", services=" + services +
                ", lastUpdated=" + lastUpdated +
                '}';
        }
    }
}
