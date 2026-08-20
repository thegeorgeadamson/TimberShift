package io.github.georgetimbershift.timbershift.tree;

import io.github.georgetimbershift.timbershift.material.MaterialClassifier;
import io.github.georgetimbershift.timbershift.model.BlockPos;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class TreeShiftExecutor {
    private final MaterialClassifier classifier;
    private final Logger logger;

    public TreeShiftExecutor(MaterialClassifier classifier, Logger logger) {
        this.classifier = classifier;
        this.logger = logger;
    }

    public ExecutionStatus execute(World world, TreeShiftPlan plan) {
        if (!plan.ready()) {
            return ExecutionStatus.WORLD_CHANGED;
        }

        Set<BlockPos> sources = new HashSet<>();
        Set<BlockPos> destinations = new HashSet<>();
        Map<BlockPos, BlockData> sourceData = new HashMap<>();
        for (LogMove move : plan.moves()) {
            if (!sources.add(move.source()) || !destinations.add(move.destination())) {
                return ExecutionStatus.WORLD_CHANGED;
            }
        }

        for (LogMove move : plan.moves()) {
            if (!isLoaded(world, move.source()) || !isLoaded(world, move.destination())) {
                return ExecutionStatus.UNLOADED_CHUNK;
            }
            Block sourceBlock = block(world, move.source());
            if (classifier.logFamily(sourceBlock.getType()) == null
                    || !sourceBlock.getBlockData().getAsString().equals(move.originalState())) {
                return ExecutionStatus.WORLD_CHANGED;
            }
            sourceData.put(move.source(), sourceBlock.getBlockData().clone());
        }

        for (LogMove move : plan.moves()) {
            int distance = move.source().y() - move.destination().y();
            for (int step = 1; step <= distance; step++) {
                BlockPos path = move.source().below(step);
                if (!isLoaded(world, path)) {
                    return ExecutionStatus.UNLOADED_CHUNK;
                }
                if (!sources.contains(path) && !block(world, path).getType().isAir()) {
                    return ExecutionStatus.WORLD_CHANGED;
                }
            }
        }

        Set<BlockPos> affected = new LinkedHashSet<>(sources);
        affected.addAll(destinations);
        Map<BlockPos, BlockData> originals = new HashMap<>();
        affected.forEach(position -> originals.put(position, block(world, position).getBlockData().clone()));

        try {
            sources.stream().sorted(BlockPos.Y_X_Z_ORDER)
                    .forEach(position -> block(world, position).setType(Material.AIR, false));
            plan.moves().stream()
                    .sorted((left, right) -> BlockPos.Y_X_Z_ORDER.compare(left.destination(), right.destination()))
                    .forEach(move -> block(world, move.destination())
                            .setBlockData(sourceData.get(move.source()).clone(), false));
        } catch (RuntimeException failure) {
            boolean restored = rollback(world, originals);
            logger.log(Level.SEVERE, "A tree shift failed while applying blocks; rollback "
                    + (restored ? "completed." : "was incomplete."), failure);
            return restored ? ExecutionStatus.FAILED_ROLLED_BACK : ExecutionStatus.FAILED_ROLLBACK_INCOMPLETE;
        }

        // The transaction itself suppresses physics to prevent half-applied neighbor updates. Once all
        // data is in place, re-applying final states with physics lets vanilla leaves and attachments react.
        try {
            affected.stream().sorted(BlockPos.Y_X_Z_ORDER).forEach(position -> {
                Block changed = block(world, position);
                changed.setBlockData(changed.getBlockData(), true);
            });
        } catch (RuntimeException physicsFailure) {
            logger.log(Level.WARNING, "Tree logs shifted, but a follow-up physics update failed.", physicsFailure);
        }
        return ExecutionStatus.APPLIED;
    }

    private boolean rollback(World world, Map<BlockPos, BlockData> originals) {
        boolean restored = true;
        List<Map.Entry<BlockPos, BlockData>> entries = new ArrayList<>(originals.entrySet());
        entries.sort(Map.Entry.comparingByKey(BlockPos.Y_X_Z_ORDER));
        for (Map.Entry<BlockPos, BlockData> entry : entries) {
            try {
                if (!isLoaded(world, entry.getKey())) {
                    restored = false;
                    continue;
                }
                block(world, entry.getKey()).setBlockData(entry.getValue().clone(), false);
            } catch (RuntimeException rollbackFailure) {
                restored = false;
                logger.log(Level.SEVERE, "Could not restore block at " + entry.getKey(), rollbackFailure);
            }
        }
        return restored;
    }

    private boolean isLoaded(World world, BlockPos position) {
        return position.y() >= world.getMinHeight()
                && position.y() < world.getMaxHeight()
                && world.isChunkLoaded(position.chunkX(), position.chunkZ());
    }

    private Block block(World world, BlockPos position) {
        return world.getBlockAt(position.x(), position.y(), position.z());
    }
}
