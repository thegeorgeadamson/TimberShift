package io.github.georgetimbershift.timbershift.leaf;

import io.github.georgetimbershift.timbershift.model.BlockPos;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/** Mirrors vanilla's six-direction leaf-distance rule: a leaf is supported by a tagged log within six steps. */
public final class LeafSupportChecker {
    private static final int MAX_SUPPORT_DISTANCE = 6;
    private static final int[][] CARDINAL_6 = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    public LeafEligibilityStatus evaluate(LeafWorld world, BlockPos candidate) {
        LeafCellSnapshot initial = world.read(candidate);
        if (initial.kind() == LeafCellKind.UNLOADED) {
            return LeafEligibilityStatus.UNLOADED;
        }
        if (initial.kind() == LeafCellKind.PERSISTENT_LEAF) {
            return LeafEligibilityStatus.PERSISTENT;
        }
        if (initial.kind() != LeafCellKind.NATURAL_LEAF) {
            return LeafEligibilityStatus.NOT_A_LEAF;
        }

        Queue<SearchNode> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(new SearchNode(candidate, 0));
        visited.add(candidate);

        while (!queue.isEmpty()) {
            SearchNode current = queue.remove();
            for (int[] direction : CARDINAL_6) {
                BlockPos adjacent = current.position.offset(direction[0], direction[1], direction[2]);
                LeafCellSnapshot cell = world.read(adjacent);
                if (cell.kind() == LeafCellKind.UNLOADED) {
                    return LeafEligibilityStatus.UNLOADED;
                }
                if (cell.kind() == LeafCellKind.LOG_SUPPORT) {
                    return LeafEligibilityStatus.SUPPORTED;
                }
                int nextDistance = current.distance + 1;
                if (nextDistance < MAX_SUPPORT_DISTANCE && cell.isLeaf() && visited.add(adjacent)) {
                    queue.add(new SearchNode(adjacent, nextDistance));
                }
            }
        }
        return LeafEligibilityStatus.ELIGIBLE;
    }

    private record SearchNode(BlockPos position, int distance) {
    }
}
