package me.onemb.placeholders;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlaceholdersCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN_PERMISSION = "onemb.placeholders.admin";
    private static final String VIEW_PERMISSION = "onemb.placeholders.view";
    private static final String EDIT_PERMISSION = "onemb.placeholders.edit";
    private static final String RELOAD_PERMISSION = "onemb.placeholders.reload";
    private static final String SEARCH_PERMISSION = "onemb.placeholders.search";
    private static final String BACKUP_PERMISSION = "onemb.placeholders.backup";
    private static final String DEBUG_PERMISSION = "onemb.placeholders.debug";
    private static final int PAGE_SIZE = 8;

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
            if (!requirePermission(sender, VIEW_PERMISSION)) {
                return true;
            }

            sendPlaceholderList(sender, null, 0, null);
            return true;
        }

        if (isInteger(args[0])) {
            if (!requirePermission(sender, VIEW_PERMISSION)) {
                return true;
            }

            sendPlaceholderList(sender, null, parsePage(args[0]), null);
            return true;
        }

        final String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "help" -> sendHelp(sender, label);
            case "list" -> {
                if (!requirePermission(sender, VIEW_PERMISSION)) {
                    return true;
                }
                handleList(sender, args);
            }
            case "get" -> {
                if (!requirePermission(sender, VIEW_PERMISSION)) {
                    return true;
                }
                handleGet(sender, args);
            }
            case "preview" -> {
                if (!requirePermission(sender, VIEW_PERMISSION)) {
                    return true;
                }
                handlePreview(sender, args);
            }
            case "search" -> {
                if (!requirePermission(sender, SEARCH_PERMISSION)) {
                    return true;
                }
                handleSearch(sender, args);
            }
            case "add" -> {
                if (!requirePermission(sender, EDIT_PERMISSION)) {
                    return true;
                }
                handleAdd(sender, args);
            }
            case "category" -> {
                if (!requirePermission(sender, EDIT_PERMISSION)) {
                    return true;
                }
                handleCategory(sender, args);
            }
            case "set" -> {
                if (!requirePermission(sender, EDIT_PERMISSION)) {
                    return true;
                }
                handleSet(sender, args);
            }
            case "remove" -> {
                if (!requirePermission(sender, EDIT_PERMISSION)) {
                    return true;
                }
                handleRemove(sender, args);
            }
            case "reload" -> {
                if (!requirePermission(sender, RELOAD_PERMISSION)) {
                    return true;
                }
                reloadPlaceholders(sender);
            }
            case "backup" -> {
                if (!requirePermission(sender, BACKUP_PERMISSION)) {
                    return true;
                }
                backupConfig(sender);
            }
            case "debug" -> {
                if (!requirePermission(sender, DEBUG_PERMISSION)) {
                    return true;
                }
                handleDebug(sender, args);
            }
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

        if (args.length == 1) {
            final String currentInput = args[0].toLowerCase(Locale.ROOT);
            final List<String> completions = new ArrayList<>();

            addCompletionIfPermitted(sender, completions, currentInput, "help", ADMIN_PERMISSION);
            addCompletionIfPermitted(sender, completions, currentInput, "list", VIEW_PERMISSION);
            addCompletionIfPermitted(sender, completions, currentInput, "get", VIEW_PERMISSION);
            addCompletionIfPermitted(sender, completions, currentInput, "preview", VIEW_PERMISSION);
            addCompletionIfPermitted(sender, completions, currentInput, "search", SEARCH_PERMISSION);
            addCompletionIfPermitted(sender, completions, currentInput, "add", EDIT_PERMISSION);
            addCompletionIfPermitted(sender, completions, currentInput, "category", EDIT_PERMISSION);
            addCompletionIfPermitted(sender, completions, currentInput, "set", EDIT_PERMISSION);
            addCompletionIfPermitted(sender, completions, currentInput, "remove", EDIT_PERMISSION);
            addCompletionIfPermitted(sender, completions, currentInput, "reload", RELOAD_PERMISSION);
            addCompletionIfPermitted(sender, completions, currentInput, "backup", BACKUP_PERMISSION);
            addCompletionIfPermitted(sender, completions, currentInput, "debug", DEBUG_PERMISSION);

            return completions;
        }

        final String subCommand = args[0].toLowerCase(Locale.ROOT);
        return switch (subCommand) {
            case "list" -> completeListArguments(args);
            case "get", "set", "remove", "preview" -> completeKeyArguments(args);
            case "add" -> completeAddArguments(args);
            case "category" -> completeCategoryArguments(args);
            case "search" -> completeSearchArguments(args);
            case "debug" -> completeDebugArguments(args);
            default -> List.of();
        };
    }

    private boolean requirePermission(final CommandSender sender, final String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }

        sender.sendMessage(ChatColor.RED + "You do not have permission: " + permission);
        return false;
    }

    private void addCompletionIfPermitted(
        final CommandSender sender,
        final List<String> completions,
        final String currentInput,
        final String completion,
        final String permission
    ) {
        if (sender.hasPermission(permission) && completion.startsWith(currentInput)) {
            completions.add(completion);
        }
    }

    private void handleList(final CommandSender sender, final String[] args) {
        String category = null;
        int page = 0;

        for (int index = 1; index < args.length; index++) {
            if (isInteger(args[index])) {
                page = parsePage(args[index]);
                continue;
            }

            if ("all".equalsIgnoreCase(args[index])) {
                category = null;
                continue;
            }

            final String normalizedCategory = plugin.normalizeCategory(args[index]);
            if (!plugin.hasCategory(normalizedCategory)) {
                sender.sendMessage(ChatColor.RED + "Unknown category: " + args[index]);
                return;
            }

            category = normalizedCategory;
        }

        sendPlaceholderList(sender, category, page, null);
    }

    private void handleGet(final CommandSender sender, final String[] args) {
        final PlaceholderEntry entry = resolveConfiguredEntry(
            sender,
            args,
            "get",
            "Usage: /_placeholders get <key> or /_placeholders get <category> <key>"
        );
        if (entry == null) {
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "Placeholder: " + ChatColor.YELLOW + "%onemb_" + entry.key() + "%");
        sender.sendMessage(ChatColor.GRAY + "  Category: " + ChatColor.WHITE + entry.category() + describeEntryFlags(entry));
        sender.sendMessage(ChatColor.GRAY + "  Type: " + ChatColor.WHITE + entry.type().name().toLowerCase(Locale.ROOT));
        if (!entry.description().isBlank()) {
            sender.sendMessage(ChatColor.GRAY + "  Description: " + ChatColor.WHITE + entry.description());
        }
        sender.sendMessage(ChatColor.GRAY + "  Stored: " + ChatColor.WHITE + "'" + safeDisplay(plugin.getStoredValueSummary(entry)) + "'");
        sender.sendMessage(ChatColor.GRAY + "  Configured output: " + ChatColor.WHITE + "'" + safeDisplay(plugin.getConfiguredOutput(entry)) + "'");
        sender.sendMessage(ChatColor.GRAY + "  Live output: " + ChatColor.WHITE + "'" + safeDisplay(plugin.getLiveOutput(entry.key())) + "'");
        sender.sendMessage(
            ChatColor.GRAY
                + "  Pending reload: "
                + ChatColor.WHITE
                + (plugin.hasPendingReloadChange(entry.key()) ? "yes" : "no")
        );
    }

    private void handlePreview(final CommandSender sender, final String[] args) {
        final PlaceholderEntry entry = resolveConfiguredEntry(
            sender,
            args,
            "preview",
            "Usage: /_placeholders preview <key> or /_placeholders preview <category> <key>"
        );
        if (entry == null) {
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "Preview: " + ChatColor.YELLOW + "%onemb_" + entry.key() + "%");
        sender.sendMessage(ChatColor.GRAY + "  Stored: " + ChatColor.WHITE + "'" + safeDisplay(plugin.getStoredValueSummary(entry)) + "'");
        sender.sendMessage(ChatColor.GRAY + "  Configured output: " + ChatColor.WHITE + "'" + safeDisplay(plugin.getConfiguredOutput(entry)) + "'");
        sender.sendMessage(ChatColor.GRAY + "  Formatted preview: " + ChatColor.WHITE + "'" + safeDisplay(plugin.getFormattedPreview(entry)) + "'");
        sender.sendMessage(ChatColor.GRAY + "  Plain preview: " + ChatColor.WHITE + "'" + safeDisplay(plugin.getPlainPreview(entry)) + "'");
        sender.sendMessage(ChatColor.GRAY + "  Live output: " + ChatColor.WHITE + "'" + safeDisplay(plugin.getLiveOutput(entry.key())) + "'");
    }

    private void handleSearch(final CommandSender sender, final String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /_placeholders search <text> [category] [page]");
            return;
        }

        int page = 0;
        int endIndexExclusive = args.length;
        String category = null;

        if (endIndexExclusive > 2 && isInteger(args[endIndexExclusive - 1])) {
            page = parsePage(args[endIndexExclusive - 1]);
            endIndexExclusive--;
        }

        if (endIndexExclusive > 2 && plugin.hasCategory(args[endIndexExclusive - 1])) {
            category = plugin.normalizeCategory(args[endIndexExclusive - 1]);
            endIndexExclusive--;
        }

        final String query = String.join(" ", slice(args, 1, endIndexExclusive)).trim();
        if (query.isBlank()) {
            sender.sendMessage(ChatColor.RED + "Search text could not be empty.");
            return;
        }

        if (!plugin.isSearchLengthValid(query)) {
            sender.sendMessage(ChatColor.RED + "Search text may be at most 128 characters.");
            return;
        }

        sendPlaceholderList(sender, category, page, query);
    }

    private void handleAdd(final CommandSender sender, final String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /_placeholders add [category:]<key> <value...> or /_placeholders add <category> <key> <value...>");
            return;
        }

        final String[] categoryAndKey;
        final String value;

        if (args.length >= 4 && plugin.hasCategory(args[1]) && !args[1].contains(":")) {
            categoryAndKey = new String[] {args[1], args[2]};
            value = String.join(" ", slice(args, 3, args.length));
        } else {
            categoryAndKey = splitCategoryAndKey(args[1]);
            value = String.join(" ", slice(args, 2, args.length));
        }

        final String category = categoryAndKey[0];
        final String key = categoryAndKey[1];
        final ActionResult result = plugin.addPlaceholderToConfig(category, key, value, getActorName(sender));

        sender.sendMessage(result.success() ? ChatColor.GREEN + result.message() : ChatColor.RED + result.message());
    }

    private void handleSet(final CommandSender sender, final String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /_placeholders set <key> <value...> or /_placeholders set <category> <key> <value...>");
            return;
        }

        final String keyReference;
        final String expectedCategory;
        final String value;

        if (args.length >= 4 && plugin.hasCategory(args[1])) {
            expectedCategory = plugin.normalizeCategory(args[1]);
            keyReference = args[1] + ":" + args[2];
            value = String.join(" ", slice(args, 3, args.length));
        } else {
            expectedCategory = null;
            keyReference = args[1];
            value = String.join(" ", slice(args, 2, args.length));
        }

        if (resolveConfiguredEntry(sender, keyReference, expectedCategory, "set") == null) {
            return;
        }

        final ActionResult result = plugin.setPlaceholderInConfig(keyReference, value, getActorName(sender));

        sender.sendMessage(result.success() ? ChatColor.GREEN + result.message() : ChatColor.RED + result.message());
    }

    private void handleCategory(final CommandSender sender, final String[] args) {
        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /_placeholders category <category> <true|false>");
            return;
        }

        final String category = plugin.normalizeCategory(args[1]);
        if (!plugin.hasCategory(category)) {
            sender.sendMessage(ChatColor.RED + "Unknown category: " + args[1]);
            return;
        }

        final @Nullable Boolean enabled = parseBooleanArgument(args[2]);
        if (enabled == null) {
            sender.sendMessage(ChatColor.RED + "Category state must be true or false.");
            sender.sendMessage(ChatColor.YELLOW + "Usage: /_placeholders category <category> <true|false>");
            return;
        }

        final ActionResult result = plugin.setCategoryEnabled(category, enabled, getActorName(sender));
        sender.sendMessage(result.success() ? ChatColor.GREEN + result.message() : ChatColor.RED + result.message());
    }

    private void handleRemove(final CommandSender sender, final String[] args) {
        if (args.length != 2 && !(args.length == 3 && plugin.hasCategory(args[1]))) {
            sender.sendMessage(ChatColor.RED + "Usage: /_placeholders remove <key> or /_placeholders remove <category> <key>");
            return;
        }

        final String expectedCategory = args.length == 3 ? plugin.normalizeCategory(args[1]) : null;
        final String keyReference = args.length == 3 ? args[1] + ":" + args[2] : args[1];

        if (resolveConfiguredEntry(sender, keyReference, expectedCategory, "remove") == null) {
            return;
        }

        final ActionResult result = plugin.removePlaceholderFromConfig(keyReference, getActorName(sender));
        sender.sendMessage(result.success() ? ChatColor.GREEN + result.message() : ChatColor.RED + result.message());
    }

    private void sendPlaceholderList(
        final CommandSender sender,
        final @Nullable String category,
        final int requestedPage,
        final @Nullable String searchQuery
    ) {
        final String normalizedQuery = searchQuery == null ? null : searchQuery.toLowerCase(Locale.ROOT);
        final ListingSettings listingSettings = plugin.getListingSettings();
        final List<PlaceholderEntry> matches = plugin.getConfiguredPlaceholders().stream()
            .filter(entry -> category == null || entry.category().equals(category))
            .filter(entry -> !listingSettings.onlyShowEnabledPlaceholders() || entry.isLiveEligible())
            .filter(entry -> normalizedQuery == null || matchesQuery(entry, normalizedQuery))
            .sorted(Comparator.comparing(PlaceholderEntry::category).thenComparing(PlaceholderEntry::key))
            .toList();

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

        if (matches.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "  No placeholders matched that request.");
            return;
        }

        final List<PlaceholderEntry> visibleEntries;
        if (sender instanceof Player) {
            final int totalPages = Math.max(1, (int) Math.ceil(matches.size() / (double) PAGE_SIZE));
            final int page = requestedPage <= 0 ? 1 : requestedPage;
            if (page < 1 || page > totalPages) {
                sender.sendMessage(ChatColor.RED + "Page must be between 1 and " + totalPages + ".");
                return;
            }

            final int startIndex = (page - 1) * PAGE_SIZE;
            final int endIndex = Math.min(startIndex + PAGE_SIZE, matches.size());
            visibleEntries = matches.subList(startIndex, endIndex);

            sender.sendMessage(
                ChatColor.GRAY
                    + "  Showing "
                    + ChatColor.WHITE
                    + (startIndex + 1)
                    + "-"
                    + endIndex
                    + "/"
                    + matches.size()
                    + ChatColor.GRAY
                    + " (page "
                    + page
                    + "/"
                    + totalPages
                    + ", "
                    + PAGE_SIZE
                    + " per page)"
            );
        } else {
            visibleEntries = matches;
            sender.sendMessage(
                ChatColor.GRAY
                    + "  Showing "
                    + ChatColor.WHITE
                    + visibleEntries.size()
                    + "/"
                    + matches.size()
                    + ChatColor.GRAY
                    + " (console output, pagination disabled)"
            );
        }

        if (category != null) {
            sender.sendMessage(ChatColor.GRAY + "  Category filter: " + ChatColor.WHITE + category);
        }

        if (searchQuery != null) {
            sender.sendMessage(ChatColor.GRAY + "  Search filter: " + ChatColor.WHITE + searchQuery);
        }

        sendFormattedPlaceholderEntries(sender, visibleEntries, listingSettings);
    }

    private void reloadPlaceholders(final CommandSender sender) {
        plugin.reloadPlaceholders();
        plugin.audit(getActorName(sender), "RELOAD", "Reloaded placeholders from config.yml.");
        sender.sendMessage(ChatColor.GREEN + "Reloaded placeholders from config.yml.");
    }

    private void backupConfig(final CommandSender sender) {
        final ActionResult result = plugin.createBackup(getActorName(sender));
        sender.sendMessage(result.success() ? ChatColor.GREEN + result.message() : ChatColor.RED + result.message());
    }

    private void handleDebug(final CommandSender sender, final String[] args) {
        if (args.length == 1) {
            sendDebugOverview(sender);
            return;
        }

        if (args.length == 2 && "config".equalsIgnoreCase(args[1])) {
            final ActionResult result = plugin.mergeMissingDefaultConfig(getActorName(sender));
            sender.sendMessage(result.success() ? ChatColor.GREEN + result.message() : ChatColor.RED + result.message());
            return;
        }

        if (args.length == 2 && "clear".equalsIgnoreCase(args[1])) {
            sendDebugClearHelp(sender);
            return;
        }

        if (args.length == 3 && "clear".equalsIgnoreCase(args[1])) {
            handleDebugClear(sender, args[2]);
            return;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /_placeholders debug [config|clear]");
        sendDebugOverview(sender);
    }

    private void handleDebugClear(final CommandSender sender, final String target) {
        final String normalizedTarget = target.toLowerCase(Locale.ROOT);
        final ActionResult result = switch (normalizedTarget) {
            case "logs" -> plugin.clearLogs(getActorName(sender));
            case "backups" -> plugin.clearBackups(getActorName(sender));
            default -> ActionResult.failure("Usage: /_placeholders debug clear <logs|backups>");
        };

        sender.sendMessage(result.success() ? ChatColor.GREEN + result.message() : ChatColor.RED + result.message());
    }

    private void sendDebugClearHelp(final CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "1MB Placeholders debug clear:");
        sender.sendMessage(
            ChatColor.YELLOW
                + "  /_placeholders debug clear logs"
                + ChatColor.WHITE
                + " - Clear log files while keeping the persistent purge-history.log record."
        );
        sender.sendMessage(
            ChatColor.YELLOW
                + "  /_placeholders debug clear backups"
                + ChatColor.WHITE
                + " - Clear saved backup files and audit who cleared them."
        );
    }

    private void sendDebugOverview(final CommandSender sender) {
        final ActionResult bundledConfigStatus = plugin.validateBundledDefaultConfig();
        final ActionResult currentConfigStatus = plugin.validateCurrentConfigFile();
        final FormattingSettings formattingSettings = plugin.getFormattingSettings();
        final ListingSettings listingSettings = plugin.getListingSettings();
        final List<String> validationIssues = plugin.getValidationIssues();

        sender.sendMessage(
            ChatColor.GOLD
                + "1MB Placeholders debug: "
                + ChatColor.WHITE
                + "(v"
                + plugin.getPluginVersion()
                + " build "
                + plugin.getBuildNumber()
                + ")"
        );
        sender.sendMessage(
            ChatColor.GRAY
                + "  Plugin: "
                + ChatColor.WHITE
                + plugin.getDescription().getName()
                + " v"
                + plugin.getPluginVersion()
                + " build "
                + plugin.getBuildNumber()
        );
        sender.sendMessage(ChatColor.GRAY + "  Credits: " + ChatColor.WHITE + "PyroTempus, mrfloris, and OpenAI");
        sender.sendMessage(
            ChatColor.GRAY
                + "  Authors metadata: "
                + ChatColor.WHITE
                + String.join(", ", plugin.getDescription().getAuthors())
        );
        sender.sendMessage(
            ChatColor.GRAY
                + "  Built for: "
                + ChatColor.WHITE
                + "Java "
                + plugin.getTargetJavaVersion()
                + " / Minecraft "
                + plugin.getMinecraftVersion()
        );
        sender.sendMessage(
            ChatColor.GRAY
                + "  Runtime Java: "
                + ChatColor.WHITE
                + System.getProperty("java.version", "unknown")
        );
        sender.sendMessage(
            ChatColor.GRAY
                + "  Server engine: "
                + ChatColor.WHITE
                + plugin.getServer().getVersion()
        );
        sender.sendMessage(
            ChatColor.GRAY
                + "  Runtime Minecraft: "
                + ChatColor.WHITE
                + plugin.getServer().getMinecraftVersion()
        );
        sender.sendMessage(ChatColor.GRAY + "  Data folder: " + ChatColor.WHITE + plugin.getDataFolderPath());
        sender.sendMessage(ChatColor.GRAY + "  Config path: " + ChatColor.WHITE + plugin.getConfigFilePath());
        sender.sendMessage(
            ChatColor.GRAY
                + "  Config file: "
                + ChatColor.WHITE
                + plugin.getConfiguredCategoryCount()
                + " categories, "
                + plugin.getEnabledCategoryCount()
                + " enabled, "
                + plugin.getDisabledCategoryCount()
                + " disabled, "
                + plugin.getConfiguredPlaceholderCount()
                + " placeholders total"
        );
        sender.sendMessage(
            ChatColor.GRAY
                + "  Current config status: "
                + (currentConfigStatus.success() ? ChatColor.GREEN : ChatColor.RED)
                + currentConfigStatus.message()
        );
        sender.sendMessage(
            ChatColor.GRAY
                + "  Active placeholders: "
                + ChatColor.WHITE
                + plugin.getLivePlaceholderCount()
        );
        sender.sendMessage(
            ChatColor.GRAY
                + "  Bundled defaults: "
                + (bundledConfigStatus.success() ? ChatColor.GREEN : ChatColor.RED)
                + bundledConfigStatus.message()
        );
        sender.sendMessage(
            ChatColor.GRAY
                + "  Backups: "
                + ChatColor.WHITE
                + plugin.countBackupFiles()
                + " file(s) in "
                + plugin.getBackupsDirectoryPath()
        );
        sender.sendMessage(
            ChatColor.GRAY
                + "  Logs: "
                + ChatColor.WHITE
                + plugin.countClearableLogFiles()
                + " clearable file(s) in "
                + plugin.getLogsDirectoryPath()
                + ChatColor.GRAY
                + (plugin.hasPurgeHistoryLog() ? " + purge-history.log" : "")
        );
        sender.sendMessage(
            ChatColor.GRAY
                + "  Formatting: "
                + ChatColor.WHITE
                + "MiniMessage="
                + onOff(formattingSettings.parseMiniMessage())
                + ", legacy=&="
                + onOff(formattingSettings.convertLegacyAmpersandCodes())
                + ", strip="
                + onOff(formattingSettings.stripFormatting())
        );
        sender.sendMessage(
            ChatColor.GRAY
                + "  Listing: "
                + ChatColor.WHITE
                + "category="
                + onOff(listingSettings.showCategory())
                + ", enabled-only="
                + onOff(listingSettings.onlyShowEnabledPlaceholders())
                + ", description="
                + onOff(listingSettings.showCategoryDescription())
                + ", type="
                + onOff(listingSettings.showType())
                + ", count="
                + onOff(listingSettings.showCategoryCount())
        );
        sender.sendMessage(
            ChatColor.GRAY
                + "  Validation: "
                + (validationIssues.isEmpty() ? ChatColor.GREEN : ChatColor.YELLOW)
                + validationIssues.size()
                + " issue(s)"
        );
        if (!validationIssues.isEmpty()) {
            final int limit = Math.min(5, validationIssues.size());
            for (int index = 0; index < limit; index++) {
                sender.sendMessage(ChatColor.YELLOW + "    - " + validationIssues.get(index));
            }

            if (validationIssues.size() > limit) {
                sender.sendMessage(
                    ChatColor.GRAY + "    ... and " + (validationIssues.size() - limit) + " more. Fix config issues and reload to re-check."
                );
            }
        }
        sender.sendMessage(
            ChatColor.YELLOW
                + "  /_placeholders debug config"
                + ChatColor.WHITE
                + " - Add missing bundled default config nodes without overwriting existing values. Creates a backup first."
        );
        sender.sendMessage(
            ChatColor.YELLOW
                + "  /_placeholders debug clear logs"
                + ChatColor.WHITE
                + " - Clear log files while keeping the persistent purge-history.log record."
        );
        sender.sendMessage(
            ChatColor.YELLOW
                + "  /_placeholders debug clear backups"
                + ChatColor.WHITE
                + " - Clear saved backup files and audit who cleared them."
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
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + ChatColor.WHITE + " - List placeholders. Players get pages, console gets all results.");
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + " <page>" + ChatColor.WHITE + " - Players can jump to another page of placeholders.");
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + " list [category] [page]" + ChatColor.WHITE + " - List placeholders, optionally filtered by category.");
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + " get <key>" + ChatColor.WHITE + " - Show stored and live details for one placeholder.");
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + " get <category> <key>" + ChatColor.WHITE + " - Same as get, with an explicit category.");
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + " preview <key>" + ChatColor.WHITE + " - Show raw, formatted, and plain previews.");
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + " preview <category> <key>" + ChatColor.WHITE + " - Same as preview, with an explicit category.");
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + " search <text> [category] [page]" + ChatColor.WHITE + " - Search keys, descriptions, and values.");
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + " add [category:]<key> <value...>" + ChatColor.WHITE + " - Save a new static placeholder to config.yml.");
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + " add <category> <key> <value...>" + ChatColor.WHITE + " - Same as add, using an explicit existing category.");
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + " category <category> <true|false>" + ChatColor.WHITE + " - Enable or disable a placeholder category in config.yml.");
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + " set <key> <value...>" + ChatColor.WHITE + " - Update an existing static placeholder in config.yml.");
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + " set <category> <key> <value...>" + ChatColor.WHITE + " - Same as set, with an explicit category.");
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + " remove <key>" + ChatColor.WHITE + " - Remove a placeholder from config.yml.");
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + " remove <category> <key>" + ChatColor.WHITE + " - Same as remove, with an explicit category.");
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + " help" + ChatColor.WHITE + " - Show this help message.");
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + " reload" + ChatColor.WHITE + " - Reload placeholders from config.yml.");
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + " backup" + ChatColor.WHITE + " - Create a backup copy of config.yml.");
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + " debug" + ChatColor.WHITE + " - Show plugin diagnostics, settings, paths, and validation issues.");
        sender.sendMessage(
            ChatColor.YELLOW
                + "  /"
                + label
                + " debug config"
                + ChatColor.WHITE
                + " - Add any missing bundled default config nodes without overwriting existing values."
        );
        sender.sendMessage(
            ChatColor.YELLOW
                + "  /"
                + label
                + " debug clear <logs|backups>"
                + ChatColor.WHITE
                + " - Clear logs or backups and keep an audit trail."
        );
    }

    private boolean matchesQuery(final PlaceholderEntry entry, final String query) {
        final String haystack = (
            entry.key()
                + " "
                + "onemb_"
                + entry.key()
                + " "
                + "%onemb_"
                + entry.key()
                + "% "
                + entry.category()
                + " "
                + entry.description()
                + " "
                + plugin.getStoredValueSummary(entry)
                + " "
                + safeDisplay(plugin.getConfiguredOutput(entry))
        ).toLowerCase(Locale.ROOT);

        return haystack.contains(query);
    }

    private void sendFormattedPlaceholderEntries(
        final CommandSender sender,
        final List<PlaceholderEntry> entries,
        final ListingSettings listingSettings
    ) {
        if (!listingSettings.showCategory()) {
            for (PlaceholderEntry entry : entries) {
                sender.sendMessage(buildPlaceholderLine(entry, listingSettings, false));
            }
            return;
        }

        final Map<String, List<PlaceholderEntry>> groupedEntries = new LinkedHashMap<>();
        for (PlaceholderEntry entry : entries) {
            groupedEntries.computeIfAbsent(entry.category(), ignored -> new ArrayList<>()).add(entry);
        }

        for (Map.Entry<String, List<PlaceholderEntry>> categoryEntry : groupedEntries.entrySet()) {
            sender.sendMessage(buildCategoryHeader(categoryEntry.getKey(), categoryEntry.getValue(), listingSettings));
            for (PlaceholderEntry entry : categoryEntry.getValue()) {
                sender.sendMessage(buildPlaceholderLine(entry, listingSettings, true));
            }
        }
    }

    private String buildCategoryHeader(
        final String categoryName,
        final List<PlaceholderEntry> entries,
        final ListingSettings listingSettings
    ) {
        final PlaceholderCategory category = plugin.getCategory(categoryName).orElse(null);
        final StringBuilder line = new StringBuilder(
            ChatColor.YELLOW + "  [" + categoryName + "]"
        );

        final List<String> metadata = new ArrayList<>();
        if (category != null && !listingSettings.onlyShowEnabledPlaceholders()) {
            metadata.add(category.enabled() ? "enabled" : "disabled");
        }

        if (listingSettings.showCategoryCount()) {
            metadata.add(entries.size() + " shown");
        }

        if (!metadata.isEmpty()) {
            line.append(ChatColor.GRAY).append(" (").append(String.join(", ", metadata)).append(")");
        }

        if (
            listingSettings.showCategoryDescription()
                && category != null
                && !category.description().isBlank()
        ) {
            line.append(ChatColor.GRAY).append(" - ").append(ChatColor.WHITE).append(category.description());
        }

        return line.toString();
    }

    private String buildPlaceholderLine(
        final PlaceholderEntry entry,
        final ListingSettings listingSettings,
        final boolean categoryAlreadyShown
    ) {
        final StringBuilder line = new StringBuilder();
        line.append(ChatColor.YELLOW);
        line.append(categoryAlreadyShown ? "    " : "  ");

        if (!categoryAlreadyShown && listingSettings.showCategory()) {
            line.append("[").append(entry.category()).append("] ");
        }

        if (listingSettings.showType()) {
            line.append(ChatColor.GRAY)
                .append("[")
                .append(entry.type().name().toLowerCase(Locale.ROOT))
                .append("] ")
                .append(ChatColor.YELLOW);
        }

        line.append("%onemb_").append(entry.key()).append("%");

        final String flags = describeEntryFlags(entry, categoryAlreadyShown);
        if (!flags.isBlank()) {
            line.append(ChatColor.GRAY).append(" [").append(flags).append("]");
        }

        line.append(ChatColor.GRAY).append(": ").append(ChatColor.WHITE).append("'").append(safeDisplay(plugin.getConfiguredOutput(entry))).append("'");
        return line.toString();
    }

    private String describeEntryFlags(final PlaceholderEntry entry, final boolean categoryAlreadyShown) {
        final List<String> flags = new ArrayList<>();

        if (!entry.categoryEnabled() && !categoryAlreadyShown) {
            flags.add("category disabled");
        }

        if (entry.type() == PlaceholderType.ROTATING && !entry.rotatingEnabled()) {
            flags.add("rotating disabled");
        }

        if (plugin.hasPendingReloadChange(entry.key())) {
            flags.add("pending reload");
        }

        if (flags.isEmpty()) {
            return "";
        }

        return String.join(", ", flags);
    }

    private String describeEntryFlags(final PlaceholderEntry entry) {
        final String flags = describeEntryFlags(entry, false);
        return flags.isBlank() ? "" : " [" + flags + "]";
    }

    private String[] splitCategoryAndKey(final String input) {
        if (!input.contains(":")) {
            return new String[] {"default", input};
        }

        final String[] parts = input.split(":", 2);
        return new String[] {
            parts[0].isBlank() ? "default" : parts[0],
            parts[1]
        };
    }

    private List<String> completeListArguments(final String[] args) {
        if (args.length == 2) {
            return matchToken(args[1], plugin.getCategoryNames());
        }

        if (args.length == 3 && plugin.hasCategory(args[1])) {
            return matchToken(args[2], List.of("1", "2", "3"));
        }

        return List.of();
    }

    private List<String> completeKeyArguments(final String[] args) {
        if (args.length == 2) {
            final List<String> suggestions = new ArrayList<>(
                plugin.getConfiguredPlaceholders().stream()
                    .map(PlaceholderEntry::key)
                    .sorted()
                    .toList()
            );
            suggestions.addAll(plugin.getCategoryNames());
            return matchToken(args[1], suggestions);
        }

        if (args.length == 3 && plugin.hasCategory(args[1])) {
            final String normalizedCategory = plugin.normalizeCategory(args[1]);
            return matchToken(
                args[2],
                plugin.getConfiguredPlaceholders().stream()
                    .filter(entry -> entry.category().equals(normalizedCategory))
                    .map(PlaceholderEntry::key)
                    .sorted()
                    .toList()
            );
        }

        return List.of();
    }

    private List<String> completeAddArguments(final String[] args) {
        if (args.length == 2) {
            return matchToken(args[1], plugin.getCategoryNames());
        }

        return List.of();
    }

    private List<String> completeCategoryArguments(final String[] args) {
        if (args.length == 2) {
            return matchToken(args[1], plugin.getCategoryNames());
        }

        if (args.length == 3 && plugin.hasCategory(args[1])) {
            return matchToken(args[2], List.of("true", "false"));
        }

        return List.of();
    }

    private List<String> completeSearchArguments(final String[] args) {
        if (args.length == 3) {
            return matchToken(args[2], plugin.getCategoryNames());
        }

        if (args.length == 4 && plugin.hasCategory(args[2])) {
            return matchToken(args[3], List.of("1", "2", "3"));
        }

        return List.of();
    }

    private List<String> completeDebugArguments(final String[] args) {
        if (args.length == 2) {
            return matchToken(args[1], List.of("config", "clear"));
        }

        if (args.length == 3 && "clear".equalsIgnoreCase(args[1])) {
            return matchToken(args[2], List.of("logs", "backups"));
        }

        return List.of();
    }

    private List<String> matchToken(final String input, final List<String> candidates) {
        final String normalizedInput = input.toLowerCase(Locale.ROOT);
        return candidates.stream()
            .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(normalizedInput))
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    private String[] slice(final String[] args, final int startInclusive, final int endExclusive) {
        final List<String> slice = new ArrayList<>();
        for (int index = startInclusive; index < endExclusive; index++) {
            slice.add(args[index]);
        }
        return slice.toArray(String[]::new);
    }

    private boolean isInteger(final String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private int parsePage(final String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private @Nullable Boolean parseBooleanArgument(final String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "on", "enable", "enabled", "yes" -> true;
            case "false", "off", "disable", "disabled", "no" -> false;
            default -> null;
        };
    }

    private String safeDisplay(final @Nullable String value) {
        return value == null ? "<not active>" : value;
    }

    private @Nullable PlaceholderEntry resolveConfiguredEntry(
        final CommandSender sender,
        final String[] args,
        final String commandName,
        final String usageMessage
    ) {
        if (args.length != 2 && !(args.length == 3 && plugin.hasCategory(args[1]))) {
            sender.sendMessage(ChatColor.RED + usageMessage);
            return null;
        }

        final String expectedCategory = args.length == 3 ? plugin.normalizeCategory(args[1]) : null;
        final String keyReference = args.length == 3 ? args[1] + ":" + args[2] : args[1];
        return resolveConfiguredEntry(sender, keyReference, expectedCategory, commandName);
    }

    private @Nullable PlaceholderEntry resolveConfiguredEntry(
        final CommandSender sender,
        final String keyReference,
        final @Nullable String expectedCategory,
        final String commandName
    ) {
        final String key = plugin.normalizePlaceholderReference(keyReference);
        final PlaceholderEntry entry = plugin.getConfiguredPlaceholderEntry(key).orElse(null);
        if (entry == null) {
            final String normalizedCategory = plugin.normalizeCategory(keyReference);
            if (
                expectedCategory == null
                    && !normalizedCategory.isBlank()
                    && key.equals(normalizedCategory)
                    && plugin.hasCategory(normalizedCategory)
            ) {
                sender.sendMessage(
                    ChatColor.RED
                        + "Category '"
                        + normalizedCategory
                        + "' was found, but a placeholder key is missing."
                );
                sender.sendMessage(
                    ChatColor.YELLOW
                        + "Usage: /_placeholders "
                        + commandName
                        + " "
                        + normalizedCategory
                        + " <placeholder>"
                );
                return null;
            }

            sender.sendMessage(ChatColor.RED + "Placeholder %onemb_" + key + "% was not found.");
            return null;
        }

        if (expectedCategory != null && !entry.category().equals(expectedCategory)) {
            sender.sendMessage(
                ChatColor.RED
                    + "Placeholder %onemb_"
                    + key
                    + "% exists in category '"
                    + entry.category()
                    + "', not '"
                    + expectedCategory
                    + "'."
            );
            return null;
        }

        return entry;
    }

    private String onOff(final boolean enabled) {
        return enabled ? "on" : "off";
    }

    private String getActorName(final CommandSender sender) {
        return sender.getName();
    }
}
