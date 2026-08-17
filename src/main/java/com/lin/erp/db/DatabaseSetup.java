package com.lin.erp.db;

import com.lin.erp.config.DbConfig;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseSetup {
    public static void main(String[] args) throws Exception {
        DbConfig config = DbConfig.loadDefault();
        if (!config.isEnabled()) {
            System.out.println("Database is disabled. Set db.enabled=true in config/db.properties.");
            return;
        }
        new DatabaseInitializer(config).initialize();
        System.out.println("Database initialized: " + config.summary());
        printCount(config, "erp_users");
        printCount(config, "erp_modules");
        printCount(config, "erp_module_table_rows");
    }

    private static void printCount(DbConfig config, String tableName) throws Exception {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            connection = Database.connect(config);
            statement = connection.createStatement();
            resultSet = statement.executeQuery("select count(*) from " + tableName);
            if (resultSet.next()) {
                System.out.println(tableName + ": " + resultSet.getInt(1));
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }
}
