package io.github.georgetimbershift.timbershift.testsupport;

import io.github.georgetimbershift.timbershift.leaf.LeafCellSnapshot;
import io.github.georgetimbershift.timbershift.leaf.LeafWorld;
import io.github.georgetimbershift.timbershift.model.BlockPos;
import io.github.georgetimbershift.timbershift.model.LogFamily;

import java.util.HashMap;
import java.util.Map;

public final class MapLeafWorld implements LeafWorld {
    private final Map<BlockPos, LeafCellSnapshot> cells = new HashMap<>();

    @Override
    public LeafCellSnapshot read(BlockPos position) {
        return cells.getOrDefault(position, LeafCellSnapshot.other());
    }

    public MapLeafWorld put(BlockPos position, LeafCellSnapshot cell) {
        cells.put(position, cell);
        return this;
    }

    public MapLeafWorld naturalLeaf(int x, int y, int z) {
        return put(new BlockPos(x, y, z), LeafCellSnapshot.naturalLeaf(LogFamily.OAK, 7));
    }

    public MapLeafWorld persistentLeaf(int x, int y, int z) {
        return put(new BlockPos(x, y, z), LeafCellSnapshot.persistentLeaf(LogFamily.OAK, 7));
    }

    public MapLeafWorld support(int x, int y, int z) {
        return put(new BlockPos(x, y, z), LeafCellSnapshot.support());
    }
}
