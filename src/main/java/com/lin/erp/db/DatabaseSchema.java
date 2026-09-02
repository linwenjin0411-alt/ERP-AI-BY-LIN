package com.lin.erp.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
}
