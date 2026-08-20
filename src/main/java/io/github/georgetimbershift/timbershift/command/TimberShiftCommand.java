package io.github.georgetimbershift.timbershift.command;

import io.github.georgetimbershift.timbershift.config.ConfigurationManager;
import io.github.georgetimbershift.timbershift.config.TimberShiftConfig;
import io.github.georgetimbershift.timbershift.config.WorldFilter;
import io.github.georgetimbershift.timbershift.service.PlayerPreferenceService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TimberShiftCommand implements CommandExecutor, TabCompleter {
    private final ConfigurationManager configuration;
    private final PlayerPreferenceService preferences;

    public TimberShiftCommand(ConfigurationManager configuration, PlayerPreferenceService preferences) {
        this.configuration = configuration;
        this.preferences = preferences;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
        String subcommand = arguments.length == 0 ? "help" : arguments[0].toLowerCase(Locale.ROOT);
        return switch (subcommand) {
            case "help" -> showHelp(sender, label);
            case "reload" -> reload(sender);
            case "toggle" -> toggle(sender);
            case "status" -> status(sender);
            default -> {
                message(sender, "&cUnknown subcommand. Use &f/" + label + " help&c.");
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] arguments
    ) {
        if (arguments.length != 1) {
            return List.of();
        }
        List<String> choices = new ArrayList<>();
        choices.add("help");
        if (sender.hasPermission("timbershift.toggle")) {
            choices.add("toggle");
        }
        if (sender.hasPermission("timbershift.command.status")) {
            choices.add("status");
        }
        if (sender.hasPermission("timbershift.command.reload") || sender.hasPermission("timbershift.admin")) {
            choices.add("reload");
        }
        String prefix = arguments[0].toLowerCase(Locale.ROOT);
        return choices.stream().filter(choice -> choice.startsWith(prefix)).toList();
    }

    private boolean showHelp(CommandSender sender, String label) {
        message(sender, "&6TimberShift &7- progressive tree shifting");
        message(sender, "&e/" + label + " status &7- show current state");
        if (sender instanceof Player && sender.hasPermission("timbershift.toggle")) {
            message(sender, "&e/" + label + " toggle &7- toggle shifting for yourself");
        }
        if (sender.hasPermission("timbershift.command.reload") || sender.hasPermission("timbershift.admin")) {
            message(sender, "&e/" + label + " reload &7- safely reload configuration");
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("timbershift.command.reload") && !sender.hasPermission("timbershift.admin")) {
            configuredMessage(sender, "no-permission");
            return true;
        }
        if (configuration.reload()) {
            configuredMessage(sender, "reloaded");
        } else {
            message(sender, "&cReload failed. See the server log; the previous configuration is still active.");
        }
        return true;
    }

    private boolean toggle(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            configuredMessage(sender, "player-only");
            return true;
        }
        if (!sender.hasPermission("timbershift.toggle")) {
            configuredMessage(sender, "no-permission");
            return true;
        }
        boolean enabled = preferences.toggle(player);
        configuredMessage(sender, enabled ? "toggled-on" : "toggled-off");
        return true;
    }

    private boolean status(CommandSender sender) {
        if (!sender.hasPermission("timbershift.command.status") && !sender.hasPermission("timbershift.admin")) {
            configuredMessage(sender, "no-permission");
            return true;
        }
        TimberShiftConfig config = configuration.current();
        message(sender, "&6TimberShift status:");
        message(sender, "&7Global: " + state(config.general().enabled()));
        message(sender, "&7Fast leaf decay: " + state(config.fastDecay().enabled()));
        if (sender instanceof Player player) {
            message(sender, "&7For you: " + state(preferences.isEnabled(player)));
            message(sender, "&7World &f" + player.getWorld().getName() + "&7: "
                    + state(new WorldFilter(config.worlds()).allows(player.getWorld().getName())));
            message(sender, "&7Sneak bypass: " + state(config.activation().sneakBypasses()));
        } else {
            message(sender, "&7World/player state: &favailable in game");
        }
        return true;
    }

    private String state(boolean enabled) {
        return enabled ? "&aenabled" : "&cdisabled";
    }

    private void configuredMessage(CommandSender sender, String key) {
        TimberShiftConfig config = configuration.current();
        message(sender, config.message("prefix") + config.message(key));
    }

    @SuppressWarnings("deprecation")
    private void message(CommandSender sender, String text) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', text));
    }
}
