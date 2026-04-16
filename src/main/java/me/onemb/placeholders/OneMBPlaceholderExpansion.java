package me.onemb.placeholders;

import java.util.Optional;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class OneMBPlaceholderExpansion extends PlaceholderExpansion {

    private final OneMBPlaceholdersPlugin plugin;

    public OneMBPlaceholderExpansion(final OneMBPlaceholdersPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getIdentifier() {
        return "onemb";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(final Player player, final @NotNull String params) {
        final Optional<String> placeholderValue = plugin.getPlaceholder(params);
        return placeholderValue.orElse(null);
    }
}
