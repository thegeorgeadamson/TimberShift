package io.github.georgetimbershift.timbershift.config;

public final class WorldFilter {
    private final TimberShiftConfig.Worlds worlds;

    public WorldFilter(TimberShiftConfig.Worlds worlds) {
        this.worlds = worlds;
    }

    public boolean allows(String worldName) {
        return worlds.allows(worldName);
    }
}
