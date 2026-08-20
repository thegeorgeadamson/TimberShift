package io.github.georgetimbershift.timbershift.leaf;

import io.github.georgetimbershift.timbershift.model.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

    List<BlockPos> pollUpTo(int maximum) {
        List<BlockPos> result = new ArrayList<>(Math.min(maximum, candidates.size()));
        while (result.size() < maximum) {
            BlockPos candidate = candidates.poll();
            if (candidate == null) {
                break;
            }
            result.add(candidate);
        }
        return result;
    }

    boolean isEmpty() {
        return candidates.isEmpty();
    }
}
