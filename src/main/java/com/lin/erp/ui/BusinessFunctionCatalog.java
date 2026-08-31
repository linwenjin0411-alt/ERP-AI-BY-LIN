package com.lin.erp.ui;

import com.lin.erp.db.MenuNode;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BusinessFunctionCatalog {
    private static final Map<String, BusinessFunctionDefinition> DEFINITIONS =
            new LinkedHashMap<String, BusinessFunctionDefinition>();

    static {
        addMasterPages();
        addSalesPages();
        addProcurementPages();
        addInventoryPages();
        addManufacturingPages();
        addFinancePages();
        addReportPages();
        addAdminPages();
    }

    private BusinessFunctionCatalog() {
    }

    public static BusinessFunctionDefinition forFunction(MenuNode function) {
        BusinessFunctionDefinition definition = function == null ? null : DEFINITIONS.get(function.getCode());
        if (definition != null) {
            return definition;
        }
        return fallback(function);
    }

    private static void add(String code, String[] fields, String[] values, String[] columns, String[][] rows,
                            String flow, String upstream, String downstream) {
        DEFINITIONS.put(code, new BusinessFunctionDefinition(fields, values, columns, rows, flow, upstream, downstream));
    }

    private static String[] transactionFields() {
        return new String[]{
                "function.field.documentNo", "function.field.businessDate", "function.field.status",
                "function.field.partner", "function.field.item", "function.field.quantity",
                "function.field.warehouse", "function.field.owner", "function.field.memo"
        };
    }

    private static String[] transactionColumns() {
        return new String[]{
                "function.table.line", "column.item", "column.qty", "column.status", "column.next"
        };
    }

    private static BusinessFunctionDefinition fallback(MenuNode function) {
        String code = function == null ? "GEN" : function.getCode();
        String module = function == null ? "MASTER" : function.getModuleCode();
        return new BusinessFunctionDefinition(
                transactionFields(),
                new String[]{codePrefix(code) + "-260831", "2026/08/31", "status.open", "LINOVA-001", "FG-3007", "120", "JP01", "user.admin.name", code},
                transactionColumns(),
                new String[][]{
                        {"1", "FG-3007", "120", "status.open", "action.details"},
                        {"2", "RM-1008", "420", "status.released", "term.stockOverview"}
                },
                module + " business page",
                "Master data and source document",
                "Next business page and operation audit"
        );
    }

    private static String codePrefix(String code) {
        return code == null || code.length() <= 3 ? "DOC" : code.substring(0, 3);
    }

    private static void addMasterPages() {
        add("MASTER_BOM",
                new String[]{"column.id", "column.item", "column.status", "column.qty", "column.uom", "column.owner", "function.field.memo"},
                new String[]{"BOM-FG-3007", "FG-3007", "status.released", "1", "EA", "owner.planner", "Smart actuator assembly"},
                new String[]{"function.table.line", "column.item", "column.qty", "column.uom", "column.status"},
                new String[][]{{"10", "RM-1008", "2", "EA", "status.released"}, {"20", "PK-2210", "1", "EA", "status.released"}},
                "Finished good -> BOM -> Raw material -> Production issue",
                "Item master",
                "MRP and production material issue");
        add("MASTER_CUSTOMER",
                new String[]{"column.id", "column.name", "function.field.partner", "column.status", "column.owner", "function.field.memo"},
                new String[]{"CUS-3001", "Northwind Manufacturing", "NET30 / Tokyo", "status.open", "owner.sales", "Settlement: monthly close"},
                new String[]{"column.id", "column.name", "column.status", "column.next"},
                new String[][]{{"CUS-3001", "Northwind Manufacturing", "status.open", "term.salesOrder"}, {"CUS-3002", "Taiyo Robotics", "status.released", "term.receivable"}},
                "Customer -> Sales order -> Shipment -> AR",
                "Trading partner master",
                "Sales order and accounts receivable");
        add("MASTER_SUPPLIER",
                new String[]{"column.id", "column.name", "function.field.partner", "column.status", "column.owner", "function.field.memo"},
                new String[]{"SUP-2007", "Sakura Metals", "NET45 / Osaka", "status.open", "owner.procurement", "Preferred supplier for motors"},
                new String[]{"column.id", "column.name", "column.status", "column.next"},
                new String[][]{{"SUP-2007", "Sakura Metals", "status.open", "term.purchaseOrder"}, {"SUP-2011", "Kanto Package", "status.released", "term.payable"}},
                "Supplier -> Purchase order -> Receipt -> AP",
                "Trading partner master",
                "Purchase order and accounts payable");
        add("MASTER_WAREHOUSE",
                new String[]{"column.id", "column.warehouse", "column.plant", "column.status", "column.owner", "function.field.memo"},
                new String[]{"WH-A", "Raw material warehouse", "JP01", "status.open", "owner.production", "Bins A-01 to A-20"},
                new String[]{"column.warehouse", "column.item", "column.qty", "column.status"},
                new String[][]{{"WH-A", "RM-1008", "420", "status.shortage"}, {"FG-01", "FG-3007", "96", "status.ready"}},
                "Warehouse -> Stock -> Receipt / Shipment / Issue / Count",
                "Plant and location master",
                "Inventory ledger and current stock");
    }

    private static void addSalesPages() {
        add("SALES_ORDER", transactionFields(),
                new String[]{"SO-2608-104", "2026/08/31", "status.released", "CUS-3001", "FG-3007", "120", "FG-01", "owner.sales", "Delivery due 2026/09/05"},
                transactionColumns(),
                new String[][]{{"1", "FG-3007", "120", "status.released", "term.shipment"}, {"2", "SP-1020", "120", "status.open", "term.delivery"}},
                "Customer / Item -> Sales order -> Shipment",
                "Customer master and item master",
                "Shipment registration");
        add("SALES_ORDER_QUERY", transactionFields(),
                new String[]{"SO-2608-104", "2026/08/31", "status.released", "CUS-3001", "FG-3007", "120", "FG-01", "owner.sales", "Ordered 120 / shipped 80 / remaining 40"},
                new String[]{"column.id", "column.customer", "column.item", "column.qty", "column.status", "column.next"},
                new String[][]{{"SO-2608-104", "Taiyo Robotics", "FG-3007", "120", "status.released", "term.shipment"}, {"SO-2608-118", "Northwind Manufacturing", "FG-3012", "80", "status.open", "term.delivery"}},
                "Sales order -> Query -> Shipment / Sales confirmation",
                "Sales order registration",
                "Shipment records and sales records");
        add("SALES_SHIPMENT", transactionFields(),
                new String[]{"DN-2608-044", "2026/08/31", "status.ready", "CUS-3001", "FG-3007", "80", "FG-01", "owner.sales", "Partial shipment allowed"},
                transactionColumns(),
                new String[][]{{"1", "FG-3007", "80", "status.ready", "menu.sales.confirmation"}, {"2", "FG-3007", "40", "status.open", "term.shipment"}},
                "Sales order + Stock -> Shipment -> Sales confirmation",
                "Sales order and available stock",
                "Inventory decrease and sales confirmation");
        add("SALES_SHIPMENT_QUERY", transactionFields(),
                new String[]{"DN-2608-044", "2026/08/31", "status.ready", "CUS-3001", "FG-3007", "80", "FG-01", "owner.sales", "Shipment query"},
                new String[]{"column.id", "column.customer", "column.item", "column.qty", "column.date", "column.status"},
                new String[][]{{"DN-2608-044", "Apex Components", "FG-3007", "80", "2026-08-31", "status.ready"}, {"DN-2608-039", "Northwind Manufacturing", "FG-3012", "30", "2026-08-30", "status.posted"}},
                "Shipment -> Query -> Sales confirmation",
                "Shipment registration",
                "Sales confirmation");
        add("SALES_CONFIRMATION", transactionFields(),
                new String[]{"SA-2608-018", "2026/08/31", "status.open", "CUS-3001", "FG-3007", "80", "FG-01", "owner.finance", "Amount 52800"},
                new String[]{"column.id", "column.customer", "column.amount", "column.status", "column.next"},
                new String[][]{{"SA-2608-018", "Northwind Manufacturing", "$52,800", "status.open", "term.receivable"}, {"SA-2608-019", "Taiyo Robotics", "$124,600", "status.posted", "term.ar"}},
                "Shipment -> Sales confirmation -> Accounts receivable",
                "Posted shipment",
                "Receivable balance");
        add("SALES_RETURN", transactionFields(),
                new String[]{"SR-2608-006", "2026/08/31", "status.open", "CUS-3001", "FG-3007", "5", "FG-01", "owner.sales", "Return against SA-2608-018"},
                transactionColumns(),
                new String[][]{{"1", "FG-3007", "5", "status.open", "term.stockOverview"}, {"2", "FG-3012", "2", "status.released", "term.receivable"}},
                "Original sales record -> Sales return -> Inventory / AR adjustment",
                "Sales confirmation",
                "Inventory increase and receivable adjustment");
    }

    private static void addProcurementPages() {
        add("PROCUREMENT_PO", transactionFields(),
                new String[]{"PO-45000127", "2026/08/31", "status.released", "SUP-2007", "RM-1008", "3000", "WH-A", "owner.procurement", "Due 2026/09/03"},
                transactionColumns(),
                new String[][]{{"1", "RM-1008", "3000", "status.released", "term.receipt"}, {"2", "PK-2210", "8000", "status.late", "term.receipt"}},
                "Supplier / Item -> Purchase order -> Receipt",
                "Supplier master and item master",
                "Goods receipt");
        add("PROCUREMENT_PO_QUERY", transactionFields(),
                new String[]{"PO-45000127", "2026/08/31", "status.late", "SUP-2007", "PK-2210", "8000", "WH-B", "owner.procurement", "Ordered 8000 / received 2000 / remaining 6000"},
                new String[]{"column.id", "column.supplier", "column.item", "column.qty", "column.due", "column.status"},
                new String[][]{{"PO-45000127", "Kanto Package", "PK-2210", "8000", "2026-09-03", "status.late"}, {"PO-45000132", "Sakura Metals", "RM-1008", "3000", "2026-09-05", "status.released"}},
                "Purchase order -> Query -> Receipt / Purchase confirmation",
                "Purchase order registration",
                "Receipt records and purchase records");
        add("PROCUREMENT_RECEIPT", transactionFields(),
                new String[]{"GR-50001988", "2026/08/31", "status.ready", "SUP-2007", "RM-1008", "1200", "WH-A", "owner.procurement", "Partial receipt"},
                transactionColumns(),
                new String[][]{{"1", "RM-1008", "1200", "status.ready", "menu.procurement.confirmation"}, {"2", "PK-2210", "2000", "status.open", "term.stockOverview"}},
                "Purchase order -> Receipt -> Inventory -> Purchase confirmation",
                "Purchase order",
                "Inventory increase and purchase confirmation");
        add("PROCUREMENT_RECEIPT_QUERY", transactionFields(),
                new String[]{"GR-50001988", "2026/08/31", "status.ready", "SUP-2007", "RM-1008", "1200", "WH-A", "owner.procurement", "Receipt query"},
                new String[]{"column.id", "column.supplier", "column.item", "column.qty", "column.date", "column.status"},
                new String[][]{{"GR-50001988", "Global Resin", "RM-1304", "1200", "2026-08-31", "status.ready"}, {"GR-50001990", "Sakura Metals", "RM-1008", "3000", "2026-08-30", "status.posted"}},
                "Receipt -> Query -> Purchase confirmation",
                "Receipt registration",
                "Purchase confirmation and inventory");
        add("PROCUREMENT_CONFIRMATION", transactionFields(),
                new String[]{"PC-2608-021", "2026/08/31", "status.open", "SUP-2007", "RM-1008", "1200", "WH-A", "owner.finance", "Amount 24600"},
                new String[]{"column.id", "column.supplier", "column.amount", "column.status", "column.next"},
                new String[][]{{"PC-2608-021", "Sakura Metals", "$24,600", "status.open", "term.payable"}, {"PC-2608-022", "Kanto Package", "$18,200", "status.posted", "term.ap"}},
                "Receipt -> Purchase confirmation -> Accounts payable",
                "Receipt record",
                "Payable balance");
        add("PROCUREMENT_RETURN", transactionFields(),
                new String[]{"PRT-2608-003", "2026/08/31", "status.open", "SUP-2007", "RM-1008", "60", "WH-A", "owner.procurement", "Return against PC-2608-021"},
                transactionColumns(),
                new String[][]{{"1", "RM-1008", "60", "status.open", "term.stockOverview"}, {"2", "PK-2210", "30", "status.released", "term.payable"}},
                "Original purchase record -> Purchase return -> Inventory / AP adjustment",
                "Purchase confirmation",
                "Inventory decrease and payable adjustment");
    }

    private static void addInventoryPages() {
        add("INVENTORY_STOCK", transactionFields(),
                new String[]{"STK-260831", "2026/08/31", "status.open", "JP01", "RM-1008", "420", "WH-A", "owner.production", "Available 360"},
                new String[]{"column.item", "column.warehouse", "column.qty", "column.status", "column.risk"},
                new String[][]{{"RM-1008", "WH-A", "420", "status.shortage", "risk.high"}, {"FG-3007", "FG-01", "96", "status.ready", "risk.low"}},
                "All stock movements -> Current inventory",
                "Receipts, shipments, issues, completions, transfers, and counts",
                "Available quantity decisions");
        add("INVENTORY_LEDGER", transactionFields(),
                new String[]{"LED-260831", "2026/08/31", "status.open", "JP01", "RM-1008", "-80", "WH-A", "owner.system", "Source DN-2608-044"},
                new String[]{"column.date", "column.type", "column.id", "column.item", "column.warehouse", "column.qty"},
                new String[][]{{"2026-08-31", "menu.sales.shipment", "DN-2608-044", "FG-3007", "FG-01", "-80"}, {"2026-08-31", "menu.procurement.receipt", "GR-50001988", "RM-1008", "WH-A", "+1200"}},
                "Inventory changes -> Stock ledger -> Current inventory",
                "All inventory-changing pages",
                "Stock overview");
        add("INVENTORY_TRANSFER", transactionFields(),
                new String[]{"TR-2608-012", "2026/08/31", "status.open", "JP01", "RM-1008", "200", "WH-A -> WH-B", "owner.production", "Total stock unchanged"},
                transactionColumns(),
                new String[][]{{"1", "RM-1008", "-200", "status.open", "term.stockOverview"}, {"2", "RM-1008", "+200", "status.open", "term.stockOverview"}},
                "Source warehouse decrease -> Transfer -> Target warehouse increase",
                "Current stock",
                "Stock ledger and current stock");
        add("INVENTORY_COUNT", transactionFields(),
                new String[]{"CC-2608-009", "2026/08/31", "status.open", "JP01", "RM-1008", "418", "WH-A", "owner.production", "System qty 420 / count qty 418"},
                new String[]{"column.item", "column.warehouse", "column.qty", "column.status", "column.next"},
                new String[][]{{"RM-1008", "WH-A", "-2", "status.open", "term.adjustment"}, {"PK-2210", "WH-B", "+12", "status.ready", "term.stockOverview"}},
                "Inventory -> Count -> Adjustment -> Stock ledger",
                "System inventory",
                "Inventory adjustment and ledger");
    }

    private static void addManufacturingPages() {
        add("MANUFACTURING_ORDER", transactionFields(),
                new String[]{"MO-2608-004", "2026/08/31", "status.open", "LINOVA", "FG-3007", "120", "JP01", "owner.planner", "Plan 2026/09/01-2026/09/05"},
                transactionColumns(),
                new String[][]{{"1", "FG-3007", "120", "status.open", "term.materialIssue"}, {"2", "RM-1008", "240", "status.shortage", "term.purchaseRequest"}},
                "Finished good + BOM -> Production order -> Issue -> Completion",
                "Item master and BOM",
                "Material issue and completion");
        add("MANUFACTURING_ORDER_QUERY", transactionFields(),
                new String[]{"MO-2608-004", "2026/08/31", "status.blocked", "LINOVA", "FG-3007", "120", "JP01", "owner.planner", "Planned 120 / complete 40 / remaining 80"},
                new String[]{"column.id", "column.item", "column.qty", "column.status", "column.due", "column.risk"},
                new String[][]{{"MO-2608-004", "FG-3007", "120", "status.blocked", "2026-09-05", "status.shortage"}, {"MO-2608-005", "FG-3041", "80", "status.released", "2026-09-06", "risk.low"}},
                "Production order -> Query -> Issue / Completion",
                "Production order registration",
                "Issue records and completion records");
        add("MANUFACTURING_ISSUE", transactionFields(),
                new String[]{"MI-2608-033", "2026/08/31", "status.open", "MO-2608-004", "RM-1008", "240", "WH-A", "owner.production", "Issue by BOM"},
                transactionColumns(),
                new String[][]{{"1", "RM-1008", "240", "status.open", "term.stockOverview"}, {"2", "PK-2210", "120", "status.open", "term.stockOverview"}},
                "Production order + BOM + Raw stock -> Material issue -> Inventory decrease",
                "Production order and BOM",
                "Raw material inventory decrease");
        add("MANUFACTURING_COMPLETE", transactionFields(),
                new String[]{"MC-2608-017", "2026/08/31", "status.open", "MO-2608-004", "FG-3007", "40", "FG-01", "owner.production", "Partial completion"},
                transactionColumns(),
                new String[][]{{"1", "FG-3007", "40", "status.open", "term.stockOverview"}, {"2", "FG-3007", "80", "status.open", "term.confirmation"}},
                "Production order -> Completion -> Finished goods inventory increase",
                "Production order",
                "Finished goods stock increase");
        add("MANUFACTURING_RETURN", transactionFields(),
                new String[]{"MR-2608-004", "2026/08/31", "status.open", "MO-2608-004", "RM-1008", "12", "WH-A", "owner.production", "Unused material return"},
                transactionColumns(),
                new String[][]{{"1", "RM-1008", "12", "status.open", "term.stockOverview"}, {"2", "PK-2210", "4", "status.ready", "term.stockOverview"}},
                "Material issue -> Material return -> Raw material stock increase",
                "Material issue record",
                "Raw material inventory increase");
    }

    private static void addFinancePages() {
        add("FINANCE_AR", transactionFields(),
                new String[]{"AR-2608-018", "2026/08/31", "status.open", "CUS-3001", "SA-2608-018", "52800", "AR", "owner.finance", "Uncollected 52800"},
                new String[]{"column.customer", "column.id", "column.amount", "column.status", "column.next"},
                new String[][]{{"Northwind Manufacturing", "SA-2608-018", "$52,800", "status.open", "menu.finance.collection"}, {"Taiyo Robotics", "SA-2608-019", "$124,600", "status.released", "menu.finance.collection"}},
                "Sales confirmation -> AR -> Collection",
                "Sales confirmation",
                "Collection registration");
        add("FINANCE_COLLECTION", transactionFields(),
                new String[]{"RC-2608-028", "2026/08/31", "status.open", "CUS-3001", "AR-2608-018", "20000", "BANK-JP01", "owner.finance", "Partial collection"},
                new String[]{"column.id", "column.customer", "column.amount", "column.date", "column.status"},
                new String[][]{{"RC-2608-028", "Northwind Manufacturing", "$20,000", "2026-08-31", "status.open"}, {"RC-2608-025", "Taiyo Robotics", "$124,600", "2026-08-30", "status.posted"}},
                "AR -> Collection -> AR balance reduction",
                "Receivable balance",
                "Uncollected balance");
        add("FINANCE_COLLECTION_QUERY", transactionFields(),
                new String[]{"RC-2608-028", "2026/08/31", "status.open", "CUS-3001", "AR-2608-018", "20000", "BANK-JP01", "owner.finance", "Receipt query"},
                new String[]{"column.customer", "column.amount", "column.date", "column.status", "column.next"},
                new String[][]{{"Northwind Manufacturing", "$20,000", "2026-08-31", "status.open", "menu.report.arBalance"}, {"Taiyo Robotics", "$124,600", "2026-08-30", "status.posted", "term.gl"}},
                "Customer -> AR -> Collection query",
                "Collection registration",
                "AR balance report");
        add("FINANCE_AP", transactionFields(),
                new String[]{"AP-2608-021", "2026/08/31", "status.open", "SUP-2007", "PC-2608-021", "24600", "AP", "owner.finance", "Unpaid 24600"},
                new String[]{"column.supplier", "column.id", "column.amount", "column.status", "column.next"},
                new String[][]{{"Sakura Metals", "PC-2608-021", "$24,600", "status.open", "menu.finance.payment"}, {"Kanto Package", "PC-2608-022", "$18,200", "status.released", "menu.finance.payment"}},
                "Purchase confirmation -> AP -> Payment",
                "Purchase confirmation",
                "Payment registration");
        add("FINANCE_PAYMENT", transactionFields(),
                new String[]{"PY-2608-014", "2026/08/31", "status.open", "SUP-2007", "AP-2608-021", "12000", "BANK-JP01", "owner.finance", "Partial payment"},
                new String[]{"column.id", "column.supplier", "column.amount", "column.date", "column.status"},
                new String[][]{{"PY-2608-014", "Sakura Metals", "$12,000", "2026-08-31", "status.open"}, {"PY-2608-011", "Kanto Package", "$18,200", "2026-08-30", "status.posted"}},
                "AP -> Payment -> AP balance reduction",
                "Payable balance",
                "Unpaid balance");
        add("FINANCE_PAYMENT_QUERY", transactionFields(),
                new String[]{"PY-2608-014", "2026/08/31", "status.open", "SUP-2007", "AP-2608-021", "12000", "BANK-JP01", "owner.finance", "Payment query"},
                new String[]{"column.supplier", "column.amount", "column.date", "column.status", "column.next"},
                new String[][]{{"Sakura Metals", "$12,000", "2026-08-31", "status.open", "menu.report.apBalance"}, {"Kanto Package", "$18,200", "2026-08-30", "status.posted", "term.gl"}},
                "Supplier -> AP -> Payment query",
                "Payment registration",
                "AP balance report");
    }

    private static void addReportPages() {
        add("REPORT_SALES_DETAIL", transactionFields(),
                new String[]{"RPT-SALES-2608", "2026/08/31", "status.ready", "CUS-3001", "FG-3007", "80", "ALL", "owner.sales", "Period sales quantity and amount"},
                new String[]{"column.date", "column.customer", "column.item", "column.qty", "column.amount"},
                new String[][]{{"2026-08-31", "Northwind Manufacturing", "FG-3007", "80", "$52,800"}, {"2026-08-30", "Taiyo Robotics", "FG-3041", "120", "$124,600"}},
                "Sales confirmation and sales return -> Sales detail",
                "Sales confirmation and returns",
                "Sales analysis and AR");
        add("REPORT_PURCHASE_DETAIL", transactionFields(),
                new String[]{"RPT-PUR-2608", "2026/08/31", "status.ready", "SUP-2007", "RM-1008", "1200", "ALL", "owner.procurement", "Period purchase quantity and amount"},
                new String[]{"column.date", "column.supplier", "column.item", "column.qty", "column.amount"},
                new String[][]{{"2026-08-31", "Sakura Metals", "RM-1008", "1200", "$24,600"}, {"2026-08-30", "Kanto Package", "PK-2210", "2000", "$18,200"}},
                "Purchase confirmation and purchase return -> Purchase detail",
                "Purchase confirmation and returns",
                "Purchase analysis and AP");
        add("REPORT_INVENTORY_DETAIL", transactionFields(),
                new String[]{"RPT-STK-2608", "2026/08/31", "status.ready", "JP01", "RM-1008", "420", "WH-A", "owner.production", "Opening / inbound / outbound / adjustment / closing"},
                new String[]{"column.item", "column.warehouse", "column.qty", "column.status", "column.risk"},
                new String[][]{{"RM-1008", "WH-A", "420", "status.shortage", "risk.high"}, {"FG-3007", "FG-01", "96", "status.ready", "risk.low"}},
                "Stock ledger -> Inventory detail",
                "Inventory ledger",
                "Inventory valuation and replenishment");
        add("REPORT_AR_BALANCE", transactionFields(),
                new String[]{"RPT-AR-2608", "2026/08/31", "status.ready", "CUS-3001", "AR", "318400", "ALL", "owner.finance", "Receivable / collection / balance"},
                new String[]{"column.customer", "column.amount", "column.status", "column.next"},
                new String[][]{{"Northwind Manufacturing", "$52,800", "status.open", "menu.finance.collection"}, {"Taiyo Robotics", "$124,600", "status.released", "menu.finance.collectionQuery"}},
                "Sales confirmation + Collection -> AR balance",
                "Receivables and collections",
                "Credit control and GL");
        add("REPORT_AP_BALANCE", transactionFields(),
                new String[]{"RPT-AP-2608", "2026/08/31", "status.ready", "SUP-2007", "AP", "174200", "ALL", "owner.finance", "Payable / payment / balance"},
                new String[]{"column.supplier", "column.amount", "column.status", "column.next"},
                new String[][]{{"Sakura Metals", "$24,600", "status.open", "menu.finance.payment"}, {"Kanto Package", "$18,200", "status.released", "menu.finance.paymentQuery"}},
                "Purchase confirmation + Payment -> AP balance",
                "Payables and payments",
                "Cash planning and GL");
    }

    private static void addAdminPages() {
        add("ADMIN_USERS", transactionFields(),
                new String[]{"USR-admin", "2026/08/31", "status.open", "LINOVA", "user.admin.name", "1", "IT", "owner.system", "Login user maintenance"},
                new String[]{"column.id", "column.name", "column.status", "column.next"},
                new String[][]{{"USR-admin", "user.admin.name", "status.open", "term.roles"}, {"USR-planner", "user.planner.name", "status.released", "term.permissions"}},
                "User -> Role -> Menu permission",
                "Company and role",
                "Role permissions");
        add("ADMIN_ROLES", transactionFields(),
                new String[]{"ROLE-PLN", "2026/08/31", "status.released", "LINOVA", "role.planner", "1", "IT", "owner.system", "Planner role"},
                new String[]{"column.id", "column.name", "column.status", "column.next"},
                new String[][]{{"ROLE-ADM", "role.admin", "status.released", "term.permissions"}, {"ROLE-PLN", "role.planner", "status.released", "term.permissions"}},
                "Role -> User / Menu permission",
                "User and organization",
                "Menu permission");
        add("ADMIN_PERMISSIONS", transactionFields(),
                new String[]{"PERM-PLN", "2026/08/31", "status.open", "ROLE-PLN", "PROCUREMENT", "1", "MENU", "owner.system", "View/create/update procurement menus"},
                new String[]{"column.id", "column.name", "column.type", "column.status", "column.next"},
                new String[][]{{"PERM-PO", "menu.procurement.po", "term.permissions", "status.released", "menu.admin.audit"}, {"PERM-MO", "menu.manufacturing.order", "term.permissions", "status.open", "menu.admin.audit"}},
                "Role -> Menu permission -> User menu",
                "Role and menu master",
                "Authorized business pages");
        add("ADMIN_AUDIT", transactionFields(),
                new String[]{"AUD-2608", "2026/08/31", "status.ready", "LINOVA", "WORKFLOW_ACTION", "1280", "SYSTEM", "owner.system", "Key operation log"},
                new String[]{"column.date", "column.owner", "column.type", "column.status", "column.next"},
                new String[][]{{"2026-08-31", "admin", "FORM_SAVE_SUCCESS", "status.ready", "action.details"}, {"2026-08-31", "planner", "WORKFLOW_ACTION_SUCCESS", "status.ready", "action.details"}},
                "Business page -> Operation log",
                "User actions",
                "Audit export and compliance review");
    }
}
