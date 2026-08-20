package io.github.georgetimbershift.timbershift.tree;

import io.github.georgetimbershift.timbershift.model.BlockPos;

import java.util.Map;

public record TreeDetectionResult(
        DetectionStatus status,
        Map<BlockPos, CellSnapshot> logs,
        int naturalLeafCount,
        int scannedBlocks
) {
    public TreeDetectionResult {
        logs = Map.copyOf(logs);
    }

    public boolean accepted() {
        return status == DetectionStatus.ACCEPTED;
    }

    public static TreeDetectionResult rejected(DetectionStatus status, int scannedBlocks) {
        return new TreeDetectionResult(status, Map.of(), 0, scannedBlocks);
    }
}
