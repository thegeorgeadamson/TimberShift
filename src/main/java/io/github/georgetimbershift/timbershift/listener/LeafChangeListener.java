package io.github.georgetimbershift.timbershift.listener;

import io.github.georgetimbershift.timbershift.leaf.LeafDecayService;
import io.github.georgetimbershift.timbershift.material.MaterialClassifier;
import io.github.georgetimbershift.timbershift.model.BlockPos;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/** Wakes only canopies previously associated with a verified TimberShift tree. */
public final class LeafChangeListener implements Listener {
    private final JavaPlugin plugin;
    private final MaterialClassifier classifier;
    private final LeafDecayService decayService;

    public LeafChangeListener(
            JavaPlugin plugin,
            MaterialClassifier classifier,
            LeafDecayService decayService
    ) {
        this.plugin = plugin;
        this.classifier = classifier;
        this.decayService = decayService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNaturalLeafBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!isNaturalLeaf(block)) {
            return;
        }
        deferRemovalCheck(event, block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNaturalLeafDecay(LeavesDecayEvent event) {
        Block block = event.getBlock();
        BlockPos position = position(block);
        if (!isNaturalLeaf(block)
                || decayService.isAcceleratedDecayEvent(block.getWorld().getUID(), position)) {
            return;
        }
        deferRemovalCheck(event, block);
    }

    private void deferRemovalCheck(org.bukkit.event.Cancellable observedEvent, Block block) {
        UUID worldId = block.getWorld().getUID();
        BlockPos position = position(block);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (observedEvent.isCancelled()) {
                return;
            }
            World world = plugin.getServer().getWorld(worldId);
            if (world == null || !world.isChunkLoaded(position.chunkX(), position.chunkZ())) {
                return;
            }
            Block current = world.getBlockAt(position.x(), position.y(), position.z());
            if (isNaturalLeaf(current)) {
                return;
            }
            decayService.onNaturalLeafRemoved(worldId, position);
        });
    }

    private boolean isNaturalLeaf(Block block) {
        return classifier.leafFamily(block.getType()) != null
                && block.getBlockData() instanceof Leaves leaves
                && !leaves.isPersistent();
    }

    private BlockPos position(Block block) {
        return new BlockPos(block.getX(), block.getY(), block.getZ());
    }
}
