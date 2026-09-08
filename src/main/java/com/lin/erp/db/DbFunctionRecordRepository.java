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
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet keys = null;
        try {
            connection = Database.connect(config);
            ensureSchema(connection);
            statement = connection.prepareStatement(mapping.insertSql(), PreparedStatement.RETURN_GENERATED_KEYS);
            statement.setString(1, mapping.documentType);
            bind(statement, 2, values);
            statement.executeUpdate();
            keys = statement.getGeneratedKeys();
            return keys.next() ? mapping.encodeId(keys.getLong(1)) : -1;
        } finally {
            if (keys != null) {
                keys.close();
            }
            close(null, statement, connection);
        }
    }

    private void updateBusinessRecord(BusinessRecordMapping mapping, long encodedId, String[] values) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = Database.connect(config);
            ensureSchema(connection);
            statement = connection.prepareStatement(mapping.updateSql());
            bind(statement, 1, values);
            statement.setLong(9, mapping.decodeId(encodedId));
            statement.executeUpdate();
        } finally {
            close(null, statement, connection);
        }
    }

    private void deleteBusinessRecord(BusinessRecordMapping mapping, long encodedId) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = Database.connect(config);
            ensureSchema(connection);
            statement = connection.prepareStatement("update " + mapping.tableName + " set active = 0 where id = ?");
            statement.setLong(1, mapping.decodeId(encodedId));
            statement.executeUpdate();
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
        for (int i = 0; i < seedRows.length; i++) {
            createBusinessSeedRecord(connection, mapping, seedRows[i]);
        }
    }

    private void createBusinessSeedRecord(Connection connection, BusinessRecordMapping mapping, String[] values) throws SQLException {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(mapping.insertSql());
            statement.setString(1, mapping.documentType);
            bind(statement, 2, values);
            statement.executeUpdate();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    private void ensureBusinessSchemas(Connection connection) throws SQLException {
        execute(connection, "create table if not exists erp_purchase_documents ("
                + "id bigint primary key auto_increment,"
                + "document_type varchar(40) not null,"
                + "document_no varchar(80) not null,"
                + "status varchar(80),"
                + "item_code varchar(80),"
                + "quantity varchar(40),"
                + "supplier_code varchar(160),"
                + "due_date varchar(40),"
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
                + "quantity varchar(40),"
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
                + "amount varchar(80),"
                + "status varchar(80),"
                + "due_date varchar(40),"
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
                + "quantity varchar(40),"
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
                + "quantity varchar(40),"
                + "status varchar(80),"
                + "due_date varchar(40),"
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
                + "quantity varchar(40),"
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
                + "quantity_per varchar(40) not null,"
                + "scrap_rate varchar(40),"
                + "effective_from varchar(40),"
                + "effective_to varchar(40),"
                + "active tinyint(1) not null default 1,"
                + "created_at timestamp not null default current_timestamp,"
                + "updated_at timestamp not null default current_timestamp on update current_timestamp,"
                + "unique key uk_erp_bom_components_line (bom_code, parent_item_code, component_item_code),"
                + "index idx_erp_bom_components_parent (parent_item_code),"
                + "index idx_erp_bom_components_component (component_item_code)"
                + ") engine=InnoDB default charset=utf8mb4");
        DatabaseSchema.ensureColumn(connection, "erp_manufacturing_documents", "risk_code", "risk_code varchar(80)");
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
        if ("INVENTORY_STOCK".equals(functionCode) || "INVENTORY_LEDGER".equals(functionCode)) {
            return inventoryMapping(functionCode, "STOCK_BALANCE", 15);
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
                "INVENTORY_STOCK", "INVENTORY_TRANSFER", "INVENTORY_LOT", "INVENTORY_COUNT"
        };
        if (prefix <= 0 || prefix >= codes.length) {
            return null;
        }
        return mappingFor(codes[prefix]);
    }

    private BusinessRecordMapping purchaseMapping(String functionCode, String documentType, int prefix) {
        return new BusinessRecordMapping(functionCode, "erp_purchase_documents", documentType, prefix,
                new String[]{"document_no", "status", "item_code", "quantity", "supplier_code", "due_date", "next_action", "memo"});
    }

    private BusinessRecordMapping salesMapping(String functionCode, String documentType, int prefix) {
        return new BusinessRecordMapping(functionCode, "erp_sales_documents", documentType, prefix,
                new String[]{"document_no", "customer_code", "amount", "status", "due_date", "next_action", "memo", "external_ref"});
    }

    private BusinessRecordMapping manufacturingMapping(String functionCode, String documentType, int prefix) {
        return new BusinessRecordMapping(functionCode, "erp_manufacturing_documents", documentType, prefix,
                new String[]{"document_no", "item_code", "quantity", "status", "due_date", "risk_code", "next_action", "bom_code"});
    }

    private BusinessRecordMapping inventoryMapping(String functionCode, String documentType, int prefix) {
        return new BusinessRecordMapping(functionCode, "erp_inventory_records", documentType, prefix,
                new String[]{"item_code", "warehouse_code", "quantity", "status", "risk_code", "next_action", "lot_no", "memo"});
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
        private final String[] valueColumns;

        private BusinessRecordMapping(String functionCode, String tableName, String documentType, int idPrefix, String[] valueColumns) {
            this.functionCode = functionCode;
            this.tableName = tableName;
            this.documentType = documentType;
            this.idPrefix = idPrefix;
            this.valueColumns = valueColumns;
        }

        private String selectSql() {
            StringBuilder sql = new StringBuilder("select id, ");
            for (int i = 0; i < valueColumns.length; i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append(valueColumns[i]).append(" as c").append(i + 1);
            }
            sql.append(" from ").append(tableName)
                    .append(" where document_type = ? and active = 1 order by id");
            return sql.toString();
        }

        private String insertSql() {
            StringBuilder sql = new StringBuilder("insert into ");
            sql.append(tableName).append(" (document_type");
            for (int i = 0; i < valueColumns.length; i++) {
                sql.append(", ").append(valueColumns[i]);
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
                sql.append(valueColumns[i]).append(" = ?");
            }
            sql.append(" where id = ? and active = 1");
            return sql.toString();
        }

        private long encodeId(long id) {
            return ((long)idPrefix * ID_MULTIPLIER) + id;
        }

        private long decodeId(long encodedId) {
            return encodedId % ID_MULTIPLIER;
        }
    }
}
