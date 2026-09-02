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
        boolean originalAutoCommit = true;
        boolean autoCommitChanged = false;
        try {
            connection = Database.connect(config);
            originalAutoCommit = connection.getAutoCommit();
            if (originalAutoCommit) {
                connection.setAutoCommit(false);
                autoCommitChanged = true;
            }
            statement = connection.createStatement();
            for (String sql : statements) {
                statement.execute(sql);
            }
            connection.commit();
        } catch (SQLException e) {
            rollback(connection);
            throw e;
        } finally {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                if (autoCommitChanged) {
                    connection.setAutoCommit(originalAutoCommit);
                }
                connection.close();
            }
        }
    }

    private void rollback(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Initialization is already failing; keep the original exception.
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

        return splitSqlStatements(sql);
    }

    private List<String> splitSqlStatements(String sql) {
        List<String> statements = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        String delimiter = ";";
        boolean singleQuote = false;
        boolean doubleQuote = false;
        boolean backtick = false;
        boolean blockComment = false;

        String[] lines = sql.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        for (String rawLine : lines) {
            String trimmed = rawLine.trim();
            if (!singleQuote && !doubleQuote && !backtick && !blockComment
                    && trimmed.toUpperCase().startsWith("DELIMITER ")) {
                delimiter = trimmed.substring("DELIMITER ".length()).trim();
                if (delimiter.length() == 0) {
                    delimiter = ";";
                }
                continue;
            }

            String line = rawLine + "\n";
            int index = 0;
            while (index < line.length()) {
                if (!singleQuote && !doubleQuote && !backtick) {
                    if (blockComment) {
                        int end = line.indexOf("*/", index);
                        if (end < 0) {
                            break;
                        }
                        blockComment = false;
                        index = end + 2;
                        continue;
                    }
                    if (line.startsWith("/*", index)) {
                        blockComment = true;
                        index += 2;
                        continue;
                    }
                    if (line.charAt(index) == '#'
                            || (line.startsWith("--", index) && isLineComment(line, index))) {
                        break;
                    }
                    if (line.startsWith(delimiter, index)) {
                        addStatement(statements, current);
                        index += delimiter.length();
                        continue;
                    }
                }

                char ch = line.charAt(index);
                current.append(ch);

                if (!doubleQuote && !backtick && ch == '\'' && !isEscaped(line, index)) {
                    singleQuote = !singleQuote;
                } else if (!singleQuote && !backtick && ch == '"' && !isEscaped(line, index)) {
                    doubleQuote = !doubleQuote;
                } else if (!singleQuote && !doubleQuote && ch == '`') {
                    backtick = !backtick;
                }
                index++;
            }
        }
        addStatement(statements, current);
        return statements;
    }

    private void addStatement(List<String> statements, StringBuilder current) {
        String statement = current.toString().trim();
        if (statement.length() > 0) {
            statements.add(statement);
        }
        current.setLength(0);
    }

    private boolean isLineComment(String line, int index) {
        int next = index + 2;
        return next >= line.length() || Character.isWhitespace(line.charAt(next));
    }

    private boolean isEscaped(String line, int index) {
        int slashCount = 0;
        int cursor = index - 1;
        while (cursor >= 0 && line.charAt(cursor) == '\\') {
            slashCount++;
            cursor--;
        }
        return slashCount % 2 == 1;
    }
}
