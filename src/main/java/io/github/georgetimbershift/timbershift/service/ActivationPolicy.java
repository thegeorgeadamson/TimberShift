package io.github.georgetimbershift.timbershift.service;

import io.github.georgetimbershift.timbershift.config.TimberShiftConfig;

public final class ActivationPolicy {
    public boolean allows(ActivationContext context, TimberShiftConfig.Activation config) {
        if (!context.globallyEnabled() || !context.worldAllowed() || !context.playerEnabled()) {
            return false;
        }
        if (config.requireAxe() && !context.hasAxe()) {
            return false;
        }
        if (config.requirePermission() && !context.hasPermission()) {
            return false;
        }
        return !config.sneakBypasses() || !context.sneaking();
    }
}
