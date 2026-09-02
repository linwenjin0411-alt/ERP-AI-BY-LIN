package com.lin.erp.ui;

import com.lin.erp.auth.UserSession;
import com.lin.erp.db.MenuNode;

import java.awt.Window;

class BomManagementPanel extends BusinessFunctionPanel {
    BomManagementPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}

class CustomerManagementPanel extends BusinessFunctionPanel {
    CustomerManagementPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}

class SupplierManagementPanel extends BusinessFunctionPanel {
    SupplierManagementPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}

class WarehouseManagementPanel extends BusinessFunctionPanel {
    WarehouseManagementPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}
