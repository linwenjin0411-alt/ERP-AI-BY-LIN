package com.lin.erp.ui;

import com.lin.erp.auth.UserSession;
import com.lin.erp.db.MenuNode;

import java.awt.Window;

class SalesOrderEntryPanel extends BusinessFunctionPanel {
    SalesOrderEntryPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}

class SalesOrderQueryPanel extends BusinessFunctionPanel {
    SalesOrderQueryPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}

class ShipmentEntryPanel extends BusinessFunctionPanel {
    ShipmentEntryPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}

class ShipmentQueryPanel extends BusinessFunctionPanel {
    ShipmentQueryPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}

class SalesConfirmationPanel extends BusinessFunctionPanel {
    SalesConfirmationPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}

class SalesReturnPanel extends BusinessFunctionPanel {
    SalesReturnPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}
