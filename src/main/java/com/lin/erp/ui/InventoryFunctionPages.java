package com.lin.erp.ui;

import com.lin.erp.auth.UserSession;
import com.lin.erp.db.MenuNode;

import java.awt.Window;

class StockOverviewPanel extends BusinessFunctionPanel {
    StockOverviewPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}

class StockLedgerQueryPanel extends BusinessFunctionPanel {
    StockLedgerQueryPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}

class StockTransferPanel extends BusinessFunctionPanel {
    StockTransferPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}

class CycleCountPanel extends BusinessFunctionPanel {
    CycleCountPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}
