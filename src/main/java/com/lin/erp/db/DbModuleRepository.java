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
    private static final int DEMO_MODULE_ROWS = 20;
    private static final String[] DEMO_ITEMS = {"FG-3007", "FG-3041", "RM-1008", "PK-2210", "RM-1304", "SP-1020"};
    private static final String[] DEMO_PARTNERS = {"Northwind Manufacturing", "Taiyo Robotics", "Sakura Metals", "Kanto Package", "Global Resin", "Apex Components"};
    private static final String[] DEMO_STATUSES = {"status.open", "status.released", "status.ready", "status.late", "status.blocked", "status.posted"};
    private static final String[] DEMO_NEXT_STEPS = {"term.shipment", "term.receipt", "term.materialIssue", "term.confirmation", "term.payable", "term.receivable", "term.gl"};
    private static final String[] DEMO_OWNERS = {"owner.sales", "owner.procurement", "owner.production", "owner.finance", "owner.planner", "owner.system"};

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

    public static int minimumDemoModuleRowCount() {
        int minimum = Integer.MAX_VALUE;
        synchronized (DEMO_MODULES) {
            for (ModulePageData module : DEMO_MODULES.values()) {
                minimum = Math.min(minimum, module.getTableRows().size());
            }
        }
        return minimum == Integer.MAX_VALUE ? 0 : minimum;
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
                new String[]{"column.id", "column.type", "column.status", "column.owner", "column.next"},
                new String[][]{
                        {"SO-2608-104", "term.salesOrder", "status.released", "owner.sales", "term.shipment"},
                        {"MRP-2608-W34", "term.mrpRun", "status.shortage", "owner.planner", "term.purchaseRequest"},
                        {"PR-2608-077", "term.purchaseRequest", "status.open", "owner.procurement", "term.purchaseOrder"},
                        {"PO-45000127", "term.purchaseOrder", "status.late", "owner.procurement", "term.receipt"},
                        {"MO-2608-004", "term.productionOrder", "status.blocked", "owner.production", "term.materialIssue"},
                        {"AR-2608-018", "term.ar", "status.open", "owner.finance", "menu.finance.collection"}
                });
        addDemoModule(modules, "MASTER", "cap.master", "master.subtitle", "OPERATIONAL",
                defaultActions(),
                new String[]{"column.id", "column.item", "column.name", "column.status", "column.owner"},
                new String[][]{
                        {"FG-3007", "FG-3007", "Smart actuator assembly", "status.released", "owner.production"},
                        {"FG-3041", "FG-3041", "Compact drive unit", "status.released", "owner.production"},
                        {"RM-1008", "RM-1008", "Aluminum housing blank", "status.released", "owner.procurement"},
                        {"PK-2210", "PK-2210", "Export carton set", "status.released", "owner.procurement"},
                        {"CUS-3001", "Northwind Manufacturing", "NET30 / USD", "status.open", "owner.sales"},
                        {"SUP-2007", "Sakura Metals", "NET45 / JPY", "status.released", "owner.procurement"}
                });
        addDemoModule(modules, "PROCUREMENT", "cap.procurement", "procurement.subtitle", "OPERATIONAL",
                workflowActions(),
                new String[]{"column.id", "column.supplier", "column.item", "column.qty", "column.status", "column.next"},
                new String[][]{
                        {"PR-2608-077", "Sakura Metals", "RM-1008", "1,200", "status.open", "term.purchaseOrder"},
                        {"PO-45000127", "Kanto Package", "PK-2210", "2,000", "status.late", "term.receipt"},
                        {"PO-45000132", "Sakura Metals", "RM-1008", "3,000", "status.released", "term.receipt"},
                        {"GR-50001988", "Sakura Metals", "RM-1008", "1,200", "status.ready", "term.invoiceCheck"},
                        {"PC-2608-021", "Sakura Metals", "$24,600", "1", "status.open", "term.payable"}
                });
        addDemoModule(modules, "SALES", "cap.sales", "sales.subtitle", "OPERATIONAL",
                workflowActions(),
                new String[]{"column.id", "column.customer", "column.item", "column.qty", "column.status", "column.next"},
                new String[][]{
                        {"QT-2608-022", "Northwind Manufacturing", "FG-3007", "80", "status.open", "term.salesOrder"},
                        {"SO-2608-104", "Taiyo Robotics", "FG-3007", "120", "status.released", "term.shipment"},
                        {"SO-2608-118", "Northwind Manufacturing", "FG-3041", "80", "status.open", "term.delivery"},
                        {"DN-2608-044", "Taiyo Robotics", "FG-3007", "80", "status.ready", "term.billing"},
                        {"SA-2608-018", "Northwind Manufacturing", "$52,800", "1", "status.open", "term.receivable"}
                });
        addDemoModule(modules, "INVENTORY", "cap.inventory", "inventory.subtitle", "OPERATIONAL",
                defaultActions(),
                new String[]{"column.id", "column.item", "column.warehouse", "column.qty", "column.status", "column.next"},
                new String[][]{
                        {"STK-FG-3007", "FG-3007", "FG-01", "96", "status.ready", "term.shipment"},
                        {"STK-FG-3041", "FG-3041", "FG-01", "42", "status.open", "term.productionOrder"},
                        {"STK-RM-1008", "RM-1008", "WH-A", "420", "status.shortage", "term.purchaseOrder"},
                        {"STK-PK-2210", "PK-2210", "WH-B", "80", "status.shortage", "term.purchaseRequest"},
                        {"TRX-2608-031", "RM-1008", "WH-A", "+1,200", "status.ready", "term.materialIssue"}
                });
        addDemoModule(modules, "MANUFACTURING", "cap.manufacturing", "manufacturing.subtitle", "OPERATIONAL",
                workflowActions(),
                new String[]{"column.id", "column.item", "column.qty", "column.status", "column.next", "column.owner"},
                new String[][]{
                        {"MRP-2608-W34", "FG-3007", "120", "status.shortage", "term.purchaseRequest", "owner.planner"},
                        {"MO-2608-004", "FG-3007", "120", "status.blocked", "term.materialIssue", "owner.production"},
                        {"MO-2608-005", "FG-3041", "80", "status.released", "term.confirmation", "owner.production"},
                        {"ISS-2608-041", "RM-1008", "240", "status.open", "term.stockOverview", "owner.production"},
                        {"COST-2608-009", "FG-3007", "$28,700", "status.ready", "term.gl", "owner.finance"}
                });
        addDemoModule(modules, "FINANCE", "cap.finance", "finance.subtitle", "OPERATIONAL",
                defaultActions(),
                new String[]{"column.id", "column.customer", "column.amount", "column.status", "column.next"},
                new String[][]{
                        {"AR-2608-018", "Northwind Manufacturing", "$52,800", "status.open", "menu.finance.collection"},
                        {"AR-2608-019", "Taiyo Robotics", "$124,600", "status.released", "term.gl"},
                        {"AP-2608-021", "Sakura Metals", "$24,600", "status.open", "menu.finance.payment"},
                        {"AP-2608-022", "Kanto Package", "$18,200", "status.released", "term.gl"},
                        {"GL-2608-INV", "Inventory valuation", "$418,900", "status.ready", "term.close"}
                });
        addDemoModule(modules, "AI", "cap.ai", "ai.subtitle", "AI",
                new String[]{"action.refresh", "action.ask", "action.export"},
                new String[]{"column.id", "column.type", "column.status", "column.owner", "column.next"},
                new String[][]{
                        {"AIQ-2608-001", "Inventory + MRP", "status.ready", "owner.planner", "MRP-2608-W34 shortage explanation"},
                        {"AIQ-2608-002", "PO + production", "status.ready", "owner.procurement", "PO-45000127 late impact"},
                        {"AIQ-2608-003", "Sales + finance", "status.ready", "owner.finance", "SO-2608-104 cash forecast"}
                });
        addDemoModule(modules, "ADMIN", "cap.admin", "admin.subtitle", "OPERATIONAL",
                defaultActions(),
                new String[]{"column.id", "column.owner", "column.status", "column.next"},
                new String[][]{
                        {"USR-admin", "owner.system", "status.released", "term.permissions"},
                        {"USR-planner", "owner.planner", "status.open", "menu.admin.workflow"},
                        {"ROLE-PLN", "role.planner", "status.released", "menu.admin.permissions"},
                        {"AUD-2608-031", "admin", "status.ready", "menu.admin.audit"}
                });
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
        String[][] demoRows = expandedDemoRows(code, columns, rows, DEMO_MODULE_ROWS);
        for (String[] row : demoRows) {
            module.addTableRow(sortOrder, copy(row));
            sortOrder += 10;
        }
        module.getFocusItems().add("Demo data is stored locally for this session.");
        modules.put(code, module);
    }

    private static String[][] expandedDemoRows(String code, String[] columns, String[][] seedRows, int targetRows) {
        if (seedRows == null || seedRows.length == 0 || seedRows.length >= targetRows) {
            return seedRows;
        }
        int columnCount = columns == null ? seedRows[0].length : columns.length;
        String[][] rows = new String[targetRows][columnCount];
        for (int rowIndex = 0; rowIndex < targetRows; rowIndex++) {
            String[] seed = seedRows[rowIndex % seedRows.length];
            String[] row = new String[columnCount];
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                String value = columnIndex < seed.length && seed[columnIndex] != null ? seed[columnIndex] : "";
                row[columnIndex] = demoValue(code, columns, columnIndex, value, rowIndex);
            }
            rows[rowIndex] = row;
        }
        return rows;
    }

    private static String demoValue(String code, String[] columns, int columnIndex, String value, int rowIndex) {
        String column = columns != null && columnIndex < columns.length ? columns[columnIndex] : "";
        int sequence = rowIndex + 1;
        if (columnIndex == 0 && looksLikeDocument(value)) {
            return documentNumber(code, value, sequence);
        }
        if ("column.id".equals(column) && looksLikeDocument(value)) {
            return documentNumber(code, value, sequence);
        }
        if ("column.item".equals(column)) {
            return DEMO_ITEMS[rowIndex % DEMO_ITEMS.length];
        }
        if ("column.customer".equals(column) || "column.supplier".equals(column)) {
            return DEMO_PARTNERS[rowIndex % DEMO_PARTNERS.length];
        }
        if ("column.qty".equals(column)) {
            return String.valueOf(Math.max(1, parseNumber(value, 80) + (rowIndex % 7) * 15));
        }
        if ("column.amount".equals(column)) {
            return "$" + (parseNumber(value, 18000) + (rowIndex % 9) * 2200);
        }
        if ("column.status".equals(column)) {
            return DEMO_STATUSES[rowIndex % DEMO_STATUSES.length];
        }
        if ("column.next".equals(column)) {
            return DEMO_NEXT_STEPS[rowIndex % DEMO_NEXT_STEPS.length];
        }
        if ("column.owner".equals(column)) {
            return DEMO_OWNERS[rowIndex % DEMO_OWNERS.length];
        }
        if ("column.type".equals(column) && value != null && value.length() > 0) {
            return value;
        }
        return value;
    }

    private static boolean looksLikeDocument(String value) {
        return value != null && value.indexOf('-') > 0;
    }

    private static String documentNumber(String code, String seed, int sequence) {
        String prefix = seed;
        int dash = seed == null ? -1 : seed.indexOf('-');
        if (dash > 0) {
            prefix = seed.substring(0, dash);
        }
        if (prefix == null || prefix.trim().length() == 0) {
            prefix = code == null || code.length() <= 3 ? "DOC" : code.substring(0, 3);
        }
        return prefix + "-2609-" + threeDigits(sequence);
    }

    private static int parseNumber(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        String digits = value.replaceAll("[^0-9-]", "");
        if (digits.length() == 0 || "-".equals(digits)) {
            return fallback;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String threeDigits(int value) {
        if (value < 10) {
            return "00" + value;
        }
        if (value < 100) {
            return "0" + value;
        }
        return String.valueOf(value);
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
