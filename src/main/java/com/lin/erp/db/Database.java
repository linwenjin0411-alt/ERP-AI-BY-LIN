package com.lin.erp.db;

import com.lin.erp.config.DbConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private Database() {
    }

    public static Connection connect(DbConfig config) throws SQLException {
        loadMysqlDriver();
        return DriverManager.getConnection(config.jdbcUrl(), config.getUsername(), config.getPassword());
    }

    private static void loadMysqlDriver() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException modernDriverMissing) {
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException legacyDriverMissing) {
                throw new SQLException("MySQL JDBC driver was not found. Put mysql-connector-j-*.jar into the lib folder.");
            }
        }
    }
}
