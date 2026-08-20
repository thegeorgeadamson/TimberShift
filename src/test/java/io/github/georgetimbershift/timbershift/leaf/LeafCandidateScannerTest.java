package io.github.georgetimbershift.timbershift.leaf;

import io.github.georgetimbershift.timbershift.config.TimberShiftConfig;
import io.github.georgetimbershift.timbershift.model.BlockPos;
import io.github.georgetimbershift.timbershift.model.LogFamily;
import io.github.georgetimbershift.timbershift.testsupport.MapLeafWorld;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeafCandidateScannerTest {
    private final LeafCandidateScanner scanner = new LeafCandidateScanner();
    private final BlockPos origin = new BlockPos(0, 0, 0);

    @Test
    void collectsOnlyConnectedMatchingNaturalLeaves() {
        MapLeafWorld world = new MapLeafWorld()
                .naturalLeaf(0, 1, 0)
                .naturalLeaf(0, 2, 0)
                .persistentLeaf(0, 3, 0)
                .put(new BlockPos(1, 1, 0), LeafCellSnapshot.naturalLeaf(LogFamily.BIRCH, 7));

        LeafScanResult result = scanner.collect(world, List.of(origin), Set.of(LogFamily.OAK),
                config(512, 2048, 12));

        assertTrue(result.collected());
        assertEquals(List.of(new BlockPos(0, 1, 0), new BlockPos(0, 2, 0)), result.candidates());
        assertFalse(result.candidates().contains(new BlockPos(0, 3, 0)));
    }

    @Test
    void oversizedConnectedCanopyAbortsWithoutPartialCandidateList() {
        MapLeafWorld world = new MapLeafWorld();
        for (int y = 1; y <= 6; y++) {
            world.naturalLeaf(0, y, 0);
        }

        LeafScanResult result = scanner.collect(world, List.of(origin), Set.of(LogFamily.OAK),
                config(4, 2048, 12));

        assertEquals(LeafScanStatus.LEAF_LIMIT_REACHED, result.status());
        assertTrue(result.candidates().isEmpty());
    }

    @Test
    void scanBudgetCannotBeExceeded() {
        MapLeafWorld world = new MapLeafWorld().naturalLeaf(0, 1, 0);

        LeafScanResult result = scanner.collect(world, List.of(origin), Set.of(LogFamily.OAK),
                config(512, 3, 12));

        assertEquals(LeafScanStatus.SCAN_LIMIT_REACHED, result.status());
        assertEquals(3, result.scannedBlocks());
    }

    @Test
    void radiusStopsTraversalOutsideAssociatedTreeArea() {
        MapLeafWorld world = new MapLeafWorld();
        for (int y = 1; y <= 5; y++) {
            world.naturalLeaf(0, y, 0);
        }

        LeafScanResult result = scanner.collect(world, List.of(origin), Set.of(LogFamily.OAK),
                config(512, 2048, 2));

        assertEquals(List.of(new BlockPos(0, 1, 0), new BlockPos(0, 2, 0)), result.candidates());
    }

    @Test
    void tallTreeCanopyRadiusIsMeasuredFromLogStructure() {
        BlockPos highLog = new BlockPos(0, 20, 0);
        MapLeafWorld world = new MapLeafWorld().naturalLeaf(0, 21, 0);

        LeafScanResult result = scanner.collect(world, List.of(origin, highLog), Set.of(LogFamily.OAK),
                config(512, 2048, 2));

        assertEquals(List.of(new BlockPos(0, 21, 0)), result.candidates());
    }

    private TimberShiftConfig.FastDecay config(int maxLeaves, int maxScanned, int radius) {
        return new TimberShiftConfig.FastDecay(true, 10, 2, 32, maxLeaves, radius, maxScanned,
                32, true, new TimberShiftConfig.LeafEffects(true, false));
    }
}
