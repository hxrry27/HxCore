package dev.hxrry.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

public class SQLiteDatabase extends Database {
    
    private final File databaseFile;
    private HikariDataSource dataSource;
    
    /**
     * @param dataFolder 
     * @param fileName 
     * @param logger 
     */

    public SQLiteDatabase(File dataFolder, String fileName, Logger logger) {
        super("SQLite:" + fileName, logger);
        
        // ensure data folder exists
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        // create database file path
        this.databaseFile = new File(dataFolder, fileName + ".db");
    }
    
    @Override
    public void connect() throws SQLException {
        // create file if it doesn't exist
        if (!databaseFile.exists()) {
            try {
                databaseFile.createNewFile();
                logger.info("Created new SQLite database: " + databaseFile.getName());
            } catch (IOException e) {
                throw new SQLException("Failed to create database file", e);
            }
        }
        
        HikariConfig config = new HikariConfig();
        
        config.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        
        // sqlite only supports 1 connection at a time for writes but multiple reads are fine
        config.setMaximumPoolSize(1);
        
        // connection pool name for logs
        config.setPoolName("HxCore-SQLite-" + databaseFile.getName());
        
        // sqlite-specific optimizations
        config.addDataSourceProperty("journal_mode", "WAL"); 
        config.addDataSourceProperty("synchronous", "NORMAL"); // balance of speed vs safety
        config.addDataSourceProperty("cache_size", "10000");
        config.addDataSourceProperty("busy_timeout", "5000"); // ms
        
        this.dataSource = new HikariDataSource(config);
        
        // run optimization commands
        try (Connection conn = dataSource.getConnection()) {
            conn.prepareStatement("PRAGMA foreign_keys = ON").execute();
            conn.prepareStatement("VACUUM").execute();
            logger.info("Connected to SQLite database: " + databaseFile.getName());
        }
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Database is not connected! Call connect() first.");
        }
        return dataSource.getConnection();
    }
    
    @Override
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            try (Connection conn = dataSource.getConnection()) {
                conn.prepareStatement("VACUUM").execute();
                logger.info("Optimized database before shutdown");
            } catch (SQLException e) {
                // not critical, ignore
            }
            
            dataSource.close();
            logger.info("Disconnected from SQLite database");
        }
    }
    
    @Override
    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }
    
    public File getDatabaseFile() {
        return databaseFile;
    }
    
    public double getSizeInMB() {
        return databaseFile.length() / (1024.0 * 1024.0);
    }
}