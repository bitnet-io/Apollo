/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.derived;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.model.DerivedIdEntity;

public class DerivedEntityKeyFactory extends LongKeyFactory<DerivedIdEntity> {
    public DerivedEntityKeyFactory() {
        super("id");
    }

    public DerivedEntityKeyFactory(String idColumn) {
        super(idColumn);
    }

    @Override
    public DbKey newKey(DerivedIdEntity derivedIdEntity) {
        if (derivedIdEntity.getDbKey() == null) {
            derivedIdEntity.setDbKey(new LongKey(derivedIdEntity.getId()));
        }
        return derivedIdEntity.getDbKey();
    }
}
