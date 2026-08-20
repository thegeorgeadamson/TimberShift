package io.github.georgetimbershift.timbershift.leaf;

import io.github.georgetimbershift.timbershift.model.BlockPos;

import java.util.List;

public record LeafScanResult(LeafScanStatus status, List<BlockPos> candidates, int scannedBlocks) {
    public LeafScanResult {
        candidates = List.copyOf(candidates);
    }

    public boolean collected() {
        return status == LeafScanStatus.COLLECTED && !candidates.isEmpty();
    }
}
