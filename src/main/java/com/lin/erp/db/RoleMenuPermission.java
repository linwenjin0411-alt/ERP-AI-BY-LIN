package com.lin.erp.db;

public class RoleMenuPermission {
    private final String scopeCode;
    private final boolean canView;
    private final boolean canCreate;
    private final boolean canUpdate;
    private final boolean canApprove;
    private final boolean administrator;

    public RoleMenuPermission(String scopeCode, boolean canView, boolean canCreate, boolean canUpdate, boolean canApprove, boolean administrator) {
        this.scopeCode = scopeCode;
        this.canView = canView;
        this.canCreate = canCreate;
        this.canUpdate = canUpdate;
        this.canApprove = canApprove;
        this.administrator = administrator;
    }

    public static RoleMenuPermission full(String scopeCode) {
        return new RoleMenuPermission(scopeCode, true, true, true, true, true);
    }

    public static RoleMenuPermission viewOnly(String scopeCode) {
        return new RoleMenuPermission(scopeCode, true, false, false, false, false);
    }

    public static RoleMenuPermission none(String scopeCode) {
        return new RoleMenuPermission(scopeCode, false, false, false, false, false);
    }

    public boolean allows(String actionKey) {
        if ("action.new".equals(actionKey)) {
            return canCreate;
        }
        if ("action.edit".equals(actionKey)) {
            return canUpdate;
        }
        if ("action.delete".equals(actionKey)) {
            return canDelete();
        }
        if ("action.approve".equals(actionKey)
                || "action.release".equals(actionKey)
                || "action.post".equals(actionKey)) {
            return canApprove;
        }
        return canView;
    }

    public boolean canDelete() {
        return administrator || canUpdate;
    }

    public String getScopeCode() {
        return scopeCode;
    }
}
