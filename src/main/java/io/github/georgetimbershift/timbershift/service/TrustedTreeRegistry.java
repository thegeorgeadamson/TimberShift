package io.github.georgetimbershift.timbershift.service;

import io.github.georgetimbershift.timbershift.model.BlockPos;
import io.github.georgetimbershift.timbershift.model.LogFamily;

import java.time.Clock;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Short-lived provenance for logs that TimberShift itself moved. It lets a verified tree finish
 * after its leaves are no longer adjacent without making arbitrary leafless structures eligible.
 */
public final class TrustedTreeRegistry {
    private final Clock clock;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<BlockKey, UUID> positions = new HashMap<>();

    public TrustedTreeRegistry() {
        this(Clock.systemUTC());
    }

    TrustedTreeRegistry(Clock clock) {
        this.clock = clock;
    }

    public UUID lookup(UUID worldId, BlockPos position, LogFamily family) {
        pruneExpired();
        UUID sessionId = positions.get(new BlockKey(worldId, position));
        Session session = sessions.get(sessionId);
        if (session == null || session.family != family) {
            return null;
        }
        return sessionId;
    }

    public boolean allBelongTo(UUID worldId, Collection<BlockPos> checked, UUID sessionId) {
        pruneExpired();
        Session session = sessions.get(sessionId);
        if (session == null || !session.worldId.equals(worldId)) {
            return false;
        }
        return checked.stream().allMatch(session.blocks::contains);
    }

    public Set<BlockPos> positions(UUID sessionId) {
        pruneExpired();
        Session session = sessions.get(sessionId);
        return session == null ? Set.of() : Set.copyOf(session.blocks);
    }

    public Set<BlockPos> leafCandidates(UUID sessionId) {
        pruneExpired();
        Session session = sessions.get(sessionId);
        return session == null ? Set.of() : Set.copyOf(session.leafCandidates);
    }

    public UUID register(
            UUID worldId,
            LogFamily family,
            Collection<BlockPos> blocks,
            int lifetimeSeconds
    ) {
        return register(worldId, family, blocks, Set.of(), lifetimeSeconds);
    }

    public UUID register(
            UUID worldId,
            LogFamily family,
            Collection<BlockPos> blocks,
            Collection<BlockPos> leafCandidates,
            int lifetimeSeconds
    ) {
        pruneExpired();
        UUID id = UUID.randomUUID();
        Session session = new Session(worldId, family, expiry(lifetimeSeconds), new HashSet<>(blocks),
                new HashSet<>(leafCandidates));
        sessions.put(id, session);
        session.blocks.forEach(position -> positions.put(new BlockKey(worldId, position), id));
        return id;
    }

    public void update(
            UUID sessionId,
            Collection<BlockPos> removed,
            Collection<BlockPos> added,
            int lifetimeSeconds
    ) {
        Session old = sessions.get(sessionId);
        Collection<BlockPos> existingLeaves = old == null ? Set.of() : old.leafCandidates;
        update(sessionId, removed, added, existingLeaves, lifetimeSeconds);
    }

    public void update(
            UUID sessionId,
            Collection<BlockPos> removed,
            Collection<BlockPos> added,
            Collection<BlockPos> leafCandidates,
            int lifetimeSeconds
    ) {
        pruneExpired();
        Session old = sessions.get(sessionId);
        if (old == null) {
            return;
        }
        Set<BlockPos> replacement = new HashSet<>(old.blocks);
        replacement.removeAll(removed);
        replacement.addAll(added);
        for (BlockPos position : removed) {
            positions.remove(new BlockKey(old.worldId, position), sessionId);
        }
        Session updated = new Session(old.worldId, old.family, expiry(lifetimeSeconds), replacement,
                new HashSet<>(leafCandidates));
        sessions.put(sessionId, updated);
        for (BlockPos position : added) {
            positions.put(new BlockKey(old.worldId, position), sessionId);
        }
    }

    public void clear() {
        sessions.clear();
        positions.clear();
    }

    private long expiry(int lifetimeSeconds) {
        return clock.millis() + lifetimeSeconds * 1000L;
    }

    private void pruneExpired() {
        long now = clock.millis();
        Iterator<Map.Entry<UUID, Session>> iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Session> entry = iterator.next();
            Session session = entry.getValue();
            if (session.expiresAtMillis <= now) {
                for (BlockPos position : session.blocks) {
                    positions.remove(new BlockKey(session.worldId, position), entry.getKey());
                }
                iterator.remove();
            }
        }
    }

    private record Session(
            UUID worldId,
            LogFamily family,
            long expiresAtMillis,
            Set<BlockPos> blocks,
            Set<BlockPos> leafCandidates
    ) {
    }
}
