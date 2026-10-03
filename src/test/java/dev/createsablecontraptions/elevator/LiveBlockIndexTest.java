package dev.createsablecontraptions.elevator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LiveBlockIndexTest {
    @Test void unchangedMembershipKeepsSnapshotWithoutRebuildingIt() {
        long[] saved = {1,2,3};
        assertSame(saved, LiveBlockIndex.update(saved,2,true));
        assertSame(saved, LiveBlockIndex.update(saved,4,false));
        assertNotSame(saved, LiveBlockIndex.update(saved,4,true));
    }
    @Test void cancelledPlacementRestoresMembership() {
        long[] original = {1, 2};
        var placed = LiveBlockIndex.update(original, 3, true);
        assertArrayEquals(new long[]{1, 2, 3}, placed);
        assertArrayEquals(original, LiveBlockIndex.update(placed, 3, false));
        assertArrayEquals(new long[]{1, 2}, original);
    }
    @Test void stateChangesAndRepeatedCallbacksDoNotDuplicateBlocks() {
        var saved = new long[]{Long.MIN_VALUE, 0, Long.MAX_VALUE};
        for (int i = 0; i < 20; i++) saved = LiveBlockIndex.update(saved, Long.MIN_VALUE, true);
        assertArrayEquals(new long[]{Long.MIN_VALUE, 0, Long.MAX_VALUE}, saved);
    }
    @Test void dismantlingSnapshotSurvivesAllRemovalCallbacks() {
        long[] snapshot = {1, 2, 3};
        long[] current = snapshot;
        for (long pos : snapshot) current = LiveBlockIndex.update(current, pos, false);
        assertArrayEquals(new long[0], current);
        assertArrayEquals(new long[]{1, 2, 3}, snapshot);
    }
    @Test void reloadAndFurtherEditsDoNotResurrectRemovedOriginalBlocks() {
        var edited = LiveBlockIndex.update(new long[]{1, 2}, 1, false);
        edited = LiveBlockIndex.update(edited, 3, true);
        var reloaded = edited.clone(); // long-array payload stored in Sable user NBT
        reloaded = LiveBlockIndex.update(reloaded, 4, true);
        assertArrayEquals(new long[]{2, 3, 4}, reloaded);
    }
}
