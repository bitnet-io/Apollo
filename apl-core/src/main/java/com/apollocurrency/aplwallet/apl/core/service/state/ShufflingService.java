/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShufflingEvent;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingStage;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingCancellationAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingCreationAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingProcessingAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingRecipientsAttachment;
import com.apollocurrency.aplwallet.apl.util.Listener;

import java.util.List;

public interface ShufflingService {

    byte[][] getData(long shufflingId, long accountId);

    byte[][] getData(long transactionId);

    void restoreData(long transactionId, long shufflingId, long accountId, byte[][] data, int timestamp, int height);

    void setData(Transaction tx, ShufflingParticipant participant, byte[][] data);

    List<ShufflingParticipant> getParticipants(long shufflingId);

    ShufflingParticipant getParticipant(long shufflingId, long accountId);

    ShufflingParticipant getLastParticipant(long shufflingId);

    void addParticipant(long shufflingId, long accountId, int index);

    int getVerifiedCount(long shufflingId);

    void changeStatusToProcessed(ShufflingParticipant participant, byte[] dataTransactionFullHash, byte[] dataHash);

    void changeStatusToVerified(ShufflingParticipant participant);

    void changeStatusToCancel(ShufflingParticipant participant, byte[][] blameData, byte[][] keySeeds);

    ShufflingParticipant getPreviousParticipant(ShufflingParticipant participant);

    boolean delete(ShufflingParticipant participant);



    void addShuffling(Transaction transaction, ShufflingCreationAttachment attachment);

    int getShufflingCount();

    int getShufflingActiveCount();

    boolean addListener(Listener<Shuffling> listener, ShufflingEvent eventType);

    boolean removeListener(Listener<Shuffling> listener, ShufflingEvent eventType);

    List<Shuffling> getAll(int from, int to);

    List<Shuffling> getActiveShufflings(int from, int to);

    List<Shuffling> getActiveShufflings();

    List<Shuffling> getFinishedShufflings(int from, int to);

    byte[] getFullHash(long shufflingId);

    Shuffling getShuffling(long shufflingId);

    Shuffling getShuffling(byte[] fullHash);

    int getHoldingShufflingCount(long holdingId, boolean includeFinished);

    List<Shuffling> getHoldingShufflings(long holdingId, ShufflingStage stage, boolean includeFinished, int from, int to);

    List<Shuffling> getAccountShufflings(long accountId, boolean includeFinished, int from, int to);

    List<Shuffling> getAssignedShufflings(long assigneeAccountId, int from, int to);

    byte[] getParticipantsHash(Iterable<ShufflingParticipant> participants);

    ShufflingAttachment processShuffling(Shuffling shuffling, final long accountId, final byte[] secretBytes, final byte[] recipientPublicKey);

    ShufflingCancellationAttachment revealKeySeeds(Shuffling shuffling, final byte[] secretBytes, long cancellingAccountId, byte[] shufflingStateHash);


    void verify(Shuffling shuffling, long accountId);

    boolean isFull(Shuffling shuffling, Block block);

    void cancelBy(Shuffling shuffling, ShufflingParticipant participant, byte[][] blameData, byte[][] keySeeds);

    void cancel(Shuffling shuffling, Block block);

    void save(Shuffling shuffling);

    void updateParticipantData(Shuffling shuffling, Transaction transaction, ShufflingProcessingAttachment attachment);

    void updateRecipients(Shuffling shuffling, Transaction transaction, ShufflingRecipientsAttachment attachment);

    void addParticipant(Shuffling shuffling, long participantId);

    byte[] getStageHash(Shuffling shuffling);

}
