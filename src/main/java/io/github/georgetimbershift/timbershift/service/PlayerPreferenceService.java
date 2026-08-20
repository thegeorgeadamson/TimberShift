package io.github.georgetimbershift.timbershift.service;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class PlayerPreferenceService {
    private final NamespacedKey enabledKey;

    public PlayerPreferenceService(Plugin plugin) {
        this.enabledKey = new NamespacedKey(plugin, "player-enabled");
    }

    public boolean isEnabled(Player player) {
        Byte value = player.getPersistentDataContainer().get(enabledKey, PersistentDataType.BYTE);
        return value == null || value != 0;
    }

    public boolean toggle(Player player) {
        boolean enabled = !isEnabled(player);
        PersistentDataContainer data = player.getPersistentDataContainer();
        if (enabled) {
            data.remove(enabledKey);
        } else {
            data.set(enabledKey, PersistentDataType.BYTE, (byte) 0);
        }
        return enabled;
    }
}
