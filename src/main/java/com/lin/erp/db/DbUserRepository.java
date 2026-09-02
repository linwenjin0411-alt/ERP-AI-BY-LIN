package com.lin.erp.db;

import com.lin.erp.config.DbConfig;
import com.lin.erp.logging.AppLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DbUserRepository {
    private final DbConfig config;

    public DbUserRepository(DbConfig config) {
        this.config = config;
    }

    public DbAccount findActiveUser(String username) throws SQLException {
        AppLogger.info("Querying active ERP user from database: " + username);
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = Database.connect(config);
            statement = connection.prepareStatement(
                    "select u.username, u.password_hash, u.display_name, r.name as role_name, c.name as company_name "
                            + "from erp_users u "
                            + "join erp_roles r on r.id = u.role_id "
                            + "join erp_companies c on c.id = u.company_id "
                            + "where lower(u.username) = lower(?) and u.active = 1 and r.active = 1 and c.active = 1"
            );
            statement.setString(1, username);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                AppLogger.warning("No active ERP user found in database: " + username);
                return null;
            }
            AppLogger.info("Active ERP user found in database: " + username);
            return new DbAccount(
                    resultSet.getString("username"),
                    resultSet.getString("password_hash"),
                    resultSet.getString("display_name"),
                    resultSet.getString("role_name"),
                    resultSet.getString("company_name")
            );
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

    public void recordLogin(String username) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = Database.connect(config);
            statement = connection.prepareStatement("update erp_users set last_login_at = current_timestamp where lower(username) = lower(?)");
            statement.setString(1, username);
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

    public int countUsers() throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = Database.connect(config);
            statement = connection.prepareStatement("select count(*) from erp_users");
            resultSet = statement.executeQuery();
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

    public void createInitialAdmin(String passwordHash) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = Database.connect(config);
            statement = connection.prepareStatement(
                    "insert into erp_users (username, password_hash, display_name, email, company_id, role_id, active) "
                            + "select 'admin', ?, 'System Administrator', 'admin@linova.local', c.id, r.id, 1 "
                            + "from erp_companies c, erp_roles r "
                            + "where c.code = 'LINOVA' and r.code = 'ADMIN'"
            );
            statement.setString(1, passwordHash);
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

    public static class DbAccount {
        private final String username;
        private final String passwordHash;
        private final String displayName;
        private final String roleName;
        private final String companyName;

        public DbAccount(String username, String passwordHash, String displayName, String roleName, String companyName) {
            this.username = username;
            this.passwordHash = passwordHash;
            this.displayName = displayName;
            this.roleName = roleName;
            this.companyName = companyName;
        }

        public String getUsername() {
            return username;
        }

        public String getPasswordHash() {
            return passwordHash;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getRoleName() {
            return roleName;
        }

        public String getCompanyName() {
            return companyName;
        }
    }
}
