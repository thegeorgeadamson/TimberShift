package io.github.georgetimbershift.timbershift.leaf;

import io.github.georgetimbershift.timbershift.model.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeafDecayQueueTest {
    @Test
    void disabledConfigurationQueuesAbsolutelyNothing() {
        LeafDecayQueue queue = new LeafDecayQueue();

        boolean accepted = queue.enqueue(false, UUID.randomUUID(), List.of(new BlockPos(0, 0, 0)),
                10, 32);

        assertFalse(accepted);
        assertTrue(queue.isEmpty());
    }

    @Test
    void initialDelayAndActiveOperationLimitAreEnforced() {
        LeafDecayQueue queue = new LeafDecayQueue();
        UUID world = UUID.randomUUID();
        assertTrue(queue.enqueue(true, world, List.of(new BlockPos(0, 0, 0)), 10, 1));
        assertFalse(queue.enqueue(true, world, List.of(new BlockPos(1, 0, 0)), 10, 1));

        assertNull(queue.pollReady(9));
        LeafDecayOperation ready = queue.pollReady(10);
        assertEquals(new BlockPos(0, 0, 0), ready.poll());
        assertTrue(ready.isEmpty());
    }

    @Test
    void operationStepTakesOnlyItsConfiguredVisibleSlice() {
        LeafDecayOperation operation = new LeafDecayOperation(UUID.randomUUID(), List.of(
                new BlockPos(0, 0, 0),
                new BlockPos(1, 0, 0),
                new BlockPos(2, 0, 0),
                new BlockPos(3, 0, 0)), 0);

        assertEquals(List.of(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0)), operation.pollUpTo(2));
        assertFalse(operation.isEmpty());
        assertEquals(List.of(new BlockPos(2, 0, 0), new BlockPos(3, 0, 0)), operation.pollUpTo(2));
        assertTrue(operation.isEmpty());
    }

    @Test
    void readyRoundReturnsEachOperationAtMostOnce() {
        LeafDecayQueue queue = new LeafDecayQueue();
        UUID world = UUID.randomUUID();
        queue.enqueue(true, world, List.of(new BlockPos(0, 0, 0)), 5, 3);
        queue.enqueue(true, world, List.of(new BlockPos(1, 0, 0)), 10, 3);
        queue.enqueue(true, world, List.of(new BlockPos(2, 0, 0)), 5, 3);

        List<LeafDecayOperation> ready = queue.pollReadyRound(5);

        assertEquals(2, ready.size());
        assertEquals(new BlockPos(0, 0, 0), ready.get(0).poll());
        assertEquals(new BlockPos(2, 0, 0), ready.get(1).poll());
        assertEquals(1, queue.size());
    }
}
