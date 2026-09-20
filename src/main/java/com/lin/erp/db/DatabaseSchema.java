package com.lin.erp.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

final class DatabaseSchema {
    private DatabaseSchema() {
    }

    static void ensureColumn(Connection connection, String tableName, String columnName, String columnDefinition) throws SQLException {
        ResultSet columns = null;
        Statement statement = null;
        try {
            columns = connection.getMetaData().getColumns(connection.getCatalog(), null, tableName, columnName);
            if (columns.next()) {
                return;
            }
            statement = connection.createStatement();
            statement.executeUpdate("alter table " + tableName + " add column " + columnDefinition);
        } finally {
            if (columns != null) {
                columns.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
    }

    static void ensureVarcharLength(Connection connection, String tableName, String columnName,
                                    int minLength, String columnDefinition) throws SQLException {
        ResultSet columns = null;
        Statement statement = null;
        try {
            columns = connection.getMetaData().getColumns(connection.getCatalog(), null, tableName, columnName);
            if (columns.next() && columns.getInt("COLUMN_SIZE") >= minLength) {
                return;
            }
            statement = connection.createStatement();
            statement.executeUpdate("alter table " + tableName + " modify column " + columnDefinition);
        } finally {
            if (columns != null) {
                columns.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
    }

    static void ensureIndex(Connection connection, String tableName, String indexName, String indexDefinition) throws SQLException {
        ResultSet indexes = null;
        Statement statement = null;
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            indexes = metaData.getIndexInfo(connection.getCatalog(), null, tableName, false, false);
            while (indexes.next()) {
                String existing = indexes.getString("INDEX_NAME");
                if (existing != null && existing.equalsIgnoreCase(indexName)) {
                    return;
                }
            }
            statement = connection.createStatement();
            statement.executeUpdate("alter table " + tableName + " add " + indexDefinition);
        } finally {
            if (indexes != null) {
                indexes.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
    }

    static void ensureUtf8mb4(Connection connection, String tableName) throws SQLException {
        ResultSet tables = null;
        try {
            tables = connection.getMetaData().getTables(connection.getCatalog(), null, tableName, null);
            if (!tables.next()) {
                return;
            }
            String type = tables.getString("TABLE_TYPE");
            if (type == null || !"TABLE".equals(type.toUpperCase(Locale.ROOT))) {
                return;
            }
            Statement statement = null;
            try {
                statement = connection.createStatement();
                statement.executeUpdate("alter table " + tableName + " convert to character set utf8mb4 collate utf8mb4_unicode_ci");
            } catch (SQLException ignored) {
                // Some managed databases restrict table conversion. Existing column-level checks still protect new schema.
            } finally {
                if (statement != null) {
                    statement.close();
                }
            }
        } finally {
            if (tables != null) {
                tables.close();
            }
        }
    }
}
