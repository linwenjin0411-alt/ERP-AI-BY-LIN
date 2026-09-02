package com.lin.erp.ui;

import com.lin.erp.auth.UserSession;
import com.lin.erp.db.MenuNode;

import java.awt.Window;

class SalesDetailReportPanel extends BusinessFunctionPanel {
    SalesDetailReportPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}

class PurchaseDetailReportPanel extends BusinessFunctionPanel {
    PurchaseDetailReportPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}

class InventoryDetailReportPanel extends BusinessFunctionPanel {
    InventoryDetailReportPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}

class ReceivableBalanceReportPanel extends BusinessFunctionPanel {
    ReceivableBalanceReportPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}

class PayableBalanceReportPanel extends BusinessFunctionPanel {
    PayableBalanceReportPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}
