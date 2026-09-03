package com.lin.erp.db;

import com.lin.erp.config.DbConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class DbFunctionRecordRepository {
    private final DbConfig config;

    public DbFunctionRecordRepository(DbConfig config) {
        this.config = config;
    }

    public List<FunctionRecord> loadRecords(String functionCode, String[][] seedRows) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = Database.connect(config);
            ensureSchema(connection);
            seedIfEmpty(connection, functionCode, seedRows);
            statement = connection.prepareStatement(
                    "select id, function_code, c1, c2, c3, c4, c5, c6, c7, c8 "
                            + "from erp_function_records where function_code = ? and active = 1 order by id"
            );
            statement.setString(1, functionCode);
            resultSet = statement.executeQuery();
            List<FunctionRecord> records = new ArrayList<FunctionRecord>();
            while (resultSet.next()) {
                records.add(new FunctionRecord(resultSet.getLong("id"), resultSet.getString("function_code"), values(resultSet)));
            }
            return records;
        } finally {
            close(resultSet, statement, connection);
        }
    }

    public long createRecord(String functionCode, String[] values) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet keys = null;
        try {
            connection = Database.connect(config);
            ensureSchema(connection);
            statement = connection.prepareStatement(
                    "insert into erp_function_records (function_code, c1, c2, c3, c4, c5, c6, c7, c8) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, functionCode);
            bind(statement, 2, values);
            statement.executeUpdate();
            keys = statement.getGeneratedKeys();
            return keys.next() ? keys.getLong(1) : -1;
        } finally {
            if (keys != null) {
                keys.close();
            }
            close(null, statement, connection);
        }
    }

    public void updateRecord(long id, String[] values) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = Database.connect(config);
            ensureSchema(connection);
            statement = connection.prepareStatement(
                    "update erp_function_records set c1 = ?, c2 = ?, c3 = ?, c4 = ?, c5 = ?, c6 = ?, c7 = ?, c8 = ? "
                            + "where id = ? and active = 1"
            );
            bind(statement, 1, values);
            statement.setLong(9, id);
            statement.executeUpdate();
        } finally {
            close(null, statement, connection);
        }
    }

    public void deleteRecord(long id) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = Database.connect(config);
            ensureSchema(connection);
            statement = connection.prepareStatement("update erp_function_records set active = 0 where id = ?");
            statement.setLong(1, id);
            statement.executeUpdate();
        } finally {
            close(null, statement, connection);
        }
    }

    private void ensureSchema(Connection connection) throws SQLException {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(
                    "create table if not exists erp_function_records ("
                            + "id bigint primary key auto_increment,"
                            + "function_code varchar(80) not null,"
                            + "c1 varchar(255), c2 varchar(255), c3 varchar(255), c4 varchar(255),"
                            + "c5 varchar(255), c6 varchar(255), c7 varchar(255), c8 varchar(255),"
                            + "active tinyint(1) not null default 1,"
                            + "created_at timestamp not null default current_timestamp,"
                            + "updated_at timestamp not null default current_timestamp on update current_timestamp,"
                            + "index idx_erp_function_records_code (function_code)"
                            + ") engine=InnoDB default charset=utf8mb4"
            );
            statement.executeUpdate();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        DatabaseSchema.ensureColumn(connection, "erp_function_records", "active", "active tinyint(1) not null default 1");
    }

    private void seedIfEmpty(Connection connection, String functionCode, String[][] seedRows) throws SQLException {
        PreparedStatement countStatement = null;
        ResultSet resultSet = null;
        try {
            countStatement = connection.prepareStatement("select count(*) from erp_function_records where function_code = ?");
            countStatement.setString(1, functionCode);
            resultSet = countStatement.executeQuery();
            if (resultSet.next() && resultSet.getInt(1) > 0) {
                return;
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (countStatement != null) {
                countStatement.close();
            }
        }
        if (seedRows == null) {
            return;
        }
        for (int i = 0; i < seedRows.length; i++) {
            createSeedRecord(connection, functionCode, seedRows[i]);
        }
    }

    private void createSeedRecord(Connection connection, String functionCode, String[] values) throws SQLException {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(
                    "insert into erp_function_records (function_code, c1, c2, c3, c4, c5, c6, c7, c8) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)"
            );
            statement.setString(1, functionCode);
            bind(statement, 2, values);
            statement.executeUpdate();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    private String[] values(ResultSet resultSet) throws SQLException {
        String[] values = new String[8];
        for (int i = 0; i < values.length; i++) {
            values[i] = resultSet.getString("c" + (i + 1));
        }
        return values;
    }

    private void bind(PreparedStatement statement, int startIndex, String[] values) throws SQLException {
        for (int i = 0; i < 8; i++) {
            String value = values == null || i >= values.length ? null : values[i];
            if (value == null || value.trim().length() == 0) {
                statement.setNull(startIndex + i, Types.VARCHAR);
            } else {
                statement.setString(startIndex + i, value.trim());
            }
        }
    }

    private void close(ResultSet resultSet, PreparedStatement statement, Connection connection) throws SQLException {
        SQLException error = null;
        try {
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (SQLException e) {
            error = e;
        }
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            error = e;
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            error = e;
        }
        if (error != null) {
            throw error;
        }
    }
}
