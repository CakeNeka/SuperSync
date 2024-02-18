package sync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    public static final String ERROR_LOG_PATH = "logs/error.log";
    public static final String SYNC_LOG_PATH = "logs/sync.log";

    private static void log(String msg, String logFile) {
        try {
            Path path = Path.of(logFile);
            Files.createDirectories(path.getParent());
            Files.writeString(path, getTime() + " " + msg + '\n', StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void logError(String msg) {
        log(msg, ERROR_LOG_PATH);
    }

    public static void logMessage(String msg) {
        log(msg, SYNC_LOG_PATH);
    }

    public static String getTime() {
        return DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.now());
    }
}
