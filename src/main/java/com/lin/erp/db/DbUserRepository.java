package com.lin.erp.db;

import com.lin.erp.config.DbConfig;
import com.lin.erp.logging.AppLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class DbUserRepository {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

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
            ensureSecurityColumns(connection);
            statement = connection.prepareStatement(
                    "select u.username, u.password_hash, u.display_name, r.code as role_code, r.name as role_name, c.name as company_name, "
                            + "u.failed_login_count, u.locked_until "
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
                    resultSet.getString("role_code"),
                    resultSet.getString("role_name"),
                    resultSet.getString("company_name"),
                    resultSet.getInt("failed_login_count"),
                    resultSet.getTimestamp("locked_until")
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
            ensureSecurityColumns(connection);
            statement = connection.prepareStatement(
                    "update erp_users set last_login_at = current_timestamp, failed_login_count = 0, locked_until = null "
                            + "where lower(username) = lower(?)"
            );
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

    public void recordFailedLogin(String username) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = Database.connect(config);
            ensureSecurityColumns(connection);
            statement = connection.prepareStatement(
                    "update erp_users set failed_login_count = failed_login_count + 1, "
                            + "locked_until = case when failed_login_count + 1 >= ? then timestampadd(minute, ?, current_timestamp) else locked_until end "
                            + "where lower(username) = lower(?) and active = 1"
            );
            statement.setInt(1, MAX_FAILED_ATTEMPTS);
            statement.setInt(2, LOCK_MINUTES);
            statement.setString(3, username);
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

    private void ensureSecurityColumns(Connection connection) throws SQLException {
        DatabaseSchema.ensureVarcharLength(connection, "erp_users", "password_hash", 255, "password_hash varchar(255) not null");
        DatabaseSchema.ensureColumn(connection, "erp_users", "failed_login_count", "failed_login_count int not null default 0");
        DatabaseSchema.ensureColumn(connection, "erp_users", "locked_until", "locked_until timestamp null");
        DatabaseSchema.ensureColumn(connection, "erp_users", "password_changed_at", "password_changed_at timestamp null");
        DatabaseSchema.ensureColumn(connection, "erp_users", "password_expires_at", "password_expires_at timestamp null");
    }

    public void updatePasswordHash(String username, String passwordHash) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = Database.connect(config);
            ensureSecurityColumns(connection);
            statement = connection.prepareStatement(
                    "update erp_users set password_hash = ?, password_changed_at = current_timestamp "
                            + "where lower(username) = lower(?) and active = 1"
            );
            statement.setString(1, passwordHash);
            statement.setString(2, username);
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

    public int countActiveAdministrators() throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = Database.connect(config);
            statement = connection.prepareStatement(
                    "select count(*) from erp_users u "
                            + "join erp_roles r on r.id = u.role_id and r.active = 1 "
                            + "where u.active = 1 and upper(r.code) = 'ADMIN'"
            );
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
            int inserted = statement.executeUpdate();
            if (inserted != 1) {
                throw new SQLException("Initial administrator was not created. Check that company LINOVA and role ADMIN exist.");
            }
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
        private final String roleCode;
        private final String roleName;
        private final String companyName;
        private final int failedLoginCount;
        private final Timestamp lockedUntil;

        public DbAccount(String username, String passwordHash, String displayName, String roleCode, String roleName,
                         String companyName, int failedLoginCount, Timestamp lockedUntil) {
            this.username = username;
            this.passwordHash = passwordHash;
            this.displayName = displayName;
            this.roleCode = roleCode;
            this.roleName = roleName;
            this.companyName = companyName;
            this.failedLoginCount = failedLoginCount;
            this.lockedUntil = lockedUntil;
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

        public String getRoleCode() {
            return roleCode;
        }

        public String getRoleName() {
            return roleName;
        }

        public String getCompanyName() {
            return companyName;
        }

        public int getFailedLoginCount() {
            return failedLoginCount;
        }

        public boolean isLocked() {
            return lockedUntil != null && lockedUntil.after(new Timestamp(System.currentTimeMillis()));
        }

        public Timestamp getLockedUntil() {
            return lockedUntil;
        }
    }
}
