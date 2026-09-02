package com.lin.erp.db;

import com.lin.erp.config.DbConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DbMenuRepository {
    private final DbConfig config;

    public DbMenuRepository(DbConfig config) {
        this.config = config;
    }

    public Map<String, List<MenuNode>> loadChildrenByModule() throws SQLException {
        return loadChildrenByModule(null);
    }

    public Map<String, List<MenuNode>> loadChildrenByModule(String roleCode) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = Database.connect(config);
            String normalizedRoleCode = normalizeRoleCode(roleCode);
            if (normalizedRoleCode.length() == 0) {
                statement = connection.prepareStatement(
                        "select code, parent_code, name_key, module_code "
                                + "from erp_menus "
                                + "where active = 1 "
                                + "order by sort_order, code"
                );
            } else {
                statement = connection.prepareStatement(
                        "select distinct m.code, m.parent_code, m.name_key, m.module_code, m.sort_order "
                                + "from erp_menus m "
                                + "join erp_role_menus rm on rm.menu_id = m.id and rm.can_view = 1 "
                                + "join erp_roles r on r.id = rm.role_id and r.active = 1 "
                                + "where m.active = 1 and upper(r.code) = ? "
                                + "order by m.sort_order, m.code"
                );
                statement.setString(1, normalizedRoleCode);
            }
            resultSet = statement.executeQuery();

            List<MenuNode> orderedMenus = new ArrayList<MenuNode>();
            Map<String, MenuNode> byCode = new LinkedHashMap<String, MenuNode>();
            while (resultSet.next()) {
                MenuNode node = new MenuNode(
                        resultSet.getString("code"),
                        resultSet.getString("parent_code"),
                        resultSet.getString("name_key"),
                        resultSet.getString("module_code")
                );
                orderedMenus.add(node);
                byCode.put(node.getCode(), node);
            }

            for (MenuNode node : orderedMenus) {
                if (node.getParentCode() != null) {
                    MenuNode parent = byCode.get(node.getParentCode());
                    if (parent != null) {
                        parent.addChild(node);
                    }
                }
            }

            Map<String, List<MenuNode>> childrenByModule = new LinkedHashMap<String, List<MenuNode>>();
            for (MenuNode node : orderedMenus) {
                if (node.getParentCode() == null && node.hasChildren()) {
                    childrenByModule.put(node.getModuleCode(), new ArrayList<MenuNode>(node.getChildren()));
                }
            }
            return childrenByModule;
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

    private String normalizeRoleCode(String roleCode) {
        if (roleCode == null) {
            return "";
        }
        return roleCode.trim().toUpperCase(Locale.ROOT);
    }
}
