package dev.hxrry.core.database;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

// makes async stuff safer innit

public class QueryResult implements AutoCloseable {
    
    private final List<Map<String, Object>> rows;
    private final List<String> columns;
    private final int rowCount;
    
    /**
     * @param rs 
     */
    public QueryResult(ResultSet rs) throws SQLException {
        this.rows = new ArrayList<>();
        this.columns = new ArrayList<>();
        
        if (rs == null) {
            this.rowCount = 0;
            return;
        }
        
        try {
            // read columns
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            
            for (int i = 1; i <= columnCount; i++) {
                columns.add(meta.getColumnName(i));
            }
            // read rows
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                
                for (String column : columns) {
                    row.put(column, rs.getObject(column));
                }
                
                rows.add(row);
            }
            
            this.rowCount = rows.size();
            
        } finally {
            // ALWAYS close the rs
            rs.close();
        }
    }
    
    public boolean isEmpty() {
        return rows.isEmpty();
    }
    
    // gets rows count
    public int size() {
        return rowCount;
    }
    
    // gets first row
    public Optional<Map<String, Object>> first() {
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
    
    // gets all rows
    public List<Map<String, Object>> all() {
        return new ArrayList<>(rows);
    }
    
    // getter for single value from first row
    public <T> Optional<T> getValue(String column, Class<T> type) {
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        
        Object value = rows.get(0).get(column);
        if (value == null) {
            return Optional.empty();
        }
        
        try {
            return Optional.of(type.cast(value));
        } catch (ClassCastException e) {
            return Optional.empty();
        }
    }
    
    // iterate over all rows
    public void forEach(RowConsumer consumer) {
        for (Map<String, Object> row : rows) {
            consumer.accept(row);
        }
    }
    
    @FunctionalInterface
    public interface RowConsumer {
        void accept(Map<String, Object> row);
    }
    
    @Override
    public void close() {
        // nothing to close - we already read everything
    }
}