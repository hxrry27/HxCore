package dev.hxrry.core.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class ConfigManager {
    
    private final JavaPlugin plugin;
    private final String fileName;
    private final File configFile;
    private FileConfiguration config;
    private FileConfiguration defaults;
    
    /**
     * @param plugin the plugin
     * @param fileName config file name (e.g., "config.yml", "messages.yml")
     */
    public ConfigManager(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.configFile = new File(plugin.getDataFolder(), fileName);
        
        // load defaults from resources
        loadDefaults();
    }
    
    public void load() {
        // create plugin folder if needed
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        // create config file if it doesn't exist
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
            plugin.getLogger().info("Created default " + fileName);
        }
        
        // load the config
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // set defaults (for any and all missing values)
        if (defaults != null) {
            config.setDefaults(defaults);
        }
        
        // check version and migrate if needed
        checkVersion();
    }

    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save " + fileName, e);
        }
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
        
        if (defaults != null) {
            config.setDefaults(defaults);
        }
    }

    private void loadDefaults() {
        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream != null) {
            defaults = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream)
            );
        }
    }

    private void checkVersion() {
        int currentVersion = config.getInt("config-version", 0);
        int expectedVersion = defaults != null ? defaults.getInt("config-version", 1) : 1;
        
        if (currentVersion < expectedVersion) {
            plugin.getLogger().warning(fileName + " is outdated (v" + currentVersion + " -> v" + expectedVersion + ")");
            
            // backup old config
            File backup = new File(plugin.getDataFolder(), fileName + ".backup-v" + currentVersion);
            configFile.renameTo(backup);
            plugin.getLogger().info("Backed up old config to " + backup.getName());
            
            // create new config with old values copied over
            plugin.saveResource(fileName, false);
            FileConfiguration newConfig = YamlConfiguration.loadConfiguration(configFile);
            
            // copy old values to new config (keeps user's settings)
            copyValues(config, newConfig);
            
            // update version
            newConfig.set("config-version", expectedVersion);
            
            // save and use new config
            try {
                newConfig.save(configFile);
                config = newConfig;
                plugin.getLogger().info("Migrated " + fileName + " to v" + expectedVersion);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to migrate config!");
            }
        }
    }

    private void copyValues(FileConfiguration from, FileConfiguration to) {
        Set<String> keys = from.getKeys(true);
        
        for (String key : keys) {
            // skip sections (not actual values)
            if (from.isConfigurationSection(key)) {
                continue;
            }
            
            // skip version key
            if (key.equals("config-version")) {
                continue;
            }
            
            // copy value if it exists in new config (don't add removed options)
            if (to.contains(key)) {
                to.set(key, from.get(key));
            }
        }
    }
    
    // ====== Convenience Methods ======
    
    // raw file config - for complex operations
    public FileConfiguration getConfig() {
        if (config == null) {
            load();
        }
        return config;
    }
    
    // auto handles colour codes etc. 
    public String getColoredString(String path) {
        String value = getConfig().getString(path);
        return value != null ? value.replace('&', '§') : null;
    }
    
    // gets a string with a default value
    public String getString(String path, String def) {
        return getConfig().getString(path, def);
    }
    
    // gets an integer with a default value
    public int getInt(String path, int def) {
        return getConfig().getInt(path, def);
    }

    // gets a boolean with a default value
    public boolean getBoolean(String path, boolean def) {
        return getConfig().getBoolean(path, def);
    }
    
    //  gets a string list
    public List<String> getStringList(String path) {
        return getConfig().getStringList(path);
    }
    
    // gets a config section
    public ConfigurationSection getSection(String path) {
        return getConfig().getConfigurationSection(path);
    }
    
    // set and immediate save
    public void set(String path, Object value) {
        getConfig().set(path, value);
        save();
    }
    
    // check if path exist
    public boolean has(String path) {
        return getConfig().contains(path);
    }
}