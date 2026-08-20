package io.github.georgetimbershift.timbershift.leaf;

import io.github.georgetimbershift.timbershift.config.ConfigurationManager;
import io.github.georgetimbershift.timbershift.config.TimberShiftConfig;
import io.github.georgetimbershift.timbershift.material.MaterialClassifier;
import io.github.georgetimbershift.timbershift.model.BlockPos;
import io.github.georgetimbershift.timbershift.model.LogFamily;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class LeafDecayService {
    private static final NamespacedKey DECAY_SOUND_KEY = NamespacedKey.minecraft("block.grass.break");

    private final JavaPlugin plugin;
    private final ConfigurationManager configuration;
    private final MaterialClassifier classifier;
    private final LeafCandidateScanner scanner = new LeafCandidateScanner();
    private final LeafSupportChecker supportChecker = new LeafSupportChecker();
    private final LeafDecayQueue queue = new LeafDecayQueue();
    private BukkitTask task;
    private long serviceTick;

    public LeafDecayService(
            JavaPlugin plugin,
            ConfigurationManager configuration,
            MaterialClassifier classifier
    ) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.classifier = classifier;
    }

    public Set<BlockPos> enqueueTreeChange(
            UUID worldId,
            BlockPos origin,
            Collection<BlockPos> treePositions,
            Set<LogFamily> families,
            Collection<BlockPos> rememberedCandidates
    ) {
        TimberShiftConfig.FastDecay config = configuration.current().fastDecay();
        if (!config.enabled() || treePositions.isEmpty() || families.isEmpty()) {
            return Set.of();
        }
        World world = plugin.getServer().getWorld(worldId);
        if (world == null || !world.isChunkLoaded(origin.chunkX(), origin.chunkZ())) {
            return boundedRememberedCandidates(rememberedCandidates, config.maxLeavesPerTree());
        }

        LeafScanResult result = scanner.collect(new BukkitLeafWorld(world, classifier),
                treePositions, families, config);
        if (result.status() != LeafScanStatus.NONE && !result.collected()) {
            debug("Leaf scan at " + origin + ": " + result.status()
                    + " (scanned=" + result.scannedBlocks() + ')');
        }

        Set<BlockPos> candidates = combineCandidates(rememberedCandidates, result.candidates(),
                config.maxLeavesPerTree());
        if (candidates.isEmpty()) {
            return Set.of();
        }

        List<BlockPos> orderedCandidates = candidates.stream().sorted(BlockPos.Y_X_Z_ORDER).toList();
        if (queue.enqueue(true, worldId, orderedCandidates, serviceTick + config.initialDelayTicks(),
                config.maxActiveOperations())) {
            ensureTask();
            debug("Queued " + candidates.size() + " leaves at " + origin
                    + " (scanned=" + result.scannedBlocks() + ')');
        } else {
            debug("Remembered leaf candidates at " + origin + ", but skipped this queue pass: "
                    + "active operation limit reached");
        }
        return candidates;
    }

    public void onConfigurationReload(TimberShiftConfig ignored) {
        // Pending work was collected under the old bounds; clearing is the safest atomic transition.
        clearAndCancel();
    }

    public void shutdown() {
        clearAndCancel();
    }

    public int pendingOperations() {
        return queue.size();
    }

    private void ensureTask() {
        if (task == null) {
            task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        }
    }

    private void tick() {
        serviceTick++;
        TimberShiftConfig.FastDecay config = configuration.current().fastDecay();
        if (!config.enabled()) {
            clearAndCancel();
            return;
        }
        if (serviceTick % config.intervalTicks() != 0) {
            return;
        }

        int remaining = config.maxLeavesPerBatch();
        while (remaining-- > 0) {
            LeafDecayOperation operation = queue.pollReady(serviceTick);
            if (operation == null) {
                break;
            }
            BlockPos candidate = operation.poll();
            if (candidate != null) {
                processCandidate(operation.worldId(), candidate, config);
            }
            queue.requeue(operation);
        }
        if (queue.isEmpty()) {
            clearAndCancel();
        }
    }

    private void processCandidate(UUID worldId, BlockPos position, TimberShiftConfig.FastDecay config) {
        World world = plugin.getServer().getWorld(worldId);
        if (world == null || !world.isChunkLoaded(position.chunkX(), position.chunkZ())) {
            return;
        }
        BukkitLeafWorld view = new BukkitLeafWorld(world, classifier);
        if (supportChecker.evaluate(view, position) != LeafEligibilityStatus.ELIGIBLE) {
            return;
        }

        Block block = world.getBlockAt(position.x(), position.y(), position.z());
        BlockData currentData = block.getBlockData();
        if (!(currentData instanceof Leaves leaves) || leaves.isPersistent()) {
            return;
        }

        LeavesDecayEvent event = new LeavesDecayEvent(block);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled() || supportChecker.evaluate(view, position) != LeafEligibilityStatus.ELIGIBLE) {
            return;
        }

        BlockData latestData = block.getBlockData();
        if (!(latestData instanceof Leaves latestLeaves) || latestLeaves.isPersistent()) {
            return;
        }
        BlockData effectData = latestData.clone();
        boolean removed;
        if (config.preserveVanillaDrops()) {
            removed = block.breakNaturally();
        } else {
            block.setType(latestLeaves.isWaterlogged() ? Material.WATER : Material.AIR, true);
            removed = true;
        }
        if (removed) {
            playEffects(world, position, effectData, config.effects());
        }
    }

    private void playEffects(
            World world,
            BlockPos position,
            BlockData data,
            TimberShiftConfig.LeafEffects effects
    ) {
        Location location = new Location(world, position.x() + 0.5, position.y() + 0.5, position.z() + 0.5);
        if (effects.particles()) {
            world.spawnParticle(Particle.BLOCK, location, 2, 0.18, 0.18, 0.18, 0.01, data);
        }
        if (effects.sounds()) {
            Sound sound = Registry.SOUNDS.get(DECAY_SOUND_KEY);
            if (sound != null) {
                world.playSound(location, sound, SoundCategory.BLOCKS, 0.15F, 1.15F);
            }
        }
    }

    private void clearAndCancel() {
        queue.clear();
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private Set<BlockPos> combineCandidates(
            Collection<BlockPos> remembered,
            Collection<BlockPos> discovered,
            int maximum
    ) {
        Set<BlockPos> boundedRemembered = boundedRememberedCandidates(remembered, maximum);
        Set<BlockPos> combined = new HashSet<>(boundedRemembered);
        combined.addAll(discovered);
        if (combined.size() <= maximum) {
            return Set.copyOf(combined);
        }
        // Keep the previously bounded canopy rather than using a newly found partial expansion.
        debug("Candidate union exceeded the per-tree leaf limit; retained the prior bounded canopy");
        return boundedRemembered;
    }

    private Set<BlockPos> boundedRememberedCandidates(Collection<BlockPos> candidates, int maximum) {
        if (candidates.size() > maximum) {
            debug("Discarded remembered canopy because it exceeds the current per-tree leaf limit");
            return Set.of();
        }
        return Set.copyOf(candidates);
    }

    private void debug(String message) {
        if (configuration.current().general().debug()) {
            plugin.getLogger().info("[debug] " + message);
        }
    }
}
