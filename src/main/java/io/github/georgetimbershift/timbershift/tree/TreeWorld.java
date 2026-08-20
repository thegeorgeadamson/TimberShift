package io.github.georgetimbershift.timbershift.tree;

import io.github.georgetimbershift.timbershift.model.BlockPos;

@FunctionalInterface
public interface TreeWorld {
    CellSnapshot read(BlockPos position);
}
