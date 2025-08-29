package dev.hxrry.hxcore.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

//TODO: consider postgres to align with f1 projects

public abstract class Database {
    // thread pool for async database operations
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    
    protected final String name;
    protected final Logger logger;
    
    /**
     * @param name identifier for logging reasons
     * @param logger 
     */

    protected Database(String name, Logger logger) {
        this.name = name;
        this.logger = logger;
    }

    public abstract void connect() throws SQLException;
    
    public abstract Connection getConnection() throws SQLException;
    
    public abstract void disconnect();
    
    // basically health check implementation atp
    public abstract boolean isConnected();
    
    /**
     * @param sql CREATE TABLE IF NOT EXISTS statement
     */
    public void createTable(String sql) throws SQLException {
        // try-with-resources ensures connection and statement are closed
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
            logger.info("Ensured table exists for: " + name);
        }
    }
    
    /**
     * @param sql sql query with ? placeholders
     * @param params values to replace ? with
     * @return future containing query results
     */

    public CompletableFuture<QueryResult> queryAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                // Set parameters safely (prevents SQL injection)
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                
                ResultSet rs = stmt.executeQuery();
                // Wrap in QueryResult to handle result set safely
                return new QueryResult(rs);
                
            } catch (SQLException e) {
                logger.severe("Query failed: " + sql);
                throw new RuntimeException(e);
            }
        }, EXECUTOR);
    }
    
    public CompletableFuture<Integer> updateAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                // set parameters safely
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                
                return stmt.executeUpdate();
                
            } catch (SQLException e) {
                logger.severe("Update failed: " + sql);
                throw new RuntimeException(e);
            }
        }, EXECUTOR);
    }
    
    public CompletableFuture<Void> transactionAsync(TransactionCallback callback) {
        return CompletableFuture.runAsync(() -> {
            Connection conn = null;
            try {
                conn = getConnection();
                conn.setAutoCommit(false); // start transaction
                
                callback.execute(conn);
                
                conn.commit(); // if all successful, commit
                
            } catch (Exception e) {
                if (conn != null) {
                    try {
                        conn.rollback(); // error occurred, rollback everything
                        logger.warning("transaction rolled back due to error");
                    } catch (SQLException ex) {
                        logger.severe("failed to rollback transaction");
                    }
                }
                throw new RuntimeException(e);
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                        conn.close();
                    } catch (SQLException e) {
                        // ignore
                    }
                }
            }
        }, EXECUTOR);
    }
    
    @FunctionalInterface
    public interface TransactionCallback {
        void execute(Connection connection) throws SQLException;
    }
}