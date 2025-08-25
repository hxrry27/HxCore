package dev.hxrry.core;

import dev.hxrry.core.cache.CacheManager;
import dev.hxrry.core.config.ConfigManager;
import dev.hxrry.core.database.Database;
import dev.hxrry.core.database.DatabaseFactory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class HxCore {

    private final JavaPlugin plugin;
    private final Logger logger;

    // core components
    private Database database;
    private CacheManager cacheManager;
    private final Map<String, ConfigManager> configs;

    // settings
    private boolean debug;

    /**
     * @param plugin The plugin using HxCore
     */
    public HxCore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.configs = new HashMap<>();
    }

    /** 
     * @return true if successful
     */
    public boolean initialize() {
        try {
            // load main config
            ConfigManager mainConfig = getConfig("config.yml");
            mainConfig.load();
            
            // set debug mode
            this.debug = mainConfig.getBoolean("debug", false);
            
            // initialize database
            String dbName = plugin.getName().toLowerCase().replace(" ", "_");
            database = DatabaseFactory.create(plugin, dbName);
            
            if (!DatabaseFactory.connectWithRetry(database, 3, 1000)) {
                logger.severe("Failed to connect to database!");
                return false;
            }
            
            // initialize cache manager
            boolean enableStats = mainConfig.getBoolean("cache.stats", false);
            cacheManager = new CacheManager(logger, enableStats);
            
            logger.info("HxCore initialized successfully for " + plugin.getName());
            return true;
            
        } catch (Exception e) {
            logger.severe("Failed to initialize HxCore: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void shutdown() {
        // clear caches
        if (cacheManager != null) {
            cacheManager.clearAll();
            
            // print final stats if in debug mode
            if (debug) {
                cacheManager.printStats();
            }
        }
        
        // close database
        if (database != null) {
            database.disconnect();
        }
        
        logger.info("HxCore shut down for " + plugin.getName());
    }
    
    /**
     * @param fileName config file name (e.g., "messages.yml")
     * @return config manager for that file
     */
    public ConfigManager getConfig(String fileName) {
        return configs.computeIfAbsent(fileName, 
            name -> new ConfigManager(plugin, name));
    }
    
    /**
     * @return
     */
    public Database getDatabase() {
        if (database == null) {
            throw new IllegalStateException("Database not initialized! Call initialize() first.");
        }
        return database;
    }
    
    /**
     * @return
     */
    public CacheManager getCacheManager() {
        if (cacheManager == null) {
            throw new IllegalStateException("Cache manager not initialized! Call initialize() first.");
        }
        return cacheManager;
    }
    
    /**
     * @return the plugin
     */
    public JavaPlugin getPlugin() {
        return plugin;
    }
    
    /**
     * @return true if debug mode on
     */
    public boolean isDebug() {
        return debug;
    }
    
    /**
     * @param message
     */
    public void debug(String message) {
        if (debug) {
            logger.info("[DEBUG] " + message);
        }
    }
}