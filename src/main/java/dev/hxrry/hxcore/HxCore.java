package dev.hxrry.hxcore;

import org.bukkit.plugin.java.JavaPlugin;

import dev.hxrry.hxcore.cache.CacheManager;
import dev.hxrry.hxcore.config.ConfigManager;
import dev.hxrry.hxcore.database.Database;
import dev.hxrry.hxcore.database.DatabaseFactory;

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

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true; // means its foila cos this isn't ON paper or purpur or puffer etc.
        } catch (ClassNotFoundException e) {
            return false; // would like to hope this means its normal paper? ultimately just proves its NOT foila tho
        }
    }

    public HxCore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.configs = new HashMap<>();
    }

    public boolean initialize() {
        try {
            // foila smelly
            if (isFolia()) { 
                logger.severe("We don't support Folia soz!");
                return false;
            }
            
            // load main config
            ConfigManager mainConfig = getConfig("config.yml");
            mainConfig.load();

            // set debug mode
            this.debug = mainConfig.getBoolean("debug", false);      
            
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
    
    public ConfigManager getConfig(String fileName) {
        return configs.computeIfAbsent(fileName, 
            name -> new ConfigManager(plugin, name));
    }
    
    public Database getDatabase() {
        if (database == null) {
            String dbName = plugin.getName().toLowerCase().replace(" ", "_");
            
            Database db = DatabaseFactory.create(plugin, dbName);
            
            if (!DatabaseFactory.connectWithRetry(db, 3, 1000)) {
                throw new IllegalStateException("Failed to connect to the database :(");
            }
            
            database = db;
        }
        return database;
    }
    
    public CacheManager getCacheManager() {
        if (cacheManager == null) {
            boolean stats = getConfig("config.yml").getBoolean("cache.stats", false);
            cacheManager = new CacheManager(logger, stats);
        }
        return cacheManager;
    }
    
    public JavaPlugin getPlugin() {
        return plugin;
    }
    
    public boolean isDebug() {
        return debug;
    }
    
    public void debug(String message) {
        if (debug) {
            logger.info("[DEBUG] " + message);
        }
    }
}