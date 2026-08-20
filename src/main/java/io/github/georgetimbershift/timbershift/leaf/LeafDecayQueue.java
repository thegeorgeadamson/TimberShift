package io.github.georgetimbershift.timbershift.leaf;

import io.github.georgetimbershift.timbershift.model.BlockPos;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.UUID;

public final class LeafDecayQueue {
    private final Deque<LeafDecayOperation> operations = new ArrayDeque<>();

    public boolean canAccept(int maximumOperations) {
        return operations.size() < maximumOperations;
    }

    public boolean enqueue(
            boolean enabled,
            UUID worldId,
            Collection<BlockPos> candidates,
            long readyAtTick,
            int maximumOperations
    ) {
        if (!enabled || candidates.isEmpty() || !canAccept(maximumOperations)) {
            return false;
        }
        operations.addLast(new LeafDecayOperation(worldId, candidates, readyAtTick));
        return true;
    }

    LeafDecayOperation pollReady(long currentTick) {
        int attempts = operations.size();
        while (attempts-- > 0) {
            LeafDecayOperation operation = operations.removeFirst();
            if (operation.readyAtTick() <= currentTick) {
                return operation;
            }
            operations.addLast(operation);
        }
        return null;
    }

    void requeue(LeafDecayOperation operation) {
        if (!operation.isEmpty()) {
            operations.addLast(operation);
        }
    }

    public int size() {
        return operations.size();
    }

    public boolean isEmpty() {
        return operations.isEmpty();
    }

    public void clear() {
        operations.clear();
    }
}
