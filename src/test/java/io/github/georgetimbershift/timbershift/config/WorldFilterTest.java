package io.github.georgetimbershift.timbershift.config;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldFilterTest {
    @Test
    void emptyBlacklistAllowsEveryWorld() {
        WorldFilter filter = new WorldFilter(new TimberShiftConfig.Worlds(WorldMode.BLACKLIST, Set.of()));

        assertTrue(filter.allows("world"));
        assertTrue(filter.allows("survival"));
    }

    @Test
    void blacklistComparisonIsCaseInsensitive() {
        WorldFilter filter = new WorldFilter(new TimberShiftConfig.Worlds(WorldMode.BLACKLIST,
                Set.of("PROTECTED_WORLD")));

        assertFalse(filter.allows("Protected_World"));
        assertTrue(filter.allows("islands"));
    }

    @Test
    void whitelistAllowsOnlyListedWorlds() {
        WorldFilter filter = new WorldFilter(new TimberShiftConfig.Worlds(WorldMode.WHITELIST,
                Set.of("survival")));

        assertTrue(filter.allows("Survival"));
        assertFalse(filter.allows("world"));
    }
}
