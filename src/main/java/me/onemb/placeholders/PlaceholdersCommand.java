package me.onemb.placeholders;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN_TEXT_SERIALIZER = PlainTextComponentSerializer.plainText();

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
            sendError(sender, "You do not have permission to use this command.");
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
                sendError(sender, "Unknown subcommand. Use /" + label + " help.");
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

        sendError(sender, "You do not have permission: " + permission);
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

    private void sendMiniMessage(final CommandSender sender, final String miniMessage) {
        sender.sendMessage(MINI_MESSAGE.deserialize(miniMessage));
    }

    private void sendError(final CommandSender sender, final String message) {
        sendMiniMessage(sender, "<red>" + MINI_MESSAGE.escapeTags(message) + "</red>");
    }

    private void sendSuccess(final CommandSender sender, final String message) {
        sendMiniMessage(sender, "<green>" + MINI_MESSAGE.escapeTags(message) + "</green>");
    }

    private void sendMutedLabelValue(final CommandSender sender, final String label, final String value) {
        sender.sendMessage(
            Component.text("  " + label + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, NamedTextColor.WHITE))
        );
    }

    private void sendCommandHelpLine(final CommandSender sender, final String commandText, final String description) {
        sender.sendMessage(
            Component.text("  " + commandText, NamedTextColor.YELLOW)
                .append(Component.text(" - " + description, NamedTextColor.WHITE))
        );
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
                sendError(sender, "Unknown category: " + args[index]);
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

        sendInteractivePlaceholderTitle(sender, "Placeholder", entry, safeDisplay(plugin.getConfiguredOutput(entry)));
        sendMutedLabelValue(sender, "Category", entry.category() + describeEntryFlags(entry));
        sendMutedLabelValue(sender, "Type", entry.type().name().toLowerCase(Locale.ROOT));
        if (!entry.description().isBlank()) {
            sendMutedLabelValue(sender, "Description", entry.description());
        }
        sendInteractiveValueLine(sender, "Stored", entry, safeDisplay(plugin.getStoredValueSummary(entry)));
        sendInteractiveValueLine(sender, "Configured output", entry, safeDisplay(plugin.getConfiguredOutput(entry)));
        sendInteractiveValueLine(sender, "Live output", entry, safeDisplay(plugin.getLiveOutput(entry.key())));
        sendMutedLabelValue(sender, "Pending reload", plugin.hasPendingReloadChange(entry.key()) ? "yes" : "no");
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

        sendInteractivePlaceholderTitle(sender, "Preview", entry, safeDisplay(plugin.getConfiguredOutput(entry)));
        sendInteractiveValueLine(sender, "Stored", entry, safeDisplay(plugin.getStoredValueSummary(entry)));
        sendInteractiveValueLine(sender, "Configured output", entry, safeDisplay(plugin.getConfiguredOutput(entry)));
        sendInteractiveValueLine(sender, "Formatted preview", entry, safeDisplay(plugin.getFormattedPreview(entry)));
        sendInteractiveValueLine(sender, "Plain preview", entry, safeDisplay(plugin.getPlainPreview(entry)));
        sendInteractiveValueLine(sender, "Live output", entry, safeDisplay(plugin.getLiveOutput(entry.key())));
    }

    private void handleSearch(final CommandSender sender, final String[] args) {
        if (args.length < 2) {
            sendError(sender, "Usage: /_placeholders search <text> [category] [page]");
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
            sendError(sender, "Search text could not be empty.");
            return;
        }

        if (!plugin.isSearchLengthValid(query)) {
            sendError(sender, "Search text may be at most 128 characters.");
            return;
        }

        sendPlaceholderList(sender, category, page, query);
    }

    private void handleAdd(final CommandSender sender, final String[] args) {
        if (args.length < 3) {
            sendError(sender, "Usage: /_placeholders add [category:]<key> <value...> or /_placeholders add <category> <key> <value...>");
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

        if (result.success()) {
            sendSuccess(sender, result.message());
        } else {
            sendError(sender, result.message());
        }
    }

    private void handleSet(final CommandSender sender, final String[] args) {
        if (args.length < 3) {
            sendError(sender, "Usage: /_placeholders set <key> <value...> or /_placeholders set <category> <key> <value...>");
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

        if (result.success()) {
            sendSuccess(sender, result.message());
        } else {
            sendError(sender, result.message());
        }
    }

    private void handleCategory(final CommandSender sender, final String[] args) {
        if (args.length != 3) {
            sendError(sender, "Usage: /_placeholders category <category> <true|false>");
            return;
        }

        final String category = plugin.normalizeCategory(args[1]);
        if (!plugin.hasCategory(category)) {
            sendError(sender, "Unknown category: " + args[1]);
            return;
        }

        final @Nullable Boolean enabled = parseBooleanArgument(args[2]);
        if (enabled == null) {
            sendError(sender, "Category state must be true or false.");
            sendMiniMessage(sender, "<yellow>Usage: /_placeholders category &lt;category&gt; &lt;true|false&gt;</yellow>");
            return;
        }

        final ActionResult result = plugin.setCategoryEnabled(category, enabled, getActorName(sender));
        if (result.success()) {
            sendSuccess(sender, result.message());
        } else {
            sendError(sender, result.message());
        }
    }

    private void handleRemove(final CommandSender sender, final String[] args) {
        if (args.length != 2 && !(args.length == 3 && plugin.hasCategory(args[1]))) {
            sendError(sender, "Usage: /_placeholders remove <key> or /_placeholders remove <category> <key>");
            return;
        }

        final String expectedCategory = args.length == 3 ? plugin.normalizeCategory(args[1]) : null;
        final String keyReference = args.length == 3 ? args[1] + ":" + args[2] : args[1];

        if (resolveConfiguredEntry(sender, keyReference, expectedCategory, "remove") == null) {
            return;
        }

        final ActionResult result = plugin.removePlaceholderFromConfig(keyReference, getActorName(sender));
        if (result.success()) {
            sendSuccess(sender, result.message());
        } else {
            sendError(sender, result.message());
        }
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
            Component.text("1MB Placeholders: ", NamedTextColor.GOLD)
                .append(
                    Component.text(
                        "(v"
                            + plugin.getPluginVersion()
                            + " build "
                            + plugin.getBuildNumber()
                            + " for "
                            + plugin.getMinecraftVersion()
                            + ")",
                        NamedTextColor.WHITE
                    )
                )
        );

        if (matches.isEmpty()) {
            sendMiniMessage(sender, "<gray>  No placeholders matched that request.</gray>");
            return;
        }

        final List<PlaceholderEntry> visibleEntries;
        if (sender instanceof Player) {
            final int totalPages = Math.max(1, (int) Math.ceil(matches.size() / (double) PAGE_SIZE));
            final int page = requestedPage <= 0 ? 1 : requestedPage;
            if (page < 1 || page > totalPages) {
                sendError(sender, "Page must be between 1 and " + totalPages + ".");
                return;
            }

            final int startIndex = (page - 1) * PAGE_SIZE;
            final int endIndex = Math.min(startIndex + PAGE_SIZE, matches.size());
            visibleEntries = matches.subList(startIndex, endIndex);

            sendMutedLabelValue(
                sender,
                "Showing",
                (startIndex + 1)
                    + "-"
                    + endIndex
                    + "/"
                    + matches.size()
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
            sendMutedLabelValue(
                sender,
                "Showing",
                visibleEntries.size() + "/" + matches.size() + " (console output, pagination disabled)"
            );
        }

        if (category != null) {
            sendMutedLabelValue(sender, "Category filter", category);
        }

        if (searchQuery != null) {
            sendMutedLabelValue(sender, "Search filter", searchQuery);
        }

        sendFormattedPlaceholderEntries(sender, visibleEntries, listingSettings);
    }

    private void reloadPlaceholders(final CommandSender sender) {
        plugin.reloadPlaceholders();
        plugin.audit(getActorName(sender), "RELOAD", "Reloaded placeholders from config.yml.");
        sendSuccess(sender, "Reloaded placeholders from config.yml.");
    }

    private void backupConfig(final CommandSender sender) {
        final ActionResult result = plugin.createBackup(getActorName(sender));
        if (result.success()) {
            sendSuccess(sender, result.message());
        } else {
            sendError(sender, result.message());
        }
    }

    private void handleDebug(final CommandSender sender, final String[] args) {
        if (args.length == 1) {
            sendDebugOverview(sender);
            return;
        }

        if (args.length == 2 && "config".equalsIgnoreCase(args[1])) {
            final ActionResult result = plugin.mergeMissingDefaultConfig(getActorName(sender));
            if (result.success()) {
                sendSuccess(sender, result.message());
            } else {
                sendError(sender, result.message());
            }
            return;
        }

        if (args.length == 2 && "permissions".equalsIgnoreCase(args[1])) {
            sendDebugPermissions(sender);
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

        sendError(sender, "Usage: /_placeholders debug [config|permissions|clear]");
        sendDebugOverview(sender);
    }

    private void handleDebugClear(final CommandSender sender, final String target) {
        final String normalizedTarget = target.toLowerCase(Locale.ROOT);
        final ActionResult result = switch (normalizedTarget) {
            case "logs" -> plugin.clearLogs(getActorName(sender));
            case "backups" -> plugin.clearBackups(getActorName(sender));
            default -> ActionResult.failure("Usage: /_placeholders debug clear <logs|backups>");
        };

        if (result.success()) {
            sendSuccess(sender, result.message());
        } else {
            sendError(sender, result.message());
        }
    }

    private void sendDebugClearHelp(final CommandSender sender) {
        sender.sendMessage(Component.text("1MB Placeholders debug clear:", NamedTextColor.GOLD));
        sendCommandHelpLine(
            sender,
            "/_placeholders debug clear logs",
            "Clear log files while keeping the persistent purge-history.log record."
        );
        sendCommandHelpLine(
            sender,
            "/_placeholders debug clear backups",
            "Clear saved backup files and audit who cleared them."
        );
    }

    private void sendDebugPermissions(final CommandSender sender) {
        sender.sendMessage(
            Component.text("1MB Placeholders permissions: ", NamedTextColor.GOLD)
                .append(
                    Component.text(
                        "(v" + plugin.getPluginVersion() + " build " + plugin.getBuildNumber() + ")",
                        NamedTextColor.WHITE
                    )
                )
        );
        sendMiniMessage(sender, "<gray>  Base permission:</gray>");
        sendCommandHelpLine(sender, "  onemb.placeholders.admin", "Required before any /_placeholders command can be used.");
        sendMiniMessage(sender, "<gray>  Functional permissions:</gray>");
        sendCommandHelpLine(sender, "  onemb.placeholders.view", "Allows list, get, and preview.");
        sendCommandHelpLine(sender, "  onemb.placeholders.search", "Allows search.");
        sendCommandHelpLine(sender, "  onemb.placeholders.edit", "Allows add, set, remove, and category toggle.");
        sendCommandHelpLine(sender, "  onemb.placeholders.reload", "Allows reload.");
        sendCommandHelpLine(sender, "  onemb.placeholders.backup", "Allows backup.");
        sendCommandHelpLine(sender, "  onemb.placeholders.debug", "Allows debug, debug config, debug permissions, and debug clear.");
        sendCommandHelpLine(sender, "  onemb.placeholders.*", "Grants all 1MB Placeholders permissions.");
    }

    private void sendDebugOverview(final CommandSender sender) {
        final ActionResult bundledConfigStatus = plugin.validateBundledDefaultConfig();
        final ActionResult currentConfigStatus = plugin.validateCurrentConfigFile();
        final FormattingSettings formattingSettings = plugin.getFormattingSettings();
        final ListingSettings listingSettings = plugin.getListingSettings();
        final List<String> validationIssues = plugin.getValidationIssues();

        sender.sendMessage(
            Component.text("1MB Placeholders debug: ", NamedTextColor.GOLD)
                .append(Component.text("(v" + plugin.getPluginVersion() + " build " + plugin.getBuildNumber() + ")", NamedTextColor.WHITE))
        );
        sendMutedLabelValue(sender, "Plugin", plugin.getPluginDisplayName() + " v" + plugin.getPluginVersion() + " build " + plugin.getBuildNumber());
        sendMutedLabelValue(sender, "Credits", "PyroTempus, mrfloris, and OpenAI");
        sendMutedLabelValue(sender, "Authors metadata", String.join(", ", plugin.getPluginAuthors()));
        sendMutedLabelValue(sender, "Built for", "Java " + plugin.getTargetJavaVersion() + " / Paper API " + plugin.getPaperApiVersion());
        sendMutedLabelValue(sender, "Declared api-version", plugin.getDeclaredApiVersion());
        sendMutedLabelValue(sender, "Jar compatibility floor", plugin.getMinecraftVersion());
        sendMutedLabelValue(sender, "Runtime Java", System.getProperty("java.version", "unknown"));
        sendMutedLabelValue(sender, "Server engine", plugin.getServer().getVersion());
        sendMutedLabelValue(sender, "Runtime Minecraft", plugin.getServer().getMinecraftVersion());
        sendMutedLabelValue(sender, "Data folder", plugin.getDataFolderPath().toString());
        sendMutedLabelValue(sender, "Config path", plugin.getConfigFilePath().toString());
        sendMutedLabelValue(
            sender,
            "Config file",
            plugin.getConfiguredCategoryCount()
                + " categories, "
                + plugin.getEnabledCategoryCount()
                + " enabled, "
                + plugin.getDisabledCategoryCount()
                + " disabled, "
                + plugin.getConfiguredPlaceholderCount()
                + " placeholders total"
        );
        sender.sendMessage(
            Component.text("  Current config status: ", NamedTextColor.GRAY)
                .append(Component.text(currentConfigStatus.message(), currentConfigStatus.success() ? NamedTextColor.GREEN : NamedTextColor.RED))
        );
        sendMutedLabelValue(sender, "Active placeholders", String.valueOf(plugin.getLivePlaceholderCount()));
        sender.sendMessage(
            Component.text("  Bundled defaults: ", NamedTextColor.GRAY)
                .append(Component.text(bundledConfigStatus.message(), bundledConfigStatus.success() ? NamedTextColor.GREEN : NamedTextColor.RED))
        );
        sendMutedLabelValue(sender, "Backups", plugin.countBackupFiles() + " file(s) in " + plugin.getBackupsDirectoryPath());
        sendMutedLabelValue(
            sender,
            "Logs",
            plugin.countClearableLogFiles()
                + " clearable file(s) in "
                + plugin.getLogsDirectoryPath()
                + (plugin.hasPurgeHistoryLog() ? " + purge-history.log" : "")
        );
        sendMutedLabelValue(
            sender,
            "Formatting",
            "MiniMessage="
                + onOff(formattingSettings.parseMiniMessage())
                + ", legacy=&="
                + onOff(formattingSettings.convertLegacyAmpersandCodes())
                + ", strip="
                + onOff(formattingSettings.stripFormatting())
        );
        sendMutedLabelValue(
            sender,
            "Listing",
            "category="
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
            Component.text("  Validation: ", NamedTextColor.GRAY)
                .append(Component.text(validationIssues.size() + " issue(s)", validationIssues.isEmpty() ? NamedTextColor.GREEN : NamedTextColor.YELLOW))
        );
        if (!validationIssues.isEmpty()) {
            final int limit = Math.min(5, validationIssues.size());
            for (int index = 0; index < limit; index++) {
                sendMiniMessage(sender, "<yellow>    - " + MINI_MESSAGE.escapeTags(validationIssues.get(index)) + "</yellow>");
            }

            if (validationIssues.size() > limit) {
                sendMiniMessage(
                    sender,
                    "<gray>    ... and "
                        + (validationIssues.size() - limit)
                        + " more. Fix config issues and reload to re-check.</gray>"
                );
            }
        }
        sendCommandHelpLine(sender, "/_placeholders debug config", "Add missing bundled default config nodes without overwriting existing values. Creates a backup first.");
        sendCommandHelpLine(sender, "/_placeholders debug permissions", "List the permission nodes used by this plugin.");
        sendCommandHelpLine(sender, "/_placeholders debug clear logs", "Clear log files while keeping the persistent purge-history.log record.");
        sendCommandHelpLine(sender, "/_placeholders debug clear backups", "Clear saved backup files and audit who cleared them.");
    }

    private void sendHelp(final CommandSender sender, final String label) {
        sender.sendMessage(
            Component.text("1MB Placeholders commands: ", NamedTextColor.GOLD)
                .append(Component.text("(v" + plugin.getPluginVersion() + " build " + plugin.getBuildNumber() + ")", NamedTextColor.WHITE))
        );
        sendCommandHelpLine(sender, "/" + label, "List placeholders. Players get pages, console gets all results.");
        sendCommandHelpLine(sender, "/" + label + " <page>", "Players can jump to another page of placeholders.");
        sendCommandHelpLine(sender, "/" + label + " list [category] [page]", "List placeholders, optionally filtered by category.");
        sendCommandHelpLine(sender, "/" + label + " get <key>", "Show stored and live details for one placeholder.");
        sendCommandHelpLine(sender, "/" + label + " get <category> <key>", "Same as get, with an explicit category.");
        sendCommandHelpLine(sender, "/" + label + " preview <key>", "Show raw, formatted, and plain previews.");
        sendCommandHelpLine(sender, "/" + label + " preview <category> <key>", "Same as preview, with an explicit category.");
        sendCommandHelpLine(sender, "/" + label + " search <text> [category] [page]", "Search keys, descriptions, and values.");
        sendCommandHelpLine(sender, "/" + label + " add [category:]<key> <value...>", "Save a new static placeholder to config.yml.");
        sendCommandHelpLine(sender, "/" + label + " add <category> <key> <value...>", "Same as add, using an explicit existing category.");
        sendCommandHelpLine(sender, "/" + label + " category <category> <true|false>", "Enable or disable a placeholder category in config.yml.");
        sendCommandHelpLine(sender, "/" + label + " set <key> <value...>", "Update an existing static placeholder in config.yml.");
        sendCommandHelpLine(sender, "/" + label + " set <category> <key> <value...>", "Same as set, with an explicit category.");
        sendCommandHelpLine(sender, "/" + label + " remove <key>", "Remove a placeholder from config.yml.");
        sendCommandHelpLine(sender, "/" + label + " remove <category> <key>", "Same as remove, with an explicit category.");
        sendCommandHelpLine(sender, "/" + label + " help", "Show this help message.");
        sendCommandHelpLine(sender, "/" + label + " reload", "Reload placeholders from config.yml.");
        sendCommandHelpLine(sender, "/" + label + " backup", "Create a backup copy of config.yml.");
        sendCommandHelpLine(sender, "/" + label + " debug", "Show plugin diagnostics, settings, paths, and validation issues.");
        sendCommandHelpLine(sender, "/" + label + " debug config", "Add any missing bundled default config nodes without overwriting existing values.");
        sendCommandHelpLine(sender, "/" + label + " debug permissions", "List the permission nodes used by this plugin.");
        sendCommandHelpLine(sender, "/" + label + " debug clear <logs|backups>", "Clear logs or backups and keep an audit trail.");
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
                sendPlaceholderEntryLine(sender, entry, listingSettings, false);
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
                sendPlaceholderEntryLine(sender, entry, listingSettings, true);
            }
        }
    }

    private String buildCategoryHeader(
        final String categoryName,
        final List<PlaceholderEntry> entries,
        final ListingSettings listingSettings
    ) {
        final PlaceholderCategory category = plugin.getCategory(categoryName).orElse(null);
        final TextComponent.Builder line = Component.text()
            .append(Component.text("  [" + categoryName + "]", NamedTextColor.YELLOW));

        final List<String> metadata = new ArrayList<>();
        if (category != null && !listingSettings.onlyShowEnabledPlaceholders()) {
            metadata.add(category.enabled() ? "enabled" : "disabled");
        }

        if (listingSettings.showCategoryCount()) {
            metadata.add(entries.size() + " shown");
        }

        if (!metadata.isEmpty()) {
            line.append(Component.text(" (" + String.join(", ", metadata) + ")", NamedTextColor.GRAY));
        }

        if (
            listingSettings.showCategoryDescription()
                && category != null
                && !category.description().isBlank()
        ) {
            line.append(Component.text(" - ", NamedTextColor.GRAY));
            line.append(Component.text(category.description(), NamedTextColor.WHITE));
        }

        return LEGACY_SERIALIZER.serialize(line.build());
    }

    private String buildPlaceholderLine(
        final PlaceholderEntry entry,
        final ListingSettings listingSettings,
        final boolean categoryAlreadyShown
    ) {
        final TextComponent.Builder line = Component.text()
            .append(Component.text(categoryAlreadyShown ? "    " : "  ", NamedTextColor.YELLOW));

        if (!categoryAlreadyShown && listingSettings.showCategory()) {
            line.append(Component.text("[" + entry.category() + "] ", NamedTextColor.YELLOW));
        }

        if (listingSettings.showType()) {
            line.append(Component.text("[" + entry.type().name().toLowerCase(Locale.ROOT) + "] ", NamedTextColor.GRAY));
        }

        line.append(Component.text("%onemb_" + entry.key() + "%", NamedTextColor.YELLOW));

        final String flags = describeEntryFlags(entry, categoryAlreadyShown);
        if (!flags.isBlank()) {
            line.append(Component.text(" [" + flags + "]", NamedTextColor.GRAY));
        }

        line.append(Component.text(": ", NamedTextColor.GRAY));
        line.append(Component.text("'" + safeDisplay(plugin.getConfiguredOutput(entry)) + "'", NamedTextColor.WHITE));
        return LEGACY_SERIALIZER.serialize(line.build());
    }

    private void sendPlaceholderEntryLine(
        final CommandSender sender,
        final PlaceholderEntry entry,
        final ListingSettings listingSettings,
        final boolean categoryAlreadyShown
    ) {
        if (sender instanceof Player player) {
            player.sendMessage(buildPlaceholderComponent(entry, listingSettings, categoryAlreadyShown));
            return;
        }

        sender.sendMessage(buildPlaceholderLine(entry, listingSettings, categoryAlreadyShown));
    }

    private Component buildPlaceholderComponent(
        final PlaceholderEntry entry,
        final ListingSettings listingSettings,
        final boolean categoryAlreadyShown
    ) {
        final String displayValue = safeDisplay(plugin.getConfiguredOutput(entry));
        final TextComponent.Builder line = Component.text();
        line.append(Component.text(categoryAlreadyShown ? "    " : "  ", NamedTextColor.YELLOW));

        if (!categoryAlreadyShown && listingSettings.showCategory()) {
            line.append(Component.text("[" + entry.category() + "] ", NamedTextColor.YELLOW));
        }

        if (listingSettings.showType()) {
            line.append(Component.text("[" + entry.type().name().toLowerCase(Locale.ROOT) + "] ", NamedTextColor.GRAY));
        }

        line.append(buildInteractivePlaceholderComponent(entry, displayValue));

        final String flags = describeEntryFlags(entry, categoryAlreadyShown);
        if (!flags.isBlank()) {
            line.append(Component.text(" [" + flags + "]", NamedTextColor.GRAY));
        }

        line.append(Component.text(": ", NamedTextColor.GRAY));
        line.append(Component.text("'", NamedTextColor.WHITE));
        line.append(buildInteractiveValueComponent(entry, displayValue, "Click to paste shown value into the chat box."));
        line.append(Component.text("'", NamedTextColor.WHITE));
        return line.build();
    }

    private void sendInteractivePlaceholderTitle(
        final CommandSender sender,
        final String label,
        final PlaceholderEntry entry,
        final String displayValue
    ) {
        if (sender instanceof Player player) {
            player.sendMessage(
                Component.text(label + ": ", NamedTextColor.GOLD)
                    .append(buildInteractivePlaceholderComponent(entry, displayValue))
            );
            return;
        }

        sender.sendMessage(
            Component.text(label + ": ", NamedTextColor.GOLD)
                .append(Component.text(buildPlaceholderToken(entry), NamedTextColor.YELLOW))
        );
    }

    private void sendInteractiveValueLine(
        final CommandSender sender,
        final String label,
        final PlaceholderEntry entry,
        final String value
    ) {
        if (sender instanceof Player player) {
            player.sendMessage(
                Component.text("  " + label + ": ", NamedTextColor.GRAY)
                    .append(Component.text("'", NamedTextColor.WHITE))
                    .append(buildInteractiveValueComponent(entry, value, "Click to paste this value into the chat box."))
                    .append(Component.text("'", NamedTextColor.WHITE))
            );
            return;
        }

        sender.sendMessage(
            Component.text("  " + label + ": ", NamedTextColor.GRAY)
                .append(Component.text("'" + value + "'", NamedTextColor.WHITE))
        );
    }

    private Component buildInteractivePlaceholderComponent(final PlaceholderEntry entry, final String displayValue) {
        final String placeholderToken = buildPlaceholderToken(entry);
        return Component.text(placeholderToken, NamedTextColor.YELLOW)
            .clickEvent(ClickEvent.suggestCommand(placeholderToken))
            .hoverEvent(
                HoverEvent.showText(
                    Component.text()
                        .append(Component.text(placeholderToken, NamedTextColor.YELLOW))
                        .append(Component.newline())
                        .append(Component.text("Value: ", NamedTextColor.GRAY))
                        .append(Component.text("'" + abbreviateForHover(displayValue) + "'", NamedTextColor.WHITE))
                        .append(Component.newline())
                        .append(Component.text("Click to paste placeholder into the chat box.", NamedTextColor.GOLD))
                        .build()
                )
            );
    }

    private Component buildInteractiveValueComponent(
        final PlaceholderEntry entry,
        final String value,
        final String instruction
    ) {
        final Component base = "<not active>".equals(value)
            ? Component.text(value, NamedTextColor.WHITE)
            : LEGACY_SERIALIZER.deserialize(value);
        final TextComponent.Builder hoverText = Component.text()
            .append(Component.text("Value: ", NamedTextColor.GRAY))
            .append(Component.text("'" + abbreviateForHover(value) + "'", NamedTextColor.WHITE))
            .append(Component.newline())
            .append(Component.text(buildPlaceholderToken(entry), NamedTextColor.YELLOW));

        if ("<not active>".equals(value)) {
            return base.hoverEvent(
                HoverEvent.showText(
                    hoverText
                        .append(Component.newline())
                        .append(Component.text("This placeholder is not currently active.", NamedTextColor.GOLD))
                        .build()
                )
            );
        }

        return base.clickEvent(ClickEvent.suggestCommand(value))
            .hoverEvent(
                HoverEvent.showText(
                    hoverText
                        .append(Component.newline())
                        .append(Component.text(instruction, NamedTextColor.GOLD))
                        .build()
                )
            );
    }

    private String buildPlaceholderToken(final PlaceholderEntry entry) {
        return "%onemb_" + entry.key() + "%";
    }

    private String abbreviateForHover(final String value) {
        final String plainValue = PLAIN_TEXT_SERIALIZER.serialize(LEGACY_SERIALIZER.deserialize(value));
        return plainValue.length() <= 180 ? plainValue : plainValue.substring(0, 177) + "...";
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
            return matchToken(args[1], List.of("config", "permissions", "clear"));
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
            sendError(sender, usageMessage);
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
                sendError(sender, "Category '" + normalizedCategory + "' was found, but a placeholder key is missing.");
                sendMiniMessage(
                    sender,
                    "<yellow>Usage: /_placeholders "
                        + MINI_MESSAGE.escapeTags(commandName)
                        + " "
                        + MINI_MESSAGE.escapeTags(normalizedCategory)
                        + " &lt;placeholder&gt;</yellow>"
                );
                return null;
            }

            sendError(sender, "Placeholder %onemb_" + key + "% was not found.");
            return null;
        }

        if (expectedCategory != null && !entry.category().equals(expectedCategory)) {
            sendError(
                sender,
                "Placeholder %onemb_"
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
