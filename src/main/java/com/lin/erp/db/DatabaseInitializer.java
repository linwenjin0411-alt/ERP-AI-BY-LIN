package com.lin.erp.db;

import com.lin.erp.config.DbConfig;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
            ensureSchemaVersionTable(connection);
            applyMigrations(connection, statement);
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

    private void ensureSchemaVersionTable(Connection connection) throws SQLException {
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.execute("create table if not exists schema_version ("
                    + "version varchar(80) primary key,"
                    + "description varchar(255),"
                    + "installed_at timestamp not null default current_timestamp"
                    + ") engine=InnoDB default charset=utf8mb4");
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    private void applyMigrations(Connection connection, Statement statement) throws SQLException {
        applyBundledMigrations(connection, statement);
        applyExternalMigrations(connection, statement);
    }

    private void applyExternalMigrations(Connection connection, Statement statement) throws SQLException {
        File dir = new File("database", "migrations");
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File first, File second) {
                return first.getName().compareTo(second.getName());
            }
        });
        for (File file : files) {
            if (!file.isFile() || !file.getName().toLowerCase().endsWith(".sql")) {
                continue;
            }
            String version = migrationVersion(file.getName());
            if (migrationApplied(connection, version)) {
                continue;
            }
            for (String sql : loadStatements(file)) {
                statement.execute(sql);
            }
            recordMigration(connection, version, file.getName());
        }
    }

    private void applyBundledMigrations(Connection connection, Statement statement) throws SQLException {
        String index = loadResourceText("database/migrations.index");
        if (index == null || index.trim().length() == 0) {
            return;
        }
        String[] lines = index.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        for (String line : lines) {
            String name = line == null ? "" : line.trim();
            if (name.length() == 0 || name.startsWith("#")) {
                continue;
            }
            String version = migrationVersion(name);
            if (migrationApplied(connection, version)) {
                continue;
            }
            String sql = loadResourceText("database/migrations/" + name);
            if (sql == null) {
                throw new SQLException("Missing bundled database migration: " + name);
            }
            for (String statementSql : splitSqlStatements(sql)) {
                statement.execute(statementSql);
            }
            recordMigration(connection, version, name);
        }
    }

    private boolean migrationApplied(Connection connection, String version) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement("select 1 from schema_version where version = ?");
            statement.setString(1, version);
            resultSet = statement.executeQuery();
            return resultSet.next();
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
    }

    private void recordMigration(Connection connection, String version, String description) throws SQLException {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(
                    "insert into schema_version (version, description) values (?, ?)"
            );
            statement.setString(1, version);
            statement.setString(2, description);
            statement.executeUpdate();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    private String migrationVersion(String fileName) {
        int marker = fileName.indexOf("__");
        String version = marker > 0 ? fileName.substring(0, marker) : fileName;
        return version.replace(".sql", "");
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

        return loadStatements(file);
    }

    private List<String> loadStatements(File file) throws SQLException {
        String sql;
        try {
            sql = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SQLException("Failed to read " + file.getPath() + ": " + e.getMessage(), e);
        }

        return splitSqlStatements(sql);
    }

    private String loadResourceText(String name) throws SQLException {
        InputStream input = DatabaseInitializer.class.getClassLoader().getResourceAsStream(name);
        if (input == null) {
            return null;
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        try {
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SQLException("Failed to read bundled resource " + name + ": " + e.getMessage(), e);
        } finally {
            try {
                input.close();
            } catch (IOException ignored) {
                // Reading already completed or failed.
            }
        }
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
