package com.lin.erp.db;

import com.lin.erp.config.DbConfig;

import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;

public class DatabaseSetup {
    public static void main(String[] args) throws Exception {
        DbConfig config = DbConfig.loadDefault();
        if (!config.isEnabled()) {
            System.out.println("Database is disabled. Set db.enabled=true in config/db.properties.");
            return;
        }
        new DatabaseInitializer(config).initialize();
        System.out.println("Database initialized: " + config.summary());
        ensureInitialAdmin(config);
        printCount(config, "erp_users");
        printCount(config, "erp_modules");
        printCount(config, "erp_module_table_rows");
    }

    private static void ensureInitialAdmin(DbConfig config) throws Exception {
        DbUserRepository users = new DbUserRepository(config);
        if (users.countUsers() > 0) {
            return;
        }

        char[] password = readInitialAdminPassword();
        try {
            validateInitialAdminPassword(password);
            users.createInitialAdmin(hashPasswordHex(password));
            System.out.println("Initial administrator created. User ID: admin");
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static char[] readInitialAdminPassword() {
        String envPassword = System.getenv("LINOVA_ADMIN_PASSWORD");
        if (envPassword != null && envPassword.trim().length() > 0) {
            return envPassword.toCharArray();
        }

        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("No users exist. Set LINOVA_ADMIN_PASSWORD before running --init-db.");
        }
        char[] first = console.readPassword("Set initial admin password: ");
        char[] second = console.readPassword("Confirm initial admin password: ");
        try {
            if (!Arrays.equals(first, second)) {
                throw new IllegalArgumentException("Initial admin passwords do not match.");
            }
            return first == null ? new char[0] : first;
        } finally {
            if (second != null) {
                Arrays.fill(second, '\0');
            }
        }
    }

    private static void validateInitialAdminPassword(char[] password) {
        if (password == null || password.length < 8) {
            throw new IllegalArgumentException("Initial admin password must be at least 8 characters.");
        }
    }

    private static String hashPasswordHex(char[] password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(new String(password).getBytes(StandardCharsets.UTF_8));
        try {
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b & 0xff));
            }
            return builder.toString();
        } finally {
            Arrays.fill(hash, (byte) 0);
        }
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
