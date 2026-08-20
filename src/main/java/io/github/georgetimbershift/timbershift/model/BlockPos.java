package io.github.georgetimbershift.timbershift.model;

import java.util.Comparator;

public record BlockPos(int x, int y, int z) {
    public static final Comparator<BlockPos> Y_X_Z_ORDER = Comparator
            .comparingInt(BlockPos::y)
            .thenComparingInt(BlockPos::x)
            .thenComparingInt(BlockPos::z);

    public BlockPos offset(int dx, int dy, int dz) {
        return new BlockPos(x + dx, y + dy, z + dz);
    }

    public BlockPos below(int distance) {
        return offset(0, -distance, 0);
    }

    public int chunkX() {
        return Math.floorDiv(x, 16);
    }

    public int chunkZ() {
        return Math.floorDiv(z, 16);
    }
}
