package io.github.georgetimbershift.timbershift.service;

import io.github.georgetimbershift.timbershift.model.BlockPos;

import java.util.UUID;

public record BlockKey(UUID worldId, BlockPos position) {
}
