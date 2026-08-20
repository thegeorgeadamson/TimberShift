package io.github.georgetimbershift.timbershift.tree;

import io.github.georgetimbershift.timbershift.model.BlockPos;
import io.github.georgetimbershift.timbershift.model.LogFamily;
import io.github.georgetimbershift.timbershift.testsupport.MapTreeWorld;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeShiftPlannerTest {
    private final TreeShiftPlanner planner = new TreeShiftPlanner();

    @Test
    void basicTreeMovesDownExactlyOneWithoutDuplication() {
        MapTreeWorld world = new MapTreeWorld();
        Map<BlockPos, CellSnapshot> logs = verticalLogs(world, 1, 4, "minecraft:oak_log[axis=y]");

        TreeShiftPlan plan = planner.plan(logs, world, 1, true);

        assertTrue(plan.ready());
        assertEquals(4, plan.moves().size());
        assertEquals(Set.of(0, 1, 2, 3), plan.moves().stream().map(move -> move.destination().y()).collect(
                java.util.stream.Collectors.toSet()));
        assertEquals(4, plan.moves().stream().map(LogMove::source).distinct().count());
        assertEquals(4, plan.moves().stream().map(LogMove::destination).distinct().count());
    }

    @Test
    void blockDataTokenIsCarriedUnchangedByPlan() {
        MapTreeWorld world = new MapTreeWorld();
        String axisState = "minecraft:oak_log[axis=x]";
        Map<BlockPos, CellSnapshot> logs = verticalLogs(world, 1, 1, axisState);

        TreeShiftPlan plan = planner.plan(logs, world, 1, true);

        assertEquals(axisState, plan.moves().getFirst().originalState());
    }

    @Test
    void solidDestinationPreventsAnyDestructiveMovement() {
        MapTreeWorld world = new MapTreeWorld().put(new BlockPos(0, 0, 0), CellSnapshot.obstruction());
        Map<BlockPos, CellSnapshot> logs = verticalLogs(world, 1, 3, "oak[axis=y]");

        TreeShiftPlan plan = planner.plan(logs, world, 1, true);

        assertEquals(ShiftPlanStatus.NO_MOVABLE_LOGS, plan.status());
        assertTrue(plan.moves().isEmpty());
        assertEquals(CellKind.OBSTRUCTION, world.read(new BlockPos(0, 0, 0)).kind());
    }

    @Test
    void twoByTwoTreeMovesOnlyFreedColumnAndNeverOverwritesNeighborBases() {
        MapTreeWorld world = new MapTreeWorld();
        Map<BlockPos, CellSnapshot> logs = new HashMap<>();
        for (int x = 0; x <= 1; x++) {
            for (int z = 0; z <= 1; z++) {
                world.put(new BlockPos(x, -1, z), CellSnapshot.naturalBlock());
                int baseY = x == 0 && z == 0 ? 1 : 0;
                for (int y = baseY; y <= 2; y++) {
                    BlockPos position = new BlockPos(x, y, z);
                    CellSnapshot log = CellSnapshot.log(LogFamily.OAK, "oak[axis=y]");
                    world.put(position, log);
                    logs.put(position, log);
                }
            }
        }

        TreeShiftPlan plan = planner.plan(logs, world, 1, true);

        assertTrue(plan.ready());
        assertEquals(Set.of(new BlockPos(0, 1, 0), new BlockPos(0, 2, 0)),
                plan.moves().stream().map(LogMove::source).collect(java.util.stream.Collectors.toSet()));
        Set<BlockPos> destinations = plan.moves().stream().map(LogMove::destination)
                .collect(java.util.stream.Collectors.toSet());
        assertFalse(destinations.contains(new BlockPos(1, 0, 0)));
        assertEquals(plan.moves().size(), destinations.size());
    }

    @Test
    void freeBranchesFallDeterministicallyWithTrunk() {
        MapTreeWorld world = new MapTreeWorld();
        Map<BlockPos, CellSnapshot> logs = verticalLogs(world, 1, 3, "oak[axis=y]");
        for (int x = 1; x <= 2; x++) {
            BlockPos branch = new BlockPos(x, 3, 0);
            CellSnapshot snapshot = CellSnapshot.log(LogFamily.OAK, "oak[axis=x]");
            world.put(branch, snapshot);
            logs.put(branch, snapshot);
        }

        TreeShiftPlan plan = planner.plan(logs, world, 1, true);

        assertTrue(plan.ready());
        assertEquals(5, plan.moves().size());
        assertEquals(new HashSet<>(logs.keySet()),
                plan.moves().stream().map(LogMove::source).collect(java.util.stream.Collectors.toSet()));
        assertEquals(plan.moves(), planner.plan(logs, world, 1, true).moves());
    }

    @Test
    void unloadedDestinationAbortsWholePlanWhenConfigured() {
        MapTreeWorld world = new MapTreeWorld().put(new BlockPos(0, 0, 0), CellSnapshot.unloaded());
        Map<BlockPos, CellSnapshot> logs = verticalLogs(world, 1, 2, "oak[axis=y]");

        TreeShiftPlan plan = planner.plan(logs, world, 1, true);

        assertEquals(ShiftPlanStatus.UNLOADED_CHUNK, plan.status());
        assertTrue(plan.moves().isEmpty());
    }

    private Map<BlockPos, CellSnapshot> verticalLogs(
            MapTreeWorld world,
            int minimumY,
            int maximumY,
            String state
    ) {
        Map<BlockPos, CellSnapshot> logs = new HashMap<>();
        for (int y = minimumY; y <= maximumY; y++) {
            BlockPos position = new BlockPos(0, y, 0);
            CellSnapshot snapshot = CellSnapshot.log(LogFamily.OAK, state);
            logs.put(position, snapshot);
            world.put(position, snapshot);
        }
        return logs;
    }
}
