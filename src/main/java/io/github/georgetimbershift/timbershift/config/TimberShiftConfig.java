package io.github.georgetimbershift.timbershift.config;

import java.util.Map;
import java.util.Locale;
import java.util.Set;

public record TimberShiftConfig(
        int configVersion,
        General general,
        Activation activation,
        Worlds worlds,
        Detection detection,
        Movement movement,
        FastDecay fastDecay,
        Effects effects,
        Map<String, String> messages
) {
    public record General(boolean enabled, boolean debug) {
    }

    public record Activation(boolean requireAxe, boolean requirePermission, boolean sneakBypasses) {
    }

    public record Worlds(WorldMode mode, Set<String> names) {
        public Worlds {
            names = names.stream()
                    .map(name -> name.toLowerCase(Locale.ROOT))
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        }

        public boolean allows(String worldName) {
            boolean listed = names.contains(worldName.toLowerCase(Locale.ROOT));
            return mode == WorldMode.WHITELIST ? listed : !listed;
        }
    }

    public record Detection(
            boolean requireNaturalLeaves,
            int minimumNaturalLeaves,
            boolean allowMixedLogFamilies,
            int maxBreakHeightAboveBase,
            int trustedTreeSeconds,
            Limits limits
    ) {
    }

    public record Limits(int maxLogs, int maxHeight, int maxHorizontalRadius, int maxScannedBlocks) {
    }

    public record Movement(int blocksPerChop, boolean abortOnUnloadedChunk) {
    }

    public record FastDecay(
            boolean enabled,
            int initialDelayTicks,
            int intervalTicks,
            int maxLeavesPerBatch,
            int maxLeavesPerTree,
            int maxRadius,
            int maxScannedBlocks,
            int maxActiveOperations,
            boolean preserveVanillaDrops,
            LeafEffects effects
    ) {
    }

    public record LeafEffects(boolean particles, boolean sounds) {
    }

    public record Effects(SoundEffect sound, ParticleEffect particles) {
    }

    public record SoundEffect(boolean enabled, String key, float volume, float pitch) {
    }

    public record ParticleEffect(boolean enabled, int count) {
    }

    public String message(String key) {
        return messages.getOrDefault(key, "");
    }
}
