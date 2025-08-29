package dev.hxrry.hxcore.database;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;
import java.util.logging.Logger;

// factory class creates the right db based off the config choices. the ONLY class plugins interact with for db's

public class DatabaseFactory {
    
    /**
     * @param plugin The plugin requesting a database
     * @param databaseName Name for the database/file
     * @return Configured database ready to connect
     */

    public static Database create(JavaPlugin plugin, String databaseName) {
        Logger logger = plugin.getLogger();
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("database");
        
        // default to sqlite if no config
        if (config == null) {
            logger.info("No database config found, defaulting to SQLite");
            return new SQLiteDatabase(plugin.getDataFolder(), databaseName, logger);
        }
        
        String type = config.getString("type", "AUTO").toUpperCase();
        
        return switch (type) {
            case "SQLITE" -> createSQLite(plugin.getDataFolder(), databaseName, logger);
            case "POSTGRESQL" -> createPostgreSQL(config.getConfigurationSection("postgresql"), logger);
            case "AUTO" -> createAuto(plugin, databaseName, config, logger);
            default -> {
                logger.warning("Unknown database type: " + type + ", defaulting to SQLite");
                yield createSQLite(plugin.getDataFolder(), databaseName, logger);
            }
        };
    }
    
    // default sqlite
    private static Database createSQLite(File dataFolder, String name, Logger logger) {
        logger.info("Using SQLite database: " + name + ".db");
        return new SQLiteDatabase(dataFolder, name, logger);
    }

    private static Database createPostgreSQL(ConfigurationSection config, Logger logger) {
        if (config == null) {
            logger.severe("PostgreSQL selected but no configuration provided!");
            logger.info("Falling back to SQLite");
            return null;
        }
        
        // read config with defaults
        String host = config.getString("host", "localhost");
        int port = config.getInt("port", 5432);
        String database = config.getString("database", "minecraft");
        String username = config.getString("username", "postgres");
        String password = config.getString("password", "");
        int maxPoolSize = config.getInt("max-pool-size", 10);
        
        // validate
        if (password.isEmpty()) {
            logger.warning("PostgreSQL password is empty - this might fail!");
        }
        
        logger.info("Using PostgreSQL database: " + username + "@" + host + ":" + port + "/" + database);
        
        return new PostgreSQLDatabase(host, port, database, username, password, maxPoolSize, logger);
    }
    
    // auto mode implementation
    private static Database createAuto(JavaPlugin plugin, String databaseName, ConfigurationSection config, Logger logger) {
        logger.info("AUTO mode: detecting best database...");
        
        // try postgresql first if configured
        ConfigurationSection pgConfig = config.getConfigurationSection("postgresql");
        if (pgConfig != null && pgConfig.contains("password")) {
            Database postgres = createPostgreSQL(pgConfig, logger);
            if (postgres != null) {
                try {
                    // test connection
                    postgres.connect();
                    logger.info("AUTO mode: Using PostgreSQL (connection successful)");
                    return postgres;
                } catch (SQLException e) {
                    logger.warning("AUTO mode: PostgreSQL failed: " + e.getMessage());
                    logger.info("AUTO mode: Falling back to SQLite");
                    postgres.disconnect();
                }
            }
        }
        
        // sqlite fallback
        logger.info("AUTO mode: Using SQLite (default)");
        return createSQLite(plugin.getDataFolder(), databaseName, logger);
    }
    
    //docker related optimisations ?

    /**
     * @param database 
     * @param maxRetries 
     * @param retryDelayMs 
     * @return 
     */
    public static boolean connectWithRetry(Database database, int maxRetries, long retryDelayMs) {
        Logger logger = Logger.getLogger("DatabaseFactory");
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                database.connect();
                return true;
                
            } catch (SQLException e) {
                if (attempt == maxRetries) {
                    logger.severe("Failed to connect after " + maxRetries + " attempts: " + e.getMessage());
                    return false;
                }
                
                logger.warning("Connection attempt " + attempt + " failed, retrying in " + retryDelayMs + "ms: " + e.getMessage());
                
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        
        return false;
    }
}