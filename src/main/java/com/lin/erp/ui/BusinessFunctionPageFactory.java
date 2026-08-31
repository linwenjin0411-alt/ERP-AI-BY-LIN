package com.lin.erp.ui;

import com.lin.erp.auth.UserSession;
import com.lin.erp.db.MenuNode;

import javax.swing.JPanel;
import java.awt.Window;

public final class BusinessFunctionPageFactory {
    private BusinessFunctionPageFactory() {
    }

    public static JPanel create(Window owner, UserSession session, MenuNode function) {
        String code = function.getCode();
        if ("MASTER_BOM".equals(code)) {
            return new BomManagementPanel(owner, session, function);
        }
        if ("MASTER_CUSTOMER".equals(code)) {
            return new CustomerManagementPanel(owner, session, function);
        }
        if ("MASTER_SUPPLIER".equals(code)) {
            return new SupplierManagementPanel(owner, session, function);
        }
        if ("MASTER_WAREHOUSE".equals(code)) {
            return new WarehouseManagementPanel(owner, session, function);
        }
        return new BusinessFunctionPanel(owner, session, function);
    }
}
