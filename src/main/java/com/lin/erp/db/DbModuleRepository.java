package com.lin.erp.db;

import com.lin.erp.config.DbConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DbModuleRepository {
    private final DbConfig config;

    public DbModuleRepository(DbConfig config) {
        this.config = config;
    }

    public List<ModulePageData> loadModules() throws SQLException {
        Connection connection = null;
        try {
            connection = Database.connect(config);
            Map<String, ModulePageData> modules = loadModuleHeaders(connection);
            loadActions(connection, modules);
            loadProcessSteps(connection, modules);
            loadMetrics(connection, modules);
            loadTableColumns(connection, modules);
            loadTableRows(connection, modules);
            loadFocusItems(connection, modules);
            return new ArrayList<ModulePageData>(modules.values());
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    public int insertTableRow(String moduleCode, String[] values) throws SQLException {
        Connection connection = null;
        PreparedStatement nextStatement = null;
        ResultSet resultSet = null;
        PreparedStatement insertStatement = null;
        try {
            connection = Database.connect(config);
            nextStatement = connection.prepareStatement("select coalesce(max(sort_order), 0) + 10 from erp_module_table_rows where module_code = ?");
            nextStatement.setString(1, moduleCode);
            resultSet = nextStatement.executeQuery();
            int sortOrder = 10;
            if (resultSet.next()) {
                sortOrder = resultSet.getInt(1);
            }

            insertStatement = connection.prepareStatement(
                    "insert into erp_module_table_rows (module_code, sort_order, c1, c2, c3, c4, c5, c6, c7, c8) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            );
            insertStatement.setString(1, moduleCode);
            insertStatement.setInt(2, sortOrder);
            bindRowValues(insertStatement, 3, values);
            insertStatement.executeUpdate();
            return sortOrder;
        } finally {
            close(resultSet, nextStatement);
            if (insertStatement != null) {
                insertStatement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

    public void updateTableRow(String moduleCode, int sortOrder, String[] values) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = Database.connect(config);
            statement = connection.prepareStatement(
                    "update erp_module_table_rows set c1 = ?, c2 = ?, c3 = ?, c4 = ?, c5 = ?, c6 = ?, c7 = ?, c8 = ? "
                            + "where module_code = ? and sort_order = ?"
            );
            bindRowValues(statement, 1, values);
            statement.setString(9, moduleCode);
            statement.setInt(10, sortOrder);
            statement.executeUpdate();
        } finally {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

    public void updateTableRowStatus(String moduleCode, int sortOrder, int statusColumnIndex, String statusValue) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = Database.connect(config);
            String columnName = "c" + (statusColumnIndex + 1);
            statement = connection.prepareStatement(
                    "update erp_module_table_rows set " + columnName + " = ? where module_code = ? and sort_order = ?"
            );
            statement.setString(1, statusValue);
            statement.setString(2, moduleCode);
            statement.setInt(3, sortOrder);
            statement.executeUpdate();
        } finally {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

    private Map<String, ModulePageData> loadModuleHeaders(Connection connection) throws SQLException {
        Map<String, ModulePageData> modules = new LinkedHashMap<String, ModulePageData>();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement(
                    "select code, title_key, subtitle_key, page_type, table_title_key, process_title_key, focus_title_key, prompt_value "
                            + "from erp_modules where active = 1 order by sort_order"
            );
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                ModulePageData module = new ModulePageData(
                        resultSet.getString("code"),
                        resultSet.getString("title_key"),
                        resultSet.getString("subtitle_key"),
                        resultSet.getString("page_type"),
                        resultSet.getString("table_title_key"),
                        resultSet.getString("process_title_key"),
                        resultSet.getString("focus_title_key"),
                        resultSet.getString("prompt_value")
                );
                modules.put(module.getCode(), module);
            }
        } finally {
            close(resultSet, statement);
        }
        return modules;
    }

    private void loadActions(Connection connection, Map<String, ModulePageData> modules) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement("select module_code, action_key from erp_module_actions order by module_code, sort_order");
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                ModulePageData module = modules.get(resultSet.getString("module_code"));
                if (module != null) {
                    module.getActions().add(resultSet.getString("action_key"));
                }
            }
        } finally {
            close(resultSet, statement);
        }
    }

    private void loadProcessSteps(Connection connection, Map<String, ModulePageData> modules) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement("select module_code, label_value from erp_module_process_steps order by module_code, sort_order");
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                ModulePageData module = modules.get(resultSet.getString("module_code"));
                if (module != null) {
                    module.getProcessSteps().add(resultSet.getString("label_value"));
                }
            }
        } finally {
            close(resultSet, statement);
        }
    }

    private void loadMetrics(Connection connection, Map<String, ModulePageData> modules) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement(
                    "select module_code, location, label_value, metric_value, note_value, accent_code "
                            + "from erp_module_metrics order by module_code, location, sort_order"
            );
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                ModulePageData module = modules.get(resultSet.getString("module_code"));
                if (module != null) {
                    module.getMetrics().add(new ModulePageData.Metric(
                            resultSet.getString("location"),
                            resultSet.getString("label_value"),
                            resultSet.getString("metric_value"),
                            resultSet.getString("note_value"),
                            resultSet.getString("accent_code")
                    ));
                }
            }
        } finally {
            close(resultSet, statement);
        }
    }

    private void loadTableColumns(Connection connection, Map<String, ModulePageData> modules) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement("select module_code, column_key from erp_module_table_columns order by module_code, sort_order");
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                ModulePageData module = modules.get(resultSet.getString("module_code"));
                if (module != null) {
                    module.getTableColumns().add(resultSet.getString("column_key"));
                }
            }
        } finally {
            close(resultSet, statement);
        }
    }

    private void loadTableRows(Connection connection, Map<String, ModulePageData> modules) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement(
                    "select module_code, sort_order, c1, c2, c3, c4, c5, c6, c7, c8 from erp_module_table_rows order by module_code, sort_order"
            );
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                ModulePageData module = modules.get(resultSet.getString("module_code"));
                if (module != null) {
                    String[] row = new String[8];
                    int used = 0;
                    for (int i = 0; i < row.length; i++) {
                        row[i] = resultSet.getString(i + 3);
                        if (row[i] != null) {
                            used = i + 1;
                        }
                    }
                    String[] compact = new String[used];
                    System.arraycopy(row, 0, compact, 0, used);
                    module.addTableRow(resultSet.getInt("sort_order"), compact);
                }
            }
        } finally {
            close(resultSet, statement);
        }
    }

    private void loadFocusItems(Connection connection, Map<String, ModulePageData> modules) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement("select module_code, item_value from erp_module_focus_items order by module_code, sort_order");
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                ModulePageData module = modules.get(resultSet.getString("module_code"));
                if (module != null) {
                    module.getFocusItems().add(resultSet.getString("item_value"));
                }
            }
        } finally {
            close(resultSet, statement);
        }
    }

    private void close(ResultSet resultSet, PreparedStatement statement) throws SQLException {
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
        if (error != null) {
            throw error;
        }
    }

    private void bindRowValues(PreparedStatement statement, int startIndex, String[] values) throws SQLException {
        for (int i = 0; i < 8; i++) {
            String value = i < values.length ? values[i] : null;
            if (value == null || value.trim().length() == 0) {
                statement.setNull(startIndex + i, Types.VARCHAR);
            } else {
                statement.setString(startIndex + i, value.trim());
            }
        }
    }
}
