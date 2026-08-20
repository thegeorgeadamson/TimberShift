package io.github.georgetimbershift.timbershift.tree;

import io.github.georgetimbershift.timbershift.model.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Computes a downward translation without changing the world. A log remains movable only when
 * every block along its path is air or another source that will also move. Repeating that rule to
 * a fixed point makes blocked vertical stacks stay put while free stacks and branches may fall.
 */
public final class TreeShiftPlanner {
    public TreeShiftPlan plan(
            Map<BlockPos, CellSnapshot> logs,
            TreeWorld world,
            int distance,
            boolean abortOnUnloadedChunk
    ) {
        List<BlockPos> ordered = logs.keySet().stream().sorted(BlockPos.Y_X_Z_ORDER).toList();
        Set<BlockPos> allSources = new HashSet<>(logs.keySet());
        Set<BlockPos> movable = new LinkedHashSet<>(ordered);

        boolean changed;
        do {
            changed = false;
            List<BlockPos> blocked = new ArrayList<>();
            for (BlockPos source : movable) {
                PathState state = inspectPath(source, distance, allSources, movable, world);
                if (state == PathState.UNLOADED && abortOnUnloadedChunk) {
                    return TreeShiftPlan.empty(ShiftPlanStatus.UNLOADED_CHUNK);
                }
                if (state != PathState.CLEAR) {
                    blocked.add(source);
                }
            }
            if (!blocked.isEmpty()) {
                movable.removeAll(blocked);
                changed = true;
            }
        } while (changed);

        if (movable.isEmpty()) {
            return TreeShiftPlan.empty(ShiftPlanStatus.NO_MOVABLE_LOGS);
        }

        List<LogMove> moves = movable.stream()
                .sorted(BlockPos.Y_X_Z_ORDER)
                .map(source -> new LogMove(source, source.below(distance), logs.get(source).state()))
                .toList();
        return new TreeShiftPlan(ShiftPlanStatus.READY, moves);
    }

    private PathState inspectPath(
            BlockPos source,
            int distance,
            Set<BlockPos> allSources,
            Set<BlockPos> movable,
            TreeWorld world
    ) {
        for (int step = 1; step <= distance; step++) {
            BlockPos position = source.below(step);
            if (allSources.contains(position)) {
                if (!movable.contains(position)) {
                    return PathState.BLOCKED;
                }
                continue;
            }
            CellKind kind = world.read(position).kind();
            if (kind == CellKind.UNLOADED) {
                return PathState.UNLOADED;
            }
            if (kind != CellKind.AIR) {
                return PathState.BLOCKED;
            }
        }
        return PathState.CLEAR;
    }

    private enum PathState {
        CLEAR,
        BLOCKED,
        UNLOADED
    }
}
