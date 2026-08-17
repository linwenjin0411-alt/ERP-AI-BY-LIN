package com.lin.erp.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MenuNode {
    private final String code;
    private final String parentCode;
    private final String nameKey;
    private final String moduleCode;
    private final List<MenuNode> children = new ArrayList<MenuNode>();

    public MenuNode(String code, String parentCode, String nameKey, String moduleCode) {
        this.code = code;
        this.parentCode = parentCode;
        this.nameKey = nameKey;
        this.moduleCode = moduleCode;
    }

    public String getCode() {
        return code;
    }

    public String getParentCode() {
        return parentCode;
    }

    public String getNameKey() {
        return nameKey;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void addChild(MenuNode child) {
        if (child != null) {
            children.add(child);
        }
    }

    public List<MenuNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public void removeChildByCode(String childCode) {
        for (int i = children.size() - 1; i >= 0; i--) {
            if (childCode.equals(children.get(i).getCode())) {
                children.remove(i);
            }
        }
    }
}
