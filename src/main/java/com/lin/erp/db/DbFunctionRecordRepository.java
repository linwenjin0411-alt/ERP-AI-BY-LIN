package com.lin.erp.db;

import com.lin.erp.config.DbConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class DbFunctionRecordRepository {
    private final DbConfig config;

    public DbFunctionRecordRepository(DbConfig config) {
        this.config = config;
    }

    public List<FunctionRecord> loadRecords(String functionCode, String[][] seedRows) throws SQLException {
        BusinessRecordMapping mapping = mappingFor(functionCode);
        if (mapping != null) {
            return loadBusinessRecords(mapping, seedRows);
        }
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
        BusinessRecordMapping mapping = mappingFor(functionCode);
        if (mapping != null) {
            return createBusinessRecord(mapping, values);
        }
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
        BusinessRecordMapping mapping = mappingForId(id);
        if (mapping != null) {
            updateBusinessRecord(mapping, id, values);
            return;
        }
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
        BusinessRecordMapping mapping = mappingForId(id);
        if (mapping != null) {
            deleteBusinessRecord(mapping, id);
            return;
        }
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
        ensureBusinessSchemas(connection);
    }

    private List<FunctionRecord> loadBusinessRecords(BusinessRecordMapping mapping, String[][] seedRows) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = Database.connect(config);
            ensureSchema(connection);
            seedBusinessIfEmpty(connection, mapping, seedRows);
            statement = connection.prepareStatement(mapping.selectSql());
            statement.setString(1, mapping.documentType);
            resultSet = statement.executeQuery();
            List<FunctionRecord> records = new ArrayList<FunctionRecord>();
            while (resultSet.next()) {
                records.add(new FunctionRecord(mapping.encodeId(resultSet.getLong("id")), mapping.functionCode, values(resultSet)));
            }
            return records;
        } finally {
            close(resultSet, statement, connection);
        }
    }

    private long createBusinessRecord(BusinessRecordMapping mapping, String[] values) throws SQLException {
        if (mapping.readOnly) {
            throw new SQLException("This screen is generated from business movements and cannot be edited directly.");
        }
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet keys = null;
        try {
            connection = Database.connect(config);
            ensureSchema(connection);
            statement = connection.prepareStatement(mapping.insertSql(), PreparedStatement.RETURN_GENERATED_KEYS);
            statement.setString(1, mapping.documentType);
            bindBusiness(statement, 2, mapping, values);
            statement.executeUpdate();
            keys = statement.getGeneratedKeys();
            long generatedId = keys.next() ? keys.getLong(1) : -1;
            if (generatedId > 0) {
                syncInventoryMovement(connection, mapping, generatedId, values, true);
                return mapping.encodeId(generatedId);
            }
            return -1;
        } finally {
            if (keys != null) {
                keys.close();
            }
            close(null, statement, connection);
        }
    }

    private void updateBusinessRecord(BusinessRecordMapping mapping, long encodedId, String[] values) throws SQLException {
        if (mapping.readOnly) {
            throw new SQLException("This screen is generated from business movements and cannot be edited directly.");
        }
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = Database.connect(config);
            ensureSchema(connection);
            statement = connection.prepareStatement(mapping.updateSql());
            bindBusiness(statement, 1, mapping, values);
            statement.setLong(9, mapping.decodeId(encodedId));
            statement.executeUpdate();
            syncInventoryMovement(connection, mapping, mapping.decodeId(encodedId), values, true);
        } finally {
            close(null, statement, connection);
        }
    }

    private void deleteBusinessRecord(BusinessRecordMapping mapping, long encodedId) throws SQLException {
        if (mapping.readOnly) {
            throw new SQLException("This screen is generated from business movements and cannot be edited directly.");
        }
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = Database.connect(config);
            ensureSchema(connection);
            statement = connection.prepareStatement("update " + mapping.tableName + " set active = 0 where id = ?");
            statement.setLong(1, mapping.decodeId(encodedId));
            statement.executeUpdate();
            syncInventoryMovement(connection, mapping, mapping.decodeId(encodedId), null, false);
        } finally {
            close(null, statement, connection);
        }
    }

    private void seedBusinessIfEmpty(Connection connection, BusinessRecordMapping mapping, String[][] seedRows) throws SQLException {
        PreparedStatement countStatement = null;
        ResultSet resultSet = null;
        try {
            countStatement = connection.prepareStatement("select count(*) from " + mapping.tableName + " where document_type = ?");
            countStatement.setString(1, mapping.documentType);
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
        if (mapping.readOnly) {
            return;
        }
        for (int i = 0; i < seedRows.length; i++) {
            createBusinessSeedRecord(connection, mapping, seedRows[i]);
        }
    }

    private void createBusinessSeedRecord(Connection connection, BusinessRecordMapping mapping, String[] values) throws SQLException {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(mapping.insertSql());
            statement.setString(1, mapping.documentType);
            bindBusiness(statement, 2, mapping, values);
            statement.executeUpdate();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    private void ensureBusinessSchemas(Connection connection) throws SQLException {
        execute(connection, "create table if not exists erp_business_statuses ("
                + "code varchar(80) primary key,"
                + "name varchar(120) not null,"
                + "status_group varchar(40) not null,"
                + "sort_order int not null default 0,"
                + "active tinyint(1) not null default 1,"
                + "created_at timestamp not null default current_timestamp,"
                + "updated_at timestamp not null default current_timestamp on update current_timestamp"
                + ") engine=InnoDB default charset=utf8mb4");
        execute(connection, "create table if not exists erp_purchase_documents ("
                + "id bigint primary key auto_increment,"
                + "document_type varchar(40) not null,"
                + "document_no varchar(80) not null,"
                + "status varchar(80),"
                + "item_code varchar(80),"
                + "quantity decimal(18,4),"
                + "supplier_code varchar(160),"
                + "due_date date,"
                + "next_action varchar(120),"
                + "memo varchar(255),"
                + "active tinyint(1) not null default 1,"
                + "created_at timestamp not null default current_timestamp,"
                + "updated_at timestamp not null default current_timestamp on update current_timestamp,"
                + "unique key uk_erp_purchase_documents_no (document_type, document_no),"
                + "index idx_erp_purchase_documents_status (status),"
                + "index idx_erp_purchase_documents_due (due_date)"
                + ") engine=InnoDB default charset=utf8mb4");
        execute(connection, "create table if not exists erp_purchase_document_lines ("
                + "id bigint primary key auto_increment,"
                + "document_id bigint not null,"
                + "line_no int not null default 10,"
                + "item_code varchar(80),"
                + "quantity decimal(18,4),"
                + "warehouse_code varchar(80),"
                + "lot_no varchar(80),"
                + "active tinyint(1) not null default 1,"
                + "created_at timestamp not null default current_timestamp,"
                + "updated_at timestamp not null default current_timestamp on update current_timestamp,"
                + "constraint fk_erp_purchase_lines_doc foreign key (document_id) references erp_purchase_documents(id)"
                + ") engine=InnoDB default charset=utf8mb4");
        execute(connection, "create table if not exists erp_sales_documents ("
                + "id bigint primary key auto_increment,"
                + "document_type varchar(40) not null,"
                + "document_no varchar(80) not null,"
                + "customer_code varchar(160),"
                + "amount decimal(18,2),"
                + "currency_code varchar(3) not null default 'JPY',"
                + "tax_rate decimal(7,4) not null default 0,"
                + "exchange_rate decimal(18,8) not null default 1,"
                + "status varchar(80),"
                + "due_date date,"
                + "next_action varchar(120),"
                + "memo varchar(255),"
                + "external_ref varchar(120),"
                + "active tinyint(1) not null default 1,"
                + "created_at timestamp not null default current_timestamp,"
                + "updated_at timestamp not null default current_timestamp on update current_timestamp,"
                + "unique key uk_erp_sales_documents_no (document_type, document_no),"
                + "index idx_erp_sales_documents_customer (customer_code),"
                + "index idx_erp_sales_documents_status (status)"
                + ") engine=InnoDB default charset=utf8mb4");
        execute(connection, "create table if not exists erp_sales_document_lines ("
                + "id bigint primary key auto_increment,"
                + "document_id bigint not null,"
                + "line_no int not null default 10,"
                + "item_code varchar(80),"
                + "quantity decimal(18,4),"
                + "warehouse_code varchar(80),"
                + "lot_no varchar(80),"
                + "active tinyint(1) not null default 1,"
                + "created_at timestamp not null default current_timestamp,"
                + "updated_at timestamp not null default current_timestamp on update current_timestamp,"
                + "constraint fk_erp_sales_lines_doc foreign key (document_id) references erp_sales_documents(id)"
                + ") engine=InnoDB default charset=utf8mb4");
        execute(connection, "create table if not exists erp_manufacturing_documents ("
                + "id bigint primary key auto_increment,"
                + "document_type varchar(40) not null,"
                + "document_no varchar(80) not null,"
                + "item_code varchar(80),"
                + "quantity decimal(18,4),"
                + "status varchar(80),"
                + "due_date date,"
                + "risk_code varchar(80),"
                + "bom_code varchar(80),"
                + "warehouse_code varchar(80),"
                + "next_action varchar(120),"
                + "active tinyint(1) not null default 1,"
                + "created_at timestamp not null default current_timestamp,"
                + "updated_at timestamp not null default current_timestamp on update current_timestamp,"
                + "unique key uk_erp_manufacturing_documents_no (document_type, document_no),"
                + "index idx_erp_manufacturing_documents_item (item_code),"
                + "index idx_erp_manufacturing_documents_status (status)"
                + ") engine=InnoDB default charset=utf8mb4");
        execute(connection, "create table if not exists erp_inventory_records ("
                + "id bigint primary key auto_increment,"
                + "document_type varchar(40) not null,"
                + "item_code varchar(80) not null,"
                + "warehouse_code varchar(80),"
                + "quantity decimal(18,4),"
                + "status varchar(80),"
                + "risk_code varchar(80),"
                + "next_action varchar(120),"
                + "lot_no varchar(80),"
                + "memo varchar(255),"
                + "active tinyint(1) not null default 1,"
                + "created_at timestamp not null default current_timestamp,"
                + "updated_at timestamp not null default current_timestamp on update current_timestamp,"
                + "index idx_erp_inventory_records_item_wh (item_code, warehouse_code),"
                + "index idx_erp_inventory_records_lot (lot_no)"
                + ") engine=InnoDB default charset=utf8mb4");
        execute(connection, "create table if not exists erp_bom_components ("
                + "id bigint primary key auto_increment,"
                + "bom_code varchar(80) not null,"
                + "parent_item_code varchar(80) not null,"
                + "component_item_code varchar(80) not null,"
                + "quantity_per decimal(18,6) not null,"
                + "scrap_rate decimal(7,4),"
                + "effective_from date,"
                + "effective_to date,"
                + "active tinyint(1) not null default 1,"
                + "created_at timestamp not null default current_timestamp,"
                + "updated_at timestamp not null default current_timestamp on update current_timestamp,"
                + "unique key uk_erp_bom_components_line (bom_code, parent_item_code, component_item_code),"
                + "index idx_erp_bom_components_parent (parent_item_code),"
                + "index idx_erp_bom_components_component (component_item_code)"
                + ") engine=InnoDB default charset=utf8mb4");
        execute(connection, "create table if not exists erp_inventory_movements ("
                + "id bigint primary key auto_increment,"
                + "source_table varchar(80) not null,"
                + "source_id bigint not null,"
                + "document_type varchar(40) not null,"
                + "document_no varchar(80) not null,"
                + "item_code varchar(80),"
                + "warehouse_code varchar(80),"
                + "lot_no varchar(80),"
                + "movement_qty decimal(18,4) not null default 0,"
                + "movement_date date,"
                + "status varchar(80),"
                + "active tinyint(1) not null default 1,"
                + "created_at timestamp not null default current_timestamp,"
                + "updated_at timestamp not null default current_timestamp on update current_timestamp,"
                + "unique key uk_erp_inventory_movements_source (source_table, source_id),"
                + "index idx_erp_inventory_movements_item_wh (item_code, warehouse_code),"
                + "index idx_erp_inventory_movements_date (movement_date)"
                + ") engine=InnoDB default charset=utf8mb4");
        DatabaseSchema.ensureColumn(connection, "erp_manufacturing_documents", "risk_code", "risk_code varchar(80)");
        DatabaseSchema.ensureColumn(connection, "erp_sales_documents", "currency_code", "currency_code varchar(3) not null default 'JPY'");
        DatabaseSchema.ensureColumn(connection, "erp_sales_documents", "tax_rate", "tax_rate decimal(7,4) not null default 0");
        DatabaseSchema.ensureColumn(connection, "erp_sales_documents", "exchange_rate", "exchange_rate decimal(18,8) not null default 1");
        seedStatuses(connection);
    }

    private void seedStatuses(Connection connection) throws SQLException {
        String[][] rows = {
                {"status.draft", "Draft", "DOCUMENT", "10"},
                {"status.open", "Open", "DOCUMENT", "20"},
                {"status.waitingApproval", "Waiting Approval", "DOCUMENT", "30"},
                {"status.ready", "Ready", "DOCUMENT", "40"},
                {"status.released", "Released", "DOCUMENT", "50"},
                {"status.posted", "Posted", "DOCUMENT", "60"},
                {"status.closed", "Closed", "DOCUMENT", "70"},
                {"status.cancelled", "Cancelled", "DOCUMENT", "80"},
                {"status.shortage", "Shortage", "RISK", "10"},
                {"status.blocked", "Blocked", "RISK", "20"}
        };
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(
                    "insert into erp_business_statuses (code, name, status_group, sort_order, active) "
                            + "values (?, ?, ?, ?, 1) on duplicate key update "
                            + "name = values(name), status_group = values(status_group), sort_order = values(sort_order), active = 1"
            );
            for (int i = 0; i < rows.length; i++) {
                statement.setString(1, rows[i][0]);
                statement.setString(2, rows[i][1]);
                statement.setString(3, rows[i][2]);
                statement.setInt(4, Integer.parseInt(rows[i][3]));
                statement.executeUpdate();
            }
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(sql);
            statement.executeUpdate();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
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

    private BusinessRecordMapping mappingForId(long encodedId) {
        int prefix = (int)(encodedId / BusinessRecordMapping.ID_MULTIPLIER);
        if (prefix <= 0) {
            return null;
        }
        return mappingForPrefix(prefix);
    }

    private BusinessRecordMapping mappingFor(String functionCode) {
        if ("PROCUREMENT_PR".equals(functionCode)) {
            return purchaseMapping(functionCode, "PURCHASE_REQUEST", 1);
        }
        if ("PROCUREMENT_PO".equals(functionCode) || "PROCUREMENT_PO_QUERY".equals(functionCode)) {
            return purchaseMapping(functionCode, "PURCHASE_ORDER", 2);
        }
        if ("PROCUREMENT_RECEIPT".equals(functionCode) || "PROCUREMENT_RECEIPT_QUERY".equals(functionCode)) {
            return purchaseMapping(functionCode, "GOODS_RECEIPT", 3);
        }
        if ("PROCUREMENT_INVOICE".equals(functionCode) || "PROCUREMENT_CONFIRMATION".equals(functionCode)) {
            return purchaseMapping(functionCode, "SUPPLIER_INVOICE", 4);
        }
        if ("PROCUREMENT_RETURN".equals(functionCode)) {
            return purchaseMapping(functionCode, "PURCHASE_RETURN", 5);
        }
        if ("SALES_QUOTATION".equals(functionCode)) {
            return salesMapping(functionCode, "SALES_QUOTATION", 6);
        }
        if ("SALES_ORDER".equals(functionCode) || "SALES_ORDER_QUERY".equals(functionCode)) {
            return salesMapping(functionCode, "SALES_ORDER", 7);
        }
        if ("SALES_SHIPMENT".equals(functionCode) || "SALES_SHIPMENT_QUERY".equals(functionCode)) {
            return salesMapping(functionCode, "SALES_SHIPMENT", 8);
        }
        if ("SALES_BILLING".equals(functionCode) || "SALES_CONFIRMATION".equals(functionCode)) {
            return salesMapping(functionCode, "SALES_BILLING", 9);
        }
        if ("SALES_RETURN".equals(functionCode)) {
            return salesMapping(functionCode, "SALES_RETURN", 10);
        }
        if ("MANUFACTURING_ORDER".equals(functionCode) || "MANUFACTURING_ORDER_QUERY".equals(functionCode)) {
            return manufacturingMapping(functionCode, "PRODUCTION_ORDER", 11);
        }
        if ("MANUFACTURING_ISSUE".equals(functionCode)) {
            return manufacturingMapping(functionCode, "MATERIAL_ISSUE", 12);
        }
        if ("MANUFACTURING_COMPLETE".equals(functionCode)) {
            return manufacturingMapping(functionCode, "PRODUCTION_COMPLETION", 13);
        }
        if ("MANUFACTURING_RETURN".equals(functionCode)) {
            return manufacturingMapping(functionCode, "MATERIAL_RETURN", 14);
        }
        if ("INVENTORY_STOCK".equals(functionCode)) {
            return inventoryMapping(functionCode, "STOCK_BALANCE", 15);
        }
        if ("INVENTORY_LEDGER".equals(functionCode)) {
            return new BusinessRecordMapping(functionCode, "erp_inventory_movements", "INVENTORY_LEDGER", 19,
                    new BusinessColumn[]{
                            BusinessColumn.text("document_no"),
                            BusinessColumn.text("warehouse_code"),
                            BusinessColumn.quantity("movement_qty"),
                            BusinessColumn.status("status"),
                            BusinessColumn.text("document_type"),
                            BusinessColumn.date("movement_date"),
                            BusinessColumn.text("lot_no"),
                            BusinessColumn.text("item_code")
                    },
                    true);
        }
        if ("INVENTORY_TRANSFER".equals(functionCode)) {
            return inventoryMapping(functionCode, "STOCK_TRANSFER", 16);
        }
        if ("INVENTORY_LOT".equals(functionCode)) {
            return inventoryMapping(functionCode, "LOT_TRACE", 17);
        }
        if ("INVENTORY_COUNT".equals(functionCode)) {
            return inventoryMapping(functionCode, "CYCLE_COUNT", 18);
        }
        return null;
    }

    private BusinessRecordMapping mappingForPrefix(int prefix) {
        String[] codes = {
                null,
                "PROCUREMENT_PR", "PROCUREMENT_PO", "PROCUREMENT_RECEIPT", "PROCUREMENT_INVOICE", "PROCUREMENT_RETURN",
                "SALES_QUOTATION", "SALES_ORDER", "SALES_SHIPMENT", "SALES_BILLING", "SALES_RETURN",
                "MANUFACTURING_ORDER", "MANUFACTURING_ISSUE", "MANUFACTURING_COMPLETE", "MANUFACTURING_RETURN",
                "INVENTORY_STOCK", "INVENTORY_TRANSFER", "INVENTORY_LOT", "INVENTORY_COUNT", "INVENTORY_LEDGER"
        };
        if (prefix <= 0 || prefix >= codes.length) {
            return null;
        }
        return mappingFor(codes[prefix]);
    }

    private BusinessRecordMapping purchaseMapping(String functionCode, String documentType, int prefix) {
        return new BusinessRecordMapping(functionCode, "erp_purchase_documents", documentType, prefix,
                new BusinessColumn[]{
                        BusinessColumn.text("document_no"),
                        BusinessColumn.status("status"),
                        BusinessColumn.text("item_code"),
                        BusinessColumn.quantity("quantity"),
                        BusinessColumn.text("supplier_code"),
                        BusinessColumn.date("due_date"),
                        BusinessColumn.text("next_action"),
                        BusinessColumn.text("memo")
                });
    }

    private BusinessRecordMapping salesMapping(String functionCode, String documentType, int prefix) {
        return new BusinessRecordMapping(functionCode, "erp_sales_documents", documentType, prefix,
                new BusinessColumn[]{
                        BusinessColumn.text("document_no"),
                        BusinessColumn.text("customer_code"),
                        BusinessColumn.amount("amount"),
                        BusinessColumn.status("status"),
                        BusinessColumn.date("due_date"),
                        BusinessColumn.text("next_action"),
                        BusinessColumn.text("memo"),
                        BusinessColumn.text("external_ref")
                });
    }

    private BusinessRecordMapping manufacturingMapping(String functionCode, String documentType, int prefix) {
        return new BusinessRecordMapping(functionCode, "erp_manufacturing_documents", documentType, prefix,
                new BusinessColumn[]{
                        BusinessColumn.text("document_no"),
                        BusinessColumn.text("item_code"),
                        BusinessColumn.quantity("quantity"),
                        BusinessColumn.status("status"),
                        BusinessColumn.date("due_date"),
                        BusinessColumn.status("risk_code"),
                        BusinessColumn.text("next_action"),
                        BusinessColumn.text("bom_code")
                });
    }

    private BusinessRecordMapping inventoryMapping(String functionCode, String documentType, int prefix) {
        return new BusinessRecordMapping(functionCode, "erp_inventory_records", documentType, prefix,
                new BusinessColumn[]{
                        BusinessColumn.text("item_code"),
                        BusinessColumn.text("warehouse_code"),
                        BusinessColumn.quantity("quantity"),
                        BusinessColumn.status("status"),
                        BusinessColumn.status("risk_code"),
                        BusinessColumn.text("next_action"),
                        BusinessColumn.text("lot_no"),
                        BusinessColumn.text("memo")
                });
    }

    private void bindBusiness(PreparedStatement statement, int startIndex, BusinessRecordMapping mapping, String[] values) throws SQLException {
        for (int i = 0; i < mapping.valueColumns.length; i++) {
            String value = values == null || i >= values.length ? null : values[i];
            mapping.valueColumns[i].bind(statement, startIndex + i, value);
        }
    }

    private void syncInventoryMovement(Connection connection, BusinessRecordMapping mapping, long sourceId, String[] values, boolean active) throws SQLException {
        if (!mapping.generatesInventoryMovement()) {
            return;
        }
        PreparedStatement statement = null;
        try {
            if (!active) {
                statement = connection.prepareStatement(
                        "update erp_inventory_movements set active = 0 where source_table = ? and source_id = ?"
                );
                statement.setString(1, mapping.tableName);
                statement.setLong(2, sourceId);
                statement.executeUpdate();
                return;
            }
            statement = connection.prepareStatement(
                    "insert into erp_inventory_movements "
                            + "(source_table, source_id, document_type, document_no, item_code, warehouse_code, lot_no, movement_qty, movement_date, status, active) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                            + "on duplicate key update document_type = values(document_type), document_no = values(document_no), "
                            + "item_code = values(item_code), warehouse_code = values(warehouse_code), lot_no = values(lot_no), "
                            + "movement_qty = values(movement_qty), movement_date = values(movement_date), status = values(status), active = values(active)"
            );
            MovementProjection projection = movementProjection(mapping, values);
            statement.setString(1, mapping.tableName);
            statement.setLong(2, sourceId);
            statement.setString(3, mapping.documentType);
            bindText(statement, 4, projection.documentNo);
            bindText(statement, 5, projection.itemCode);
            bindText(statement, 6, projection.warehouseCode);
            bindText(statement, 7, projection.lotNo);
            statement.setBigDecimal(8, projection.quantity == null ? BigDecimal.ZERO : projection.quantity);
            bindDate(statement, 9, projection.movementDate);
            bindText(statement, 10, projection.status);
            statement.setInt(11, active ? 1 : 0);
            statement.executeUpdate();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    private MovementProjection movementProjection(BusinessRecordMapping mapping, String[] values) {
        MovementProjection projection = new MovementProjection();
        projection.documentNo = mapping.value(values, 0);
        projection.status = mapping.value(values, 3);
        projection.quantity = signedMovementQuantity(mapping.documentType, parseDecimal(mapping.value(values, 2)));
        if ("erp_purchase_documents".equals(mapping.tableName)) {
            projection.status = mapping.value(values, 1);
            projection.itemCode = mapping.value(values, 2);
            projection.quantity = signedMovementQuantity(mapping.documentType, parseDecimal(mapping.value(values, 3)));
            projection.movementDate = mapping.value(values, 5);
            return projection;
        }
        if ("erp_sales_documents".equals(mapping.tableName)) {
            projection.itemCode = mapping.value(values, 7);
            projection.movementDate = mapping.value(values, 4);
            return projection;
        }
        if ("erp_manufacturing_documents".equals(mapping.tableName)) {
            projection.itemCode = mapping.value(values, 1);
            projection.movementDate = mapping.value(values, 4);
            projection.lotNo = mapping.value(values, 7);
            return projection;
        }
        if ("erp_inventory_records".equals(mapping.tableName)) {
            projection.itemCode = mapping.value(values, 0);
            projection.warehouseCode = mapping.value(values, 1);
            projection.quantity = signedMovementQuantity(mapping.documentType, parseDecimal(mapping.value(values, 2)));
            projection.lotNo = mapping.value(values, 6);
            projection.documentNo = projection.itemCode == null ? mapping.documentType + "-" + System.currentTimeMillis() : projection.itemCode + "-" + mapping.documentType;
            return projection;
        }
        return projection;
    }

    private BigDecimal signedMovementQuantity(String documentType, BigDecimal quantity) {
        if (quantity == null) {
            return BigDecimal.ZERO;
        }
        if ("SALES_SHIPMENT".equals(documentType)
                || "PURCHASE_RETURN".equals(documentType)
                || "MATERIAL_ISSUE".equals(documentType)) {
            return quantity.abs().negate();
        }
        if ("SALES_RETURN".equals(documentType)
                || "GOODS_RECEIPT".equals(documentType)
                || "PRODUCTION_COMPLETION".equals(documentType)
                || "MATERIAL_RETURN".equals(documentType)
                || "CYCLE_COUNT".equals(documentType)
                || "STOCK_BALANCE".equals(documentType)) {
            return quantity.abs();
        }
        return BigDecimal.ZERO;
    }

    private static BigDecimal parseDecimal(String value) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        String normalized = value.trim().replace(",", "").replace(" ", "").replace("$", "").replace("￥", "");
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        String normalized = value.trim().replace('/', '-');
        try {
            return LocalDate.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static void bindText(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.trim().length() == 0) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
        }
    }

    private static void bindDate(PreparedStatement statement, int index, String value) throws SQLException {
        LocalDate date = parseDate(value);
        if (date == null) {
            statement.setNull(index, Types.DATE);
        } else {
            statement.setDate(index, java.sql.Date.valueOf(date));
        }
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

    private static final class BusinessRecordMapping {
        private static final long ID_MULTIPLIER = 1000000000000L;

        private final String functionCode;
        private final String tableName;
        private final String documentType;
        private final int idPrefix;
        private final BusinessColumn[] valueColumns;
        private final boolean readOnly;

        private BusinessRecordMapping(String functionCode, String tableName, String documentType, int idPrefix, BusinessColumn[] valueColumns) {
            this(functionCode, tableName, documentType, idPrefix, valueColumns, false);
        }

        private BusinessRecordMapping(String functionCode, String tableName, String documentType, int idPrefix, BusinessColumn[] valueColumns, boolean readOnly) {
            this.functionCode = functionCode;
            this.tableName = tableName;
            this.documentType = documentType;
            this.idPrefix = idPrefix;
            this.valueColumns = valueColumns;
            this.readOnly = readOnly;
        }

        private String selectSql() {
            StringBuilder sql = new StringBuilder("select id, ");
            for (int i = 0; i < valueColumns.length; i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append(valueColumns[i].selectExpression()).append(" as c").append(i + 1);
            }
            sql.append(" from ").append(tableName)
                    .append(" where document_type = ? and active = 1 order by id");
            return sql.toString();
        }

        private String insertSql() {
            StringBuilder sql = new StringBuilder("insert into ");
            sql.append(tableName).append(" (document_type");
            for (int i = 0; i < valueColumns.length; i++) {
                sql.append(", ").append(valueColumns[i].name);
            }
            sql.append(") values (?").append(", ?, ?, ?, ?, ?, ?, ?, ?)");
            return sql.toString();
        }

        private String updateSql() {
            StringBuilder sql = new StringBuilder("update ");
            sql.append(tableName).append(" set ");
            for (int i = 0; i < valueColumns.length; i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append(valueColumns[i].name).append(" = ?");
            }
            sql.append(" where id = ? and active = 1");
            return sql.toString();
        }

        private boolean generatesInventoryMovement() {
            return "GOODS_RECEIPT".equals(documentType)
                    || "PURCHASE_RETURN".equals(documentType)
                    || "SALES_SHIPMENT".equals(documentType)
                    || "SALES_RETURN".equals(documentType)
                    || "MATERIAL_ISSUE".equals(documentType)
                    || "PRODUCTION_COMPLETION".equals(documentType)
                    || "MATERIAL_RETURN".equals(documentType)
                    || "STOCK_BALANCE".equals(documentType)
                    || "STOCK_TRANSFER".equals(documentType)
                    || "CYCLE_COUNT".equals(documentType);
        }

        private String value(String[] values, int index) {
            if (values == null || index < 0 || index >= values.length) {
                return null;
            }
            return values[index];
        }

        private long encodeId(long id) {
            return ((long)idPrefix * ID_MULTIPLIER) + id;
        }

        private long decodeId(long encodedId) {
            return encodedId % ID_MULTIPLIER;
        }
    }

    private static final class BusinessColumn {
        private static final String TEXT = "TEXT";
        private static final String QUANTITY = "QUANTITY";
        private static final String AMOUNT = "AMOUNT";
        private static final String DATE = "DATE";
        private static final String STATUS = "STATUS";

        private final String name;
        private final String type;

        private BusinessColumn(String name, String type) {
            this.name = name;
            this.type = type;
        }

        private static BusinessColumn text(String name) {
            return new BusinessColumn(name, TEXT);
        }

        private static BusinessColumn quantity(String name) {
            return new BusinessColumn(name, QUANTITY);
        }

        private static BusinessColumn amount(String name) {
            return new BusinessColumn(name, AMOUNT);
        }

        private static BusinessColumn date(String name) {
            return new BusinessColumn(name, DATE);
        }

        private static BusinessColumn status(String name) {
            return new BusinessColumn(name, STATUS);
        }

        private String selectExpression() {
            if (DATE.equals(type)) {
                return "date_format(" + name + ", '%Y-%m-%d')";
            }
            return name;
        }

        private void bind(PreparedStatement statement, int index, String value) throws SQLException {
            if (QUANTITY.equals(type) || AMOUNT.equals(type)) {
                BigDecimal decimal = parseDecimal(value);
                if (decimal == null) {
                    statement.setNull(index, Types.DECIMAL);
                } else {
                    statement.setBigDecimal(index, decimal);
                }
                return;
            }
            if (DATE.equals(type)) {
                bindDate(statement, index, value);
                return;
            }
            bindText(statement, index, value);
        }
    }

    private static final class MovementProjection {
        private String documentNo;
        private String itemCode;
        private String warehouseCode;
        private String lotNo;
        private BigDecimal quantity;
        private String movementDate;
        private String status;
    }
}
