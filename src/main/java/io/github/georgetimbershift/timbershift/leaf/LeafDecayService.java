package io.github.georgetimbershift.timbershift.leaf;

import io.github.georgetimbershift.timbershift.config.ConfigurationManager;
import io.github.georgetimbershift.timbershift.config.TimberShiftConfig;
import io.github.georgetimbershift.timbershift.material.MaterialClassifier;
import io.github.georgetimbershift.timbershift.model.BlockPos;
import io.github.georgetimbershift.timbershift.model.LogFamily;
import io.github.georgetimbershift.timbershift.service.BlockKey;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class LeafDecayService {
    private static final NamespacedKey DECAY_SOUND_KEY = NamespacedKey.minecraft("block.grass.break");

    private final JavaPlugin plugin;
    private final ConfigurationManager configuration;
    private final MaterialClassifier classifier;
    private final LeafCandidateScanner scanner = new LeafCandidateScanner();
    private final LeafSupportChecker supportChecker = new LeafSupportChecker();
    private final LeafDecayQueue queue = new LeafDecayQueue();
    private final Map<UUID, LeafCanopyWatch> canopyWatches = new LinkedHashMap<>();
    private final Set<BlockKey> activeDecayEvents = new HashSet<>();
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

        List<BlockPos> shuffledCandidates = shuffled(candidates);
        if (queue.enqueue(true, worldId, shuffledCandidates, serviceTick + config.initialDelayTicks(),
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

    public void rememberCanopy(
            UUID sessionId,
            UUID worldId,
            Collection<BlockPos> candidates,
            int lifetimeSeconds
    ) {
        TimberShiftConfig.FastDecay config = configuration.current().fastDecay();
        pruneCanopyWatches();
        if (!config.enabled() || candidates.isEmpty() || candidates.size() > config.maxLeavesPerTree()) {
            canopyWatches.remove(sessionId);
            return;
        }
        long expiresAt = System.currentTimeMillis() + lifetimeSeconds * 1000L;
        LeafCanopyWatch existing = canopyWatches.get(sessionId);
        if (existing != null) {
            existing.refresh(candidates, expiresAt);
            return;
        }
        while (canopyWatches.size() >= config.maxActiveOperations()) {
            Iterator<UUID> iterator = canopyWatches.keySet().iterator();
            if (!iterator.hasNext()) {
                break;
            }
            iterator.next();
            iterator.remove();
        }
        canopyWatches.put(sessionId, new LeafCanopyWatch(worldId, candidates, expiresAt));
    }

    public void onNaturalLeafRemoved(UUID worldId, BlockPos position) {
        TimberShiftConfig config = configuration.current();
        if (!config.general().enabled() || !config.fastDecay().enabled()) {
            return;
        }
        World world = plugin.getServer().getWorld(worldId);
        if (world == null || !config.worlds().allows(world.getName())) {
            return;
        }

        pruneCanopyWatches();
        for (LeafCanopyWatch watch : canopyWatches.values()) {
            if (!watch.worldId().equals(worldId) || !watch.isTriggeredBy(position)) {
                continue;
            }
            watch.forget(position);
            if (watch.isEmpty() || !watch.mayWake(serviceTick)) {
                continue;
            }
            TimberShiftConfig.FastDecay decay = config.fastDecay();
            long readyAt = serviceTick + decay.initialDelayTicks();
            if (queue.enqueue(true, worldId, shuffled(watch.candidates()), readyAt,
                    decay.maxActiveOperations())) {
                watch.deferWakeUntil(readyAt + decay.intervalTicks());
                ensureTask();
                debug("Requeued a remembered canopy after nearby natural leaf removal at " + position);
            }
        }
        canopyWatches.values().removeIf(LeafCanopyWatch::isEmpty);
    }

    public boolean isAcceleratedDecayEvent(UUID worldId, BlockPos position) {
        return activeDecayEvents.contains(new BlockKey(worldId, position));
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
        for (LeafDecayOperation operation : queue.pollReadyRound(serviceTick)) {
            if (remaining <= 0) {
                queue.requeue(operation);
                continue;
            }
            int operationRemaining = Math.min(config.leavesPerStep(), remaining);
            for (BlockPos candidate : operation.pollUpTo(operationRemaining)) {
                processCandidate(operation.worldId(), candidate, config);
                remaining--;
            }
            queue.requeue(operation);
        }
        if (queue.isEmpty()) {
            cancelTask();
        }
    }

    private void processCandidate(UUID worldId, BlockPos position, TimberShiftConfig.FastDecay config) {
        World world = plugin.getServer().getWorld(worldId);
        if (world == null || !world.isChunkLoaded(position.chunkX(), position.chunkZ())) {
            return;
        }
        BukkitLeafWorld view = new BukkitLeafWorld(world, classifier);
        LeafEligibilityStatus eligibility = supportChecker.evaluate(view, position);
        if (eligibility != LeafEligibilityStatus.ELIGIBLE) {
            if (eligibility == LeafEligibilityStatus.PERSISTENT
                    || eligibility == LeafEligibilityStatus.NOT_A_LEAF) {
                forgetWatchedCandidate(worldId, position);
            }
            return;
        }

        Block block = world.getBlockAt(position.x(), position.y(), position.z());
        BlockData currentData = block.getBlockData();
        if (!(currentData instanceof Leaves leaves) || leaves.isPersistent()) {
            return;
        }

        LeavesDecayEvent event = new LeavesDecayEvent(block);
        BlockKey eventKey = new BlockKey(worldId, position);
        activeDecayEvents.add(eventKey);
        try {
            plugin.getServer().getPluginManager().callEvent(event);
        } finally {
            activeDecayEvents.remove(eventKey);
        }
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
            forgetWatchedCandidate(worldId, position);
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
        canopyWatches.clear();
        activeDecayEvents.clear();
        cancelTask();
    }

    private void cancelTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private List<BlockPos> shuffled(Collection<BlockPos> candidates) {
        List<BlockPos> result = new ArrayList<>(candidates);
        Collections.shuffle(result, ThreadLocalRandom.current());
        return result;
    }

    private void pruneCanopyWatches() {
        long now = System.currentTimeMillis();
        canopyWatches.values().removeIf(watch -> watch.isExpired(now) || watch.isEmpty());
    }

    private void forgetWatchedCandidate(UUID worldId, BlockPos position) {
        for (LeafCanopyWatch watch : canopyWatches.values()) {
            if (watch.worldId().equals(worldId)) {
                watch.forget(position);
            }
        }
        canopyWatches.values().removeIf(LeafCanopyWatch::isEmpty);
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
