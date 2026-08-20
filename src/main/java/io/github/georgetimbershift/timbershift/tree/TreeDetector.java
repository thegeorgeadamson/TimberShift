package io.github.georgetimbershift.timbershift.tree;

import io.github.georgetimbershift.timbershift.config.TimberShiftConfig;
import io.github.georgetimbershift.timbershift.model.BlockPos;
import io.github.georgetimbershift.timbershift.model.LogFamily;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class TreeDetector {
    private static final int[][] ADJACENT_26 = createAdjacent26();
    private static final int[][] CARDINAL_6 = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    public TreeDetectionResult detect(
            TreeWorld world,
            BlockPos brokenPosition,
            LogFamily originalFamily,
            TimberShiftConfig.Detection config,
            boolean trustedContinuation,
            boolean abortOnUnloadedChunk
    ) {
        Scanner scanner = new Scanner(world, config.limits().maxScannedBlocks(), abortOnUnloadedChunk);
        try {
            Map<BlockPos, CellSnapshot> logs = discoverLogs(scanner, brokenPosition, originalFamily, config);
            if (logs.isEmpty()) {
                return TreeDetectionResult.rejected(DetectionStatus.NO_CONNECTED_LOGS, scanner.count());
            }

            int minY = Math.min(brokenPosition.y(), logs.keySet().stream().mapToInt(BlockPos::y).min().orElseThrow());
            int maxY = logs.keySet().stream().mapToInt(BlockPos::y).max().orElseThrow();
            if (brokenPosition.y() - minY > config.maxBreakHeightAboveBase()) {
                return TreeDetectionResult.rejected(DetectionStatus.BREAK_TOO_HIGH, scanner.count());
            }
            if (maxY - minY + 1 > config.limits().maxHeight()) {
                return TreeDetectionResult.rejected(DetectionStatus.HEIGHT_LIMIT_REACHED, scanner.count());
            }

            Set<LogFamily> families = new HashSet<>();
            logs.values().forEach(snapshot -> families.add(snapshot.family()));
            Set<BlockPos> naturalLeaves = new HashSet<>();
            boolean structureContact = false;
            for (BlockPos log : logs.keySet()) {
                for (int[] direction : ADJACENT_26) {
                    BlockPos adjacent = log.offset(direction[0], direction[1], direction[2]);
                    CellSnapshot cell = scanner.read(adjacent);
                    if (cell.kind() == CellKind.NATURAL_LEAF && families.contains(cell.family())) {
                        naturalLeaves.add(adjacent);
                    }
                }
                if (log.y() > minY + 1 && touchesObviousStructure(scanner, log)) {
                    structureContact = true;
                }
            }

            if (structureContact) {
                return TreeDetectionResult.rejected(DetectionStatus.OBVIOUS_STRUCTURE_CONTACT, scanner.count());
            }
            if (config.requireNaturalLeaves() && !trustedContinuation
                    && naturalLeaves.size() < config.minimumNaturalLeaves()) {
                return new TreeDetectionResult(DetectionStatus.INSUFFICIENT_NATURAL_LEAVES,
                        Map.of(), naturalLeaves.size(), scanner.count());
            }

            return new TreeDetectionResult(DetectionStatus.ACCEPTED, logs, naturalLeaves.size(), scanner.count());
        } catch (DetectionAbort abort) {
            return TreeDetectionResult.rejected(abort.status, scanner.count());
        }
    }

    private Map<BlockPos, CellSnapshot> discoverLogs(
            Scanner scanner,
            BlockPos brokenPosition,
            LogFamily originalFamily,
            TimberShiftConfig.Detection config
    ) {
        Map<BlockPos, CellSnapshot> logs = new LinkedHashMap<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> queued = new HashSet<>();

        for (int[] direction : ADJACENT_26) {
            BlockPos seed = brokenPosition.offset(direction[0], direction[1], direction[2]);
            CellSnapshot cell = scanner.read(seed);
            if (cell.kind() == CellKind.LOG) {
                requireCompatibleFamily(cell, originalFamily, config.allowMixedLogFamilies());
                queue.add(seed);
                queued.add(seed);
            }
        }

        while (!queue.isEmpty()) {
            BlockPos current = queue.remove();
            CellSnapshot currentCell = scanner.read(current);
            if (currentCell.kind() != CellKind.LOG || logs.containsKey(current)) {
                continue;
            }
            enforceBounds(current, brokenPosition, config.limits());
            if (logs.size() >= config.limits().maxLogs()) {
                throw new DetectionAbort(DetectionStatus.LOG_LIMIT_REACHED);
            }
            requireCompatibleFamily(currentCell, originalFamily, config.allowMixedLogFamilies());
            logs.put(current, currentCell);

            for (int[] direction : ADJACENT_26) {
                BlockPos adjacent = current.offset(direction[0], direction[1], direction[2]);
                if (logs.containsKey(adjacent) || queued.contains(adjacent)) {
                    continue;
                }
                CellSnapshot cell = scanner.read(adjacent);
                if (cell.kind() == CellKind.LOG) {
                    requireCompatibleFamily(cell, originalFamily, config.allowMixedLogFamilies());
                    enforceBounds(adjacent, brokenPosition, config.limits());
                    queue.add(adjacent);
                    queued.add(adjacent);
                }
            }
        }
        return logs;
    }

    private void requireCompatibleFamily(CellSnapshot cell, LogFamily originalFamily, boolean allowMixed) {
        if (!allowMixed && cell.family() != originalFamily) {
            throw new DetectionAbort(DetectionStatus.MIXED_LOG_FAMILY);
        }
    }

    private void enforceBounds(BlockPos position, BlockPos origin, TimberShiftConfig.Limits limits) {
        if (Math.abs(position.x() - origin.x()) > limits.maxHorizontalRadius()
                || Math.abs(position.z() - origin.z()) > limits.maxHorizontalRadius()) {
            throw new DetectionAbort(DetectionStatus.HORIZONTAL_LIMIT_REACHED);
        }
        if (Math.abs(position.y() - origin.y()) >= limits.maxHeight()) {
            throw new DetectionAbort(DetectionStatus.HEIGHT_LIMIT_REACHED);
        }
    }

    private boolean touchesObviousStructure(Scanner scanner, BlockPos log) {
        for (int[] direction : CARDINAL_6) {
            if (scanner.read(log.offset(direction[0], direction[1], direction[2])).kind()
                    == CellKind.OBSTRUCTION) {
                return true;
            }
        }
        return false;
    }

    private static int[][] createAdjacent26() {
        List<int[]> directions = new ArrayList<>(26);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        directions.add(new int[]{x, y, z});
                    }
                }
            }
        }
        return directions.toArray(int[][]::new);
    }

    private static final class Scanner {
        private final TreeWorld world;
        private final int maximum;
        private final boolean abortOnUnloadedChunk;
        private final Map<BlockPos, CellSnapshot> cache = new HashMap<>();

        private Scanner(TreeWorld world, int maximum, boolean abortOnUnloadedChunk) {
            this.world = world;
            this.maximum = maximum;
            this.abortOnUnloadedChunk = abortOnUnloadedChunk;
        }

        private CellSnapshot read(BlockPos position) {
            CellSnapshot cached = cache.get(position);
            if (cached != null) {
                return cached;
            }
            if (cache.size() >= maximum) {
                throw new DetectionAbort(DetectionStatus.SCAN_LIMIT_REACHED);
            }
            CellSnapshot result = world.read(position);
            if (result.kind() == CellKind.UNLOADED) {
                if (abortOnUnloadedChunk) {
                    throw new DetectionAbort(DetectionStatus.UNLOADED_CHUNK);
                }
                result = CellSnapshot.obstruction();
            }
            cache.put(position, result);
            return result;
        }

        private int count() {
            return cache.size();
        }
    }

    private static final class DetectionAbort extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final DetectionStatus status;

        private DetectionAbort(DetectionStatus status) {
            super(status.name(), null, false, false);
            this.status = status;
        }
    }
}
