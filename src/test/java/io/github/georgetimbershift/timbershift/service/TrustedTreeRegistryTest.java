package io.github.georgetimbershift.timbershift.service;

import io.github.georgetimbershift.timbershift.model.BlockPos;
import io.github.georgetimbershift.timbershift.model.LogFamily;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrustedTreeRegistryTest {
    @Test
    void provenanceMovesWithoutExpandingAndExpires() {
        MutableClock clock = new MutableClock();
        TrustedTreeRegistry registry = new TrustedTreeRegistry(clock);
        UUID world = UUID.randomUUID();
        BlockPos original = new BlockPos(0, 1, 0);
        BlockPos shifted = new BlockPos(0, 0, 0);
        BlockPos leaf = new BlockPos(0, 5, 0);
        BlockPos unrelated = new BlockPos(1, 0, 0);

        UUID session = registry.register(world, LogFamily.OAK, List.of(original), List.of(leaf), 30);

        assertTrue(registry.allBelongTo(world, List.of(original), session));
        assertEquals(Set.of(leaf), registry.leafCandidates(session));
        assertFalse(registry.allBelongTo(world, List.of(original, unrelated), session));
        registry.update(session, List.of(original), List.of(shifted), 30);
        assertNull(registry.lookup(world, original, LogFamily.OAK));
        assertTrue(session.equals(registry.lookup(world, shifted, LogFamily.OAK)));

        clock.advanceSeconds(31);
        assertNull(registry.lookup(world, shifted, LogFamily.OAK));
        assertTrue(registry.leafCandidates(session).isEmpty());
    }

    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-08-20T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }
    }
}
