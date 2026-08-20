package io.github.georgetimbershift.timbershift.leaf;

import io.github.georgetimbershift.timbershift.material.MaterialClassifier;
import io.github.georgetimbershift.timbershift.model.BlockPos;
import io.github.georgetimbershift.timbershift.model.LogFamily;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;

public final class BukkitLeafWorld implements LeafWorld {
    private final World world;
    private final MaterialClassifier classifier;

    public BukkitLeafWorld(World world, MaterialClassifier classifier) {
        this.world = world;
        this.classifier = classifier;
    }

    @Override
    public LeafCellSnapshot read(BlockPos position) {
        if (position.y() < world.getMinHeight() || position.y() >= world.getMaxHeight()) {
            return LeafCellSnapshot.other();
        }
        if (!world.isChunkLoaded(position.chunkX(), position.chunkZ())) {
            return LeafCellSnapshot.unloaded();
        }

        Block block = world.getBlockAt(position.x(), position.y(), position.z());
        BlockData data = block.getBlockData();
        if (data instanceof Leaves leaves) {
            LogFamily family = classifier.leafFamily(block.getType());
            return leaves.isPersistent()
                    ? LeafCellSnapshot.persistentLeaf(family, leaves.getDistance())
                    : LeafCellSnapshot.naturalLeaf(family, leaves.getDistance());
        }
        if (Tag.PREVENTS_NEARBY_LEAF_DECAY.isTagged(block.getType())) {
            return LeafCellSnapshot.support();
        }
        return LeafCellSnapshot.other();
    }
}
