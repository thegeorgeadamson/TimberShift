package io.github.georgetimbershift.timbershift;

import io.github.georgetimbershift.timbershift.command.TimberShiftCommand;
import io.github.georgetimbershift.timbershift.config.ConfigurationManager;
import io.github.georgetimbershift.timbershift.listener.TreeBreakListener;
import io.github.georgetimbershift.timbershift.listener.LeafChangeListener;
import io.github.georgetimbershift.timbershift.listener.PlacedLogListener;
import io.github.georgetimbershift.timbershift.leaf.LeafDecayService;
import io.github.georgetimbershift.timbershift.material.MaterialClassifier;
import io.github.georgetimbershift.timbershift.service.PlayerPreferenceService;
import io.github.georgetimbershift.timbershift.service.PlacedLogTracker;
import io.github.georgetimbershift.timbershift.service.TreeShiftService;
import io.github.georgetimbershift.timbershift.service.TrustedTreeRegistry;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class TimberShiftPlugin extends JavaPlugin {
    private TreeShiftService shiftService;
    private LeafDecayService leafDecayService;

    @Override
    public void onEnable() {
        long startedAtNanos = System.nanoTime();
        ConfigurationManager configuration = new ConfigurationManager(this);
        configuration.loadInitial();

        MaterialClassifier classifier = new MaterialClassifier();
        PlayerPreferenceService preferences = new PlayerPreferenceService(this);
        TrustedTreeRegistry trustedTrees = new TrustedTreeRegistry();
        PlacedLogTracker placedLogs = new PlacedLogTracker(this);
        leafDecayService = new LeafDecayService(this, configuration, classifier);
        configuration.addReloadListener(leafDecayService::onConfigurationReload);
        shiftService = new TreeShiftService(this, configuration, classifier, trustedTrees, placedLogs,
                leafDecayService);

        getServer().getPluginManager().registerEvents(
                new TreeBreakListener(configuration, classifier, preferences, placedLogs, shiftService), this);
        getServer().getPluginManager().registerEvents(
                new LeafChangeListener(this, classifier, leafDecayService), this);
        getServer().getPluginManager().registerEvents(
                new PlacedLogListener(this, classifier, placedLogs), this);

        PluginCommand command = getCommand("timbershift");
        if (command == null) {
            throw new IllegalStateException("timbershift command is missing from plugin.yml");
        }
        TimberShiftCommand handler = new TimberShiftCommand(configuration, preferences);
        command.setExecutor(handler);
        command.setTabCompleter(handler);

        long elapsedMillis = Math.max(1L, (System.nanoTime() - startedAtNanos) / 1_000_000L);
        printStartupBanner(configuration.current().fastDecay().enabled(), elapsedMillis);
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

    private void printStartupBanner(boolean fastDecayEnabled, long elapsedMillis) {
        ConsoleCommandSender console = getServer().getConsoleSender();
        String gold = ChatColor.GOLD.toString();
        String yellow = ChatColor.YELLOW.toString();
        String green = ChatColor.GREEN.toString();
        String gray = ChatColor.GRAY.toString();
        console.sendMessage("");
        console.sendMessage(gold + " _______ _           _                _____ _     _  __ _");
        console.sendMessage(gold + "|__   __(_)         | |              / ____| |   (_)/ _| |");
        console.sendMessage(gold + "   | |   _ _ __ ___ | |__   ___ _ __| (___ | |__  _| |_| |_");
        console.sendMessage(gold + "   | |  | | '_ ` _ \\| '_ \\ / _ \\ '__|\\___ \\| '_ \\| |  _| __|");
        console.sendMessage(gold + "   | |  | | | | | | | |_) |  __/ |   ____) | | | | | | | |_");
        console.sendMessage(gold + "   |_|  |_|_| |_| |_|_.__/ \\___|_|  |_____/|_| |_|_|_|  \\__|");
        console.sendMessage(gray + "  by " + green + "George Adamson");
        console.sendMessage(yellow + "  v" + getDescription().getVersion() + gray
                + " | Minecraft " + minecraftVersion() + " | " + getServer().getName());
        console.sendMessage(gray + "  Fast leaf decay " + (fastDecayEnabled ? green + "enabled" : yellow + "disabled")
                + gray + " | Loaded in " + yellow + elapsedMillis + "ms");
        console.sendMessage("");
    }
}
