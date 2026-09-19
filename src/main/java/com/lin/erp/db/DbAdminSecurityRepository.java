package com.lin.erp.db;

import com.lin.erp.config.DbConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class DbAdminSecurityRepository {
    private final DbConfig config;
    private final String actorUser;
    private final String actorCompany;
    private final String actorRole;

    public DbAdminSecurityRepository(DbConfig config, String actorUser, String actorCompany, String actorRole) {
        this.config = config;
        this.actorUser = actorUser == null ? "system" : actorUser;
        this.actorCompany = actorCompany == null ? "LINOVA" : actorCompany;
        this.actorRole = actorRole == null ? "SYSTEM" : actorRole;
    }

    public List<String[]> loadUsers() throws SQLException {
        Connection connection = Database.connect(config);
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            ensureSchema(connection);
            statement = connection.prepareStatement(
                    "select u.username, u.display_name, u.email, c.code as company_code, r.code as role_code, "
                            + "case when u.active = 1 then 'status.open' else 'status.cancelled' end as status, "
                            + "u.failed_login_count, date_format(u.locked_until, '%Y-%m-%d %H:%i') as locked_until "
                            + "from erp_users u join erp_companies c on c.id = u.company_id join erp_roles r on r.id = u.role_id "
                            + "order by u.username"
            );
            resultSet = statement.executeQuery();
            List<String[]> rows = new ArrayList<String[]>();
            while (resultSet.next()) {
                rows.add(new String[]{
                        resultSet.getString("username"),
                        resultSet.getString("display_name"),
                        resultSet.getString("email"),
                        resultSet.getString("company_code"),
                        resultSet.getString("role_code"),
                        resultSet.getString("status"),
                        String.valueOf(resultSet.getInt("failed_login_count")),
                        nullToEmpty(resultSet.getString("locked_until"))
                });
            }
            return rows;
        } finally {
            close(resultSet, statement, connection);
        }
    }

    public void saveUser(String originalUsername, String[] values) throws SQLException {
        String username = required(values, 0, "Username");
        String displayName = required(values, 1, "Display name");
        String email = value(values, 2);
        String companyCode = valueOr(values, 3, "LINOVA");
        String roleCode = valueOr(values, 4, "PLANNER");
        boolean active = !"status.cancelled".equals(value(values, 5));
        String resetPassword = value(values, 7);
        validatePasswordIfPresent(resetPassword);

        Connection connection = Database.connect(config);
        PreparedStatement statement = null;
        try {
            ensureSchema(connection);
            connection.setAutoCommit(false);
            long companyId = idFor(connection, "erp_companies", companyCode, "Company");
            long roleId = idFor(connection, "erp_roles", roleCode, "Role");
            if (originalUsername == null || originalUsername.trim().length() == 0) {
                ensureUsernameAvailable(connection, username, null);
                statement = connection.prepareStatement(
                        "insert into erp_users (username, password_hash, display_name, email, company_id, role_id, active, password_changed_at) "
                                + "values (?, ?, ?, ?, ?, ?, ?, current_timestamp)"
                );
                statement.setString(1, username);
                statement.setString(2, hashPassword(resetPassword.length() == 0 ? temporaryPassword(username) : resetPassword));
                statement.setString(3, displayName);
                bindNullable(statement, 4, email);
                statement.setLong(5, companyId);
                statement.setLong(6, roleId);
                statement.setInt(7, active ? 1 : 0);
                statement.executeUpdate();
                recordAudit(connection, "ADMIN_USERS", username, "CREATE", "SUCCESS", "User created.");
            } else {
                protectLastAdmin(connection, originalUsername, roleCode, active);
                ensureUsernameAvailable(connection, username, originalUsername);
                statement = connection.prepareStatement(
                        "update erp_users set username = ?, display_name = ?, email = ?, company_id = ?, role_id = ?, active = ?, "
                                + "failed_login_count = case when ? = 1 then 0 else failed_login_count end, "
                                + "locked_until = case when ? = 1 then null else locked_until end "
                                + (resetPassword.length() == 0 ? "" : ", password_hash = ?, password_changed_at = current_timestamp ")
                                + "where lower(username) = lower(?)"
                );
                statement.setString(1, username);
                statement.setString(2, displayName);
                bindNullable(statement, 3, email);
                statement.setLong(4, companyId);
                statement.setLong(5, roleId);
                statement.setInt(6, active ? 1 : 0);
                statement.setInt(7, active ? 1 : 0);
                statement.setInt(8, active ? 1 : 0);
                int index = 9;
                if (resetPassword.length() > 0) {
                    statement.setString(index++, hashPassword(resetPassword));
                }
                statement.setString(index, originalUsername);
                statement.executeUpdate();
                recordAudit(connection, "ADMIN_USERS", username, "UPDATE", "SUCCESS",
                        resetPassword.length() == 0 ? "User updated." : "User updated and password reset.");
            }
            connection.commit();
        } finally {
            if (statement != null) {
                statement.close();
            }
            close(null, null, connection);
        }
    }

    public void deactivateUser(String username) throws SQLException {
        Connection connection = Database.connect(config);
        PreparedStatement statement = null;
        try {
            ensureSchema(connection);
            connection.setAutoCommit(false);
            protectLastAdmin(connection, username, "PLANNER", false);
            statement = connection.prepareStatement("update erp_users set active = 0 where lower(username) = lower(?)");
            statement.setString(1, username);
            statement.executeUpdate();
            recordAudit(connection, "ADMIN_USERS", username, "DELETE", "SUCCESS", "User deactivated.");
            connection.commit();
        } finally {
            if (statement != null) {
                statement.close();
            }
            close(null, null, connection);
        }
    }

    public List<String[]> loadRoles() throws SQLException {
        Connection connection = Database.connect(config);
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            ensureSchema(connection);
            statement = connection.prepareStatement(
                    "select r.code, r.name, case when r.active = 1 then 'status.open' else 'status.cancelled' end as status, "
                            + "count(u.id) as users_count "
                            + "from erp_roles r left join erp_users u on u.role_id = r.id and u.active = 1 "
                            + "group by r.id, r.code, r.name, r.active order by r.code"
            );
            resultSet = statement.executeQuery();
            List<String[]> rows = new ArrayList<String[]>();
            while (resultSet.next()) {
                rows.add(new String[]{
                        resultSet.getString("code"),
                        resultSet.getString("name"),
                        resultSet.getString("status"),
                        String.valueOf(resultSet.getInt("users_count"))
                });
            }
            return rows;
        } finally {
            close(resultSet, statement, connection);
        }
    }

    public void saveRole(String originalCode, String[] values) throws SQLException {
        String code = required(values, 0, "Role code").toUpperCase();
        String name = required(values, 1, "Role name");
        boolean active = !"status.cancelled".equals(value(values, 2));
        Connection connection = Database.connect(config);
        PreparedStatement statement = null;
        try {
            ensureSchema(connection);
            connection.setAutoCommit(false);
            if ("ADMIN".equals(originalCode) && (!"ADMIN".equals(code) || !active)) {
                throw new SQLException("The built-in ADMIN role cannot be renamed or deactivated.");
            }
            if (originalCode == null || originalCode.trim().length() == 0) {
                ensureRoleAvailable(connection, code, null);
                statement = connection.prepareStatement("insert into erp_roles (code, name, active) values (?, ?, ?)");
                statement.setString(1, code);
                statement.setString(2, name);
                statement.setInt(3, active ? 1 : 0);
                statement.executeUpdate();
                recordAudit(connection, "ADMIN_ROLES", code, "CREATE", "SUCCESS", "Role created.");
            } else {
                ensureRoleAvailable(connection, code, originalCode);
                statement = connection.prepareStatement("update erp_roles set code = ?, name = ?, active = ? where upper(code) = upper(?)");
                statement.setString(1, code);
                statement.setString(2, name);
                statement.setInt(3, active ? 1 : 0);
                statement.setString(4, originalCode);
                statement.executeUpdate();
                recordAudit(connection, "ADMIN_ROLES", code, "UPDATE", "SUCCESS", "Role updated.");
            }
            connection.commit();
        } finally {
            if (statement != null) {
                statement.close();
            }
            close(null, null, connection);
        }
    }

    public List<String[]> loadPermissions() throws SQLException {
        Connection connection = Database.connect(config);
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            ensureSchema(connection);
            statement = connection.prepareStatement(
                    "select r.code as role_code, m.code as menu_code, m.name_key, rm.can_view, rm.can_create, rm.can_update, rm.can_approve "
                            + "from erp_role_menus rm join erp_roles r on r.id = rm.role_id join erp_menus m on m.id = rm.menu_id "
                            + "order by r.code, m.module_code, m.sort_order, m.code"
            );
            resultSet = statement.executeQuery();
            List<String[]> rows = new ArrayList<String[]>();
            while (resultSet.next()) {
                rows.add(new String[]{
                        resultSet.getString("role_code"),
                        resultSet.getString("menu_code"),
                        resultSet.getString("name_key"),
                        bool(resultSet.getInt("can_view")),
                        bool(resultSet.getInt("can_create")),
                        bool(resultSet.getInt("can_update")),
                        bool(resultSet.getInt("can_approve"))
                });
            }
            return rows;
        } finally {
            close(resultSet, statement, connection);
        }
    }

    public void savePermission(String[] values) throws SQLException {
        String roleCode = required(values, 0, "Role");
        String menuCode = required(values, 1, "Menu");
        Connection connection = Database.connect(config);
        PreparedStatement statement = null;
        try {
            ensureSchema(connection);
            connection.setAutoCommit(false);
            long roleId = idFor(connection, "erp_roles", roleCode, "Role");
            long menuId = idFor(connection, "erp_menus", menuCode, "Menu");
            statement = connection.prepareStatement(
                    "insert into erp_role_menus (role_id, menu_id, can_view, can_create, can_update, can_approve) "
                            + "values (?, ?, ?, ?, ?, ?) on duplicate key update "
                            + "can_view = values(can_view), can_create = values(can_create), can_update = values(can_update), can_approve = values(can_approve)"
            );
            statement.setLong(1, roleId);
            statement.setLong(2, menuId);
            statement.setInt(3, flag(values, 3));
            statement.setInt(4, flag(values, 4));
            statement.setInt(5, flag(values, 5));
            statement.setInt(6, flag(values, 6));
            statement.executeUpdate();
            recordAudit(connection, "ADMIN_PERMISSIONS", roleCode + ":" + menuCode, "UPDATE", "SUCCESS", "Permission updated.");
            connection.commit();
        } finally {
            if (statement != null) {
                statement.close();
            }
            close(null, null, connection);
        }
    }

    private void ensureSchema(Connection connection) throws SQLException {
        DatabaseSchema.ensureColumn(connection, "erp_users", "failed_login_count", "failed_login_count int not null default 0");
        DatabaseSchema.ensureColumn(connection, "erp_users", "locked_until", "locked_until timestamp null");
        DatabaseSchema.ensureColumn(connection, "erp_users", "password_changed_at", "password_changed_at timestamp null");
        DatabaseSchema.ensureColumn(connection, "erp_users", "password_expires_at", "password_expires_at timestamp null");
    }

    private long idFor(Connection connection, String table, String code, String label) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement("select id from " + table + " where upper(code) = upper(?) and active = 1");
            statement.setString(1, code);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                throw new SQLException(label + " does not exist or is inactive: " + code);
            }
            return resultSet.getLong(1);
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
    }

    private void ensureUsernameAvailable(Connection connection, String username, String originalUsername) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement("select username from erp_users where lower(username) = lower(?)");
            statement.setString(1, username);
            resultSet = statement.executeQuery();
            if (resultSet.next() && (originalUsername == null || !resultSet.getString(1).equalsIgnoreCase(originalUsername))) {
                throw new SQLException("Username already exists: " + username);
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
    }

    private void ensureRoleAvailable(Connection connection, String code, String originalCode) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement("select code from erp_roles where upper(code) = upper(?)");
            statement.setString(1, code);
            resultSet = statement.executeQuery();
            if (resultSet.next() && (originalCode == null || !resultSet.getString(1).equalsIgnoreCase(originalCode))) {
                throw new SQLException("Role code already exists: " + code);
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
    }

    private void protectLastAdmin(Connection connection, String username, String newRoleCode, boolean active) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement(
                    "select upper(r.code) from erp_users u join erp_roles r on r.id = u.role_id where lower(u.username) = lower(?)"
            );
            statement.setString(1, username);
            resultSet = statement.executeQuery();
            if (!resultSet.next() || !"ADMIN".equals(resultSet.getString(1))) {
                return;
            }
            if ("ADMIN".equalsIgnoreCase(newRoleCode) && active) {
                return;
            }
            resultSet.close();
            statement.close();
            statement = connection.prepareStatement(
                    "select count(*) from erp_users u join erp_roles r on r.id = u.role_id "
                            + "where u.active = 1 and upper(r.code) = 'ADMIN' and lower(u.username) <> lower(?)"
            );
            statement.setString(1, username);
            resultSet = statement.executeQuery();
            if (resultSet.next() && resultSet.getInt(1) == 0) {
                throw new SQLException("At least one active administrator must remain.");
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
    }

    private void recordAudit(Connection connection, String functionCode, String recordId, String actionCode, String resultCode, String details) throws SQLException {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(
                    "insert into erp_business_operation_logs (actor_user, company_code, role_code, function_code, record_id, action_code, result_code, details) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?)"
            );
            statement.setString(1, actorUser);
            statement.setString(2, actorCompany);
            statement.setString(3, actorRole);
            statement.setString(4, functionCode);
            statement.setNull(5, Types.BIGINT);
            statement.setString(6, actionCode);
            statement.setString(7, resultCode);
            statement.setString(8, recordId + " | " + details);
            statement.executeUpdate();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    private String hashPassword(String password) throws SQLException {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b & 0xff));
            }
            java.util.Arrays.fill(hash, (byte) 0);
            return builder.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new SQLException("SHA-256 is not available.", e);
        }
    }

    private String temporaryPassword(String username) {
        return "Reset@" + Math.abs(username.hashCode());
    }

    private void validatePasswordIfPresent(String password) throws SQLException {
        if (password == null || password.length() == 0) {
            return;
        }
        if (password.length() < 8 || !password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")) {
            throw new SQLException("Password must be at least 8 characters and include letters and numbers.");
        }
    }

    private int flag(String[] values, int index) {
        String value = value(values, index).trim().toLowerCase();
        return ("1".equals(value) || "true".equals(value) || "yes".equals(value) || "y".equals(value)
                || "view".equals(value) || "create".equals(value) || "update".equals(value) || "approve".equals(value)) ? 1 : 0;
    }

    private String bool(int value) {
        return value == 0 ? "false" : "true";
    }

    private String required(String[] values, int index, String label) throws SQLException {
        String value = value(values, index);
        if (value.length() == 0) {
            throw new SQLException(label + " is required.");
        }
        return value;
    }

    private String valueOr(String[] values, int index, String defaultValue) {
        String value = value(values, index);
        return value.length() == 0 ? defaultValue : value;
    }

    private String value(String[] values, int index) {
        if (values == null || index >= values.length || values[index] == null) {
            return "";
        }
        return values[index].trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void bindNullable(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.trim().length() == 0) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
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
            if (error == null) {
                error = e;
            }
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            if (error == null) {
                error = e;
            }
        }
        if (error != null) {
            throw error;
        }
    }
}
