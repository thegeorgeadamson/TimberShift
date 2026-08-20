package io.github.georgetimbershift.timbershift.leaf;

import io.github.georgetimbershift.timbershift.model.BlockPos;

@FunctionalInterface
public interface LeafWorld {
    LeafCellSnapshot read(BlockPos position);
}
