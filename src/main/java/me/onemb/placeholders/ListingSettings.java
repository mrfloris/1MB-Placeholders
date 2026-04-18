package me.onemb.placeholders;

import org.bukkit.configuration.file.FileConfiguration;

record ListingSettings(
    boolean showCategory,
    boolean onlyShowEnabledPlaceholders,
    boolean showCategoryDescription,
    boolean showType,
    boolean showCategoryCount
) {

    static ListingSettings fromConfig(final FileConfiguration config) {
        return new ListingSettings(
            config.getBoolean("Listing.show-category", true),
            config.getBoolean("Listing.only-show-enabled-placeholders", true),
            config.getBoolean("Listing.show-category-description", true),
            config.getBoolean("Listing.show-type", true),
            config.getBoolean("Listing.show-category-count", true)
        );
    }
}
