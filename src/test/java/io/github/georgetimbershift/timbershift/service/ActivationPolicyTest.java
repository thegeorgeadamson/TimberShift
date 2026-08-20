package io.github.georgetimbershift.timbershift.service;

import io.github.georgetimbershift.timbershift.config.TimberShiftConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivationPolicyTest {
    private final ActivationPolicy policy = new ActivationPolicy();
    private final TimberShiftConfig.Activation defaults = new TimberShiftConfig.Activation(true, true, true);

    @Test
    void defaultActivationAllowsEligiblePlayer() {
        assertTrue(policy.allows(new ActivationContext(true, true, true, true, false, true), defaults));
    }

    @Test
    void missingPermissionRejects() {
        assertFalse(policy.allows(new ActivationContext(true, true, true, false, false, true), defaults));
    }

    @Test
    void sneakingBypasses() {
        assertFalse(policy.allows(new ActivationContext(true, true, true, true, true, true), defaults));
    }

    @Test
    void playerToggleRejects() {
        assertFalse(policy.allows(new ActivationContext(true, true, true, true, false, false), defaults));
    }

    @Test
    void nonAxeRejectsWhenRequired() {
        assertFalse(policy.allows(new ActivationContext(true, true, false, true, false, true), defaults));
    }
}
