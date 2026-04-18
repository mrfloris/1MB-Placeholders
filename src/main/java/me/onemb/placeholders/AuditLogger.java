package me.onemb.placeholders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

final class AuditLogger {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    private final Logger logger;
    private final Path actionLogFile;
    private final Path maintenanceLogFile;

    AuditLogger(final Path dataFolder, final Logger logger) {
        this.logger = logger;
        this.actionLogFile = dataFolder.resolve("logs").resolve("actions.log");
        this.maintenanceLogFile = dataFolder.resolve("logs").resolve("purge-history.log");
    }

    void log(final String actor, final String action, final String details) {
        writeLine(actionLogFile, actor, action, details);
    }

    void logPersistent(final String actor, final String action, final String details) {
        writeLine(maintenanceLogFile, actor, action, details);
    }

    private void writeLine(final Path logFile, final String actor, final String action, final String details) {
        final String line = "["
            + ZonedDateTime.now().format(TIMESTAMP_FORMATTER)
            + "] "
            + actor
            + " | "
            + action
            + " | "
            + details
            + System.lineSeparator();

        try {
            Files.createDirectories(logFile.getParent());
            Files.writeString(logFile, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            logger.warning("Unable to write audit log entry: " + exception.getMessage());
        }
    }
}
