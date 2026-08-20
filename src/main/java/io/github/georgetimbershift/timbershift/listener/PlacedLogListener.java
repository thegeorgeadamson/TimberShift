package io.github.georgetimbershift.timbershift.listener;

import io.github.georgetimbershift.timbershift.material.MaterialClassifier;
import io.github.georgetimbershift.timbershift.model.BlockPos;
import io.github.georgetimbershift.timbershift.service.PlacedLogTracker;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class PlacedLogListener implements Listener {
    private final JavaPlugin plugin;
    private final MaterialClassifier classifier;
    private final PlacedLogTracker tracker;

    public PlacedLogListener(
            JavaPlugin plugin,
            MaterialClassifier classifier,
            PlacedLogTracker tracker
    ) {
        this.plugin = plugin;
        this.classifier = classifier;
        this.tracker = tracker;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (classifier.logFamily(block.getType()) == null) {
            return;
        }
        UUID worldId = block.getWorld().getUID();
        BlockPos position = position(block);
        defer(event, () -> {
            World world = loadedWorld(worldId, position);
            if (world != null && classifier.logFamily(world.getBlockAt(position.x(), position.y(), position.z())
                    .getType()) != null) {
                tracker.mark(world, position);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBreak(BlockBreakEvent event) {
        forgetIfTrackedAfter(event, List.of(event.getBlock()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        forgetIfTrackedAfter(event, List.of(event.getBlock()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        forgetIfTrackedAfter(event, event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        forgetIfTrackedAfter(event, event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        moveTrackedAfter(event, event.getBlocks(), event.getDirection());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        moveTrackedAfter(event, event.getBlocks(), event.getDirection());
    }

    private void forgetIfTrackedAfter(Cancellable event, Collection<Block> blocks) {
        List<TrackedPosition> tracked = trackedPositions(blocks);
        if (tracked.isEmpty()) {
            return;
        }
        defer(event, () -> {
            for (TrackedPosition entry : tracked) {
                World world = loadedWorld(entry.worldId(), entry.position());
                if (world == null) {
                    continue;
                }
                if (classifier.logFamily(world.getBlockAt(entry.position().x(), entry.position().y(),
                        entry.position().z()).getType()) == null) {
                    tracker.forget(world, entry.position());
                }
            }
        });
    }

    private void moveTrackedAfter(Cancellable event, Collection<Block> blocks, BlockFace direction) {
        List<TrackedPosition> sources = trackedPositions(blocks);
        if (sources.isEmpty()) {
            return;
        }
        defer(event, () -> {
            for (TrackedPosition source : sources) {
                World world = loadedWorld(source.worldId(), source.position());
                if (world != null) {
                    tracker.forget(world, source.position());
                }
            }
            for (TrackedPosition source : sources) {
                BlockPos destination = source.position().offset(direction.getModX(), direction.getModY(),
                        direction.getModZ());
                World world = loadedWorld(source.worldId(), destination);
                if (world != null && classifier.logFamily(world.getBlockAt(destination.x(), destination.y(),
                        destination.z()).getType()) != null) {
                    tracker.mark(world, destination);
                }
            }
        });
    }

    private List<TrackedPosition> trackedPositions(Collection<Block> blocks) {
        List<TrackedPosition> result = new ArrayList<>();
        for (Block block : blocks) {
            if (classifier.logFamily(block.getType()) == null) {
                continue;
            }
            BlockPos position = position(block);
            if (tracker.isPlayerPlaced(block.getWorld(), position)) {
                result.add(new TrackedPosition(block.getWorld().getUID(), position));
            }
        }
        return result;
    }

    private void defer(Cancellable event, Runnable action) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!event.isCancelled()) {
                action.run();
            }
        });
    }

    private World loadedWorld(UUID worldId, BlockPos position) {
        World world = plugin.getServer().getWorld(worldId);
        return world != null && world.isChunkLoaded(position.chunkX(), position.chunkZ()) ? world : null;
    }

    private BlockPos position(Block block) {
        return new BlockPos(block.getX(), block.getY(), block.getZ());
    }

    private record TrackedPosition(UUID worldId, BlockPos position) {
    }
}
