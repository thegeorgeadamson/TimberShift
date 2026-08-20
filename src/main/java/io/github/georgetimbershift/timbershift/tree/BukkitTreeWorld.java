package io.github.georgetimbershift.timbershift.tree;

import io.github.georgetimbershift.timbershift.material.MaterialClassifier;
import io.github.georgetimbershift.timbershift.model.BlockPos;
import org.bukkit.World;

public final class BukkitTreeWorld implements TreeWorld {
    private final World world;
    private final MaterialClassifier classifier;

    public BukkitTreeWorld(World world, MaterialClassifier classifier) {
        this.world = world;
        this.classifier = classifier;
    }

    @Override
    public CellSnapshot read(BlockPos position) {
        if (position.y() < world.getMinHeight() || position.y() >= world.getMaxHeight()) {
            return CellSnapshot.obstruction();
        }
        if (!world.isChunkLoaded(position.chunkX(), position.chunkZ())) {
            return CellSnapshot.unloaded();
        }
        return classifier.classify(world.getBlockAt(position.x(), position.y(), position.z()));
    }
}
