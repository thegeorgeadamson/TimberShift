package io.github.georgetimbershift.timbershift.leaf;

import io.github.georgetimbershift.timbershift.model.LogFamily;

import java.util.Objects;

public record LeafCellSnapshot(LeafCellKind kind, LogFamily family, int distance) {
    public LeafCellSnapshot {
        Objects.requireNonNull(kind, "kind");
    }

    public static LeafCellSnapshot naturalLeaf(LogFamily family, int distance) {
        return new LeafCellSnapshot(LeafCellKind.NATURAL_LEAF, family, distance);
    }

    public static LeafCellSnapshot persistentLeaf(LogFamily family, int distance) {
        return new LeafCellSnapshot(LeafCellKind.PERSISTENT_LEAF, family, distance);
    }

    public static LeafCellSnapshot support() {
        return new LeafCellSnapshot(LeafCellKind.LOG_SUPPORT, null, 0);
    }

    public static LeafCellSnapshot other() {
        return new LeafCellSnapshot(LeafCellKind.OTHER, null, 0);
    }

    public static LeafCellSnapshot unloaded() {
        return new LeafCellSnapshot(LeafCellKind.UNLOADED, null, 0);
    }

    public boolean isLeaf() {
        return kind == LeafCellKind.NATURAL_LEAF || kind == LeafCellKind.PERSISTENT_LEAF;
    }
}
