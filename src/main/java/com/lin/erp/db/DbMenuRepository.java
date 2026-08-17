package com.lin.erp.db;

import com.lin.erp.config.DbConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DbMenuRepository {
    private final DbConfig config;

    public DbMenuRepository(DbConfig config) {
        this.config = config;
    }

    public Map<String, List<MenuNode>> loadChildrenByModule() throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = Database.connect(config);
            statement = connection.prepareStatement(
                    "select code, parent_code, name_key, module_code "
                            + "from erp_menus "
                            + "where active = 1 "
                            + "order by sort_order, code"
            );
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
}
