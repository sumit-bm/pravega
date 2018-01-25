/**
 * Copyright (c) 2017 Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.segmentstore.storage.impl.bookkeeperstorage;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.concurrent.GuardedBy;
import lombok.Getter;
import org.apache.bookkeeper.client.LedgerHandle;

/**
 * Stores metadata about a single BookKeeper Ledger.
 */
class LedgerData {
    // Retrieved from ZK
    @Getter
    private final LedgerHandle ledgerHandle;

    @GuardedBy("this")
    //Retrieved from ZK
    private int startOffset;
    //Version to ensure CAS in ZK
    private final  int updateVersion;
    //Epoch under which the ledger is created
    private final long containerEpoch;

    // Temporary variables. These are not persisted to ZK.
    //These are interpreted from BookKeeper and may be updated inproc.
    @GuardedBy("this")
    private long length;

    @GuardedBy("this")
    private long lastAddConfirmed = -1;

    @GuardedBy("this")
    private long lastReadOffset = 0;
    @GuardedBy("this")
    private long lastReadEntry = 0;
    @GuardedBy("this")
    private AtomicBoolean readonly;

    public LedgerData(LedgerHandle ledgerHandle, int offset, int updateVersion, long containerEpoch) {
        this.ledgerHandle = ledgerHandle;
        this.startOffset = offset;
        this.updateVersion = updateVersion;
        this.containerEpoch = containerEpoch;
        this.readonly = new AtomicBoolean(false);
    }

    public byte[] serialize() {
        int size = Long.SIZE + Long.SIZE;

        ByteBuffer bb = ByteBuffer.allocate(size);
        bb.putLong(this.ledgerHandle.getId());
        bb.putLong(this.containerEpoch);
        return bb.array();
    }

    synchronized void increaseLengthBy(int size) {
        this.length += size;
    }

    synchronized long getNearestEntryIDToOffset(long offset) {
        if (this.lastReadOffset < offset) {
            return lastReadEntry;
        }
        return 0;
    }

    synchronized void saveLastReadOffset(long offset, long entryId) {
        this.lastReadOffset = offset;
        this.lastReadEntry = entryId;
    }

    synchronized void setLastAddConfirmed(long lastAddConfirmed) {
        this.lastAddConfirmed = lastAddConfirmed;
    }

    synchronized long getLastAddConfirmed() {
        return this.lastAddConfirmed;
    }

    synchronized void setReadonly(boolean readonly) {
        this.readonly.set(readonly);
    }

    synchronized void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }

    synchronized int getStartOffset() {
        return startOffset;
    }

    synchronized void setLength(int length) {
        this.length = length;
    }
}