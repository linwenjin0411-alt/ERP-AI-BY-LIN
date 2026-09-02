package com.lin.erp.db;

import com.lin.erp.config.DbConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DbItemMasterRepository {
    private final DbConfig config;

    public DbItemMasterRepository(DbConfig config) {
        this.config = config;
    }

    public List<ItemMasterRecord> loadItems() throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = Database.connect(config);
            ensureSchema(connection);
            statement = connection.prepareStatement(
                    "select item_code, item_name, item_type, uom, plant, status, safety_stock, lead_time_days "
                            + "from erp_item_masters where active = 1 order by item_code"
            );
            resultSet = statement.executeQuery();
            List<ItemMasterRecord> records = new ArrayList<ItemMasterRecord>();
            while (resultSet.next()) {
                records.add(new ItemMasterRecord(
                        resultSet.getString("item_code"),
                        resultSet.getString("item_name"),
                        resultSet.getString("item_type"),
                        resultSet.getString("uom"),
                        resultSet.getString("plant"),
                        resultSet.getString("status"),
                        resultSet.getString("safety_stock"),
                        resultSet.getString("lead_time_days")
                ));
            }
            return records;
        } finally {
            close(resultSet, statement, connection);
        }
    }

    public void createItem(ItemMasterRecord record) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = Database.connect(config);
            ensureSchema(connection);
            statement = connection.prepareStatement(
                    "insert into erp_item_masters "
                            + "(item_code, item_name, item_type, uom, plant, status, safety_stock, lead_time_days) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?)"
            );
            bind(statement, record);
            statement.executeUpdate();
        } finally {
            close(null, statement, connection);
        }
    }

    public void updateItem(String originalItemCode, ItemMasterRecord record) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = Database.connect(config);
            ensureSchema(connection);
            statement = connection.prepareStatement(
                    "update erp_item_masters set item_code = ?, item_name = ?, item_type = ?, uom = ?, "
                            + "plant = ?, status = ?, safety_stock = ?, lead_time_days = ? where item_code = ?"
            );
            bind(statement, record);
            statement.setString(9, originalItemCode);
            statement.executeUpdate();
        } finally {
            close(null, statement, connection);
        }
    }

    public void deleteItem(String itemCode) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = Database.connect(config);
            ensureSchema(connection);
            statement = connection.prepareStatement(
                    "update erp_item_masters set active = 0, status = 'status.cancelled' where item_code = ?"
            );
            statement.setString(1, itemCode);
            statement.executeUpdate();
        } finally {
            close(null, statement, connection);
        }
    }

    private void ensureSchema(Connection connection) throws SQLException {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(
                    "create table if not exists erp_item_masters ("
                            + "id bigint primary key auto_increment,"
                            + "item_code varchar(80) not null unique,"
                            + "item_name varchar(180) not null,"
                            + "item_type varchar(80) not null,"
                            + "uom varchar(30) not null,"
                            + "plant varchar(40) not null,"
                            + "status varchar(80) not null,"
                            + "safety_stock varchar(40),"
                            + "lead_time_days varchar(40),"
                            + "active tinyint(1) not null default 1,"
                            + "created_at timestamp not null default current_timestamp,"
                            + "updated_at timestamp not null default current_timestamp on update current_timestamp"
                            + ") engine=InnoDB default charset=utf8mb4"
            );
            statement.executeUpdate();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        DatabaseSchema.ensureColumn(connection, "erp_item_masters", "active", "active tinyint(1) not null default 1");
        seedIfEmpty(connection);
    }

    private void seedIfEmpty(Connection connection) throws SQLException {
        PreparedStatement countStatement = null;
        ResultSet resultSet = null;
        PreparedStatement insertStatement = null;
        try {
            countStatement = connection.prepareStatement("select count(*) from erp_item_masters");
            resultSet = countStatement.executeQuery();
            if (resultSet.next() && resultSet.getInt(1) > 0) {
                return;
            }
            insertStatement = connection.prepareStatement(
                    "insert into erp_item_masters "
                            + "(item_code, item_name, item_type, uom, plant, status, safety_stock, lead_time_days) values "
                            + "('RM-1008', 'Servo motor 2kW', 'Purchased material', 'EA', 'JP01', 'status.released', '500', '14'),"
                            + "('PK-2210', 'Export carton package', 'Packaging', 'EA', 'JP01', 'status.released', '1200', '7'),"
                            + "('FG-3007', 'Smart actuator assembly', 'Finished good', 'EA', 'JP01', 'status.open', '80', '21')"
            );
            insertStatement.executeUpdate();
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (countStatement != null) {
                countStatement.close();
            }
            if (insertStatement != null) {
                insertStatement.close();
            }
        }
    }

    private void bind(PreparedStatement statement, ItemMasterRecord record) throws SQLException {
        statement.setString(1, record.getItemCode());
        statement.setString(2, record.getItemName());
        statement.setString(3, record.getItemType());
        statement.setString(4, record.getUom());
        statement.setString(5, record.getPlant());
        statement.setString(6, record.getStatus());
        statement.setString(7, record.getSafetyStock());
        statement.setString(8, record.getLeadTimeDays());
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
