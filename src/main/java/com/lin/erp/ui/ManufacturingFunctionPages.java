package com.lin.erp.ui;

import com.lin.erp.auth.UserSession;
import com.lin.erp.db.MenuNode;

import java.awt.Window;

class ProductionOrderEntryPanel extends BusinessFunctionPanel {
    ProductionOrderEntryPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}

class ProductionOrderQueryPanel extends BusinessFunctionPanel {
    ProductionOrderQueryPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}

class MaterialIssuePanel extends BusinessFunctionPanel {
    MaterialIssuePanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}

class ProductionCompletionPanel extends BusinessFunctionPanel {
    ProductionCompletionPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}

class MaterialReturnPanel extends BusinessFunctionPanel {
    MaterialReturnPanel(Window owner, UserSession session, MenuNode function) {
        super(owner, session, function);
    }
}
