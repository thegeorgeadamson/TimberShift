package io.github.georgetimbershift.timbershift.tree;

import io.github.georgetimbershift.timbershift.model.LogFamily;

import java.util.Objects;

public record CellSnapshot(CellKind kind, LogFamily family, String state) {
    public CellSnapshot {
        Objects.requireNonNull(kind, "kind");
        state = state == null ? "" : state;
        if ((kind == CellKind.LOG || kind == CellKind.NATURAL_LEAF || kind == CellKind.PERSISTENT_LEAF)
                && family == null) {
            throw new IllegalArgumentException("A log or leaf cell needs a family");
        }
    }

    public static CellSnapshot air() {
        return new CellSnapshot(CellKind.AIR, null, "minecraft:air");
    }

    public static CellSnapshot log(LogFamily family, String state) {
        return new CellSnapshot(CellKind.LOG, family, state);
    }

    public static CellSnapshot naturalLeaf(LogFamily family) {
        return new CellSnapshot(CellKind.NATURAL_LEAF, family, "natural-leaf");
    }

    public static CellSnapshot persistentLeaf(LogFamily family) {
        return new CellSnapshot(CellKind.PERSISTENT_LEAF, family, "persistent-leaf");
    }

    public static CellSnapshot naturalBlock() {
        return new CellSnapshot(CellKind.NATURAL_BLOCK, null, "natural");
    }

    public static CellSnapshot obstruction() {
        return new CellSnapshot(CellKind.OBSTRUCTION, null, "obstruction");
    }

    public static CellSnapshot unloaded() {
        return new CellSnapshot(CellKind.UNLOADED, null, "unloaded");
    }
}
