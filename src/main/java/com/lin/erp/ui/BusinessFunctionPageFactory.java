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
        if ("SALES_ORDER".equals(code)) {
            return new SalesOrderEntryPanel(owner, session, function);
        }
        if ("SALES_ORDER_QUERY".equals(code)) {
            return new SalesOrderQueryPanel(owner, session, function);
        }
        if ("SALES_SHIPMENT".equals(code)) {
            return new ShipmentEntryPanel(owner, session, function);
        }
        if ("SALES_SHIPMENT_QUERY".equals(code)) {
            return new ShipmentQueryPanel(owner, session, function);
        }
        if ("SALES_CONFIRMATION".equals(code)) {
            return new SalesConfirmationPanel(owner, session, function);
        }
        if ("SALES_RETURN".equals(code)) {
            return new SalesReturnPanel(owner, session, function);
        }
        if ("PROCUREMENT_PO".equals(code)) {
            return new PurchaseOrderEntryPanel(owner, session, function);
        }
        if ("PROCUREMENT_PO_QUERY".equals(code)) {
            return new PurchaseOrderQueryPanel(owner, session, function);
        }
        if ("PROCUREMENT_RECEIPT".equals(code)) {
            return new GoodsReceiptEntryPanel(owner, session, function);
        }
        if ("PROCUREMENT_RECEIPT_QUERY".equals(code)) {
            return new GoodsReceiptQueryPanel(owner, session, function);
        }
        if ("PROCUREMENT_CONFIRMATION".equals(code)) {
            return new PurchaseConfirmationPanel(owner, session, function);
        }
        if ("PROCUREMENT_RETURN".equals(code)) {
            return new PurchaseReturnPanel(owner, session, function);
        }
        return new BusinessFunctionPanel(owner, session, function);
    }
}
