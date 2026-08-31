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
        if ("INVENTORY_STOCK".equals(code)) {
            return new StockOverviewPanel(owner, session, function);
        }
        if ("INVENTORY_LEDGER".equals(code)) {
            return new StockLedgerQueryPanel(owner, session, function);
        }
        if ("INVENTORY_TRANSFER".equals(code)) {
            return new StockTransferPanel(owner, session, function);
        }
        if ("INVENTORY_COUNT".equals(code)) {
            return new CycleCountPanel(owner, session, function);
        }
        if ("MANUFACTURING_ORDER".equals(code)) {
            return new ProductionOrderEntryPanel(owner, session, function);
        }
        if ("MANUFACTURING_ORDER_QUERY".equals(code)) {
            return new ProductionOrderQueryPanel(owner, session, function);
        }
        if ("MANUFACTURING_ISSUE".equals(code)) {
            return new MaterialIssuePanel(owner, session, function);
        }
        if ("MANUFACTURING_COMPLETE".equals(code)) {
            return new ProductionCompletionPanel(owner, session, function);
        }
        if ("MANUFACTURING_RETURN".equals(code)) {
            return new MaterialReturnPanel(owner, session, function);
        }
        if ("FINANCE_AR".equals(code)) {
            return new AccountsReceivableQueryPanel(owner, session, function);
        }
        if ("FINANCE_COLLECTION".equals(code)) {
            return new CollectionEntryPanel(owner, session, function);
        }
        if ("FINANCE_COLLECTION_QUERY".equals(code)) {
            return new CollectionQueryPanel(owner, session, function);
        }
        if ("FINANCE_AP".equals(code)) {
            return new AccountsPayableQueryPanel(owner, session, function);
        }
        if ("FINANCE_PAYMENT".equals(code)) {
            return new PaymentEntryPanel(owner, session, function);
        }
        if ("FINANCE_PAYMENT_QUERY".equals(code)) {
            return new PaymentQueryPanel(owner, session, function);
        }
        if ("REPORT_SALES_DETAIL".equals(code)) {
            return new SalesDetailReportPanel(owner, session, function);
        }
        if ("REPORT_PURCHASE_DETAIL".equals(code)) {
            return new PurchaseDetailReportPanel(owner, session, function);
        }
        if ("REPORT_INVENTORY_DETAIL".equals(code)) {
            return new InventoryDetailReportPanel(owner, session, function);
        }
        if ("REPORT_AR_BALANCE".equals(code)) {
            return new ReceivableBalanceReportPanel(owner, session, function);
        }
        if ("REPORT_AP_BALANCE".equals(code)) {
            return new PayableBalanceReportPanel(owner, session, function);
        }
        if ("ADMIN_USERS".equals(code)) {
            return new UserManagementPanel(owner, session, function);
        }
        if ("ADMIN_ROLES".equals(code)) {
            return new RoleManagementPanel(owner, session, function);
        }
        if ("ADMIN_PERMISSIONS".equals(code)) {
            return new PermissionManagementPanel(owner, session, function);
        }
        if ("ADMIN_AUDIT".equals(code)) {
            return new OperationLogPanel(owner, session, function);
        }
        return new BusinessFunctionPanel(owner, session, function);
    }
}
