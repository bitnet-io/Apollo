/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.currency;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LinkKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyMint;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Singleton
public class CurrencyMintTable extends VersionedDeletableEntityDbTable<CurrencyMint> {

    public static final LinkKeyFactory<CurrencyMint> currencyMintDbKeyFactory = new LinkKeyFactory<>("currency_id", "account_id") {
        @Override
        public DbKey newKey(CurrencyMint currencyMint) {
            if (currencyMint.getDbKey() == null) {
                currencyMint.setDbKey(super.newKey(currencyMint.getCurrencyId(), currencyMint.getAccountId()));
            }
            return currencyMint.getDbKey();
        }
    };

    @Inject
    public CurrencyMintTable(DatabaseManager databaseManager,
                             Event<FullTextOperationData> fullTextOperationDataEvent) {
        super("currency_mint", currencyMintDbKeyFactory, null,
                databaseManager, fullTextOperationDataEvent);
    }

    @Override
    public CurrencyMint load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new CurrencyMint(rs, dbKey);
    }

    @Override
    public void save(Connection con, CurrencyMint currencyMint) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO currency_mint (currency_id, account_id, counter, height, latest, deleted) "
                + "VALUES (?, ?, ?, ?, TRUE, FALSE) "
                + "ON DUPLICATE KEY UPDATE currency_id = VALUES(currency_id), account_id = VALUES(account_id), counter = VALUES(counter), "
                + "height = VALUES(height), latest = TRUE, deleted = FALSE")
        ) {
            int i = 0;
            pstmt.setLong(++i, currencyMint.getCurrencyId());
            pstmt.setLong(++i, currencyMint.getAccountId());
            pstmt.setLong(++i, currencyMint.getCounter());
            pstmt.setInt(++i, currencyMint.getHeight());
            pstmt.executeUpdate();
        }
    }

}
