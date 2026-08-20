package io.github.georgetimbershift.timbershift;

import io.github.georgetimbershift.timbershift.command.TimberShiftCommand;
import io.github.georgetimbershift.timbershift.config.ConfigurationManager;
import io.github.georgetimbershift.timbershift.listener.TreeBreakListener;
import io.github.georgetimbershift.timbershift.leaf.LeafDecayService;
import io.github.georgetimbershift.timbershift.material.MaterialClassifier;
import io.github.georgetimbershift.timbershift.service.PlayerPreferenceService;
import io.github.georgetimbershift.timbershift.service.TreeShiftService;
import io.github.georgetimbershift.timbershift.service.TrustedTreeRegistry;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class TimberShiftPlugin extends JavaPlugin {
    private TreeShiftService shiftService;
    private LeafDecayService leafDecayService;

    @Override
    public void onEnable() {
        ConfigurationManager configuration = new ConfigurationManager(this);
        configuration.loadInitial();

        MaterialClassifier classifier = new MaterialClassifier();
        PlayerPreferenceService preferences = new PlayerPreferenceService(this);
        TrustedTreeRegistry trustedTrees = new TrustedTreeRegistry();
        leafDecayService = new LeafDecayService(this, configuration, classifier);
        configuration.addReloadListener(leafDecayService::onConfigurationReload);
        shiftService = new TreeShiftService(this, configuration, classifier, trustedTrees, leafDecayService);

        getServer().getPluginManager().registerEvents(
                new TreeBreakListener(configuration, classifier, preferences, shiftService), this);

        PluginCommand command = getCommand("timbershift");
        if (command == null) {
            throw new IllegalStateException("timbershift command is missing from plugin.yml");
        }
        TimberShiftCommand handler = new TimberShiftCommand(configuration, preferences);
        command.setExecutor(handler);
        command.setTabCompleter(handler);

        getLogger().info("TimberShift v" + getDescription().getVersion() + " enabled");
        getLogger().info("Platform: " + getServer().getName() + ' ' + getServer().getVersion());
        getLogger().info("Minecraft: " + minecraftVersion());
    }

    @Override
    public void onDisable() {
        if (shiftService != null) {
            shiftService.shutdown();
        }
        if (leafDecayService != null) {
            leafDecayService.shutdown();
        }
    }

    private String minecraftVersion() {
        String bukkitVersion = getServer().getBukkitVersion();
        int buildQualifier = bukkitVersion.indexOf(".build.");
        if (buildQualifier >= 0) {
            return bukkitVersion.substring(0, buildQualifier);
        }
        int qualifier = bukkitVersion.indexOf('-');
        return qualifier < 0 ? bukkitVersion : bukkitVersion.substring(0, qualifier);
    }
}
