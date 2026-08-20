package io.github.georgetimbershift.timbershift.tree;

import io.github.georgetimbershift.timbershift.config.TimberShiftConfig;
import io.github.georgetimbershift.timbershift.model.BlockPos;
import io.github.georgetimbershift.timbershift.model.LogFamily;
import io.github.georgetimbershift.timbershift.testsupport.MapTreeWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeDetectorTest {
    private final TreeDetector detector = new TreeDetector();

    @Test
    void acceptsBasicNaturalTreeAfterBottomLogBreak() {
        MapTreeWorld world = basicTree(false);

        TreeDetectionResult result = detector.detect(world, new BlockPos(0, 0, 0), LogFamily.OAK,
                detection(256, 6000), false, true);

        assertTrue(result.accepted());
        assertEquals(4, result.logs().size());
        assertEquals(4, result.naturalLeafCount());
    }

    @Test
    void rejectsPlayerLogPillarWithoutLeaves() {
        MapTreeWorld world = new MapTreeWorld()
                .log(0, 1, 0, "oak[axis=y]")
                .log(0, 2, 0, "oak[axis=y]")
                .log(0, 3, 0, "oak[axis=y]");

        TreeDetectionResult result = detector.detect(world, new BlockPos(0, 0, 0), LogFamily.OAK,
                detection(256, 6000), false, true);

        assertEquals(DetectionStatus.INSUFFICIENT_NATURAL_LEAVES, result.status());
    }

    @Test
    void persistentLeavesDoNotCountAsNaturalEvidence() {
        MapTreeWorld world = basicTree(true);

        TreeDetectionResult result = detector.detect(world, new BlockPos(0, 0, 0), LogFamily.OAK,
                detection(256, 6000), false, true);

        assertEquals(DetectionStatus.INSUFFICIENT_NATURAL_LEAVES, result.status());
        assertEquals(0, result.naturalLeafCount());
    }

    @Test
    void oversizedConnectedStructureAbortsInsteadOfPartiallyDetecting() {
        MapTreeWorld world = basicTree(false).log(0, 5, 0, "oak[axis=y]");

        TreeDetectionResult result = detector.detect(world, new BlockPos(0, 0, 0), LogFamily.OAK,
                detection(3, 6000), false, true);

        assertEquals(DetectionStatus.LOG_LIMIT_REACHED, result.status());
        assertTrue(result.logs().isEmpty());
    }

    @Test
    void scanBudgetCannotBeExceeded() {
        MapTreeWorld world = basicTree(false);

        TreeDetectionResult result = detector.detect(world, new BlockPos(0, 0, 0), LogFamily.OAK,
                detection(256, 10), false, true);

        assertEquals(DetectionStatus.SCAN_LIMIT_REACHED, result.status());
        assertEquals(10, result.scannedBlocks());
    }

    @Test
    void obviousStructureTouchingUpperTrunkIsRejected() {
        MapTreeWorld world = basicTree(false)
                .put(new BlockPos(1, 3, 0), CellSnapshot.obstruction());

        TreeDetectionResult result = detector.detect(world, new BlockPos(0, 0, 0), LogFamily.OAK,
                detection(256, 6000), false, true);

        assertEquals(DetectionStatus.OBVIOUS_STRUCTURE_CONTACT, result.status());
    }

    @Test
    void trustedContinuationCanFinishAfterLeavesDisappear() {
        MapTreeWorld world = new MapTreeWorld()
                .log(0, 1, 0, "oak[axis=y]")
                .log(0, 2, 0, "oak[axis=y]");

        TreeDetectionResult result = detector.detect(world, new BlockPos(0, 0, 0), LogFamily.OAK,
                detection(256, 6000), true, true);

        assertTrue(result.accepted());
        assertEquals(0, result.naturalLeafCount());
    }

    @Test
    void unloadedBoundaryAbortsWithoutReadingThroughIt() {
        MapTreeWorld world = basicTree(false)
                .put(new BlockPos(-1, -1, -1), CellSnapshot.unloaded());

        TreeDetectionResult result = detector.detect(world, new BlockPos(0, 0, 0), LogFamily.OAK,
                detection(256, 6000), false, true);

        assertEquals(DetectionStatus.UNLOADED_CHUNK, result.status());
    }

    private MapTreeWorld basicTree(boolean persistentLeaves) {
        MapTreeWorld world = new MapTreeWorld()
                .log(0, 1, 0, "oak[axis=y]")
                .log(0, 2, 0, "oak[axis=y]")
                .log(0, 3, 0, "oak[axis=y]")
                .log(0, 4, 0, "oak[axis=y]");
        int[][] leaves = {{1, 4, 0}, {-1, 4, 0}, {0, 4, 1}, {0, 4, -1}};
        for (int[] leaf : leaves) {
            if (persistentLeaves) {
                world.persistentLeaf(leaf[0], leaf[1], leaf[2]);
            } else {
                world.naturalLeaf(leaf[0], leaf[1], leaf[2]);
            }
        }
        return world;
    }

    private TimberShiftConfig.Detection detection(int maxLogs, int maxScanned) {
        return new TimberShiftConfig.Detection(true, 4, false, 1, 180,
                new TimberShiftConfig.Limits(maxLogs, 48, 12, maxScanned));
    }
}
