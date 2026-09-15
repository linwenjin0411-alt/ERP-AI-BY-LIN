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
    private static final Map<String, ModulePageData> DEMO_MODULES = createDemoModules();

    private final DbConfig config;

    public DbModuleRepository(DbConfig config) {
        this.config = config;
    }

    public List<ModulePageData> loadModules() throws SQLException {
        if (!config.isEnabled()) {
            return copyDemoModules();
        }
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
        if (!config.isEnabled()) {
            synchronized (DEMO_MODULES) {
                ModulePageData module = DEMO_MODULES.get(moduleCode);
                if (module == null) {
                    return 10;
                }
                int sortOrder = nextDemoSortOrder(module);
                module.addTableRow(sortOrder, copy(values));
                return sortOrder;
            }
        }
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
        if (!config.isEnabled()) {
            synchronized (DEMO_MODULES) {
                ModulePageData module = DEMO_MODULES.get(moduleCode);
                if (module != null) {
                    replaceDemoRow(module, sortOrder, values);
                }
            }
            return;
        }
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
        if (!config.isEnabled()) {
            synchronized (DEMO_MODULES) {
                ModulePageData module = DEMO_MODULES.get(moduleCode);
                if (module != null) {
                    int index = module.getTableRowSortOrders().indexOf(Integer.valueOf(sortOrder));
                    if (index >= 0 && index < module.getTableRows().size()) {
                        String[] row = module.getTableRows().get(index);
                        if (statusColumnIndex >= 0 && statusColumnIndex < row.length) {
                            row[statusColumnIndex] = statusValue;
                        }
                    }
                }
            }
            return;
        }
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

    private static Map<String, ModulePageData> createDemoModules() {
        Map<String, ModulePageData> modules = new LinkedHashMap<String, ModulePageData>();
        addDemoModule(modules, "DASHBOARD", "module.dashboard", "brand.flow", "DASHBOARD",
                new String[]{"action.refresh"},
                new String[]{"column.id", "column.status", "column.owner"},
                new String[][]{{"TODAY", "status.ready", "owner.system"}});
        addDemoModule(modules, "MASTER", "cap.master", "master.subtitle", "OPERATIONAL",
                defaultActions(),
                new String[]{"column.id", "column.item", "column.name", "column.status"},
                new String[][]{{"MAT-001", "FG-3007", "Smart actuator assembly", "status.open"}});
        addDemoModule(modules, "PROCUREMENT", "cap.procurement", "procurement.subtitle", "OPERATIONAL",
                workflowActions(),
                new String[]{"column.id", "column.supplier", "column.item", "column.qty", "column.status"},
                new String[][]{{"PO-DEMO-001", "SUP-2007", "RM-1008", "10", "status.open"}});
        addDemoModule(modules, "SALES", "cap.sales", "sales.subtitle", "OPERATIONAL",
                workflowActions(),
                new String[]{"column.id", "column.customer", "column.item", "column.qty", "column.status"},
                new String[][]{{"SO-DEMO-001", "CUS-3001", "FG-3007", "6", "status.open"}});
        addDemoModule(modules, "INVENTORY", "cap.inventory", "inventory.subtitle", "OPERATIONAL",
                defaultActions(),
                new String[]{"column.id", "column.item", "column.warehouse", "column.qty", "column.status"},
                new String[][]{{"STK-DEMO-001", "FG-3007", "FG-01", "24", "status.ready"}});
        addDemoModule(modules, "MANUFACTURING", "cap.manufacturing", "manufacturing.subtitle", "OPERATIONAL",
                workflowActions(),
                new String[]{"column.id", "column.item", "column.qty", "column.status", "column.next"},
                new String[][]{{"MO-DEMO-001", "FG-3007", "3", "status.open", "action.release"}});
        addDemoModule(modules, "FINANCE", "cap.finance", "finance.subtitle", "OPERATIONAL",
                defaultActions(),
                new String[]{"column.id", "column.customer", "column.amount", "column.status"},
                new String[][]{{"AR-DEMO-001", "CUS-3001", "600", "status.open"}});
        addDemoModule(modules, "AI", "cap.ai", "ai.subtitle", "AI",
                new String[]{"action.refresh", "action.ask", "action.export"},
                new String[]{"column.id", "column.status", "column.next"},
                new String[][]{{"AI-DEMO-001", "status.ready", "action.details"}});
        addDemoModule(modules, "ADMIN", "cap.admin", "admin.subtitle", "OPERATIONAL",
                defaultActions(),
                new String[]{"column.id", "column.owner", "column.status"},
                new String[][]{{"USR-admin", "owner.system", "status.open"}});
        return modules;
    }

    private static String[] defaultActions() {
        return new String[]{"action.refresh", "action.new", "action.edit", "action.export"};
    }

    private static String[] workflowActions() {
        return new String[]{"action.refresh", "action.new", "action.edit", "action.release", "action.post", "action.export"};
    }

    private static void addDemoModule(Map<String, ModulePageData> modules, String code, String titleKey, String subtitleKey,
                                      String pageType, String[] actions, String[] columns, String[][] rows) {
        ModulePageData module = new ModulePageData(code, titleKey, subtitleKey, pageType,
                "table.records", "panel.process", "panel.todo", null);
        for (String action : actions) {
            module.getActions().add(action);
        }
        module.getProcessSteps().add("process.create");
        module.getProcessSteps().add("process.review");
        module.getProcessSteps().add("process.complete");
        module.getMetrics().add(new ModulePageData.Metric("TOP", "panel.kpi", String.valueOf(rows.length), "status.ready", "accent"));
        for (String column : columns) {
            module.getTableColumns().add(column);
        }
        int sortOrder = 10;
        for (String[] row : rows) {
            module.addTableRow(sortOrder, copy(row));
            sortOrder += 10;
        }
        module.getFocusItems().add("Demo data is stored locally for this session.");
        modules.put(code, module);
    }

    private static List<ModulePageData> copyDemoModules() {
        synchronized (DEMO_MODULES) {
            List<ModulePageData> copies = new ArrayList<ModulePageData>();
            for (ModulePageData module : DEMO_MODULES.values()) {
                copies.add(copyModule(module));
            }
            return copies;
        }
    }

    private static ModulePageData copyModule(ModulePageData source) {
        ModulePageData copy = new ModulePageData(source.getCode(), source.getTitleKey(), source.getSubtitleKey(),
                source.getPageType(), source.getTableTitleKey(), source.getProcessTitleKey(),
                source.getFocusTitleKey(), source.getPromptValue());
        copy.getActions().addAll(source.getActions());
        copy.getProcessSteps().addAll(source.getProcessSteps());
        copy.getMetrics().addAll(source.getMetrics());
        copy.getTableColumns().addAll(source.getTableColumns());
        for (int i = 0; i < source.getTableRows().size(); i++) {
            copy.addTableRow(source.getTableRowSortOrders().get(i).intValue(), copy(source.getTableRows().get(i)));
        }
        copy.getFocusItems().addAll(source.getFocusItems());
        return copy;
    }

    private static int nextDemoSortOrder(ModulePageData module) {
        int max = 0;
        for (Integer sortOrder : module.getTableRowSortOrders()) {
            if (sortOrder != null && sortOrder.intValue() > max) {
                max = sortOrder.intValue();
            }
        }
        return max + 10;
    }

    private static void replaceDemoRow(ModulePageData module, int sortOrder, String[] values) {
        int index = module.getTableRowSortOrders().indexOf(Integer.valueOf(sortOrder));
        if (index >= 0 && index < module.getTableRows().size()) {
            module.getTableRows().set(index, copy(values));
        }
    }

    private static String[] copy(String[] values) {
        if (values == null) {
            return new String[0];
        }
        String[] copy = new String[values.length];
        System.arraycopy(values, 0, copy, 0, values.length);
        return copy;
    }
}
