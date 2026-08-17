package com.lin.erp.db;

import com.lin.erp.config.DbConfig;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseInitializer {
    private final DbConfig config;

    public DatabaseInitializer(DbConfig config) {
        this.config = config;
    }

    public void initialize() throws SQLException {
        List<String> statements = loadSchemaStatements();
        Connection connection = null;
        Statement statement = null;
        try {
            connection = Database.connect(config);
            statement = connection.createStatement();
            for (String sql : statements) {
                statement.execute(sql);
            }
        } finally {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

    private List<String> loadSchemaStatements() throws SQLException {
        File file = new File("database", "schema.mysql.sql");
        if (!file.isFile()) {
            throw new SQLException("Missing database/schema.mysql.sql");
        }

        String sql;
        try {
            sql = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SQLException("Failed to read database/schema.mysql.sql: " + e.getMessage(), e);
        }

        StringBuilder cleaned = new StringBuilder();
        String[] lines = sql.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--") || trimmed.length() == 0) {
                continue;
            }
            cleaned.append(line).append('\n');
        }

        List<String> statements = new ArrayList<String>();
        String[] parts = cleaned.toString().split(";");
        for (String part : parts) {
            String statement = part.trim();
            if (statement.length() > 0) {
                statements.add(statement);
            }
        }
        return statements;
    }
}
