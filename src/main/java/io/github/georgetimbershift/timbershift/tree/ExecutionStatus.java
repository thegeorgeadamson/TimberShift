package io.github.georgetimbershift.timbershift.tree;

public enum ExecutionStatus {
    APPLIED,
    WORLD_CHANGED,
    UNLOADED_CHUNK,
    FAILED_ROLLED_BACK,
    FAILED_ROLLBACK_INCOMPLETE
}
