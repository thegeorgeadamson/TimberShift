package io.github.georgetimbershift.timbershift.service;

import io.github.georgetimbershift.timbershift.model.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DropRelocationPlannerTest {
    private static final BlockPos ORIGIN = new BlockPos(10, 64, 10);

    @Test
    void choosesTheOpenSideNearestThePlayer() {
        assertEquals(new BlockPos(11, 64, 10),
                DropRelocationPlanner.chooseSide(ORIGIN, 13.0, 10.5, ignored -> true).orElseThrow());
    }

    @Test
    void skipsBlockedCandidatesWithoutChoosingTheFilledOrigin() {
        Set<BlockPos> blocked = Set.of(
                new BlockPos(11, 64, 10),
                new BlockPos(11, 64, 11),
                new BlockPos(11, 64, 9));

        BlockPos chosen = DropRelocationPlanner.chooseSide(
                ORIGIN, 13.0, 10.5, position -> !blocked.contains(position)).orElseThrow();

        assertEquals(new BlockPos(10, 64, 11), chosen);
    }

    @Test
    void returnsEmptyWhenEverySideIsBlocked() {
        assertTrue(DropRelocationPlanner.chooseSide(ORIGIN, 13.0, 10.5, ignored -> false).isEmpty());
    }

    @Test
    void preservesTheVanillaWithinBlockPositionAtTheSafeDestination() {
        DropRelocationPlanner.DropPosition position = DropRelocationPlanner.preserveVanillaPosition(
                ORIGIN,
                new BlockPos(11, 64, 10),
                10.23,
                64.62,
                10.74);

        assertEquals(11.23, position.x(), 0.000_001);
        assertEquals(64.62, position.y(), 0.000_001);
        assertEquals(10.74, position.z(), 0.000_001);
    }

    @Test
    void keepsEdgeSpawnsInsideTheDestinationBlock() {
        DropRelocationPlanner.DropPosition position = DropRelocationPlanner.preserveVanillaPosition(
                ORIGIN,
                new BlockPos(9, 64, 10),
                10.01,
                64.99,
                10.5);

        assertEquals(9.15, position.x(), 0.000_001);
        assertEquals(64.85, position.y(), 0.000_001);
        assertEquals(10.5, position.z(), 0.000_001);
    }
}
