package io.github.georgetimbershift.timbershift.service;

import io.github.georgetimbershift.timbershift.model.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

final class DropRelocationPlanner {
    private static final double MIN_ITEM_OFFSET = 0.15;
    private static final double MAX_ITEM_OFFSET = 0.85;
    private static final int[][] SIDE_OFFSETS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    private DropRelocationPlanner() {
    }

    static Optional<BlockPos> chooseSide(
            BlockPos origin,
            double preferredX,
            double preferredZ,
            Predicate<BlockPos> isOpen
    ) {
        List<BlockPos> candidates = new ArrayList<>(SIDE_OFFSETS.length);
        for (int[] offset : SIDE_OFFSETS) {
            candidates.add(origin.offset(offset[0], 0, offset[1]));
        }
        candidates.sort(Comparator.comparingDouble(position -> distanceSquared(position, preferredX, preferredZ)));
        return candidates.stream().filter(isOpen).findFirst();
    }

    static DropPosition preserveVanillaPosition(
            BlockPos origin,
            BlockPos destination,
            double originalX,
            double originalY,
            double originalZ
    ) {
        return new DropPosition(
                translateCoordinate(origin.x(), destination.x(), originalX),
                translateCoordinate(origin.y(), destination.y(), originalY),
                translateCoordinate(origin.z(), destination.z(), originalZ));
    }

    private static double translateCoordinate(int origin, int destination, double coordinate) {
        double offset = coordinate - origin;
        double safeOffset = Math.max(MIN_ITEM_OFFSET, Math.min(MAX_ITEM_OFFSET, offset));
        return destination + safeOffset;
    }

    private static double distanceSquared(BlockPos position, double x, double z) {
        double deltaX = position.x() + 0.5 - x;
        double deltaZ = position.z() + 0.5 - z;
        return deltaX * deltaX + deltaZ * deltaZ;
    }

    record DropPosition(double x, double y, double z) {
    }
}
