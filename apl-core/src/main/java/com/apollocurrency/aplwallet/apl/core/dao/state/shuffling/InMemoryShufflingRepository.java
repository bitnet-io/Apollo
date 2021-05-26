/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.shuffling;

import com.apollocurrency.aplwallet.apl.core.dao.state.InMemoryVersionedDerivedEntityRepository;
import com.apollocurrency.aplwallet.apl.core.db.ChangeUtils;
import com.apollocurrency.aplwallet.apl.core.db.model.EntityWithChanges;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingStage;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;

import javax.enterprise.inject.Vetoed;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Vetoed
public class InMemoryShufflingRepository extends InMemoryVersionedDerivedEntityRepository<Shuffling> implements ShufflingRepository {

    private static final String RECIPIENTS_PUBLIC_KEYS = "recipient_public_keys";
    private static final String BLOCKS_REMAINING       = "blocks_remaining";
    private static final String ASSIGNEE_ACCOUNT_ID    = "assignee_account_id";
    private static final String REGISTRANT_COUNT = "registrant_count";
    private static final String STAGE                  ="stage";
    private static final Comparator<Shuffling> DEFAULT_COMPARATOR = Comparator.comparing(Shuffling::getId);
    private static final Comparator<Shuffling> FINISHED_LAST_BLOCKS_REMAINING_ASC_HEIGHT_DESC_COMPARATOR = (o1, o2) -> {
        // blocks_remaining asc nulls last
        int diff = Integer.compare(o1.getBlocksRemaining(), o2.getBlocksRemaining());
        if (diff != 0) {
            if (o1.getBlocksRemaining() == 0) {
                return 1;
            } else if (o2.getBlocksRemaining() == 0) {
                return -1;
            } else {
                return diff;
            }
        }
        // height desc
        diff = Integer.compare(o2.getHeight(), o1.getHeight());
        if (diff == 0) {
            // db_id asc
            diff = Long.compare(o1.getDbId(), o2.getDbId());
        }
        return diff;
    };

    public InMemoryShufflingRepository() {
        super(dbKeyFactory, List.of("recipient_public_keys", "blocks_remaining", "assignee_account_id", "registrant_count", "stage"));
    }

    public Shuffling getCopy(long id) {
        return getCopy(dbKeyFactory.newKey(id));
    }

    @Override
    public int getCount() {
        return getInReadLock(() -> (int) latestStream().count());
    }

    @Override
    public int getActiveCount() {
        return getInReadLock(()-> (int)
                latestStream()
                .filter(s -> s.getBlocksRemaining() > 0)
                .count());
    }

    @Override
    public List<Shuffling> extractAll(int from, int to) {
        return getAll(FINISHED_LAST_BLOCKS_REMAINING_ASC_HEIGHT_DESC_COMPARATOR, from, to);
    }

    @Override
    public List<Shuffling> getActiveShufflings(int from, int to) {
        return getInReadLock(() ->
                CollectionUtil.limitStream(
                        latestStream()
                                .filter(s -> s.getBlocksRemaining() > 0) // skip finished
                                .sorted(Comparator.comparing(Shuffling::getBlocksRemaining)  // order by blocks_remaining asc and then by height desc
                                        .thenComparing(Comparator.comparing(Shuffling::getHeight).reversed())
                                .thenComparing(Shuffling::getDbId))
                        , from, to)
                        .map(Shuffling::deepCopy) // copy shuffling to ensure that returned instance changes will not affect stored shufflings
                        .collect(Collectors.toList()));
    }

    @Override
    public List<Shuffling> getFinishedShufflings(int from, int to) {
        return getInReadLock(()->
                CollectionUtil.limitStream(
                        latestStream()
                                .filter(s -> s.getBlocksRemaining() <= 0) // include only finished
                                .sorted(Comparator.comparing(Shuffling::getHeight).reversed().thenComparing(Shuffling::getDbId))
                        , from, to)
                .collect(Collectors.toList()));
    }

