package me.onemb.placeholders;

import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class OneMBPlaceholdersPlugin extends JavaPlugin {

    private record ConfigLoadResult(YamlConfiguration configuration, String message) {}

    private static final Pattern VALID_KEY_PATTERN = Pattern.compile("^[a-z0-9_]+$");
    private static final Pattern BRACE_HEX_PATTERN = Pattern.compile("\\{#([A-Fa-f0-9]{6})}");
    private static final Pattern BRACE_NAME_PATTERN = Pattern.compile("\\{#([A-Za-z_]+)}");
    private static final int MAX_CATEGORY_LENGTH = 32;
    private static final int MAX_KEY_LENGTH = 64;
    private static final int MAX_VALUE_LENGTH = 512;
    private static final int MAX_SEARCH_LENGTH = 128;

    private final BuildMetadata buildMetadata = BuildMetadata.load();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();
    private final PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();

    private FormattingSettings formattingSettings = new FormattingSettings(true, true, false);
    private ListingSettings listingSettings = new ListingSettings(true, false, true, true, true);
    private FileConfiguration runtimeConfig = new YamlConfiguration();
    private List<String> validationIssues = List.of();
    private String lastConfigLoadMessage = "Config not loaded yet.";
    private Map<String, PlaceholderCategory> categories = new LinkedHashMap<>();
    private Map<String, PlaceholderEntry> configuredPlaceholders = new LinkedHashMap<>();
    private Map<String, PlaceholderEntry> livePlaceholders = new LinkedHashMap<>();
    private OneMBPlaceholderExpansion placeholderExpansion;
    private AuditLogger auditLogger;

    @Override
    public void onEnable() {
        createDefaultConfigIfMissing();
        auditLogger = new AuditLogger(getDataFolder().toPath(), getLogger());
        refreshPlaceholderState(true);
        registerCommands();
        registerPlaceholderExpansion();

        getLogger().info(
            "1MB-Placeholders v"
                + buildMetadata.pluginVersion()
                + " build "
                + buildMetadata.buildNumber()
                + " enabled for Minecraft "
                + buildMetadata.minecraftVersion()
                + " using Java "
                + buildMetadata.targetJava()
                + "."
        );
        getLogger().info(
            "Loaded "
                + configuredPlaceholders.size()
                + " configured placeholder(s), with "
                + livePlaceholders.size()
                + " currently active."
        );
    }

    public void reloadPlaceholders() {
        reloadConfig();
        refreshPlaceholderState(true);
    }

    public List<PlaceholderEntry> getConfiguredPlaceholders() {
        return List.copyOf(configuredPlaceholders.values());
    }

    public Optional<String> getPlaceholder(final String identifier) {
        final PlaceholderEntry placeholderEntry = livePlaceholders.get(normalizePlaceholderReference(identifier));
        if (placeholderEntry == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(getConfiguredOutput(placeholderEntry));
    }

    public String getPluginVersion() {
        return buildMetadata.pluginVersion();
    }

    public String getBuildNumber() {
        return buildMetadata.buildNumber();
    }

    public String getMinecraftVersion() {
        return buildMetadata.minecraftVersion();
    }

    public String getTargetJavaVersion() {
        return buildMetadata.targetJava();
    }

    public int getConfiguredPlaceholderCount() {
        return configuredPlaceholders.size();
    }

    public int getLivePlaceholderCount() {
        return livePlaceholders.size();
    }

    public int getConfiguredCategoryCount() {
        return categories.size();
    }

    public long getEnabledCategoryCount() {
        return categories.values().stream().filter(PlaceholderCategory::enabled).count();
    }

    public long getDisabledCategoryCount() {
        return categories.values().stream().filter(category -> !category.enabled()).count();
    }

    public ListingSettings getListingSettings() {
        return listingSettings;
    }

    public FormattingSettings getFormattingSettings() {
        return formattingSettings;
    }

    public List<String> getValidationIssues() {
        return List.copyOf(validationIssues);
    }

    public int getValidationIssueCount() {
        return validationIssues.size();
    }

    public String getLastConfigLoadMessage() {
        return lastConfigLoadMessage;
    }

    public Path getDataFolderPath() {
        return getDataFolder().toPath();
    }

    public Path getConfigFilePath() {
        return getDataFolderPath().resolve("config.yml");
    }

    public Path getBackupsDirectoryPath() {
        return getDataFolderPath().resolve("backups");
    }

    public Path getLogsDirectoryPath() {
        return getDataFolderPath().resolve("logs");
    }

    public long countBackupFiles() {
        return countRegularFiles(getBackupsDirectoryPath(), null);
    }

    public long countClearableLogFiles() {
        return countRegularFiles(getLogsDirectoryPath(), getLogsDirectoryPath().resolve("purge-history.log"));
    }

    public boolean hasPurgeHistoryLog() {
        return Files.isRegularFile(getLogsDirectoryPath().resolve("purge-history.log"));
    }

    public List<String> getCategoryNames() {
        return categories.keySet().stream().sorted().toList();
    }

    public boolean hasCategory(final String categoryName) {
        return categories.containsKey(normalizeCategory(categoryName));
    }

    public Optional<PlaceholderEntry> getConfiguredPlaceholderEntry(final String key) {
        return Optional.ofNullable(configuredPlaceholders.get(normalizePlaceholderReference(key)));
    }

    public Optional<PlaceholderEntry> getLivePlaceholderEntry(final String key) {
        return Optional.ofNullable(livePlaceholders.get(normalizePlaceholderReference(key)));
    }

    public Optional<PlaceholderCategory> getCategory(final String categoryName) {
        return Optional.ofNullable(categories.get(normalizeCategory(categoryName)));
    }

    public String getStoredValueSummary(final PlaceholderEntry entry) {
        return switch (entry.type()) {
            case STATIC -> entry.staticValue();
            case BUILTIN -> "<builtin:" + entry.builtinSource() + ">";
            case ROTATING -> String.join(" | ", entry.rotatingValues());
        };
    }

    public String getConfiguredOutput(final PlaceholderEntry entry) {
        final String resolvedValue = resolveRawValue(entry);
        if (resolvedValue == null) {
            return null;
        }

        return applyConfiguredFormatting(resolvedValue);
    }

    public String getLiveOutput(final String key) {
        return getLivePlaceholderEntry(key).map(this::getConfiguredOutput).orElse(null);
    }

    public String getFormattedPreview(final PlaceholderEntry entry) {
        final String resolvedValue = resolveRawValue(entry);
        if (resolvedValue == null) {
            return null;
        }

        return applyFormatting(resolvedValue, true, true, false);
    }

    public String getPlainPreview(final PlaceholderEntry entry) {
        final String resolvedValue = resolveRawValue(entry);
        if (resolvedValue == null) {
            return null;
        }

        return applyFormatting(resolvedValue, true, true, true);
    }

    public boolean hasPendingReloadChange(final String key) {
        final String normalizedReference = normalizePlaceholderReference(key);
        final PlaceholderEntry configuredEntry = configuredPlaceholders.get(normalizedReference);
        final PlaceholderEntry liveEntry = livePlaceholders.get(normalizedReference);

        if (configuredEntry == null && liveEntry == null) {
            return false;
        }

        if (configuredEntry == null || liveEntry == null) {
            return true;
        }

        return !configuredEntry.fingerprint().equals(liveEntry.fingerprint());
    }

    public String normalizeCategory(final String input) {
        return sanitizeIdentifier(input);
    }

    public String normalizeKey(final String input) {
        return sanitizeIdentifier(input);
    }

    public String normalizePlaceholderReference(final String input) {
        String trimmed = Objects.toString(input, "").trim().replace("%", "");
        if (trimmed.contains(":")) {
            trimmed = trimmed.substring(trimmed.indexOf(':') + 1);
        }
        if (trimmed.toLowerCase(Locale.ROOT).startsWith("onemb_")) {
            trimmed = trimmed.substring("onemb_".length());
        }

        return normalizeKey(trimmed);
    }

    public boolean isValidPlaceholderKey(final String key) {
        return VALID_KEY_PATTERN.matcher(key).matches();
    }

    public boolean isValidCategoryName(final String category) {
        return !category.isBlank() && category.length() <= MAX_CATEGORY_LENGTH && VALID_KEY_PATTERN.matcher(category).matches();
    }

    public String sanitizeCommandValue(final String input) {
        final String sanitized = Objects.toString(input, "")
            .replaceAll("[\\r\\n\\t]+", " ")
            .replaceAll("[\\p{Cntrl}&&[^\\u00A7]]", "")
            .trim();

        return sanitized.length() <= MAX_VALUE_LENGTH ? sanitized : sanitized.substring(0, MAX_VALUE_LENGTH);
    }

    public boolean isValueLengthValid(final String value) {
        return value.length() <= MAX_VALUE_LENGTH;
    }

    public boolean isSearchLengthValid(final String query) {
        return query.length() <= MAX_SEARCH_LENGTH;
    }

    public ActionResult addPlaceholderToConfig(
        final String requestedCategory,
        final String requestedKey,
        final String value,
        final String actor
    ) {
        final String category = normalizeCategory(requestedCategory);
        final String key = normalizePlaceholderReference(requestedKey);

        if (!isValueLengthValid(Objects.toString(value, ""))) {
            return ActionResult.failure("Placeholder values may be at most " + MAX_VALUE_LENGTH + " characters.");
        }

        final String sanitizedValue = sanitizeCommandValue(value);

        if (!isValidCategoryName(category)) {
            return ActionResult.failure("Category names may only use lowercase letters, numbers, and underscores, up to " + MAX_CATEGORY_LENGTH + " characters.");
        }

        if (!isValidPlaceholderKey(key) || key.length() > MAX_KEY_LENGTH) {
            return ActionResult.failure("Placeholder keys may only use lowercase letters, numbers, and underscores, up to " + MAX_KEY_LENGTH + " characters.");
        }

        if (sanitizedValue.isBlank()) {
            return ActionResult.failure("Placeholder values cannot be empty.");
        }

        if (configuredPlaceholders.containsKey(key)) {
            return ActionResult.failure("Placeholder %" + "onemb_" + key + "% already exists.");
        }

        final ConfigurationSection categoryPlaceholders = getOrCreateCategoryPlaceholdersSection(category);
        final String rootPath = categoryPlaceholders.getCurrentPath() + "." + key;
        getConfig().set(rootPath + ".type", "static");
        getConfig().set(rootPath + ".description", "");
        getConfig().set(rootPath + ".value", sanitizedValue);

        saveConfigAndRefreshConfiguredState();
        audit(actor, "ADD", "Saved %" + "onemb_" + key + "% to category '" + category + "' (reload required).");

        if (requestedKey.equals(key) && requestedCategory.equals(category)) {
            return ActionResult.success(
                "Saved %onemb_" + key + "% to category '" + category + "'. Run /_placeholders reload when ready."
            );
        }

        return ActionResult.success(
            "Saved %onemb_" + key + "% to category '" + category + "' (sanitized from '" + requestedCategory + ":" + requestedKey + "'). Run /_placeholders reload when ready."
        );
    }

    public ActionResult setPlaceholderInConfig(final String requestedKey, final String value, final String actor) {
        final String key = normalizePlaceholderReference(requestedKey);

        if (!isValueLengthValid(Objects.toString(value, ""))) {
            return ActionResult.failure("Placeholder values may be at most " + MAX_VALUE_LENGTH + " characters.");
        }

        final String sanitizedValue = sanitizeCommandValue(value);
        final PlaceholderEntry placeholderEntry = configuredPlaceholders.get(key);
        final String previousValue = placeholderEntry == null ? null : getConfiguredOutput(placeholderEntry);

        if (placeholderEntry == null) {
            return ActionResult.failure("Placeholder %onemb_" + key + "% was not found.");
        }

        if (placeholderEntry.type() != PlaceholderType.STATIC) {
            return ActionResult.failure("Only static placeholders can be changed with /_placeholders set.");
        }

        if (sanitizedValue.isBlank()) {
            return ActionResult.failure("Placeholder values cannot be empty.");
        }

        if (isSimpleValueNode(placeholderEntry.configPath())) {
            getConfig().set(placeholderEntry.configPath(), sanitizedValue);
        } else {
            getConfig().set(placeholderEntry.configPath() + ".value", sanitizedValue);
        }

        saveConfigAndRefreshConfiguredState();
        audit(actor, "SET", "Updated %onemb_" + key + "% in config.yml (reload required).");

        return ActionResult.success(
            "Saved %onemb_"
                + key
                + "% in category '"
                + placeholderEntry.category()
                + "' to config.yml (was '"
                + summarizeValueForMessage(previousValue)
                + "'). Run /_placeholders reload when ready."
        );
    }

    public ActionResult removePlaceholderFromConfig(final String requestedKey, final String actor) {
        final String key = normalizePlaceholderReference(requestedKey);
        final PlaceholderEntry placeholderEntry = configuredPlaceholders.get(key);

        if (placeholderEntry == null) {
            return ActionResult.failure("Placeholder %onemb_" + key + "% was not found.");
        }

        getConfig().set(placeholderEntry.configPath(), null);
        saveConfigAndRefreshConfiguredState();
        audit(actor, "REMOVE", "Removed %onemb_" + key + "% from config.yml (reload required).");

        return ActionResult.success(
            "Removed %onemb_"
                + key
                + "% from category '"
                + placeholderEntry.category()
                + "' (type "
                + placeholderEntry.type().name().toLowerCase(Locale.ROOT)
                + "). Run /_placeholders reload when ready."
        );
    }

    public ActionResult createBackup(final String actor) {
        final Path configPath = getDataFolder().toPath().resolve("config.yml");
        final Path backupsDirectory = getDataFolder().toPath().resolve("backups");

        try {
            Files.createDirectories(backupsDirectory);
            final String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));
            Path backupPath = backupsDirectory.resolve("config-" + timestamp + ".yml");
            int suffix = 1;

            while (Files.exists(backupPath)) {
                backupPath = backupsDirectory.resolve("config-" + timestamp + "-" + suffix + ".yml");
                suffix++;
            }

            Files.copy(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            audit(actor, "BACKUP", "Created backup " + backupPath.getFileName());
            return ActionResult.success("Created backup: " + backupPath.getFileName());
        } catch (IOException exception) {
            return ActionResult.failure("Unable to create backup: " + exception.getMessage());
        }
    }

    public ActionResult mergeMissingDefaultConfig(final String actor) {
        final ConfigLoadResult bundledConfigResult = loadBundledDefaultConfig();
        if (bundledConfigResult.configuration() == null) {
            return ActionResult.failure(bundledConfigResult.message());
        }

        final ConfigLoadResult currentConfigResult = loadCurrentConfigFile();
        if (currentConfigResult.configuration() == null) {
            return ActionResult.failure(currentConfigResult.message());
        }

        final YamlConfiguration bundledDefaults = bundledConfigResult.configuration();
        final YamlConfiguration currentConfig = currentConfigResult.configuration();
        final int missingNodes = countMissingNodes(bundledDefaults, currentConfig, "");
        if (missingNodes <= 0) {
            return ActionResult.success("config.yml already contains all bundled default nodes.");
        }

        final ActionResult backupResult = createBackup(actor);
        if (!backupResult.success()) {
            return backupResult;
        }

        final int addedNodes = mergeMissingNodes(bundledDefaults, currentConfig, "");

        try {
            currentConfig.save(getDataFolder().toPath().resolve("config.yml").toFile());
            reloadConfig();
            refreshPlaceholderState(false);
        } catch (IOException exception) {
            return ActionResult.failure("Unable to save merged config.yml: " + exception.getMessage());
        }

        audit(actor, "DEBUG_CONFIG", "Merged " + addedNodes + " missing default config node(s) into config.yml.");

        return ActionResult.success(
            "Added "
                + addedNodes
                + " missing default config node(s) to config.yml. "
                + backupResult.message()
                + " Run /_placeholders reload when ready."
        );
    }

    public ActionResult validateBundledDefaultConfig() {
        final ConfigLoadResult bundledConfigResult = loadBundledDefaultConfig();
        if (bundledConfigResult.configuration() == null) {
            return ActionResult.failure(bundledConfigResult.message());
        }

        return ActionResult.success("Bundled default config.yml is valid.");
    }

    public ActionResult validateCurrentConfigFile() {
        final ConfigLoadResult currentConfigResult = loadCurrentConfigFile();
        if (currentConfigResult.configuration() == null) {
            return ActionResult.failure(currentConfigResult.message());
        }

        return ActionResult.success(currentConfigResult.message());
    }

    public void audit(final String actor, final String action, final String details) {
        if (auditLogger != null) {
            auditLogger.log(actor, action, details);
        }
    }

    public void auditPersistent(final String actor, final String action, final String details) {
        if (auditLogger != null) {
            auditLogger.logPersistent(actor, action, details);
        }
    }

    public ActionResult clearBackups(final String actor) {
        final Path backupsDirectory = getDataFolder().toPath().resolve("backups");

        if (!Files.isDirectory(backupsDirectory)) {
            return ActionResult.success("No backup files were found.");
        }

        try (var files = Files.list(backupsDirectory)) {
            final List<Path> backupFiles = files.filter(Files::isRegularFile).toList();
            if (backupFiles.isEmpty()) {
                return ActionResult.success("No backup files were found.");
            }

            for (Path backupFile : backupFiles) {
                Files.deleteIfExists(backupFile);
            }

            final String details = "Cleared " + backupFiles.size() + " backup file(s).";
            auditPersistent(actor, "CLEAR_BACKUPS", details);
            audit(actor, "CLEAR_BACKUPS", details);
            return ActionResult.success(details);
        } catch (IOException exception) {
            return ActionResult.failure("Unable to clear backups: " + exception.getMessage());
        }
    }

    public ActionResult clearLogs(final String actor) {
        final Path logsDirectory = getDataFolder().toPath().resolve("logs");
        final Path persistentLogFile = logsDirectory.resolve("purge-history.log");
        final Path actionLogFile = logsDirectory.resolve("actions.log");

        if (!Files.isDirectory(logsDirectory)) {
            auditPersistent(actor, "CLEAR_LOGS", "Requested log clear, but logs directory did not exist.");
            audit(actor, "CLEAR_LOGS", "Requested log clear, but logs directory did not exist.");
            return ActionResult.success("No clearable log files were found.");
        }

        try (var files = Files.list(logsDirectory)) {
            final List<Path> logFiles = files
                .filter(Files::isRegularFile)
                .filter(path -> !path.equals(persistentLogFile))
                .toList();

            if (logFiles.isEmpty()) {
                auditPersistent(actor, "CLEAR_LOGS", "Requested log clear, but no clearable log files were found.");
                audit(actor, "CLEAR_LOGS", "Requested log clear, but no clearable log files were found.");
                return ActionResult.success("No clearable log files were found.");
            }

            auditPersistent(actor, "CLEAR_LOGS", "Cleared " + logFiles.size() + " log file(s) from logs/.");

            for (Path logFile : logFiles) {
                Files.deleteIfExists(logFile);
            }

            audit(actor, "CLEAR_LOGS", "Log files were cleared. Persistent history kept in purge-history.log.");

            if (!Files.exists(actionLogFile)) {
                audit(actor, "CLEAR_LOGS", "Recreated actions.log after clear.");
            }

            return ActionResult.success(
                "Cleared "
                    + logFiles.size()
                    + " log file(s). Persistent purge history was kept in purge-history.log."
            );
        } catch (IOException exception) {
            return ActionResult.failure("Unable to clear logs: " + exception.getMessage());
        }
    }

    private void createDefaultConfigIfMissing() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        final File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            saveDefaultConfig();
        }
    }

    private void refreshPlaceholderState(final boolean activateLivePlaceholders) {
        final ConfigLoadResult configResult = loadCurrentConfigFile();
        final FileConfiguration config = configResult.configuration() != null
            ? configResult.configuration()
            : new YamlConfiguration();

        if (configResult.configuration() == null) {
            getLogger().warning("Unable to load config.yml from disk: " + configResult.message());
        }

        final FormattingSettings loadedFormattingSettings = FormattingSettings.fromConfig(config);
        final ListingSettings loadedListingSettings = ListingSettings.fromConfig(config);
        final List<String> parsedValidationIssues = new ArrayList<>();
        final Map<String, PlaceholderCategory> parsedCategories = new LinkedHashMap<>();
        final Map<String, PlaceholderEntry> parsedConfiguredPlaceholders = new LinkedHashMap<>();
        final Map<String, PlaceholderEntry> parsedLivePlaceholders = new LinkedHashMap<>();
        final ConfigurationSection rootSection = config.getConfigurationSection("Placeholders");

        if (rootSection != null) {
            for (String childKey : rootSection.getKeys(false)) {
                if (isCategorySection(rootSection, childKey)) {
                    loadCategorySection(
                        childKey,
                        Objects.requireNonNull(rootSection.getConfigurationSection(childKey)),
                        parsedValidationIssues,
                        parsedCategories,
                        parsedConfiguredPlaceholders,
                        parsedLivePlaceholders
                    );
                } else {
                    loadLegacyDefaultPlaceholder(
                        childKey,
                        rootSection,
                        parsedValidationIssues,
                        parsedCategories,
                        parsedConfiguredPlaceholders,
                        parsedLivePlaceholders
                    );
                }
            }
        } else {
            parsedValidationIssues.add("Placeholders section is missing from config.yml.");
        }

        formattingSettings = loadedFormattingSettings;
        listingSettings = loadedListingSettings;
        runtimeConfig = config;
        validationIssues = List.copyOf(parsedValidationIssues);
        lastConfigLoadMessage = configResult.message();
        categories = parsedCategories;
        configuredPlaceholders = parsedConfiguredPlaceholders;

        if (activateLivePlaceholders) {
            livePlaceholders = parsedLivePlaceholders;
        }

        if (!validationIssues.isEmpty()) {
            getLogger().warning(
                "Config validation found " + validationIssues.size() + " issue(s). Use /_placeholders debug for details."
            );
        }
    }

    private ConfigLoadResult loadBundledDefaultConfig() {
        try (InputStream inputStream = getResource("config.yml")) {
            if (inputStream == null) {
                return new ConfigLoadResult(null, "Bundled default config.yml was not found in the plugin jar.");
            }

            return parseYamlText(
                new String(inputStream.readAllBytes(), StandardCharsets.UTF_8),
                "Bundled default config.yml"
            );
        } catch (IOException exception) {
            return new ConfigLoadResult(
                null,
                "Unable to read bundled default config.yml: " + summarizeConfigurationError(exception.getMessage())
            );
        }
    }

    private ConfigLoadResult loadCurrentConfigFile() {
        final Path configPath = getDataFolder().toPath().resolve("config.yml");
        if (!Files.exists(configPath)) {
            return new ConfigLoadResult(null, "Current config.yml was not found on disk.");
        }

        try {
            return parseYamlText(
                Files.readString(configPath, StandardCharsets.UTF_8),
                "Current config.yml"
            );
        } catch (IOException exception) {
            return new ConfigLoadResult(
                null,
                "Unable to read current config.yml: " + summarizeConfigurationError(exception.getMessage())
            );
        }
    }

    private ConfigLoadResult parseYamlText(final String yamlText, final String label) {
        try {
            final YamlConfiguration configuration = new YamlConfiguration();
            configuration.loadFromString(yamlText);
            return new ConfigLoadResult(configuration, label + " is valid.");
        } catch (InvalidConfigurationException exception) {
            return new ConfigLoadResult(
                null,
                label + " is invalid: " + summarizeConfigurationError(exception.getMessage())
            );
        }
    }

    private String summarizeConfigurationError(final String message) {
        final String normalized = Objects.toString(message, "unknown error")
            .replaceAll("\\s+", " ")
            .trim();
        return normalized.length() <= 220 ? normalized : normalized.substring(0, 220) + "...";
    }

    private int countMissingNodes(
        final ConfigurationSection defaultSection,
        final ConfigurationSection liveSection,
        final String basePath
    ) {
        int missingNodes = 0;

        for (String key : defaultSection.getKeys(false)) {
            final String path = basePath.isBlank() ? key : basePath + "." + key;
            final Object defaultValue = defaultSection.get(key);

            if (defaultValue instanceof ConfigurationSection nestedDefaultSection) {
                final boolean missingSection = !liveSection.isConfigurationSection(path);
                if (missingSection) {
                    missingNodes++;
                }

                missingNodes += countMissingNodes(
                    nestedDefaultSection,
                    liveSection,
                    path
                );
                continue;
            }

            if (!liveSection.contains(path)) {
                missingNodes++;
            }
        }

        return missingNodes;
    }

    private int mergeMissingNodes(
        final ConfigurationSection defaultSection,
        final ConfigurationSection liveSection,
        final String basePath
    ) {
        int addedNodes = 0;

        for (String key : defaultSection.getKeys(false)) {
            final String path = basePath.isBlank() ? key : basePath + "." + key;
            final Object defaultValue = defaultSection.get(key);

            if (defaultValue instanceof ConfigurationSection nestedDefaultSection) {
                if (!liveSection.isConfigurationSection(path)) {
                    liveSection.createSection(path);
                    addedNodes++;
                }

                addedNodes += mergeMissingNodes(nestedDefaultSection, liveSection, path);
                continue;
            }

            if (!liveSection.contains(path)) {
                liveSection.set(path, defaultValue);
                addedNodes++;
            }
        }

        return addedNodes;
    }

    private boolean isCategorySection(final ConfigurationSection rootSection, final String childKey) {
        final ConfigurationSection categorySection = rootSection.getConfigurationSection(childKey);
        return categorySection != null
            && (categorySection.contains("placeholders") || categorySection.contains("enabled") || categorySection.contains("description"));
    }

    private void loadCategorySection(
        final String rawCategoryName,
        final ConfigurationSection categorySection,
        final List<String> parsedValidationIssues,
        final Map<String, PlaceholderCategory> parsedCategories,
        final Map<String, PlaceholderEntry> parsedConfiguredPlaceholders,
        final Map<String, PlaceholderEntry> parsedLivePlaceholders
    ) {
        final String categoryName = normalizeCategory(rawCategoryName);
        if (!rawCategoryName.equals(categoryName)) {
            parsedValidationIssues.add(
                "Category '" + rawCategoryName + "' should use lowercase letters, numbers, and underscores."
            );
        }

        final PlaceholderCategory placeholderCategory = new PlaceholderCategory(
            categoryName,
            "Placeholders." + rawCategoryName,
            categorySection.getBoolean("enabled", true),
            categorySection.getString("description", "")
        );

        parsedCategories.put(categoryName, placeholderCategory);

        final ConfigurationSection placeholdersSection = categorySection.getConfigurationSection("placeholders");
        if (placeholdersSection == null) {
            parsedValidationIssues.add(
                "Category '" + rawCategoryName + "' is missing its placeholders section."
            );
            return;
        }

        for (String key : placeholdersSection.getKeys(false)) {
            final String configPath = "Placeholders." + rawCategoryName + ".placeholders." + key;
            final PlaceholderEntry entry = loadPlaceholderEntry(
                placeholdersSection,
                key,
                categoryName,
                placeholderCategory.enabled(),
                configPath,
                parsedValidationIssues
            );

            addParsedEntry(entry, parsedConfiguredPlaceholders, parsedLivePlaceholders, parsedValidationIssues);
        }
    }

    private void loadLegacyDefaultPlaceholder(
        final String key,
        final ConfigurationSection rootSection,
        final List<String> parsedValidationIssues,
        final Map<String, PlaceholderCategory> parsedCategories,
        final Map<String, PlaceholderEntry> parsedConfiguredPlaceholders,
        final Map<String, PlaceholderEntry> parsedLivePlaceholders
    ) {
        parsedValidationIssues.add(
            "Legacy placeholder '" + key + "' is still stored directly under Placeholders. Move it into a category."
        );
        parsedCategories.putIfAbsent(
            "default",
            new PlaceholderCategory("default", "Placeholders.default", true, "Legacy placeholders without a category.")
        );

        final PlaceholderEntry entry = new PlaceholderEntry(
            key,
            "default",
            "Placeholders." + key,
            "",
            PlaceholderType.STATIC,
            Objects.toString(rootSection.get(key), ""),
            "",
            true,
            true,
            List.of()
        );

        addParsedEntry(entry, parsedConfiguredPlaceholders, parsedLivePlaceholders, parsedValidationIssues);
    }

    private PlaceholderEntry loadPlaceholderEntry(
        final ConfigurationSection placeholdersSection,
        final String rawKey,
        final String categoryName,
        final boolean categoryEnabled,
        final String configPath,
        final List<String> parsedValidationIssues
    ) {
        if (placeholdersSection.isString(rawKey) || !placeholdersSection.isConfigurationSection(rawKey)) {
            parsedValidationIssues.add(
                "Placeholder '" + rawKey + "' in category '" + categoryName + "' is using legacy shorthand syntax."
            );
            return new PlaceholderEntry(
                rawKey,
                categoryName,
                configPath,
                "",
                PlaceholderType.STATIC,
                Objects.toString(placeholdersSection.get(rawKey), ""),
                "",
                categoryEnabled,
                true,
                List.of()
            );
        }

        final ConfigurationSection entrySection = Objects.requireNonNull(placeholdersSection.getConfigurationSection(rawKey));
        final String configuredType = entrySection.getString("type", "").toLowerCase(Locale.ROOT);
        final PlaceholderType placeholderType;
        if (!configuredType.isBlank() && !"static".equals(configuredType) && !"builtin".equals(configuredType) && !"rotating".equals(configuredType)) {
            parsedValidationIssues.add(
                "Placeholder '" + rawKey + "' in category '" + categoryName + "' uses unknown type '" + configuredType + "'."
            );
        }

        if (entrySection.contains("builtin") || "builtin".equals(configuredType)) {
            placeholderType = PlaceholderType.BUILTIN;
        } else if (entrySection.contains("values") || "rotating".equals(configuredType)) {
            placeholderType = PlaceholderType.ROTATING;
        } else {
            placeholderType = PlaceholderType.STATIC;
        }

        if (placeholderType == PlaceholderType.STATIC && !entrySection.contains("value")) {
            parsedValidationIssues.add(
                "Static placeholder '" + rawKey + "' in category '" + categoryName + "' is missing a value field."
            );
        }

        if (placeholderType == PlaceholderType.BUILTIN) {
            final String builtinSource = entrySection.getString("builtin", "");
            if (builtinSource.isBlank()) {
                parsedValidationIssues.add(
                    "Built-in placeholder '" + rawKey + "' in category '" + categoryName + "' is missing its builtin source."
                );
            } else if (!isSupportedBuiltinSource(builtinSource)) {
                parsedValidationIssues.add(
                    "Built-in placeholder '" + rawKey + "' in category '" + categoryName + "' uses unknown source '" + builtinSource + "'."
                );
            }
        }

        if (placeholderType == PlaceholderType.ROTATING) {
            if (entrySection.getStringList("values").isEmpty()) {
                parsedValidationIssues.add(
                    "Rotating placeholder '" + rawKey + "' in category '" + categoryName + "' has no values."
                );
            }
            if (entrySection.getInt("interval-seconds", 60) < 1) {
                parsedValidationIssues.add(
                    "Rotating placeholder '" + rawKey + "' in category '" + categoryName + "' has interval-seconds below 1."
                );
            }
        }

        return new PlaceholderEntry(
            rawKey,
            categoryName,
            configPath,
            entrySection.getString("description", ""),
            placeholderType,
            entrySection.getString("value", ""),
            entrySection.getString("builtin", ""),
            categoryEnabled,
            entrySection.getBoolean("rotating-enabled", true),
            entrySection.getStringList("values")
        );
    }

    private void addParsedEntry(
        final PlaceholderEntry entry,
        final Map<String, PlaceholderEntry> parsedConfiguredPlaceholders,
        final Map<String, PlaceholderEntry> parsedLivePlaceholders,
        final List<String> parsedValidationIssues
    ) {
        if (!isValidPlaceholderKey(entry.key())) {
            parsedValidationIssues.add(
                "Skipping invalid placeholder key '" + entry.key() + "' in category '" + entry.category() + "'."
            );
            getLogger().warning(
                "Skipping invalid placeholder key '" + entry.key() + "' in category '" + entry.category() + "'."
            );
            return;
        }

        final PlaceholderEntry existingEntry = parsedConfiguredPlaceholders.get(entry.key());
        if (existingEntry != null) {
            parsedValidationIssues.add(
                "Duplicate placeholder key '" + entry.key() + "' exists in categories '" + existingEntry.category() + "' and '" + entry.category() + "'."
            );
            getLogger().warning(
                "Skipping duplicate placeholder key '"
                    + entry.key()
                    + "' in category '"
                    + entry.category()
                    + "' because it already exists in category '"
                    + existingEntry.category()
                    + "'."
            );
            return;
        }

        parsedConfiguredPlaceholders.put(entry.key(), entry);

        if (entry.isLiveEligible()) {
            parsedLivePlaceholders.put(entry.key(), entry);
        }
    }

    private String resolveRawValue(final PlaceholderEntry entry) {
        return switch (entry.type()) {
            case STATIC -> entry.staticValue();
            case BUILTIN -> resolveBuiltinValue(entry.builtinSource());
            case ROTATING -> resolveRotatingValue(entry);
        };
    }

    private String resolveBuiltinValue(final String builtinSource) {
        final LocalDateTime now = LocalDateTime.now();
        final String normalizedSource = normalizeKey(builtinSource);

        return switch (normalizedSource) {
            case "plugin_version" -> getPluginVersion();
            case "plugin_build" -> getBuildNumber();
            case "plugin_version_build" -> "v" + getPluginVersion() + " build " + getBuildNumber();
            case "minecraft_version" -> getMinecraftVersion();
            case "java_version" -> System.getProperty("java.version", buildMetadata.targetJava());
            case "java_runtime_version" -> System.getProperty("java.runtime.version", System.getProperty("java.version", ""));
            case "paper_version" -> Objects.toString(getServer().getMinecraftVersion(), getMinecraftVersion());
            case "server_engine" -> Objects.toString(getServer().getVersion(), "");
            case "server_name" -> Objects.toString(getServer().getName(), "");
            case "online_players" -> String.valueOf(Bukkit.getOnlinePlayers().size());
            case "max_players" -> String.valueOf(Bukkit.getMaxPlayers());
            case "day_name" -> now.format(DateTimeFormatter.ofPattern("EEEE"));
            case "day_of_week" -> String.valueOf(now.getDayOfWeek().getValue());
            case "day_of_month" -> String.valueOf(now.getDayOfMonth());
            case "month_name" -> now.format(DateTimeFormatter.ofPattern("MMMM"));
            case "month_number" -> String.format("%02d", now.getMonthValue());
            case "year" -> String.valueOf(now.getYear());
            case "date_iso" -> now.toLocalDate().toString();
            case "time_24h" -> now.format(DateTimeFormatter.ofPattern("HH:mm"));
            default -> {
                getLogger().warning("Unknown built-in placeholder source: " + builtinSource);
                yield null;
            }
        };
    }

    private boolean isSupportedBuiltinSource(final String builtinSource) {
        return switch (normalizeKey(builtinSource)) {
            case "plugin_version",
                "plugin_build",
                "plugin_version_build",
                "minecraft_version",
                "java_version",
                "java_runtime_version",
                "paper_version",
                "server_engine",
                "server_name",
                "online_players",
                "max_players",
                "day_name",
                "day_of_week",
                "day_of_month",
                "month_name",
                "month_number",
                "year",
                "date_iso",
                "time_24h" -> true;
            default -> false;
        };
    }

    private String resolveRotatingValue(final PlaceholderEntry entry) {
        if (!entry.rotatingEnabled() || entry.rotatingValues().isEmpty()) {
            return null;
        }

        final ConfigurationSection entrySection = runtimeConfig.getConfigurationSection(entry.configPath());
        final int intervalSeconds = entrySection != null ? Math.max(1, entrySection.getInt("interval-seconds", 60)) : 60;
        final int index = Math.floorMod((int) (System.currentTimeMillis() / 1000L / intervalSeconds), entry.rotatingValues().size());
        return entry.rotatingValues().get(index);
    }

    private String applyConfiguredFormatting(final String value) {
        return applyFormatting(
            value,
            formattingSettings.parseMiniMessage(),
            formattingSettings.convertLegacyAmpersandCodes(),
            formattingSettings.stripFormatting()
        );
    }

    private String applyFormatting(
        final String value,
        final boolean parseMiniMessage,
        final boolean convertLegacyAmpersandCodes,
        final boolean stripFormatting
    ) {
        if (value == null) {
            return null;
        }

        String output = value;

        if (parseMiniMessage) {
            final Component component = miniMessage.deserialize(convertBraceColorSyntax(output));
            output = stripFormatting
                ? plainSerializer.serialize(component)
                : legacySerializer.serialize(component);
        }

        if (convertLegacyAmpersandCodes) {
            output = ChatColor.translateAlternateColorCodes('&', output);
        }

        if (stripFormatting) {
            output = ChatColor.stripColor(output);
        }

        return output;
    }

    private String convertBraceColorSyntax(final String input) {
        String output = BRACE_HEX_PATTERN.matcher(input).replaceAll("<#$1>");
        output = BRACE_NAME_PATTERN.matcher(output).replaceAll(matchResult -> switch (matchResult.group(1).toLowerCase(Locale.ROOT)) {
            case "orange" -> "<#FFA500>";
            case "gold" -> "<gold>";
            case "yellow" -> "<yellow>";
            case "green" -> "<green>";
            case "red" -> "<red>";
            case "blue" -> "<blue>";
            case "aqua" -> "<aqua>";
            case "white" -> "<white>";
            case "gray", "grey" -> "<gray>";
            default -> matchResult.group(0);
        });
        return output;
    }

    private String sanitizeIdentifier(final String input) {
        String sanitized = Normalizer.normalize(Objects.toString(input, ""), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .trim()
            .replaceAll("[\\s-]+", "_")
            .replaceAll("[^a-z0-9_]", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_+", "")
            .replaceAll("_+$", "");

        if (sanitized.isBlank()) {
            return "";
        }

        return sanitized;
    }

    private ConfigurationSection getOrCreateCategoryPlaceholdersSection(final String category) {
        final ConfigurationSection rootSection = getConfig().getConfigurationSection("Placeholders") != null
            ? Objects.requireNonNull(getConfig().getConfigurationSection("Placeholders"))
            : getConfig().createSection("Placeholders");
        final ConfigurationSection categorySection = rootSection.getConfigurationSection(category) != null
            ? Objects.requireNonNull(rootSection.getConfigurationSection(category))
            : rootSection.createSection(category);

        if (!categorySection.contains("enabled")) {
            categorySection.set("enabled", true);
        }

        if (!categorySection.contains("description")) {
            categorySection.set("description", "");
        }

        return categorySection.getConfigurationSection("placeholders") != null
            ? Objects.requireNonNull(categorySection.getConfigurationSection("placeholders"))
            : categorySection.createSection("placeholders");
    }

    private boolean isSimpleValueNode(final String configPath) {
        return !getConfig().isConfigurationSection(configPath);
    }

    private void saveConfigAndRefreshConfiguredState() {
        saveConfig();
        refreshPlaceholderState(false);
    }

    private long countRegularFiles(final Path directory, final Path excludedPath) {
        if (!Files.isDirectory(directory)) {
            return 0L;
        }

        try (var files = Files.list(directory)) {
            return files
                .filter(Files::isRegularFile)
                .filter(path -> excludedPath == null || !path.equals(excludedPath))
                .count();
        } catch (IOException exception) {
            return 0L;
        }
    }

    private String summarizeValueForMessage(final String value) {
        final String text = Objects.toString(value, "<empty>");
        return text.length() <= 80 ? text : text.substring(0, 80) + "...";
    }

    private void registerCommands() {
        final PlaceholdersCommand placeholdersCommand = new PlaceholdersCommand(this);
        final var placeholdersPluginCommand = Objects.requireNonNull(
            getCommand("_placeholders"),
            "Command /_placeholders is missing from plugin.yml."
        );
        placeholdersPluginCommand.setExecutor(placeholdersCommand);
        placeholdersPluginCommand.setTabCompleter(placeholdersCommand);
    }

    private void registerPlaceholderExpansion() {
        final PluginManager pluginManager = getServer().getPluginManager();
        if (!pluginManager.isPluginEnabled("PlaceholderAPI")) {
            getLogger().warning("PlaceholderAPI is not enabled. Placeholders will not register.");
            return;
        }

        if (placeholderExpansion == null) {
            placeholderExpansion = new OneMBPlaceholderExpansion(this);
        }

        final boolean registered = placeholderExpansion.register();
        if (!registered) {
            getLogger().warning("PlaceholderAPI expansion registration reported a failure.");
        }
    }
}
