package me.onemb.placeholders;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlaceholdersCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN_PERMISSION = "onemb.placeholders.admin";

    private final OneMBPlaceholdersPlugin plugin;

    public PlaceholdersCommand(final OneMBPlaceholdersPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
        final @NotNull CommandSender sender,
        final @NotNull Command command,
        final @NotNull String label,
        final @NotNull String[] args
    ) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendPlaceholderList(sender);
            return true;
        }

        final String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "help" -> sendHelp(sender, label);
            case "reload" -> reloadPlaceholders(sender);
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /" + label + " help.");
                sendHelp(sender, label);
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
        final @NotNull CommandSender sender,
        final @NotNull Command command,
        final @NotNull String alias,
        final @NotNull String[] args
    ) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            return List.of();
        }

        if (args.length != 1) {
            return List.of();
        }

        final String currentInput = args[0].toLowerCase(Locale.ROOT);
        final List<String> completions = new ArrayList<>();

        if ("help".startsWith(currentInput)) {
            completions.add("help");
        }

        if ("reload".startsWith(currentInput)) {
            completions.add("reload");
        }

        return completions;
    }

    private void sendPlaceholderList(final CommandSender sender) {
        final Map<String, String> placeholders = plugin.getPlaceholders();

        sender.sendMessage(
            ChatColor.GOLD
                + "1MB Placeholders: "
                + ChatColor.WHITE
                + "(v"
                + plugin.getPluginVersion()
                + " build "
                + plugin.getBuildNumber()
                + " for "
                + plugin.getMinecraftVersion()
                + ")"
        );
        if (placeholders.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "  No placeholders are currently configured.");
            return;
        }

        placeholders.forEach((key, value) ->
            sender.sendMessage(ChatColor.YELLOW + "  %onemb_" + key + "%: " + ChatColor.WHITE + "'" + value + "'")
        );
    }

    private void reloadPlaceholders(final CommandSender sender) {
        plugin.reloadPlaceholders();
        sender.sendMessage(
            ChatColor.GREEN + "Reloaded " + plugin.getPlaceholders().size() + " placeholder(s) from config.yml."
        );
    }

    private void sendHelp(final CommandSender sender, final String label) {
        sender.sendMessage(
            ChatColor.GOLD
                + "1MB Placeholders commands: "
                + ChatColor.WHITE
                + "(v"
                + plugin.getPluginVersion()
                + " build "
                + plugin.getBuildNumber()
                + ")"
        );
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + ChatColor.WHITE + " - List all configured placeholders and their values.");
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + " help" + ChatColor.WHITE + " - Show this help message.");
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + " reload" + ChatColor.WHITE + " - Reload placeholders from config.yml.");
    }
}
