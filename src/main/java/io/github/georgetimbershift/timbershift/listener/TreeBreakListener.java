package io.github.georgetimbershift.timbershift.listener;

import io.github.georgetimbershift.timbershift.config.ConfigurationManager;
import io.github.georgetimbershift.timbershift.config.TimberShiftConfig;
import io.github.georgetimbershift.timbershift.material.MaterialClassifier;
import io.github.georgetimbershift.timbershift.model.BlockPos;
import io.github.georgetimbershift.timbershift.model.LogFamily;
import io.github.georgetimbershift.timbershift.service.ActivationContext;
import io.github.georgetimbershift.timbershift.service.ActivationPolicy;
import io.github.georgetimbershift.timbershift.service.PlayerPreferenceService;
import io.github.georgetimbershift.timbershift.service.TreeShiftService;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public final class TreeBreakListener implements Listener {
    private final ConfigurationManager configuration;
    private final MaterialClassifier classifier;
    private final PlayerPreferenceService preferences;
    private final TreeShiftService shifts;
    private final ActivationPolicy activation = new ActivationPolicy();

    public TreeBreakListener(
            ConfigurationManager configuration,
            MaterialClassifier classifier,
            PlayerPreferenceService preferences,
            TreeShiftService shifts
    ) {
        this.configuration = configuration;
        this.classifier = classifier;
        this.preferences = preferences;
        this.shifts = shifts;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPermittedBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        LogFamily family = classifier.logFamily(block.getType());
        if (family == null) {
            return;
        }

        Player player = event.getPlayer();
        TimberShiftConfig config = configuration.current();
        ActivationContext context = new ActivationContext(
                config.general().enabled(),
                config.worlds().allows(block.getWorld().getName()),
                classifier.isAxe(player.getInventory().getItemInMainHand()),
                player.hasPermission("timbershift.use"),
                player.isSneaking(),
                preferences.isEnabled(player));
        if (!activation.allows(context, config.activation())) {
            return;
        }

        // MONITOR is observation-only: no event field or world state is changed here. The next-tick
        // task also verifies that vanilla actually removed the block before considering movement.
        shifts.schedule(block.getWorld(), new BlockPos(block.getX(), block.getY(), block.getZ()), family, event);
    }
}
