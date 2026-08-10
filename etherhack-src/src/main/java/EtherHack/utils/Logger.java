/*
 * Decompiled with CFR 0.152.
 */
package EtherHack.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE_PREFIX = "etherhack_";
    private static final long MAX_LOG_SIZE = 0xA00000L;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat fileFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static String currentLogFile = null;

    private static void initializeLogDirectory() {
        try {
            Path logPath = Paths.get(LOG_DIR, new String[0]);
            if (!Files.exists(logPath, new LinkOption[0])) {
                Files.createDirectories(logPath, new FileAttribute[0]);
            }
        }
        catch (IOException e) {
            System.err.println("[DeiClient] Failed to create logs directory: " + e.getMessage());
        }
    }

    private static String getCurrentLogFile() {
        if (currentLogFile == null || Logger.shouldRotateLog()) {
            currentLogFile = "logs/etherhack_" + fileFormat.format(new Date()) + ".log";
        }
        return currentLogFile;
    }

    private static boolean shouldRotateLog() {
        if (currentLogFile == null) {
            return true;
        }
        try {
            Path logPath = Paths.get(currentLogFile, new String[0]);
            if (!Files.exists(logPath, new LinkOption[0])) {
                return true;
            }
            return Files.size(logPath) > 0xA00000L;
        }
        catch (IOException e) {
            return true;
        }
    }

    private static void writeToFile(String level, String message) {
        try {
            String logFile = Logger.getCurrentLogFile();
            try (FileWriter fw = new FileWriter(logFile, true);
                 PrintWriter pw = new PrintWriter(fw);){
                String timestamp = dateFormat.format(new Date());
                pw.println("[" + timestamp + "] [" + level + "] " + message);
            }
        }
        catch (IOException e) {
            System.err.println("[DeiClient] Failed to write to log file: " + e.getMessage());
        }
    }

    public static void print(String var0) {
        System.out.println("[DeiClient]: " + var0);
        Logger.writeToFile("INFO", var0);
    }

    public static void printLog(String var0) {
        System.out.println((Object)("[DeiClient]: " + var0));
        Logger.writeToFile("INFO", var0);
    }

    public static void error(String message) {
        System.err.println("[DeiClient ERROR]: " + message);
        System.out.println((Object)("[DeiClient]: " + message));
        Logger.writeToFile("ERROR", message);
    }

    public static void error(String message, Throwable throwable) {
        String errorMsg = message + " - " + throwable.getMessage();
        System.err.println("[DeiClient ERROR]: " + errorMsg);
        System.out.println((Object)("[DeiClient]: " + errorMsg));
        Logger.writeToFile("ERROR", errorMsg);
        Logger.logException(throwable);
    }

    public static void crash(String message, Throwable throwable) {
        String crashMsg = "CRASH: " + message;
        System.err.println("[DeiClient CRASH]: " + crashMsg);
        System.out.println((Object)("[DeiClient CRASH]: " + crashMsg));
        Logger.writeToFile("CRASH", crashMsg);
        Logger.logException(throwable);
    }

    public static void logException(Throwable throwable) {
        try {
            String logFile = Logger.getCurrentLogFile();
            try (FileWriter fw = new FileWriter(logFile, true);
                 PrintWriter pw = new PrintWriter(fw);){
                String timestamp = dateFormat.format(new Date());
                pw.println("[" + timestamp + "] [EXCEPTION] Stack trace:");
                throwable.printStackTrace(pw);
                pw.println("--- End of stack trace ---");
            }
        }
        catch (IOException e) {
            System.err.println("[DeiClient] Failed to log exception: " + e.getMessage());
            throwable.printStackTrace();
        }
    }

    public static void warn(String message) {
        System.out.println("[DeiClient WARN]: " + message);
        System.out.println((Object)("[DeiClient]: " + message));
        Logger.writeToFile("WARN", message);
    }

    public static void debug(String message) {
        System.out.println("[DeiClient DEBUG]: " + message);
        Logger.writeToFile("DEBUG", message);
    }

    public static void printCredits() {
        System.out.println();
        System.out.println();
        System.out.println(" ____       _  ____ _ _            _   ");
        System.out.println("|  _ \\  ___(_)/ ___| (_) ___ _ __ | |_ ");
        System.out.println("| | | |/ _ \\ | |   | | |/ _ \\ '_ \\| __|");
        System.out.println("| |_| |  __/ | |___| | |  __/ | | | |_ ");
        System.out.println("|____/ \\___|_|\\____|_|_|\\___|_| |_|\\__|");
        System.out.println();
        System.out.println("           Powered by dei0");
        System.out.println();
    }

    static {
        Logger.initializeLogDirectory();
    }
}