    @Override
    public Shuffling get(long shufflingId) {
        return get(dbKeyFactory.newKey(shufflingId));
    }

    @Override
    public int getHoldingShufflingCount(long holdingId, boolean includeFinished) {
        return getInReadLock(()-> {
            Stream<Shuffling> shufflingStream = latestStream()
                    .filter(s -> s.getHoldingId() == holdingId);
            if (!includeFinished) {
                shufflingStream = shufflingStream.filter(s -> s.getBlocksRemaining() > 0);
            }
            return (int) shufflingStream.count();
        });
    }

    @Override
    public List<Shuffling> getHoldingShufflings(long holdingId, ShufflingStage stage, boolean includeFinished, int from, int to) {
        return getInReadLock(()-> {
            Stream<Shuffling> shufflingStream = latestStream()
                    .filter(s -> s.getHoldingId() == holdingId);
            if (!includeFinished) {
                shufflingStream = shufflingStream.filter(s -> s.getBlocksRemaining() > 0);
            }
            if (stage != null) {
                shufflingStream = shufflingStream.filter(s -> s.getStage() == stage);
            }

            return CollectionUtil.limitStream(
                    shufflingStream.sorted(FINISHED_LAST_BLOCKS_REMAINING_ASC_HEIGHT_DESC_COMPARATOR)
                    , from, to)
                    .collect(Collectors.toList());
        });
    }

    @Override
    public List<Shuffling> getAssignedShufflings(long assigneeAccountId, int from, int to) {
        return getInReadLock(()->
            CollectionUtil.limitStream(
                    latestStream()
                            .filter(s -> s.getAssigneeAccountId() == assigneeAccountId && s.getStage() == ShufflingStage.PROCESSING)
                            .sorted(FINISHED_LAST_BLOCKS_REMAINING_ASC_HEIGHT_DESC_COMPARATOR)
                    , from, to)
                    .collect(Collectors.toList())
        );
    }

    @Override
    public List<Shuffling> getAccountShufflings(long accountId, boolean includeFinished, int from, int to) {
        throw new UnsupportedOperationException("Unable to select account shufflings using in-memory table");
    }

    private Stream<Shuffling> latestStream() {
        return getAllEntities().values()
                .stream()
                .map(EntityWithChanges::getEntity)
                .filter(VersionedDerivedEntity::isLatest); // skip deleted
    }

    @Override
    public Value analyzeChanges(String columnName, Object prevValue, Shuffling entity) {
        switch (columnName) {
            case BLOCKS_REMAINING:
                return ChangeUtils.getChange(entity.getBlocksRemaining(), prevValue);
            case STAGE:
                return ChangeUtils.getChange(entity.getStage(), prevValue);
            case ASSIGNEE_ACCOUNT_ID:
                return ChangeUtils.getChange(entity.getAssigneeAccountId(), prevValue);
            case RECIPIENTS_PUBLIC_KEYS:
                return ChangeUtils.getDoubleByteArrayChange(entity.getRecipientPublicKeys(), prevValue);
            case REGISTRANT_COUNT:
                return ChangeUtils.getChange(entity.getRegistrantCount(), prevValue);
            default:
                throw new IllegalArgumentException("Unable to find change analyzer for column '" + columnName + "'");
        }
    }


    @Override
    public void setColumn(String columnName, Object value, Shuffling entity) {
        switch (columnName) {
            case BLOCKS_REMAINING:
                entity.setBlocksRemaining(((short) value));
                break;
            case STAGE:
                entity.setStage((ShufflingStage) value);
                break;
            case ASSIGNEE_ACCOUNT_ID:
                entity.setAssigneeAccountId((long) value);
                break;
            case RECIPIENTS_PUBLIC_KEYS:
                entity.setRecipientPublicKeys((byte[][]) value);
                break;
            case REGISTRANT_COUNT:
                entity.setRegistrantCount((byte)value);
                break;
            default:
                throw new IllegalArgumentException("Unable to set column '" + columnName + "'");
        }
    }
}
