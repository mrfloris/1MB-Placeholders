package me.onemb.placeholders;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

final class BuildMetadata {

    private final String pluginVersion;
    private final String buildNumber;
    private final String targetJava;
    private final String minecraftVersion;

    private BuildMetadata(
        final String pluginVersion,
        final String buildNumber,
        final String targetJava,
        final String minecraftVersion
    ) {
        this.pluginVersion = pluginVersion;
        this.buildNumber = buildNumber;
        this.targetJava = targetJava;
        this.minecraftVersion = minecraftVersion;
    }

    static BuildMetadata load() {
        final Properties properties = new Properties();

        try (InputStream inputStream = BuildMetadata.class.getClassLoader().getResourceAsStream("build-info.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load build metadata.", exception);
        }

        return new BuildMetadata(
            properties.getProperty("pluginVersion", "unknown"),
            properties.getProperty("buildNumber", "unknown"),
            properties.getProperty("targetJava", "unknown"),
            properties.getProperty("minecraftVersion", "unknown")
        );
    }

    String pluginVersion() {
        return pluginVersion;
    }

    String buildNumber() {
        return buildNumber;
    }

    String targetJava() {
        return targetJava;
    }

    String minecraftVersion() {
        return minecraftVersion;
    }
}
