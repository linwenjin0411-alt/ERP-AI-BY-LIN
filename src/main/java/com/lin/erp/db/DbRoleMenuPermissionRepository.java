package com.lin.erp.db;

import com.lin.erp.config.DbConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

public class DbRoleMenuPermissionRepository {
    private final DbConfig config;

    public DbRoleMenuPermissionRepository(DbConfig config) {
        this.config = config;
    }

    public RoleMenuPermission loadForMenu(String roleCode, String menuCode) throws SQLException {
        if (!config.isEnabled()) {
            return RoleMenuPermission.full(menuCode);
        }
        String normalizedRoleCode = normalize(roleCode);
        if (normalizedRoleCode.length() == 0 || menuCode == null || menuCode.trim().length() == 0) {
            return RoleMenuPermission.none(menuCode);
        }

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = Database.connect(config);
            statement = connection.prepareStatement(
                    "select rm.can_view, rm.can_create, rm.can_update, rm.can_approve "
                            + "from erp_role_menus rm "
                            + "join erp_roles r on r.id = rm.role_id and r.active = 1 "
                            + "join erp_menus m on m.id = rm.menu_id and m.active = 1 "
                            + "where upper(r.code) = ? and m.code = ?"
            );
            statement.setString(1, normalizedRoleCode);
            statement.setString(2, menuCode);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                return RoleMenuPermission.none(menuCode);
            }
            return permission(menuCode, normalizedRoleCode, resultSet);
        } finally {
            close(resultSet, statement, connection);
        }
    }

    public RoleMenuPermission loadForModule(String roleCode, String moduleCode) throws SQLException {
        if (!config.isEnabled()) {
            return RoleMenuPermission.full(moduleCode);
        }
        String normalizedRoleCode = normalize(roleCode);
        if (normalizedRoleCode.length() == 0 || moduleCode == null || moduleCode.trim().length() == 0) {
            return RoleMenuPermission.none(moduleCode);
        }

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = Database.connect(config);
            statement = connection.prepareStatement(
                    "select max(rm.can_view) as can_view, max(rm.can_create) as can_create, "
                            + "max(rm.can_update) as can_update, max(rm.can_approve) as can_approve "
                            + "from erp_role_menus rm "
                            + "join erp_roles r on r.id = rm.role_id and r.active = 1 "
                            + "join erp_menus m on m.id = rm.menu_id and m.active = 1 "
                            + "where upper(r.code) = ? and m.module_code = ?"
            );
            statement.setString(1, normalizedRoleCode);
            statement.setString(2, moduleCode);
            resultSet = statement.executeQuery();
            if (!resultSet.next() || !resultSet.getBoolean("can_view")) {
                return RoleMenuPermission.none(moduleCode);
            }
            return permission(moduleCode, normalizedRoleCode, resultSet);
        } finally {
            close(resultSet, statement, connection);
        }
    }

    private RoleMenuPermission permission(String scopeCode, String roleCode, ResultSet resultSet) throws SQLException {
        return new RoleMenuPermission(
                scopeCode,
                resultSet.getBoolean("can_view"),
                resultSet.getBoolean("can_create"),
                resultSet.getBoolean("can_update"),
                resultSet.getBoolean("can_approve"),
                "ADMIN".equals(roleCode)
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
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
