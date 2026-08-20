package io.github.georgetimbershift.timbershift.service;

import io.github.georgetimbershift.timbershift.model.BlockPos;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Stores player placement provenance in the owning chunk's persistent plugin data. */
public final class PlacedLogTracker {
    private static final int[] EMPTY = new int[0];

    private final NamespacedKey key;

    public PlacedLogTracker(JavaPlugin plugin) {
        this.key = new NamespacedKey(plugin, "player_placed_logs");
    }

    public boolean isPlayerPlaced(World world, BlockPos position) {
        Chunk chunk = loadedChunk(world, position);
        if (chunk == null) {
            return false;
        }
        int packed = pack(world, position);
        int[] entries = chunk.getPersistentDataContainer()
                .getOrDefault(key, PersistentDataType.INTEGER_ARRAY, EMPTY);
        return PlacedLogIndex.contains(entries, packed);
    }

    public void mark(World world, BlockPos position) {
        Chunk chunk = loadedChunk(world, position);
        if (chunk == null) {
            return;
        }
        PersistentDataContainer data = chunk.getPersistentDataContainer();
        int[] entries = data.getOrDefault(key, PersistentDataType.INTEGER_ARRAY, EMPTY);
        data.set(key, PersistentDataType.INTEGER_ARRAY,
                PlacedLogIndex.add(entries, pack(world, position)));
    }

    public void forget(World world, BlockPos position) {
        Chunk chunk = loadedChunk(world, position);
        if (chunk == null) {
            return;
        }
        PersistentDataContainer data = chunk.getPersistentDataContainer();
        int[] entries = data.getOrDefault(key, PersistentDataType.INTEGER_ARRAY, EMPTY);
        int[] replacement = PlacedLogIndex.remove(entries, pack(world, position));
        if (replacement.length == 0) {
            data.remove(key);
        } else if (replacement != entries) {
            data.set(key, PersistentDataType.INTEGER_ARRAY, replacement);
        }
    }

    private Chunk loadedChunk(World world, BlockPos position) {
        if (!world.isChunkLoaded(position.chunkX(), position.chunkZ())) {
            return null;
        }
        return world.getChunkAt(position.chunkX(), position.chunkZ());
    }

    private int pack(World world, BlockPos position) {
        return PlacedLogIndex.pack(position.x(), position.y(), position.z(), world.getMinHeight());
    }
}
