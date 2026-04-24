package me.onemb.placeholders;

import java.util.Optional;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class OneMBPlaceholderExpansion extends PlaceholderExpansion {

    private final OneMBPlaceholdersPlugin plugin;

    public OneMBPlaceholderExpansion(final OneMBPlaceholdersPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginAuthors());
    }

    @Override
    public @NotNull String getIdentifier() {
        return "onemb";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMetaSafe().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(final OfflinePlayer player, final @NotNull String params) {
        final Optional<String> placeholderValue = plugin.getPlaceholder(params);
        return placeholderValue.orElse(null);
    }
}
