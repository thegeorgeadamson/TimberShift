package io.github.georgetimbershift.timbershift.service;

public record ActivationContext(
        boolean globallyEnabled,
        boolean worldAllowed,
        boolean hasAxe,
        boolean hasPermission,
        boolean sneaking,
        boolean playerEnabled
) {
}
