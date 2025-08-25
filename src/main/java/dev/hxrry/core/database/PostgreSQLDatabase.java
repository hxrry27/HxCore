package dev.hxrry.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class PostgreSQLDatabase extends Database {
    
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final int maxPoolSize;
    
    private HikariDataSource dataSource;
    
    /**
     * @param host
     * @param port
     * @param database
     * @param username
     * @param password
     * @param maxPoolSize 10 to 20 max really
     * @param logger
     */

    public PostgreSQLDatabase(String host, int port, String database, String username, String password, int maxPoolSize, Logger logger) {
        super("PostgreSQL:" + database, logger);
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.maxPoolSize = maxPoolSize;
    }
    
    @Override
    public void connect() throws SQLException {
        HikariConfig config = new HikariConfig();
        
        config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(2); // 2 connections ready
        config.setIdleTimeout(600000); // 10 min
        config.setConnectionTimeout(30000); // 30 s
        config.setLeakDetectionThreshold(60000); // >1m connection hold
        
        // postgresql optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        
        config.setPoolName("HxCore-PostgreSQL-" + database);
        
        config.setConnectionTestQuery("SELECT 1");
        
        try {
            this.dataSource = new HikariDataSource(config);
            
            // test the connection
            try (Connection conn = dataSource.getConnection()) {
                logger.info("Connected to PostgreSQL database: " + database + " on " + host);
            }
        } catch (Exception e) {
            throw new SQLException("Failed to connect to PostgreSQL: " + e.getMessage(), e);
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
            dataSource.close();
            logger.info("Disconnected from PostgreSQL database");
        }
    }
    
    @Override
    public boolean isConnected() {
        if (dataSource == null || dataSource.isClosed()) {
            return false;
        }
        
        // actually test conn
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(1);
        } catch (SQLException e) {
            return false;
        }
    }

    public CompletableFuture<Void> upsertAsync(String table, String keyColumn, Object keyValue, Object... columnValuePairs) {
        // upsert helper because native support
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(table).append(" (");
        StringBuilder values = new StringBuilder("VALUES (");
        StringBuilder update = new StringBuilder("ON CONFLICT (").append(keyColumn).append(") DO UPDATE SET ");
        
        sql.append(keyColumn).append(", ");
        values.append("?, ");
        
        for (int i = 0; i < columnValuePairs.length; i += 2) {
            String column = (String) columnValuePairs[i];
            sql.append(column);
            values.append("?");
            update.append(column).append(" = EXCLUDED.").append(column);
            
            if (i < columnValuePairs.length - 2) {
                sql.append(", ");
                values.append(", ");
                update.append(", ");
            }
        }
        
        sql.append(") ");
        values.append(") ");
        
        String query = sql.toString() + values.toString() + update.toString();
        
        // build params array
        Object[] params = new Object[columnValuePairs.length / 2 + 1];
        params[0] = keyValue;
        for (int i = 0, j = 1; i < columnValuePairs.length; i += 2, j++) {
            params[j] = columnValuePairs[i + 1];
        }
        
        return updateAsync(query, params).thenApply(rows -> null);
    }
}