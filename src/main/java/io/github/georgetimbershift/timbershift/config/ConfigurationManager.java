package io.github.georgetimbershift.timbershift.config;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class ConfigurationManager {
    public static final int CURRENT_CONFIG_VERSION = 2;

    private final JavaPlugin plugin;
    private final AtomicReference<TimberShiftConfig> active = new AtomicReference<>();
    private final List<Consumer<TimberShiftConfig>> reloadListeners = new ArrayList<>();

    public ConfigurationManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadInitial() {
        plugin.saveDefaultConfig();
        try {
            active.set(parse(loadFromDisk()));
        } catch (IOException | InvalidConfigurationException exception) {
            throw new IllegalStateException("Could not load TimberShift configuration", exception);
        }
    }

    public boolean reload() {
        try {
            TimberShiftConfig replacement = parse(loadFromDisk());
            active.set(replacement);
            notifyReloadListeners(replacement);
            return true;
        } catch (IOException | InvalidConfigurationException | RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE,
                    "Configuration reload failed; the previous configuration remains active.", exception);
            return false;
        }
    }

    public TimberShiftConfig current() {
        TimberShiftConfig config = active.get();
        if (config == null) {
            throw new IllegalStateException("Configuration has not been loaded");
        }
        return config;
    }

    public void addReloadListener(Consumer<TimberShiftConfig> listener) {
        reloadListeners.add(Objects.requireNonNull(listener, "listener"));
    }

    private TimberShiftConfig parse(FileConfiguration yaml) {
        int version = positiveInt(yaml, "config-version", CURRENT_CONFIG_VERSION, 1, Integer.MAX_VALUE);
        if (version != CURRENT_CONFIG_VERSION) {
            plugin.getLogger().warning("config-version is " + version + ", but this release expects "
                    + CURRENT_CONFIG_VERSION + ". Known settings will still be loaded.");
        }

        TimberShiftConfig.General general = new TimberShiftConfig.General(
                booleanValue(yaml, "general.enabled", true),
                booleanValue(yaml, "general.debug", false));
        TimberShiftConfig.Activation activation = new TimberShiftConfig.Activation(
                booleanValue(yaml, "activation.require-axe", true),
                booleanValue(yaml, "activation.require-permission", true),
                booleanValue(yaml, "activation.sneak-bypasses", true));

        WorldMode worldMode = enumValue(yaml.getString("worlds.mode"), WorldMode.BLACKLIST,
                WorldMode.class, "worlds.mode");
        Set<String> worldNames = new HashSet<>();
        if (yaml.contains("worlds.list") && !yaml.isList("worlds.list")) {
            warn("worlds.list", String.valueOf(yaml.get("worlds.list")), "[]");
        }
        for (String name : yaml.getStringList("worlds.list")) {
            String normalized = name.strip().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                worldNames.add(normalized);
            }
        }
        TimberShiftConfig.Worlds worlds = new TimberShiftConfig.Worlds(worldMode, worldNames);

        TimberShiftConfig.Limits limits = new TimberShiftConfig.Limits(
                positiveInt(yaml, "tree-detection.limits.max-logs", 256, 2, 4096),
                positiveInt(yaml, "tree-detection.limits.max-height", 48, 2, 384),
                positiveInt(yaml, "tree-detection.limits.max-horizontal-radius", 12, 1, 64),
                positiveInt(yaml, "tree-detection.limits.max-scanned-blocks", 6000, 100, 100_000));
        TimberShiftConfig.Detection detection = new TimberShiftConfig.Detection(
                booleanValue(yaml, "tree-detection.require-natural-leaves", true),
                positiveInt(yaml, "tree-detection.minimum-natural-leaves", 4, 0, 512),
                booleanValue(yaml, "tree-detection.allow-mixed-log-families", false),
                positiveInt(yaml, "tree-detection.max-break-height-above-base", 1, 0, 8),
                positiveInt(yaml, "tree-detection.trusted-tree-seconds", 180, 10, 3600),
                limits);
        TimberShiftConfig.Movement movement = new TimberShiftConfig.Movement(
                positiveInt(yaml, "movement.blocks-per-chop", 1, 1, 4),
                booleanValue(yaml, "movement.abort-on-unloaded-chunk", true));

        TimberShiftConfig.FastDecay fastDecay = new TimberShiftConfig.FastDecay(
                booleanValue(yaml, "leaves.fast-decay.enabled", true),
                positiveInt(yaml, "leaves.fast-decay.initial-delay-ticks", 10, 0, 1200),
                positiveInt(yaml, "leaves.fast-decay.interval-ticks", 2, 1, 200),
                positiveInt(yaml, "leaves.fast-decay.max-leaves-per-batch", 32, 1, 256),
                positiveInt(yaml, "leaves.fast-decay.max-leaves-per-tree", 512, 16, 4096),
                positiveInt(yaml, "leaves.fast-decay.max-radius", 12, 2, 32),
                positiveInt(yaml, "leaves.fast-decay.max-scanned-blocks", 2048, 100, 20_000),
                positiveInt(yaml, "leaves.fast-decay.max-active-operations", 32, 1, 128),
                booleanValue(yaml, "leaves.fast-decay.preserve-vanilla-drops", true),
                new TimberShiftConfig.LeafEffects(
                        booleanValue(yaml, "leaves.fast-decay.effects.particles", true),
                        booleanValue(yaml, "leaves.fast-decay.effects.sounds", false)));

        String soundKey = yaml.getString("effects.sound.key", "minecraft:block.wood.place");
        soundKey = soundKey == null ? "minecraft:block.wood.place" : soundKey.strip().toLowerCase(Locale.ROOT);
        NamespacedKey parsedSoundKey = NamespacedKey.fromString(soundKey);
        if (parsedSoundKey == null || Registry.SOUNDS.get(parsedSoundKey) == null) {
            warn("effects.sound.key", soundKey, "minecraft:block.wood.place");
            soundKey = "minecraft:block.wood.place";
        }
        TimberShiftConfig.SoundEffect sound = new TimberShiftConfig.SoundEffect(
                booleanValue(yaml, "effects.sound.enabled", true),
                soundKey,
                (float) decimal(yaml, "effects.sound.volume", 0.35, 0.0, 4.0),
                (float) decimal(yaml, "effects.sound.pitch", 0.85, 0.5, 2.0));
        TimberShiftConfig.ParticleEffect particles = new TimberShiftConfig.ParticleEffect(
                booleanValue(yaml, "effects.particles.enabled", true),
                positiveInt(yaml, "effects.particles.count", 4, 0, 32));

        Map<String, String> messages = new HashMap<>();
        List.of("prefix", "no-permission", "reloaded", "toggled-on", "toggled-off", "player-only")
                .forEach(key -> messages.put(key,
                        Objects.requireNonNullElse(yaml.getString("messages." + key, ""), "")));

        return new TimberShiftConfig(version, general, activation, worlds, detection, movement, fastDecay,
                new TimberShiftConfig.Effects(sound, particles), Map.copyOf(messages));
    }

    private void notifyReloadListeners(TimberShiftConfig replacement) {
        for (Consumer<TimberShiftConfig> listener : reloadListeners) {
            try {
                listener.accept(replacement);
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.WARNING,
                        "A configuration reload listener failed after the new configuration was activated.",
                        exception);
            }
        }
    }

    private int positiveInt(FileConfiguration yaml, String path, int fallback, int minimum, int maximum) {
        if (yaml.contains(path) && !yaml.isInt(path)) {
            warn(path, String.valueOf(yaml.get(path)), Integer.toString(fallback));
            return fallback;
        }
        int value = yaml.getInt(path, fallback);
        if (value < minimum || value > maximum) {
            warn(path, Integer.toString(value), Integer.toString(fallback));
            return fallback;
        }
        return value;
    }

    private FileConfiguration loadFromDisk() throws IOException, InvalidConfigurationException {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(configFile);
        return yaml;
    }

    private double decimal(FileConfiguration yaml, String path, double fallback, double minimum, double maximum) {
        if (yaml.contains(path) && !yaml.isDouble(path) && !yaml.isInt(path)) {
            warn(path, String.valueOf(yaml.get(path)), Double.toString(fallback));
            return fallback;
        }
        double value = yaml.getDouble(path, fallback);
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            warn(path, Double.toString(value), Double.toString(fallback));
            return fallback;
        }
        return value;
    }

    private <E extends Enum<E>> E enumValue(String raw, E fallback, Class<E> type, String path) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, raw.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            // Warning below includes the invalid value and safe fallback.
        }
        warn(path, String.valueOf(raw), fallback.name());
        return fallback;
    }

    private boolean booleanValue(FileConfiguration yaml, String path, boolean fallback) {
        if (!yaml.contains(path)) {
            return fallback;
        }
        if (!yaml.isBoolean(path)) {
            warn(path, String.valueOf(yaml.get(path)), Boolean.toString(fallback));
            return fallback;
        }
        return yaml.getBoolean(path);
    }

    private void warn(String path, String value, String fallback) {
        plugin.getLogger().warning("Invalid " + path + " value '" + value + "'; using " + fallback + '.');
    }
}
