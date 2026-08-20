package io.github.georgetimbershift.timbershift.leaf;

import io.github.georgetimbershift.timbershift.model.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeafCanopyWatchTest {
    @Test
    void candidateOrCardinalNeighborRemovalWakesCanopyButDiagonalDoesNot() {
        LeafCanopyWatch watch = new LeafCanopyWatch(UUID.randomUUID(),
                List.of(new BlockPos(5, 10, 5)), 2_000L);

        assertTrue(watch.isTriggeredBy(new BlockPos(5, 10, 5)));
        assertTrue(watch.isTriggeredBy(new BlockPos(4, 10, 5)));
        assertFalse(watch.isTriggeredBy(new BlockPos(4, 9, 5)));
        assertFalse(watch.isTriggeredBy(new BlockPos(3, 10, 5)));
    }

    @Test
    void wakeThrottleAndExpiryAreIndependent() {
        LeafCanopyWatch watch = new LeafCanopyWatch(UUID.randomUUID(),
                List.of(new BlockPos(0, 0, 0)), 2_000L);

        assertTrue(watch.mayWake(10));
        watch.deferWakeUntil(15);
        assertFalse(watch.mayWake(14));
        assertTrue(watch.mayWake(15));
        assertFalse(watch.isExpired(1_999L));
        assertTrue(watch.isExpired(2_000L));
    }

    @Test
    void refreshAndForgetReplaceTheRememberedCanopy() {
        BlockPos oldLeaf = new BlockPos(0, 0, 0);
        BlockPos newLeaf = new BlockPos(1, 1, 1);
        LeafCanopyWatch watch = new LeafCanopyWatch(UUID.randomUUID(), List.of(oldLeaf), 1_000L);

        watch.refresh(List.of(newLeaf), 3_000L);
        assertFalse(watch.isTriggeredBy(oldLeaf));
        assertTrue(watch.isTriggeredBy(newLeaf));
        watch.forget(newLeaf);
        assertTrue(watch.isEmpty());
        assertFalse(watch.isExpired(2_999L));
    }
}
