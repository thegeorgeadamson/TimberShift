package io.github.georgetimbershift.timbershift.tree;

import io.github.georgetimbershift.timbershift.material.MaterialClassifier;
import io.github.georgetimbershift.timbershift.model.BlockPos;
import io.github.georgetimbershift.timbershift.service.PlacedLogTracker;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class BukkitTreeWorld implements TreeWorld {
    private final World world;
    private final MaterialClassifier classifier;
    private final PlacedLogTracker placedLogs;
    private final boolean protectPlayerPlacedLogs;

    public BukkitTreeWorld(
            World world,
            MaterialClassifier classifier,
            PlacedLogTracker placedLogs,
            boolean protectPlayerPlacedLogs
    ) {
        this.world = world;
        this.classifier = classifier;
        this.placedLogs = placedLogs;
        this.protectPlayerPlacedLogs = protectPlayerPlacedLogs;
    }

    @Override
    public CellSnapshot read(BlockPos position) {
        if (position.y() < world.getMinHeight() || position.y() >= world.getMaxHeight()) {
            return CellSnapshot.obstruction();
        }
        if (!world.isChunkLoaded(position.chunkX(), position.chunkZ())) {
            return CellSnapshot.unloaded();
        }
        Block block = world.getBlockAt(position.x(), position.y(), position.z());
        if (protectPlayerPlacedLogs && classifier.logFamily(block.getType()) != null
                && placedLogs.isPlayerPlaced(world, position)) {
            return CellSnapshot.obstruction();
        }
        return classifier.classify(block);
    }
}
