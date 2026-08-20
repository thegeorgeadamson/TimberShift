package io.github.georgetimbershift.timbershift.leaf;

import io.github.georgetimbershift.timbershift.model.BlockPos;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;
import java.util.UUID;

final class LeafDecayOperation {
    private final UUID worldId;
    private final Queue<BlockPos> candidates;
    private final long readyAtTick;

    LeafDecayOperation(UUID worldId, Collection<BlockPos> candidates, long readyAtTick) {
        this.worldId = worldId;
        this.candidates = new ArrayDeque<>(candidates);
        this.readyAtTick = readyAtTick;
    }

    UUID worldId() {
        return worldId;
    }

    long readyAtTick() {
        return readyAtTick;
    }

    BlockPos poll() {
        return candidates.poll();
    }

    boolean isEmpty() {
        return candidates.isEmpty();
    }
}
