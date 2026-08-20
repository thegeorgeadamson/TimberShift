package io.github.georgetimbershift.timbershift.service;

import io.github.georgetimbershift.timbershift.config.ConfigurationManager;
import io.github.georgetimbershift.timbershift.config.TimberShiftConfig;
import io.github.georgetimbershift.timbershift.material.MaterialClassifier;
import io.github.georgetimbershift.timbershift.leaf.LeafDecayService;
import io.github.georgetimbershift.timbershift.model.BlockPos;
import io.github.georgetimbershift.timbershift.model.LogFamily;
import io.github.georgetimbershift.timbershift.tree.BukkitTreeWorld;
import io.github.georgetimbershift.timbershift.tree.DetectionStatus;
import io.github.georgetimbershift.timbershift.tree.ExecutionStatus;
import io.github.georgetimbershift.timbershift.tree.LogMove;
import io.github.georgetimbershift.timbershift.tree.TreeDetectionResult;
import io.github.georgetimbershift.timbershift.tree.TreeDetector;
import io.github.georgetimbershift.timbershift.tree.TreeShiftExecutor;
import io.github.georgetimbershift.timbershift.tree.TreeShiftPlan;
import io.github.georgetimbershift.timbershift.tree.TreeShiftPlanner;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class TreeShiftService {
    private final JavaPlugin plugin;
    private final ConfigurationManager configuration;
    private final MaterialClassifier classifier;
    private final TrustedTreeRegistry trustedTrees;
    private final PlacedLogTracker placedLogs;
    private final LeafDecayService leafDecay;
    private final TreeDetector detector;
    private final TreeShiftPlanner planner;
    private final TreeShiftExecutor executor;
    private final Set<BlockKey> pending = new HashSet<>();
    private final Set<BlockKey> locked = new HashSet<>();

    public TreeShiftService(
            JavaPlugin plugin,
            ConfigurationManager configuration,
            MaterialClassifier classifier,
            TrustedTreeRegistry trustedTrees,
            PlacedLogTracker placedLogs,
            LeafDecayService leafDecay
    ) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.classifier = classifier;
        this.trustedTrees = trustedTrees;
        this.placedLogs = placedLogs;
        this.leafDecay = leafDecay;
        this.detector = new TreeDetector();
        this.planner = new TreeShiftPlanner();
        this.executor = new TreeShiftExecutor(classifier, plugin.getLogger());
    }

    public boolean schedule(World world, BlockPos broken, LogFamily family, BlockBreakEvent observedEvent) {
        UUID worldId = world.getUID();
        BlockKey anchorKey = new BlockKey(worldId, broken);
        if (!pending.add(anchorKey)) {
            debug("Ignored duplicate pending break at " + broken + " in " + world.getName());
            return false;
        }
        UUID trustedSession = trustedTrees.lookup(worldId, broken, family);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            pending.remove(anchorKey);
            if (observedEvent.isCancelled()) {
                debug("Aborted shift at " + broken + ": another listener cancelled the observed break");
                return;
            }
            process(worldId, broken, family, trustedSession);
        });
        return true;
    }

    public void shutdown() {
        pending.clear();
        locked.clear();
        trustedTrees.clear();
    }

    private void process(UUID worldId, BlockPos broken, LogFamily family, UUID trustedSession) {
        World world = plugin.getServer().getWorld(worldId);
        if (world == null) {
            debug("Aborted shift because the world unloaded: " + worldId);
            return;
        }
        try {
            TimberShiftConfig config = configuration.current();
            if (!config.general().enabled()) {
                return;
            }
            if (!world.isChunkLoaded(broken.chunkX(), broken.chunkZ())) {
                debug("Aborted shift at " + broken + ": original chunk unloaded");
                return;
            }
            if (!world.getBlockAt(broken.x(), broken.y(), broken.z()).getType().isAir()) {
                debug("Aborted shift at " + broken + ": the permitted break did not leave an empty position");
                return;
            }

            BukkitTreeWorld view = new BukkitTreeWorld(world, classifier, placedLogs,
                    config.detection().protectPlayerPlacedLogs());
            boolean trustedContinuation = trustedSession != null;
            TreeDetectionResult detection = detector.detect(view, broken, family, config.detection(),
                    trustedContinuation, config.movement().abortOnUnloadedChunk());
            if (!detection.accepted()) {
                if (detection.status() == DetectionStatus.NO_CONNECTED_LOGS
                        && trustedContinuation) {
                    Set<BlockPos> knownTree = new HashSet<>(trustedTrees.positions(trustedSession));
                    knownTree.add(broken);
                    Set<BlockPos> leafCandidates = leafDecay.enqueueTreeChange(worldId, broken, knownTree,
                            Set.of(family), trustedTrees.leafCandidates(trustedSession));
                    trustedTrees.update(trustedSession, Set.of(broken), Set.of(), leafCandidates,
                            config.detection().trustedTreeSeconds());
                    leafDecay.rememberCanopy(trustedSession, worldId, leafCandidates,
                            config.detection().trustedTreeSeconds());
                }
                debug("Rejected structure at " + broken + ": " + detection.status()
                        + " (scanned=" + detection.scannedBlocks() + ", leaves="
                        + detection.naturalLeafCount() + ')');
                return;
            }
            if (trustedContinuation
                    && !trustedTrees.allBelongTo(worldId, detection.logs().keySet(), trustedSession)) {
                debug("Rejected trusted continuation at " + broken + ": connected logs changed or expired");
                return;
            }

            Set<BlockPos> leafScanPositions = new HashSet<>(detection.logs().keySet());
            leafScanPositions.add(broken);
            Set<LogFamily> detectedFamilies = new HashSet<>();
            detectedFamilies.add(family);
            detection.logs().values().forEach(snapshot -> detectedFamilies.add(snapshot.family()));
            Set<BlockPos> rememberedLeaves = trustedContinuation
                    ? trustedTrees.leafCandidates(trustedSession)
                    : Set.of();
            Set<BlockPos> leafCandidates = leafDecay.enqueueTreeChange(worldId, broken, leafScanPositions,
                    detectedFamilies, rememberedLeaves);

            TreeShiftPlan plan = planner.plan(detection.logs(), view, config.movement().blocksPerChop(),
                    config.movement().abortOnUnloadedChunk());
            if (!plan.ready()) {
                rememberBreakWithoutShift(worldId, trustedSession, broken, leafCandidates,
                        config.detection().trustedTreeSeconds());
                debug("No safe movement at " + broken + ": " + plan.status());
                return;
            }

            List<BlockKey> operationKeys = operationKeys(worldId, plan);
            if (!acquire(operationKeys)) {
                debug("Skipped overlapping shift at " + broken);
                return;
            }
            ExecutionStatus execution;
            try {
                execution = executor.execute(world, plan);
            } finally {
                release(operationKeys);
            }
            if (execution != ExecutionStatus.APPLIED) {
                rememberBreakWithoutShift(worldId, trustedSession, broken, leafCandidates,
                        config.detection().trustedTreeSeconds());
                debug("Shift at " + broken + " was not applied: " + execution);
                return;
            }

            rememberTransformation(worldId, family, broken, detection, plan, trustedSession, leafCandidates,
                    config.detection().trustedTreeSeconds());
            playEffects(world, broken, plan, config.effects());
            debug("Shifted " + plan.moves().size() + " of " + detection.logs().size() + " logs at "
                    + broken + " (scanned=" + detection.scannedBlocks() + ", leaves="
                    + detection.naturalLeafCount() + ')');
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE,
                    "Unexpected error while processing a tree break at " + broken + " in " + world.getName(),
                    exception);
        }
    }

    private void rememberTransformation(
            UUID worldId,
            LogFamily family,
            BlockPos broken,
            TreeDetectionResult detection,
            TreeShiftPlan plan,
            UUID existingSession,
            Collection<BlockPos> leafCandidates,
            int lifetimeSeconds
    ) {
        Set<BlockPos> removed = new HashSet<>();
        removed.add(broken);
        plan.moves().forEach(move -> removed.add(move.source()));
        Set<BlockPos> added = new HashSet<>();
        plan.moves().forEach(move -> added.add(move.destination()));

        if (existingSession != null) {
            trustedTrees.update(existingSession, removed, added, leafCandidates, lifetimeSeconds);
            leafDecay.rememberCanopy(existingSession, worldId, leafCandidates, lifetimeSeconds);
            return;
        }

        Set<BlockPos> transformedTree = new HashSet<>(detection.logs().keySet());
        transformedTree.removeAll(removed);
        transformedTree.addAll(added);
        UUID newSession = trustedTrees.register(worldId, family, transformedTree, leafCandidates,
                lifetimeSeconds);
        leafDecay.rememberCanopy(newSession, worldId, leafCandidates, lifetimeSeconds);
    }

    private void rememberBreakWithoutShift(
            UUID worldId,
            UUID existingSession,
            BlockPos broken,
            Collection<BlockPos> leafCandidates,
            int lifetimeSeconds
    ) {
        if (existingSession != null) {
            trustedTrees.update(existingSession, Set.of(broken), Set.of(), leafCandidates, lifetimeSeconds);
            leafDecay.rememberCanopy(existingSession, worldId, leafCandidates, lifetimeSeconds);
        }
    }

    private void playEffects(
            World world,
            BlockPos broken,
            TreeShiftPlan plan,
            TimberShiftConfig.Effects effects
    ) {
        Location location = new Location(world, broken.x() + 0.5, broken.y() + 0.5, broken.z() + 0.5);
        if (effects.sound().enabled()) {
            NamespacedKey key = NamespacedKey.fromString(effects.sound().key());
            Sound sound = key == null ? null : Registry.SOUNDS.get(key);
            if (sound != null) {
                world.playSound(location, sound, SoundCategory.BLOCKS,
                        effects.sound().volume(), effects.sound().pitch());
            }
        }
        if (effects.particles().enabled() && effects.particles().count() > 0) {
            LogMove sample = plan.moves().getFirst();
            BlockData data = world.getBlockAt(sample.destination().x(), sample.destination().y(),
                    sample.destination().z()).getBlockData();
            world.spawnParticle(Particle.BLOCK, location, effects.particles().count(),
                    0.22, 0.22, 0.22, 0.01, data);
        }
    }

    private List<BlockKey> operationKeys(UUID worldId, TreeShiftPlan plan) {
        Collection<BlockKey> keys = new ArrayList<>(plan.moves().size() * 2);
        for (LogMove move : plan.moves()) {
            keys.add(new BlockKey(worldId, move.source()));
            keys.add(new BlockKey(worldId, move.destination()));
        }
        return List.copyOf(keys);
    }

    private boolean acquire(Collection<BlockKey> keys) {
        if (keys.stream().anyMatch(locked::contains)) {
            return false;
        }
        locked.addAll(keys);
        return true;
    }

    private void release(Collection<BlockKey> keys) {
        locked.removeAll(keys);
    }

    private void debug(String message) {
        if (configuration.current().general().debug()) {
            plugin.getLogger().info("[debug] " + message);
        }
    }
}
