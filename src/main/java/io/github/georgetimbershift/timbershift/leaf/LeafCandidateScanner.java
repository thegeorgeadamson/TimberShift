package io.github.georgetimbershift.timbershift.leaf;

import io.github.georgetimbershift.timbershift.config.TimberShiftConfig;
import io.github.georgetimbershift.timbershift.model.BlockPos;
import io.github.georgetimbershift.timbershift.model.LogFamily;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class LeafCandidateScanner {
    private static final int[][] CARDINAL_6 = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    public LeafScanResult collect(
            LeafWorld world,
            Collection<BlockPos> treePositions,
            Set<LogFamily> families,
            TimberShiftConfig.FastDecay config
    ) {
        if (treePositions.isEmpty()) {
            return new LeafScanResult(LeafScanStatus.NONE, List.of(), 0);
        }
        ScanBounds bounds = ScanBounds.around(treePositions, config.maxRadius());
        BoundedReader reader = new BoundedReader(world, config.maxScannedBlocks());
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> discovered = new HashSet<>();
        List<BlockPos> candidates = new ArrayList<>();
        try {
            for (BlockPos treePosition : treePositions) {
                for (int[] direction : CARDINAL_6) {
                    BlockPos seed = treePosition.offset(direction[0], direction[1], direction[2]);
                    addIfCandidate(seed, bounds, families, reader, discovered, queue);
                }
            }

            while (!queue.isEmpty()) {
                BlockPos current = queue.remove();
                candidates.add(current);
                if (candidates.size() > config.maxLeavesPerTree()) {
                    return new LeafScanResult(LeafScanStatus.LEAF_LIMIT_REACHED, List.of(), reader.count());
                }
                for (int[] direction : CARDINAL_6) {
                    BlockPos adjacent = current.offset(direction[0], direction[1], direction[2]);
                    addIfCandidate(adjacent, bounds, families, reader, discovered, queue);
                }
            }
        } catch (ScanLimitReached ignored) {
            return new LeafScanResult(LeafScanStatus.SCAN_LIMIT_REACHED, List.of(), reader.count());
        }

        if (candidates.isEmpty()) {
            return new LeafScanResult(LeafScanStatus.NONE, List.of(), reader.count());
        }
        candidates.sort(BlockPos.Y_X_Z_ORDER);
        return new LeafScanResult(LeafScanStatus.COLLECTED, candidates, reader.count());
    }

    private void addIfCandidate(
            BlockPos position,
            ScanBounds bounds,
            Set<LogFamily> families,
            BoundedReader reader,
            Set<BlockPos> discovered,
            Queue<BlockPos> queue
    ) {
        if (discovered.contains(position) || !bounds.contains(position)) {
            return;
        }
        LeafCellSnapshot cell = reader.read(position);
        if (cell.kind() == LeafCellKind.NATURAL_LEAF && cell.family() != null
                && families.contains(cell.family())) {
            discovered.add(position);
            queue.add(position);
        }
    }

    private record ScanBounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        private static ScanBounds around(Collection<BlockPos> positions, int radius) {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (BlockPos position : positions) {
                minX = Math.min(minX, position.x());
                minY = Math.min(minY, position.y());
                minZ = Math.min(minZ, position.z());
                maxX = Math.max(maxX, position.x());
                maxY = Math.max(maxY, position.y());
                maxZ = Math.max(maxZ, position.z());
            }
            return new ScanBounds(minX - radius, maxX + radius, minY - radius, maxY + radius,
                    minZ - radius, maxZ + radius);
        }

        private boolean contains(BlockPos position) {
            return position.x() >= minX && position.x() <= maxX
                    && position.y() >= minY && position.y() <= maxY
                    && position.z() >= minZ && position.z() <= maxZ;
        }
    }

    private static final class BoundedReader {
        private final LeafWorld world;
        private final int maximum;
        private final Map<BlockPos, LeafCellSnapshot> cache = new HashMap<>();

        private BoundedReader(LeafWorld world, int maximum) {
            this.world = world;
            this.maximum = maximum;
        }

        private LeafCellSnapshot read(BlockPos position) {
            LeafCellSnapshot existing = cache.get(position);
            if (existing != null) {
                return existing;
            }
            if (cache.size() >= maximum) {
                throw new ScanLimitReached();
            }
            LeafCellSnapshot cell = world.read(position);
            cache.put(position, cell);
            return cell;
        }

        private int count() {
            return cache.size();
        }
    }

    private static final class ScanLimitReached extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private ScanLimitReached() {
            super(null, null, false, false);
        }
    }
}
