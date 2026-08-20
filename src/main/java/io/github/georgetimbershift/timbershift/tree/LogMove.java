package io.github.georgetimbershift.timbershift.tree;

import io.github.georgetimbershift.timbershift.model.BlockPos;

public record LogMove(BlockPos source, BlockPos destination, String originalState) {
}
