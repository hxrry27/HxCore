package dev.hxrry.hxcore.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * centralized logging utility for hx plugins
 */
public class Log {
    private static Logger logger = Bukkit.getLogger();
    private static String prefix = "";
    private static boolean debugEnabled = false;
    
    /**
     * initialize the logger with a plugin
     */
    public static void init(@NotNull Plugin plugin) {
        logger = plugin.getLogger();
        prefix = "[" + plugin.getName() + "] ";
        
        // check if debug is enabled in config
        debugEnabled = plugin.getConfig().getBoolean("debug", false);
    }
    
    /**
     * log info message
     */
    public static void info(@NotNull String message) {
        logger.info(message);
    }
    
    /**
     * log info message with formatting
     */
    public static void info(@NotNull String message, Object... args) {
        logger.info(String.format(message, args));
    }
    
    /**
     * log warning message
     */
    public static void warning(@NotNull String message) {
        logger.warning(message);
    }
    
    /**
     * log warning message with formatting
     */
    public static void warning(@NotNull String message, Object... args) {
        logger.warning(String.format(message, args));
    }
    
    /**
     * log error message
     */
    public static void error(@NotNull String message) {
        logger.severe(message);
    }
    
    /**
     * log error message with exception
     */
    public static void error(@NotNull String message, @NotNull Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }
    
    /**
     * log debug message (only if debug enabled)
     */
    public static void debug(@NotNull String message) {
        if (debugEnabled) {
            logger.info("[DEBUG] " + message);
        }
    }
    
    /**
     * log debug message with formatting
     */
    public static void debug(@NotNull String message, Object... args) {
        if (debugEnabled) {
            logger.info("[DEBUG] " + String.format(message, args));
        }
    }
    
    /**
     * enable or disable debug logging
     */
    public static void setDebug(boolean enabled) {
        debugEnabled = enabled;
        if (enabled) {
            logger.info("Debug logging enabled");
        }
    }
    
    /**
     * check if debug is enabled
     */
    public static boolean isDebugEnabled() {
        return debugEnabled;
    }
}