package me.onemb.placeholders;

import org.bukkit.configuration.file.FileConfiguration;

record FormattingSettings(
    boolean parseMiniMessage,
    boolean convertLegacyAmpersandCodes,
    boolean stripFormatting
) {

    static FormattingSettings fromConfig(final FileConfiguration config) {
        return new FormattingSettings(
            config.getBoolean("Formatting.parse-mini-message", false),
            config.getBoolean("Formatting.convert-legacy-ampersand-codes", false),
            config.getBoolean("Formatting.strip-formatting", false)
        );
    }
}
