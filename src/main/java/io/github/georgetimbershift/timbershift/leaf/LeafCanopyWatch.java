package io.github.georgetimbershift.timbershift.leaf;

import io.github.georgetimbershift.timbershift.model.BlockPos;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

final class LeafCanopyWatch {
    private static final int[][] CARDINAL_6 = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    private final UUID worldId;
    private final Set<BlockPos> candidates;
    private long expiresAtMillis;
    private long nextWakeAtTick;

    LeafCanopyWatch(UUID worldId, Collection<BlockPos> candidates, long expiresAtMillis) {
        this.worldId = worldId;
        this.candidates = new HashSet<>(candidates);
        this.expiresAtMillis = expiresAtMillis;
    }

    UUID worldId() {
        return worldId;
    }

    boolean isExpired(long nowMillis) {
        return expiresAtMillis <= nowMillis;
    }

    void refresh(Collection<BlockPos> replacement, long newExpiryMillis) {
        candidates.clear();
        candidates.addAll(replacement);
        expiresAtMillis = newExpiryMillis;
    }

    boolean isTriggeredBy(BlockPos removedLeaf) {
        if (candidates.contains(removedLeaf)) {
            return true;
        }
        for (int[] direction : CARDINAL_6) {
            if (candidates.contains(removedLeaf.offset(direction[0], direction[1], direction[2]))) {
                return true;
            }
        }
        return false;
    }

    boolean mayWake(long currentTick) {
        return currentTick >= nextWakeAtTick;
    }

    void deferWakeUntil(long tick) {
        nextWakeAtTick = tick;
    }

    void forget(BlockPos position) {
        candidates.remove(position);
    }

    boolean isEmpty() {
        return candidates.isEmpty();
    }

    Set<BlockPos> candidates() {
        return Set.copyOf(candidates);
    }
}
