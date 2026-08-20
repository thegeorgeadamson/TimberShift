package io.github.georgetimbershift.timbershift.testsupport;

import io.github.georgetimbershift.timbershift.model.BlockPos;
import io.github.georgetimbershift.timbershift.tree.CellSnapshot;
import io.github.georgetimbershift.timbershift.tree.TreeWorld;

import java.util.HashMap;
import java.util.Map;

public final class MapTreeWorld implements TreeWorld {
    private final Map<BlockPos, CellSnapshot> cells = new HashMap<>();

    @Override
    public CellSnapshot read(BlockPos position) {
        return cells.getOrDefault(position, CellSnapshot.air());
    }

    public MapTreeWorld put(BlockPos position, CellSnapshot snapshot) {
        cells.put(position, snapshot);
        return this;
    }

    public MapTreeWorld log(int x, int y, int z, String state) {
        return put(new BlockPos(x, y, z), CellSnapshot.log(
                io.github.georgetimbershift.timbershift.model.LogFamily.OAK, state));
    }

    public MapTreeWorld naturalLeaf(int x, int y, int z) {
        return put(new BlockPos(x, y, z), CellSnapshot.naturalLeaf(
                io.github.georgetimbershift.timbershift.model.LogFamily.OAK));
    }

    public MapTreeWorld persistentLeaf(int x, int y, int z) {
        return put(new BlockPos(x, y, z), CellSnapshot.persistentLeaf(
                io.github.georgetimbershift.timbershift.model.LogFamily.OAK));
    }
}
