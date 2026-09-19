package com.lin.erp.ui;

import com.lin.erp.auth.UserSession;
import com.lin.erp.db.MenuNode;

import java.awt.Window;

class UserManagementPanel extends AdminSecurityPanel {
    UserManagementPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}

class RoleManagementPanel extends AdminSecurityPanel {
    RoleManagementPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}

class PermissionManagementPanel extends AdminSecurityPanel {
    PermissionManagementPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}

class OperationLogPanel extends BusinessFunctionPanel {
    OperationLogPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}
