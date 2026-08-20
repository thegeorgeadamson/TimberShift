package io.github.georgetimbershift.timbershift.leaf;

import io.github.georgetimbershift.timbershift.model.BlockPos;
import io.github.georgetimbershift.timbershift.testsupport.MapLeafWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeafSupportCheckerTest {
    private static final BlockPos CANDIDATE = new BlockPos(0, 0, 0);
    private final LeafSupportChecker checker = new LeafSupportChecker();

    @Test
    void nonPersistentUnsupportedLeafIsEligible() {
        MapLeafWorld world = new MapLeafWorld().naturalLeaf(0, 0, 0);

        assertEquals(LeafEligibilityStatus.ELIGIBLE, checker.evaluate(world, CANDIDATE));
    }

    @Test
    void persistentPlayerPlacedLeafIsNeverEligible() {
        MapLeafWorld world = new MapLeafWorld().persistentLeaf(0, 0, 0);

        assertEquals(LeafEligibilityStatus.PERSISTENT, checker.evaluate(world, CANDIDATE));
    }

    @Test
    void leafWithVanillaRangeLogSupportIsProtected() {
        MapLeafWorld world = new MapLeafWorld()
                .naturalLeaf(0, 0, 0)
                .naturalLeaf(1, 0, 0)
                .naturalLeaf(2, 0, 0)
                .support(3, 0, 0);

        assertEquals(LeafEligibilityStatus.SUPPORTED, checker.evaluate(world, CANDIDATE));
    }

    @Test
    void touchingCanopyOfAdjacentSupportedTreeIsProtected() {
        MapLeafWorld world = new MapLeafWorld()
                .naturalLeaf(0, 0, 0)
                .naturalLeaf(1, 0, 0)
                .naturalLeaf(2, 0, 0)
                .naturalLeaf(3, 0, 0)
                .naturalLeaf(4, 0, 0)
                .support(5, 0, 0);

        assertEquals(LeafEligibilityStatus.SUPPORTED, checker.evaluate(world, CANDIDATE));
    }

    @Test
    void changedQueuedLeafStateIsObservedAtProcessingTime() {
        MapLeafWorld world = new MapLeafWorld().naturalLeaf(0, 0, 0);
        assertEquals(LeafEligibilityStatus.ELIGIBLE, checker.evaluate(world, CANDIDATE));

        world.persistentLeaf(0, 0, 0);
        assertEquals(LeafEligibilityStatus.PERSISTENT, checker.evaluate(world, CANDIDATE));

        world.naturalLeaf(0, 0, 0).support(1, 0, 0);
        assertEquals(LeafEligibilityStatus.SUPPORTED, checker.evaluate(world, CANDIDATE));

        world.put(CANDIDATE, LeafCellSnapshot.other());
        assertEquals(LeafEligibilityStatus.NOT_A_LEAF, checker.evaluate(world, CANDIDATE));
    }

    @Test
    void unloadedSupportSearchIsConservativelySkipped() {
        MapLeafWorld world = new MapLeafWorld()
                .naturalLeaf(0, 0, 0)
                .put(new BlockPos(1, 0, 0), LeafCellSnapshot.unloaded());

        assertEquals(LeafEligibilityStatus.UNLOADED, checker.evaluate(world, CANDIDATE));
    }
}
