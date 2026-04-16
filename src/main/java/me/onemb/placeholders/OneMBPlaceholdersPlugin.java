package me.onemb.placeholders;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class OneMBPlaceholdersPlugin extends JavaPlugin {

    private final BuildMetadata buildMetadata = BuildMetadata.load();
    private Map<String, String> placeholders = new LinkedHashMap<>();
    private OneMBPlaceholderExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        createDefaultConfigIfMissing();
        loadPlaceholdersFromConfig();
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
        getLogger().info("Loaded " + placeholders.size() + " custom placeholder(s).");
    }

    public void reloadPlaceholders() {
        reloadConfig();
        loadPlaceholdersFromConfig();
    }

    public Map<String, String> getPlaceholders() {
        return Collections.unmodifiableMap(placeholders);
    }

    public Optional<String> getPlaceholder(final String identifier) {
        return Optional.ofNullable(placeholders.get(identifier));
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

    private void createDefaultConfigIfMissing() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        final File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            saveDefaultConfig();
        }
    }

    private void loadPlaceholdersFromConfig() {
        final Map<String, String> loadedPlaceholders = new LinkedHashMap<>();
        final ConfigurationSection section = getConfig().getConfigurationSection("Placeholders");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                loadedPlaceholders.put(key, Objects.toString(section.get(key), ""));
            }
        }

        placeholders = loadedPlaceholders;
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
