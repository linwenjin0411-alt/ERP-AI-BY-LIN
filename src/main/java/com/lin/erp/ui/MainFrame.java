package com.lin.erp.ui;

import com.lin.erp.auth.UserSession;
import com.lin.erp.config.DbConfig;
import com.lin.erp.db.DbItemMasterRepository;
import com.lin.erp.db.DbMenuRepository;
import com.lin.erp.db.DbModuleRepository;
import com.lin.erp.db.DbRoleMenuPermissionRepository;
import com.lin.erp.db.MenuNode;
import com.lin.erp.db.ModulePageData;
import com.lin.erp.db.RoleMenuPermission;
import com.lin.erp.i18n.I18n;
import com.lin.erp.i18n.Language;
import com.lin.erp.logging.AppLogger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainFrame extends JFrame {
    private final UserSession session;
    private final List<ModulePageData> modules;
    private final List<JButton> navButtons = new ArrayList<JButton>();
    private final List<JButton> secondaryMenuButtons = new ArrayList<JButton>();
    private final DbModuleRepository moduleRepository;
    private final DbMenuRepository menuRepository;
    private final DbItemMasterRepository itemMasterRepository;
    private final DbRoleMenuPermissionRepository permissionRepository;
    private final DbConfig dbConfig;
    private final Map<String, List<MenuNode>> childMenusByModule = new LinkedHashMap<String, List<MenuNode>>();
    private final Map<String, RoleMenuPermission> modulePermissions = new LinkedHashMap<String, RoleMenuPermission>();
    private final Map<String, JFrame> functionWindows = new LinkedHashMap<String, JFrame>();

    private ModulePageData currentModule;
    private MenuNode currentSubMenu;
    private ModulePageData activeTableModule;
    private JTable activeTable;
    private JPanel contentPanel;
    private JPanel secondaryMenuPanel;
    private JPanel secondaryMenuListPanel;
    private JLabel secondaryMenuTitleLabel;
    private JLabel brandModeLabel;
    private JLabel brandTaglineLabel;
    private JLabel screenTitleLabel;
    private JLabel screenSubtitleLabel;
    private JLabel companyLabel;
    private JLabel userLabel;
    private JLabel roleLabel;
    private JLabel recentPageLabel;
    private JLabel periodLabel;
    private JTextField searchField;
    private JButton topSearchButton;
    private JButton helpButton;
    private JComboBox<Language> languageBox;
    private boolean refreshingLanguage;
    private boolean searchPlaceholderVisible;

    public MainFrame(UserSession session) {
        this.session = session;
        this.dbConfig = DbConfig.loadDefault();
        this.modules = loadModulesForStartup(dbConfig);
        this.moduleRepository = new DbModuleRepository(dbConfig);
        this.menuRepository = new DbMenuRepository(dbConfig);
        this.itemMasterRepository = new DbItemMasterRepository(dbConfig);
        this.permissionRepository = new DbRoleMenuPermissionRepository(dbConfig);
        initializeFrame();
    }

    public MainFrame(UserSession session, List<ModulePageData> modules) {
        this.session = session;
        this.dbConfig = DbConfig.loadDefault();
        this.modules = normalizeStartupModules(modules, dbConfig);
        this.moduleRepository = new DbModuleRepository(dbConfig);
        this.menuRepository = new DbMenuRepository(dbConfig);
        this.itemMasterRepository = new DbItemMasterRepository(dbConfig);
        this.permissionRepository = new DbRoleMenuPermissionRepository(dbConfig);
        initializeFrame();
    }

    private void initializeFrame() {
        loadSecondaryMenus();
        applyModuleViewPermissions();
        if (!modules.isEmpty()) {
            currentModule = modules.get(0);
        }
        currentSubMenu = firstVisibleSubMenu(currentModule);
        AppLogger.userAction("WORKSPACE_OPEN", "user=" + session.getUsername() + " | module=" + moduleCode(currentModule));

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImages(AppIcon.images());
        setMinimumSize(new Dimension(1240, 780));
        setLocationByPlatform(true);
        setContentPane(createContent());
        refreshTexts();
        registerWorkspaceShortcuts();
        pack();
        setLocationRelativeTo(null);
    }

    public static List<ModulePageData> loadModulesForStartup() {
        return loadModulesForStartup(DbConfig.loadDefault());
    }

    private static List<ModulePageData> normalizeStartupModules(List<ModulePageData> loadedModules, DbConfig config) {
        if (config.isEnabled()) {
            if (loadedModules == null || loadedModules.isEmpty()) {
                return loadModulesForStartup(config);
            }
            return new ArrayList<ModulePageData>(loadedModules);
        }
        if (containsUsableModules(loadedModules)) {
            return new ArrayList<ModulePageData>(loadedModules);
        }
        AppLogger.info("Demo mode received no usable module data. Loading full demo menu.");
        return loadModulesForStartup(config);
    }

    private static boolean containsUsableModules(List<ModulePageData> loadedModules) {
        if (loadedModules == null || loadedModules.isEmpty()) {
            return false;
        }
        for (ModulePageData module : loadedModules) {
            if (module != null && !"ERROR".equals(module.getPageType())) {
                return true;
            }
        }
        return false;
    }

    private static List<ModulePageData> loadModulesForStartup(DbConfig config) {
        try {
            List<ModulePageData> modules = new DbModuleRepository(config).loadModules();
            AppLogger.info("Loaded ERP module data from " + (config.isEnabled() ? "database" : "demo store")
                    + ". Module count: " + modules.size());
            return modules;
        } catch (SQLException e) {
            AppLogger.error("Failed to load ERP module data.", e);
            List<ModulePageData> fallback = new ArrayList<ModulePageData>();
            fallback.add(ModulePageData.error("Database module data is unavailable: " + e.getMessage()));
            return fallback;
        }
    }

    private void loadSecondaryMenus() {
        childMenusByModule.clear();
        if (!dbConfig.isEnabled()) {
            mergeFallbackSecondaryMenus();
            return;
        }
        boolean loadedFromDatabase = false;
        try {
            childMenusByModule.putAll(menuRepository.loadChildrenByModule(session.getRoleCode()));
            loadedFromDatabase = true;
            AppLogger.info("Loaded ERP secondary menus from database. Role: " + session.getRoleCode()
                    + ", root count: " + childMenusByModule.size());
        } catch (SQLException e) {
            AppLogger.error("Failed to load ERP secondary menus. No fallback menus are used in database mode.", e);
        }
        if (!loadedFromDatabase && !dbConfig.isEnabled()) {
            mergeFallbackSecondaryMenus();
        }
    }

    private void applyModuleViewPermissions() {
        if (childMenusByModule.isEmpty()) {
            if (dbConfig.isEnabled()) {
                Iterator<ModulePageData> iterator = modules.iterator();
                while (iterator.hasNext()) {
                    ModulePageData module = iterator.next();
                    if (!isAlwaysVisibleModule(module)) {
                        iterator.remove();
                    }
                }
            }
            return;
        }
        Iterator<ModulePageData> iterator = modules.iterator();
        while (iterator.hasNext()) {
            ModulePageData module = iterator.next();
            if (!isAlwaysVisibleModule(module) && !childMenusByModule.containsKey(module.getCode())) {
                AppLogger.info("Module hidden by role menu permissions. Role: " + session.getRoleCode()
                        + ", module: " + module.getCode());
                iterator.remove();
            }
        }
    }

    private boolean isAlwaysVisibleModule(ModulePageData module) {
        return module != null && "DASHBOARD".equals(module.getCode());
    }

    private void mergeFallbackSecondaryMenus() {
        addFallbackFunction("MASTER", "MASTER_MAINT", "menu.area.master.maintenance", "MASTER_PRODUCT", "menu.section.product",
                "MASTER_ITEM", "menu.master.item");
        addFallbackFunction("MASTER", "MASTER_MAINT", "menu.area.master.maintenance", "MASTER_PRODUCT", "menu.section.product",
                "MASTER_BOM", "menu.master.bom");
        addFallbackFunction("MASTER", "MASTER_MAINT", "menu.area.master.maintenance", "MASTER_PARTNER", "menu.section.partner",
                "MASTER_CUSTOMER", "menu.master.customer");
        addFallbackFunction("MASTER", "MASTER_MAINT", "menu.area.master.maintenance", "MASTER_PARTNER", "menu.section.partner",
                "MASTER_SUPPLIER", "menu.master.supplier");
        addFallbackFunction("MASTER", "MASTER_MAINT", "menu.area.master.maintenance", "MASTER_LOGISTICS", "menu.section.logistics",
                "MASTER_WAREHOUSE", "menu.master.warehouse");
        addFallbackFunction("MASTER", "MASTER_GOVERNANCE", "menu.area.governance", "MASTER_CONTROL", "menu.section.controls",
                "MASTER_CHANGE_AUDIT", "menu.master.changeAudit");

        addFallbackFunction("PROCUREMENT", "PROCUREMENT_SOURCE", "menu.area.procurement.source", "PROCUREMENT_BUYING", "menu.section.buying",
                "PROCUREMENT_PR", "menu.procurement.pr");
        addFallbackFunction("PROCUREMENT", "PROCUREMENT_SOURCE", "menu.area.procurement.source", "PROCUREMENT_BUYING", "menu.section.buying",
                "PROCUREMENT_PO", "menu.procurement.po");
        addFallbackFunction("PROCUREMENT", "PROCUREMENT_SOURCE", "menu.area.procurement.source", "PROCUREMENT_BUYING", "menu.section.buying",
                "PROCUREMENT_PO_QUERY", "menu.procurement.poQuery");
        addFallbackFunction("PROCUREMENT", "PROCUREMENT_RECEIVE", "menu.area.procurement.receiving", "PROCUREMENT_INBOUND", "menu.section.inbound",
                "PROCUREMENT_RECEIPT", "menu.procurement.receipt");
        addFallbackFunction("PROCUREMENT", "PROCUREMENT_RECEIVE", "menu.area.procurement.receiving", "PROCUREMENT_INBOUND", "menu.section.inbound",
                "PROCUREMENT_RECEIPT_QUERY", "menu.procurement.receiptQuery");
        addFallbackFunction("PROCUREMENT", "PROCUREMENT_RECEIVE", "menu.area.procurement.receiving", "PROCUREMENT_SETTLEMENT", "menu.section.settlement",
                "PROCUREMENT_CONFIRMATION", "menu.procurement.confirmation");
        addFallbackFunction("PROCUREMENT", "PROCUREMENT_RECEIVE", "menu.area.procurement.receiving", "PROCUREMENT_SETTLEMENT", "menu.section.settlement",
                "PROCUREMENT_INVOICE", "menu.procurement.invoice");
        addFallbackFunction("PROCUREMENT", "PROCUREMENT_RECEIVE", "menu.area.procurement.receiving", "PROCUREMENT_SETTLEMENT", "menu.section.settlement",
                "PROCUREMENT_RETURN", "menu.procurement.return");

        addFallbackFunction("SALES", "SALES_DOMESTIC", "menu.area.sales.domestic", "SALES_ORDERING", "menu.section.ordering",
                "SALES_QUOTATION", "menu.sales.quotation");
        addFallbackFunction("SALES", "SALES_DOMESTIC", "menu.area.sales.domestic", "SALES_ORDERING", "menu.section.ordering",
                "SALES_ORDER", "menu.sales.order");
        addFallbackFunction("SALES", "SALES_DOMESTIC", "menu.area.sales.domestic", "SALES_ORDERING", "menu.section.ordering",
                "SALES_ORDER_QUERY", "menu.sales.orderQuery");
        addFallbackFunction("SALES", "SALES_DOMESTIC", "menu.area.sales.domestic", "SALES_FULFILLMENT", "menu.section.fulfillment",
                "SALES_SHIPMENT", "menu.sales.shipment");
        addFallbackFunction("SALES", "SALES_DOMESTIC", "menu.area.sales.domestic", "SALES_FULFILLMENT", "menu.section.fulfillment",
                "SALES_SHIPMENT_QUERY", "menu.sales.shipmentQuery");
        addFallbackFunction("SALES", "SALES_DOMESTIC", "menu.area.sales.domestic", "SALES_BILLING_SECTION", "menu.section.billing",
                "SALES_CONFIRMATION", "menu.sales.confirmation");
        addFallbackFunction("SALES", "SALES_DOMESTIC", "menu.area.sales.domestic", "SALES_BILLING_SECTION", "menu.section.billing",
                "SALES_BILLING", "menu.sales.billing");
        addFallbackFunction("SALES", "SALES_DOMESTIC", "menu.area.sales.domestic", "SALES_BILLING_SECTION", "menu.section.billing",
                "SALES_RETURN", "menu.sales.return");
        addFallbackFunction("SALES", "SALES_EXPORT_AREA", "menu.area.sales.export", "SALES_EXPORT_SECTION", "menu.section.export",
                "SALES_EXPORT_ORDER", "menu.sales.exportOrder");
        addFallbackFunction("SALES", "SALES_APPROVAL_AREA", "menu.area.approval", "SALES_APPROVAL_SECTION", "menu.section.approval",
                "SALES_APPROVAL_REVIEW", "menu.sales.approvalReview");

        addFallbackFunction("INVENTORY", "INVENTORY_CONTROL", "menu.area.inventory.control", "INVENTORY_STOCK_SECTION", "menu.section.stock",
                "INVENTORY_STOCK", "menu.inventory.stock");
        addFallbackFunction("INVENTORY", "INVENTORY_CONTROL", "menu.area.inventory.control", "INVENTORY_STOCK_SECTION", "menu.section.stock",
                "INVENTORY_LEDGER", "menu.inventory.ledger");
        addFallbackFunction("INVENTORY", "INVENTORY_CONTROL", "menu.area.inventory.control", "INVENTORY_STOCK_SECTION", "menu.section.stock",
                "INVENTORY_TRANSFER", "menu.inventory.transfer");
        addFallbackFunction("INVENTORY", "INVENTORY_TRACE_AREA", "menu.area.inventory.trace", "INVENTORY_TRACE_SECTION", "menu.section.trace",
                "INVENTORY_LOT", "menu.inventory.lot");
        addFallbackFunction("INVENTORY", "INVENTORY_TRACE_AREA", "menu.area.inventory.trace", "INVENTORY_COUNT_SECTION", "menu.section.counting",
                "INVENTORY_COUNT", "menu.inventory.count");

        addFallbackFunction("MANUFACTURING", "MANUFACTURING_PLANNING", "menu.area.manufacturing.planning", "MANUFACTURING_PLAN_SECTION", "menu.section.planning",
                "MANUFACTURING_MRP", "menu.manufacturing.mrp");
        addFallbackFunction("MANUFACTURING", "MANUFACTURING_PLANNING", "menu.area.manufacturing.planning", "MANUFACTURING_PLAN_SECTION", "menu.section.planning",
                "MANUFACTURING_ORDER", "menu.manufacturing.order");
        addFallbackFunction("MANUFACTURING", "MANUFACTURING_PLANNING", "menu.area.manufacturing.planning", "MANUFACTURING_PLAN_SECTION", "menu.section.planning",
                "MANUFACTURING_ORDER_QUERY", "menu.manufacturing.orderQuery");
        addFallbackFunction("MANUFACTURING", "MANUFACTURING_EXECUTION", "menu.area.manufacturing.execution", "MANUFACTURING_EXEC_SECTION", "menu.section.execution",
                "MANUFACTURING_ISSUE", "menu.manufacturing.issue");
        addFallbackFunction("MANUFACTURING", "MANUFACTURING_EXECUTION", "menu.area.manufacturing.execution", "MANUFACTURING_EXEC_SECTION", "menu.section.execution",
                "MANUFACTURING_COMPLETE", "menu.manufacturing.complete");
        addFallbackFunction("MANUFACTURING", "MANUFACTURING_EXECUTION", "menu.area.manufacturing.execution", "MANUFACTURING_EXEC_SECTION", "menu.section.execution",
                "MANUFACTURING_RETURN", "menu.manufacturing.return");
        addFallbackFunction("MANUFACTURING", "MANUFACTURING_EXECUTION", "menu.area.manufacturing.execution", "MANUFACTURING_COST_SECTION", "menu.section.costing",
                "MANUFACTURING_COST", "menu.manufacturing.cost");

        addFallbackFunction("FINANCE", "FINANCE_ACCOUNTING", "menu.area.finance.accounting", "FINANCE_RECEIVABLES", "menu.section.receivables",
                "FINANCE_AR", "menu.finance.ar");
        addFallbackFunction("FINANCE", "FINANCE_ACCOUNTING", "menu.area.finance.accounting", "FINANCE_RECEIVABLES", "menu.section.receivables",
                "FINANCE_COLLECTION", "menu.finance.collection");
        addFallbackFunction("FINANCE", "FINANCE_ACCOUNTING", "menu.area.finance.accounting", "FINANCE_RECEIVABLES", "menu.section.receivables",
                "FINANCE_COLLECTION_QUERY", "menu.finance.collectionQuery");
        addFallbackFunction("FINANCE", "FINANCE_ACCOUNTING", "menu.area.finance.accounting", "FINANCE_PAYABLES", "menu.section.payables",
                "FINANCE_AP", "menu.finance.ap");
        addFallbackFunction("FINANCE", "FINANCE_ACCOUNTING", "menu.area.finance.accounting", "FINANCE_PAYABLES", "menu.section.payables",
                "FINANCE_PAYMENT", "menu.finance.payment");
        addFallbackFunction("FINANCE", "FINANCE_ACCOUNTING", "menu.area.finance.accounting", "FINANCE_PAYABLES", "menu.section.payables",
                "FINANCE_PAYMENT_QUERY", "menu.finance.paymentQuery");
        addFallbackFunction("FINANCE", "FINANCE_CLOSE_AREA", "menu.area.finance.close", "FINANCE_GL_SECTION", "menu.section.ledger",
                "FINANCE_GL", "menu.finance.gl");
        addFallbackFunction("FINANCE", "FINANCE_CLOSE_AREA", "menu.area.finance.close", "FINANCE_PERIOD_SECTION", "menu.section.close",
                "FINANCE_CLOSE", "menu.finance.close");
        addFallbackFunction("FINANCE", "FINANCE_REPORT_AREA", "menu.area.reports", "FINANCE_REPORT_SECTION", "menu.section.reports",
                "REPORT_SALES_DETAIL", "menu.report.salesDetail");
        addFallbackFunction("FINANCE", "FINANCE_REPORT_AREA", "menu.area.reports", "FINANCE_REPORT_SECTION", "menu.section.reports",
                "REPORT_PURCHASE_DETAIL", "menu.report.purchaseDetail");
        addFallbackFunction("FINANCE", "FINANCE_REPORT_AREA", "menu.area.reports", "FINANCE_REPORT_SECTION", "menu.section.reports",
                "REPORT_INVENTORY_DETAIL", "menu.report.inventoryDetail");
        addFallbackFunction("FINANCE", "FINANCE_REPORT_AREA", "menu.area.reports", "FINANCE_REPORT_SECTION", "menu.section.reports",
                "REPORT_AR_BALANCE", "menu.report.arBalance");
        addFallbackFunction("FINANCE", "FINANCE_REPORT_AREA", "menu.area.reports", "FINANCE_REPORT_SECTION", "menu.section.reports",
                "REPORT_AP_BALANCE", "menu.report.apBalance");

        addFallbackFunction("AI", "AI_COPILOT", "menu.area.ai.copilot", "AI_ASSIST_SECTION", "menu.section.aiAssist",
                "AI_QUERY", "menu.ai.query");
        addFallbackFunction("AI", "AI_COPILOT", "menu.area.ai.copilot", "AI_ASSIST_SECTION", "menu.section.aiAssist",
                "AI_EXPLAIN", "menu.ai.explain");
        addFallbackFunction("AI", "AI_COPILOT", "menu.area.ai.copilot", "AI_REPORT_SECTION", "menu.section.reports",
                "AI_SUMMARY", "menu.ai.summary");

        addFallbackFunction("ADMIN", "ADMIN_SECURITY", "menu.area.admin.security", "ADMIN_USER_SECTION", "menu.section.users",
                "ADMIN_USERS", "menu.admin.users");
        addFallbackFunction("ADMIN", "ADMIN_SECURITY", "menu.area.admin.security", "ADMIN_ROLE_SECTION", "menu.section.roles",
                "ADMIN_ROLES", "menu.admin.roles");
        addFallbackFunction("ADMIN", "ADMIN_SECURITY", "menu.area.admin.security", "ADMIN_ROLE_SECTION", "menu.section.roles",
                "ADMIN_PERMISSIONS", "menu.admin.permissions");
        addFallbackFunction("ADMIN", "ADMIN_SECURITY", "menu.area.admin.security", "ADMIN_ROLE_SECTION", "menu.section.roles",
                "ADMIN_LICENSE", "menu.admin.license");
        addFallbackFunction("ADMIN", "ADMIN_AUDIT_AREA", "menu.area.admin.audit", "ADMIN_AUDIT_SECTION", "menu.section.audit",
                "ADMIN_AUDIT", "menu.admin.audit");
    }

    private void addFallbackFunction(String moduleCode, String areaCode, String areaNameKey,
                                     String sectionCode, String sectionNameKey,
                                     String functionCode, String functionNameKey) {
        MenuNode area = ensureRootMenu(moduleCode, areaCode, areaNameKey);
        MenuNode section = ensureChildMenu(area, sectionCode, sectionNameKey, moduleCode);
        removeRootMenu(moduleCode, functionCode);
        area.removeChildByCode(functionCode);
        if (findChild(section, functionCode) == null) {
            section.addChild(new MenuNode(functionCode, sectionCode, functionNameKey, moduleCode));
        }
    }

    private MenuNode ensureRootMenu(String moduleCode, String menuCode, String nameKey) {
        List<MenuNode> children = childMenusByModule.get(moduleCode);
        if (children == null) {
            children = new ArrayList<MenuNode>();
            childMenusByModule.put(moduleCode, children);
        }
        MenuNode existing = findRootMenu(children, menuCode);
        if (existing != null) {
            return existing;
        }
        MenuNode created = new MenuNode(menuCode, moduleCode, nameKey, moduleCode);
        children.add(created);
        return created;
    }

    private MenuNode ensureChildMenu(MenuNode parent, String menuCode, String nameKey, String moduleCode) {
        MenuNode existing = findChild(parent, menuCode);
        if (existing != null) {
            return existing;
        }
        MenuNode created = new MenuNode(menuCode, parent.getCode(), nameKey, moduleCode);
        parent.addChild(created);
        return created;
    }

    private MenuNode findRootMenu(List<MenuNode> menus, String menuCode) {
        for (MenuNode menu : menus) {
            if (menuCode.equals(menu.getCode())) {
                return menu;
            }
        }
        return null;
    }

    private MenuNode findChild(MenuNode parent, String menuCode) {
        for (MenuNode child : parent.getChildren()) {
            if (menuCode.equals(child.getCode())) {
                return child;
            }
        }
        return null;
    }

    private void removeRootMenu(String moduleCode, String menuCode) {
        List<MenuNode> children = childMenusByModule.get(moduleCode);
        if (children == null) {
            return;
        }
        for (int i = children.size() - 1; i >= 0; i--) {
            if (menuCode.equals(children.get(i).getCode())) {
                children.remove(i);
            }
        }
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(AppTheme.PAGE_BACKGROUND);
        root.add(createSidebar(), BorderLayout.WEST);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(createSecondaryMenu(), BorderLayout.WEST);
        center.add(createWorkspace(), BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);
        return root;
    }

    private JPanel createSecondaryMenu() {
        secondaryMenuPanel = new JPanel(new BorderLayout());
        secondaryMenuPanel.setPreferredSize(new Dimension(188, 780));
        secondaryMenuPanel.setBackground(new Color(253, 254, 255));
        secondaryMenuPanel.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 0, 1, AppTheme.BORDER),
                AppTheme.emptyBorder(24, 14, 22, 14)
        ));

        secondaryMenuTitleLabel = new JLabel();
        secondaryMenuTitleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        secondaryMenuTitleLabel.setFont(AppTheme.font(Font.BOLD, 15));
        secondaryMenuPanel.add(secondaryMenuTitleLabel, BorderLayout.NORTH);

        secondaryMenuListPanel = new JPanel();
        secondaryMenuListPanel.setLayout(new BoxLayout(secondaryMenuListPanel, BoxLayout.Y_AXIS));
        secondaryMenuListPanel.setOpaque(false);
        secondaryMenuListPanel.setBorder(AppTheme.emptyBorder(16, 0, 0, 0));
        secondaryMenuPanel.add(secondaryMenuListPanel, BorderLayout.CENTER);
        return secondaryMenuPanel;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(256, 780));
        sidebar.setBackground(AppTheme.SIDEBAR_BACKGROUND);
        sidebar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 0, 1, AppTheme.BORDER),
                AppTheme.emptyBorder(26, 18, 22, 18)
        ));

        JPanel brand = new JPanel(new GridLayout(0, 1, 0, 5));
        brand.setOpaque(false);
        JLabel name = new JLabel(I18n.APP_NAME);
        name.setForeground(Color.WHITE);
        name.setFont(AppTheme.font(Font.BOLD, 22));
        brandModeLabel = new JLabel();
        brandModeLabel.setForeground(new Color(131, 229, 224));
        brandModeLabel.setFont(AppTheme.font(Font.BOLD, 12));
        brandTaglineLabel = new JLabel();
        brandTaglineLabel.setForeground(new Color(188, 198, 214));
        brandTaglineLabel.setFont(AppTheme.font(Font.PLAIN, 12));
        brand.add(name);
        brand.add(brandModeLabel);
        brand.add(brandTaglineLabel);
        sidebar.add(brand, BorderLayout.NORTH);

        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setOpaque(false);
        menu.setBorder(AppTheme.emptyBorder(28, 0, 20, 0));
        for (ModulePageData module : modules) {
            JButton button = createNavButton(module);
            navButtons.add(button);
            menu.add(button);
            menu.add(Box.createVerticalStrut(7));
        }
        sidebar.add(menu, BorderLayout.CENTER);

        JPanel userPanel = new JPanel(new GridLayout(0, 1, 0, 5));
        userPanel.setOpaque(true);
        userPanel.setBackground(new Color(55, 60, 68));
        userPanel.setBorder(AppTheme.emptyBorder(12, 12, 12, 12));
        userLabel = new JLabel();
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(AppTheme.font(Font.BOLD, 13));
        roleLabel = new JLabel();
        roleLabel.setForeground(new Color(188, 198, 214));
        roleLabel.setFont(AppTheme.font(Font.PLAIN, 12));
        recentPageLabel = new JLabel();
        recentPageLabel.setForeground(new Color(188, 198, 214));
        recentPageLabel.setFont(AppTheme.font(Font.PLAIN, 11));
        JButton logoutButton = new JButton(t("action.logout"));
        logoutButton.putClientProperty("JButton.buttonType", "roundRect");
        logoutButton.putClientProperty("FlatLaf.style", "arc: 8; borderWidth: 1; focusWidth: 1");
        logoutButton.setBackground(new Color(70, 78, 90));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setFocusPainted(false);
        logoutButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutButton.setBorder(AppTheme.emptyBorder(7, 10, 7, 10));
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logout();
            }
        });
        userPanel.add(userLabel);
        userPanel.add(roleLabel);
        userPanel.add(recentPageLabel);
        userPanel.add(logoutButton);
        sidebar.add(userPanel, BorderLayout.SOUTH);

        return sidebar;
    }

    private JButton createNavButton(final ModulePageData module) {
        JButton button = new JButton();
        button.putClientProperty("module", module);
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("FlatLaf.style", "arc: 10; borderWidth: 0; focusWidth: 1");
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setPreferredSize(new Dimension(220, 40));
        button.setMinimumSize(new Dimension(220, 40));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setIcon(moduleIcon(module));
        button.setIconTextGap(8);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String fromModule = moduleCode(currentModule);
                currentModule = module;
                currentSubMenu = firstVisibleSubMenu(module);
                AppLogger.userAction("PAGE_CLICK", "user=" + session.getUsername()
                        + " | fromModule=" + fromModule
                        + " | toModule=" + module.getCode()
                        + " | pageTitle=" + english(module.getTitleKey()));
                refreshTexts();
            }
        });
        return button;
    }

    private javax.swing.Icon moduleIcon(ModulePageData module) {
        String code = module == null ? "" : module.getCode();
        Color color = "DASHBOARD".equals(code) ? AppTheme.ACCENT
                : "MASTER".equals(code) ? new Color(54, 126, 224)
                : "PROCUREMENT".equals(code) ? new Color(22, 163, 74)
                : "SALES".equals(code) ? new Color(236, 132, 31)
                : "INVENTORY".equals(code) ? new Color(20, 184, 166)
                : "MANUFACTURING".equals(code) ? new Color(139, 92, 246)
                : "FINANCE".equals(code) ? new Color(15, 118, 110)
                : "AI".equals(code) ? new Color(99, 102, 241)
                : "ADMIN".equals(code) ? new Color(100, 116, 139)
                : AppTheme.ACCENT;
        return new ActionIcon("action.details", color);
    }

    private JPanel createWorkspace() {
        JPanel workspace = new JPanel(new BorderLayout(0, 18));
        workspace.setBackground(AppTheme.PAGE_BACKGROUND);
        workspace.setBorder(AppTheme.emptyBorder(24, 28, 24, 28));
        workspace.add(createTopBar(), BorderLayout.NORTH);

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        workspace.add(contentPanel, BorderLayout.CENTER);
        return workspace;
    }

    private JPanel createTopBar() {
        RoundedPanel topBar = new RoundedPanel(AppTheme.PANEL_BACKGROUND, 8);
        topBar.setLayout(new BorderLayout(18, 0));
        topBar.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                AppTheme.emptyBorder(15, 18, 15, 18)
        ));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        screenTitleLabel = new JLabel();
        screenTitleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        screenTitleLabel.setFont(AppTheme.font(Font.BOLD, 28));
        screenSubtitleLabel = new JLabel();
        screenSubtitleLabel.setForeground(AppTheme.TEXT_MUTED);
        screenSubtitleLabel.setFont(AppTheme.font(Font.PLAIN, 13));
        titleBlock.add(screenTitleLabel);
        titleBlock.add(screenSubtitleLabel);
        topBar.add(titleBlock, BorderLayout.CENTER);

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tools.setOpaque(false);
        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(270, 36));
        searchField.setForeground(AppTheme.TEXT_MUTED);
        searchField.putClientProperty("JComponent.roundRect", Boolean.TRUE);
        searchField.putClientProperty("FlatLaf.style", "arc: 10; borderWidth: 1; focusWidth: 1");
        searchField.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                AppTheme.emptyBorder(0, 11, 0, 11)
        ));
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (searchPlaceholderVisible) {
                    searchField.setText("");
                    searchField.setForeground(AppTheme.TEXT_PRIMARY);
                    searchPlaceholderVisible = false;
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                applySearchPlaceholderIfEmpty();
            }
        });
        searchField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runWorkspaceSearch();
            }
        });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshMenuFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshMenuFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshMenuFilter();
            }
        });
        tools.add(searchField);

        topSearchButton = new JButton(t("function.search"));
        topSearchButton.putClientProperty("JButton.buttonType", "roundRect");
        topSearchButton.putClientProperty("FlatLaf.style", "arc: 10; borderWidth: 0; focusWidth: 1");
        topSearchButton.setBackground(AppTheme.ACCENT);
        topSearchButton.setForeground(Color.WHITE);
        topSearchButton.setFocusPainted(false);
        topSearchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        topSearchButton.setBorder(AppTheme.emptyBorder(9, 14, 9, 14));
        topSearchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runWorkspaceSearch();
            }
        });
        tools.add(topSearchButton);

        languageBox = new JComboBox<Language>(Language.values());
        languageBox.setPreferredSize(new Dimension(124, 36));
        languageBox.setBackground(Color.WHITE);
        languageBox.putClientProperty("JComponent.roundRect", Boolean.TRUE);
        languageBox.putClientProperty("FlatLaf.style", "arc: 10; borderWidth: 1; focusWidth: 1");
        languageBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (refreshingLanguage) {
                    return;
                }
                Language selected = (Language) languageBox.getSelectedItem();
                if (selected != null && selected != session.getLanguage()) {
                    Language previous = session.getLanguage();
                    session.setLanguage(selected);
                    logUserAction("LANGUAGE_CHANGE", "from=" + previous.name() + " | to=" + selected.name());
                    refreshTexts();
                    AppMessages.info(MainFrame.this, t("message.language.changed"));
                }
            }
        });
        tools.add(languageBox);

        helpButton = new JButton(t("action.help"));
        helpButton.putClientProperty("JButton.buttonType", "roundRect");
        helpButton.putClientProperty("FlatLaf.style", "arc: 10; borderWidth: 1; focusWidth: 1");
        helpButton.setBackground(Color.WHITE);
        helpButton.setForeground(AppTheme.TEXT_PRIMARY);
        helpButton.setFocusPainted(false);
        helpButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        helpButton.setBorder(AppTheme.emptyBorder(9, 12, 9, 12));
        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAbout();
            }
        });
        tools.add(helpButton);

        JPanel context = new JPanel(new GridLayout(0, 1, 0, 2));
        context.setOpaque(false);
        periodLabel = new JLabel("", SwingConstants.RIGHT);
        periodLabel.setForeground(AppTheme.TEXT_PRIMARY);
        periodLabel.setFont(AppTheme.font(Font.BOLD, 12));
        companyLabel = new JLabel("", SwingConstants.RIGHT);
        companyLabel.setForeground(AppTheme.TEXT_MUTED);
        companyLabel.setFont(AppTheme.font(Font.PLAIN, 12));
        context.add(periodLabel);
        context.add(companyLabel);
        tools.add(context);
        topBar.add(tools, BorderLayout.EAST);

        return topBar;
    }

    private void refreshTexts() {
        setTitle(I18n.APP_NAME);
        brandModeLabel.setText(t(dbConfig.isEnabled() ? "app.productionMode" : "app.demoMode"));
        brandModeLabel.setVisible(true);
        brandTaglineLabel.setText(t("app.tagline"));
        companyLabel.setText(text(session.getCompanyNameKey()));
        periodLabel.setText(t("top.period"));
        topSearchButton.setText(t("function.search"));
        helpButton.setText(t("action.help"));
        applySearchPlaceholderIfEmpty();
        userLabel.setText(text(session.getDisplayNameKey()));
        roleLabel.setText(text(session.getRoleNameKey()));
        if (recentPageLabel != null && recentPageLabel.getText().trim().length() == 0) {
            recentPageLabel.setText(t("workspace.recent.none"));
        }

        refreshingLanguage = true;
        languageBox.setSelectedItem(session.getLanguage());
        refreshingLanguage = false;

        if (currentModule == null && !modules.isEmpty()) {
            currentModule = modules.get(0);
        }
        currentSubMenu = resolveVisibleSubMenu(currentModule, currentSubMenu);

        if (currentModule != null) {
            if (currentSubMenu != null) {
                screenTitleLabel.setText(text(currentModule.getTitleKey()));
                screenSubtitleLabel.setText(text(currentSubMenu.getNameKey()) + " / " + t("submenu.opened"));
            } else {
                screenTitleLabel.setText(text(currentModule.getTitleKey()));
                screenSubtitleLabel.setText(text(currentModule.getSubtitleKey()));
            }
        }

        for (JButton button : navButtons) {
            ModulePageData module = (ModulePageData) button.getClientProperty("module");
            button.setText(text(module.getTitleKey()));
            button.setIcon(moduleIcon(module));
            button.setIconTextGap(8);
            styleNavButton(button, module == currentModule);
        }

        refreshSecondaryMenu();

        contentPanel.removeAll();
        if (currentSubMenu != null) {
            contentPanel.add(createFunctionMenuPage(currentSubMenu), BorderLayout.CENTER);
        } else {
            contentPanel.add(createPage(currentModule), BorderLayout.CENTER);
        }
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void styleNavButton(JButton button, boolean selected) {
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(new CompoundBorder(
                new MatteBorder(0, 4, 0, 0, selected ? AppTheme.ACCENT : AppTheme.SIDEBAR_BACKGROUND),
                AppTheme.emptyBorder(10, 12, 10, 10)
        ));
        if (selected) {
            button.setBackground(AppTheme.NAV_SELECTED);
            button.setForeground(Color.WHITE);
            button.setFont(AppTheme.font(Font.BOLD, 13));
        } else {
            button.setBackground(AppTheme.SIDEBAR_BACKGROUND);
            button.setForeground(new Color(218, 226, 238));
            button.setFont(AppTheme.font(Font.PLAIN, 13));
        }
    }

    private void applySearchPlaceholderIfEmpty() {
        if (searchField == null) {
            return;
        }
        String value = searchField.getText();
        if (value == null || value.trim().length() == 0 || searchPlaceholderVisible) {
            searchPlaceholderVisible = true;
            searchField.setForeground(AppTheme.TEXT_MUTED);
            searchField.setText(t("top.search"));
        }
    }

    private String searchQuery() {
        if (searchField == null || searchPlaceholderVisible) {
            return "";
        }
        String value = searchField.getText();
        return value == null ? "" : value.trim().toLowerCase(I18n.locale(session.getLanguage()));
    }

    private void refreshMenuFilter() {
        if (searchPlaceholderVisible || secondaryMenuListPanel == null) {
            return;
        }
        refreshSecondaryMenu();
        if (currentSubMenu != null) {
            contentPanel.removeAll();
            contentPanel.add(createPage(currentModule), BorderLayout.CENTER);
            contentPanel.revalidate();
            contentPanel.repaint();
        }
    }

    private void runWorkspaceSearch() {
        String query = searchQuery();
        if (query.length() == 0) {
            searchField.requestFocusInWindow();
            return;
        }
        for (ModulePageData module : modules) {
            if (matches(query, text(module.getTitleKey())) || matches(query, text(module.getSubtitleKey()))) {
                showModule(module);
                AppMessages.info(this, t("message.search.matched") + text(module.getTitleKey()));
                return;
            }
            MenuNode match = findMenuMatch(module, query);
            if (match != null) {
                currentModule = module;
                currentSubMenu = firstVisibleSubMenu(module);
                refreshTexts();
                openFunctionWindow(match);
                AppMessages.info(this, t("message.search.matched") + text(match.getNameKey()));
                return;
            }
            if (matchesTable(query, module)) {
                showModule(module);
                AppMessages.info(this, t("message.search.matched") + text(module.getTableTitleKey()));
                return;
            }
        }
        AppMessages.info(this, t("message.search.none"));
    }

    private boolean matches(String query, String value) {
        return value != null && value.toLowerCase(I18n.locale(session.getLanguage())).contains(query);
    }

    private MenuNode findMenuMatch(ModulePageData module, String query) {
        List<MenuNode> children = module == null ? null : childMenusByModule.get(module.getCode());
        if (children == null) {
            return null;
        }
        for (MenuNode area : children) {
            MenuNode match = findMenuMatch(area, query);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private MenuNode findMenuMatch(MenuNode node, String query) {
        if (matches(query, text(node.getNameKey())) && !node.hasChildren()) {
            return node;
        }
        for (MenuNode child : node.getChildren()) {
            MenuNode match = findMenuMatch(child, query);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private boolean matchesTable(String query, ModulePageData module) {
        if (module == null) {
            return false;
        }
        for (String column : module.getTableColumns()) {
            if (matches(query, text(column))) {
                return true;
            }
        }
        for (String[] row : module.getTableRows()) {
            for (String value : row) {
                if (matches(query, text(value))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesMenuTree(MenuNode node, String query) {
        if (node == null || query.length() == 0) {
            return true;
        }
        if (matches(query, text(node.getNameKey())) || matches(query, node.getCode())) {
            return true;
        }
        for (MenuNode child : node.getChildren()) {
            if (matchesMenuTree(child, query)) {
                return true;
            }
        }
        return false;
    }

    private void showAbout() {
        AppMessages.information(this, t("about.title"), t("about.body"), t("dialog.confirm.ok"));
    }

    private void showModule(ModulePageData module) {
        currentModule = module;
        currentSubMenu = firstVisibleSubMenu(module);
        refreshTexts();
    }

    private void registerWorkspaceShortcuts() {
        JComponent root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "focusSearch");
        root.getActionMap().put("focusSearch", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchPlaceholderVisible = true;
                searchField.requestFocusInWindow();
            }
        });
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK), "refresh");
        root.getActionMap().put("refresh", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleToolbarAction("action.refresh");
            }
        });
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK), "export");
        root.getActionMap().put("export", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleToolbarAction("action.export");
            }
        });
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK), "logout");
        root.getActionMap().put("logout", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logout();
            }
        });
    }

    private void logout() {
        logUserAction("LOGOUT", "user=" + session.getUsername());
        for (JFrame window : new ArrayList<JFrame>(functionWindows.values())) {
            window.dispose();
        }
        functionWindows.clear();
        new LoginFrame(new com.lin.erp.auth.AuthService()).setVisible(true);
        dispose();
    }

    private MenuNode firstVisibleSubMenu(ModulePageData module) {
        List<MenuNode> children = module == null ? null : childMenusByModule.get(module.getCode());
        if (children == null || children.isEmpty()) {
            return null;
        }
        return children.get(0);
    }

    private MenuNode resolveVisibleSubMenu(ModulePageData module, MenuNode requested) {
        List<MenuNode> children = module == null ? null : childMenusByModule.get(module.getCode());
        if (children == null || children.isEmpty()) {
            return null;
        }
        if (requested != null) {
            for (MenuNode child : children) {
                if (requested.getCode().equals(child.getCode())) {
                    return child;
                }
            }
        }
        return children.get(0);
    }

    private void refreshSecondaryMenu() {
        if (secondaryMenuPanel == null || secondaryMenuListPanel == null) {
            return;
        }
        secondaryMenuButtons.clear();
        secondaryMenuListPanel.removeAll();

        List<MenuNode> children = currentModule == null ? null : childMenusByModule.get(currentModule.getCode());
        boolean hasChildren = children != null && !children.isEmpty();
        secondaryMenuPanel.setVisible(hasChildren);
        if (!hasChildren) {
            secondaryMenuListPanel.revalidate();
            secondaryMenuListPanel.repaint();
            return;
        }

        secondaryMenuTitleLabel.setText(t("submenu.title"));
        String query = searchQuery();
        for (MenuNode child : children) {
            if (!matchesMenuTree(child, query)) {
                continue;
            }
            JButton button = createSecondaryMenuButton(child);
            secondaryMenuButtons.add(button);
            secondaryMenuListPanel.add(button);
            secondaryMenuListPanel.add(Box.createVerticalStrut(8));
        }
        secondaryMenuListPanel.revalidate();
        secondaryMenuListPanel.repaint();
    }

    private JButton createSecondaryMenuButton(final MenuNode menu) {
        JButton button = new JButton();
        button.putClientProperty("menu", menu);
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("FlatLaf.style", "arc: 10; borderWidth: 0; focusWidth: 1");
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setPreferredSize(new Dimension(160, 40));
        button.setMinimumSize(new Dimension(160, 40));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setText(text(menu.getNameKey()));
        styleSecondaryMenuButton(button, menu == currentSubMenu);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentSubMenu = menu;
                logUserAction("SUBMENU_CLICK", "submenu=" + menu.getCode()
                        + " | submenuName=" + english(menu.getNameKey()));
                refreshTexts();
            }
        });
        return button;
    }

    private void styleSecondaryMenuButton(JButton button, boolean selected) {
        button.setBorder(new CompoundBorder(
                new MatteBorder(0, 3, 0, 0, selected ? AppTheme.TEAL : new Color(253, 254, 255)),
                AppTheme.emptyBorder(8, 10, 8, 8)
        ));
        if (selected) {
            button.setBackground(new Color(237, 250, 248));
            button.setForeground(new Color(20, 112, 107));
            button.setFont(AppTheme.font(Font.BOLD, 12));
        } else {
            button.setBackground(new Color(253, 254, 255));
            button.setForeground(new Color(77, 91, 113));
            button.setFont(AppTheme.font(Font.PLAIN, 12));
        }
    }

    private JPanel createPage(ModulePageData data) {
        if (data == null) {
            return createMessagePage("No module data was loaded.");
        }
        if ("ERROR".equals(data.getPageType())) {
            return createMessagePage(data.getSubtitleKey());
        }
        if ("DASHBOARD".equals(data.getPageType())) {
            return createDashboardPage(data);
        }
        if ("AI".equals(data.getPageType())) {
            return createAiPage(data);
        }
        return createOperationalPage(data);
    }

    private JPanel createFunctionMenuPage(MenuNode area) {
        JPanel page = new JPanel(new BorderLayout(0, 12));
        page.setOpaque(false);

        JPanel sections = new JPanel(new GridBagLayout());
        sections.setOpaque(false);
        sections.setBorder(AppTheme.emptyBorder(0, 0, 24, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 14, 0);

        List<MenuNode> directFunctions = new ArrayList<MenuNode>();
        int row = 0;
        for (MenuNode child : area.getChildren()) {
            if (child.hasChildren()) {
                List<MenuNode> filtered = filteredFunctions(child.getChildren());
                if (!filtered.isEmpty() || menuHeaderMatches(child)) {
                    gbc.gridy = row++;
                    sections.add(createFunctionSection(child, filtered.isEmpty() ? child.getChildren() : filtered), gbc);
                }
            } else {
                if (menuHeaderMatches(child)) {
                    directFunctions.add(child);
                }
            }
        }

        if (!directFunctions.isEmpty()) {
            gbc.gridy = row++;
            sections.add(createFunctionSection(area, directFunctions), gbc);
        }

        if (row == 0) {
            gbc.gridy = row++;
            List<MenuNode> single = new ArrayList<MenuNode>();
            single.add(area);
            sections.add(createFunctionSection(area, single), gbc);
        }

        gbc.gridy = row;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        sections.add(filler, gbc);

        JScrollPane scrollPane = new JScrollPane(sections);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        page.add(scrollPane, BorderLayout.CENTER);
        return page;
    }

    private List<MenuNode> filteredFunctions(List<MenuNode> functions) {
        String query = searchQuery();
        if (query.length() == 0) {
            return functions;
        }
        List<MenuNode> filtered = new ArrayList<MenuNode>();
        for (MenuNode function : functions) {
            if (matchesMenuTree(function, query)) {
                filtered.add(function);
            }
        }
        return filtered;
    }

    private boolean menuHeaderMatches(MenuNode node) {
        String query = searchQuery();
        return query.length() == 0 || matches(query, text(node.getNameKey())) || matches(query, node.getCode());
    }

    private JPanel createFunctionSection(MenuNode section, List<MenuNode> functions) {
        JPanel container = new JPanel(new BorderLayout(0, 5));
        container.setOpaque(false);

        JLabel header = new JLabel(text(section.getNameKey()));
        header.setOpaque(true);
        header.setBackground(AppTheme.TEAL);
        header.setForeground(Color.WHITE);
        header.setFont(AppTheme.font(Font.BOLD, 13));
        header.setBorder(AppTheme.emptyBorder(7, 12, 7, 12));
        container.add(header, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 4, 8, 6));
        grid.setOpaque(false);
        grid.setBorder(AppTheme.emptyBorder(0, 4, 0, 4));
        for (MenuNode function : functions) {
            grid.add(createFunctionButton(function));
        }
        container.add(grid, BorderLayout.CENTER);
        return container;
    }

    private JButton createFunctionButton(final MenuNode function) {
        JButton button = new JButton(text(function.getNameKey()));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setPreferredSize(new Dimension(210, 40));
        button.setMinimumSize(new Dimension(170, 40));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("FlatLaf.style", "arc: 4; borderWidth: 1; focusWidth: 1");
        button.setBackground(new Color(229, 232, 238));
        button.setForeground(AppTheme.TEXT_PRIMARY);
        button.setFont(AppTheme.font(Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(207, 214, 224)),
                AppTheme.emptyBorder(9, 8, 9, 8)
        ));
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFunctionWindow(function);
            }
        });
        return button;
    }

    private void openFunctionWindow(MenuNode function) {
        logUserAction("FUNCTION_CLICK", "submenu=" + (currentSubMenu == null ? "NONE" : currentSubMenu.getCode())
                + " | function=" + function.getCode()
                + " | functionName=" + english(function.getNameKey()));
        JFrame existing = functionWindows.get(function.getCode());
        if (existing != null && existing.isDisplayable()) {
            existing.toFront();
            existing.requestFocus();
            existing.setState(JFrame.NORMAL);
            logUserAction("FUNCTION_WINDOW_FOCUS", "function=" + function.getCode());
            return;
        }
        if (!functionPermission(function).allows("action.refresh")) {
            logUserAction("FUNCTION_OPEN_DENIED", "function=" + function.getCode());
            AppMessages.error(this, t("message.error.title"), t("message.permission.denied"));
            return;
        }

        JFrame window = new JFrame(text(function.getNameKey()) + " - " + I18n.APP_NAME);
        window.setIconImages(AppIcon.images());
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        window.setMinimumSize(new Dimension(1080, 660));
        window.setPreferredSize(new Dimension(1180, 720));
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                functionWindows.remove(function.getCode());
                if (recentPageLabel != null) {
                    recentPageLabel.setText(t("workspace.recent") + text(function.getNameKey()));
                }
                logUserAction("FUNCTION_WINDOW_CLOSE", "function=" + function.getCode());
            }
        });

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(AppTheme.PAGE_BACKGROUND);
        root.add(createFunctionWindowHeader(function), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(AppTheme.emptyBorder(18, 18, 18, 18));
        if ("MASTER_ITEM".equals(function.getCode())) {
            body.add(new ItemMasterPanel(window, session, itemMasterRepository), BorderLayout.CENTER);
        } else {
            body.add(BusinessFunctionPageFactory.create(window, session, function), BorderLayout.CENTER);
        }
        root.add(body, BorderLayout.CENTER);

        window.setContentPane(root);
        functionWindows.put(function.getCode(), window);
        window.pack();
        window.setLocationRelativeTo(this);
        window.setVisible(true);
        logUserAction("FUNCTION_WINDOW_OPEN", "function=" + function.getCode()
                + " | functionName=" + english(function.getNameKey()));
    }

    private JPanel createFunctionWindowHeader(MenuNode function) {
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setBackground(new Color(38, 43, 51));
        header.setBorder(AppTheme.emptyBorder(12, 18, 12, 18));

        JLabel brand = new JLabel(I18n.APP_NAME);
        brand.setForeground(Color.WHITE);
        brand.setFont(AppTheme.font(Font.BOLD, 18));
        header.add(brand, BorderLayout.WEST);

        JLabel title = new JLabel("<html><div style='text-align:right'>"
                + text(function.getNameKey()) + "<br><span style='font-size:10px;color:#b7c2d4'>"
                + breadcrumb(function) + "</span></div></html>", SwingConstants.RIGHT);
        title.setForeground(new Color(218, 226, 238));
        title.setFont(AppTheme.font(Font.BOLD, 13));
        header.add(title, BorderLayout.EAST);
        return header;
    }

    private String breadcrumb(MenuNode function) {
        String module = currentModule == null ? "" : text(currentModule.getTitleKey());
        String area = currentSubMenu == null ? "" : text(currentSubMenu.getNameKey());
        String page = function == null ? "" : text(function.getNameKey());
        return module + " / " + area + " / " + page;
    }

    private JPanel createTransactionFunctionPanel(final JFrame window, final MenuNode function) {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        panel.add(createOperationStrip(window, function), BorderLayout.NORTH);
        panel.add(createTransactionFormCard(function), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createOperationStrip(final JFrame window, final MenuNode function) {
        RoundedPanel strip = new RoundedPanel(Color.WHITE, 8);
        strip.setLayout(new BorderLayout(16, 0));
        strip.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(231, 236, 244)),
                AppTheme.emptyBorder(10, 12, 10, 12)
        ));

        JPanel modes = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        modes.setOpaque(false);
        JLabel modeLabel = new JLabel(t("function.operation"));
        modeLabel.setForeground(AppTheme.TEXT_MUTED);
        modeLabel.setFont(AppTheme.font(Font.BOLD, 11));
        modes.add(modeLabel);

        ButtonGroup group = new ButtonGroup();
        String[] modeKeys = {"function.mode.register", "function.mode.correct", "function.mode.cancel", "function.mode.reference"};
        for (int i = 0; i < modeKeys.length; i++) {
            JRadioButton radio = new JRadioButton(t(modeKeys[i]));
            radio.setOpaque(false);
            radio.setForeground(AppTheme.TEXT_PRIMARY);
            radio.setFont(AppTheme.font(Font.PLAIN, 12));
            radio.putClientProperty("FlatLaf.style", "focusWidth: 0");
            radio.setSelected(i == 0);
            group.add(radio);
            modes.add(radio);
        }
        strip.add(modes, BorderLayout.WEST);

        JPanel search = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        search.setOpaque(false);
        JLabel targetLabel = new JLabel(t("function.search.target"));
        targetLabel.setForeground(AppTheme.TEXT_MUTED);
        targetLabel.setFont(AppTheme.font(Font.BOLD, 11));
        search.add(targetLabel);
        JTextField target = new JTextField(defaultDocumentNumber(function));
        target.setPreferredSize(new Dimension(190, 32));
        styleCompactField(target);
        search.add(target);
        JButton searchButton = new JButton(t("function.search"));
        searchButton.putClientProperty("JButton.buttonType", "roundRect");
        searchButton.putClientProperty("FlatLaf.style", "arc: 8; borderWidth: 0; focusWidth: 0");
        searchButton.setBackground(AppTheme.TEAL);
        searchButton.setForeground(Color.WHITE);
        searchButton.setBorder(AppTheme.emptyBorder(7, 14, 7, 14));
        searchButton.setFocusPainted(false);
        searchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logUserAction("FUNCTION_SEARCH", "function=" + function.getCode());
                AppMessages.info(window, t("message.refresh.done"));
            }
        });
        search.add(searchButton);
        strip.add(search, BorderLayout.EAST);
        return strip;
    }

    private JPanel createTransactionFormCard(MenuNode function) {
        RoundedPanel card = new RoundedPanel(Color.WHITE, 8);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 235, 244)),
                AppTheme.emptyBorder(16, 16, 16, 16)
        ));

        JPanel band = new JPanel(new BorderLayout());
        band.setOpaque(false);
        JLabel title = new JLabel(t("function.tab.system"));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setFont(AppTheme.font(Font.BOLD, 15));
        band.add(title, BorderLayout.WEST);
        JLabel code = new JLabel(function.getCode(), SwingConstants.RIGHT);
        code.setForeground(AppTheme.TEXT_MUTED);
        code.setFont(AppTheme.font(Font.PLAIN, 11));
        band.add(code, BorderLayout.EAST);
        card.add(band, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, new Color(238, 242, 247)),
                AppTheme.emptyBorder(14, 0, 4, 0)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 7, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        addFormField(form, gbc, 0, 0, "function.field.documentNo", defaultDocumentNumber(function));
        addFormField(form, gbc, 0, 1, "function.field.businessDate", "2026/08/17");
        addFormField(form, gbc, 0, 2, "function.field.status", text("status.open"));
        addFormField(form, gbc, 1, 0, "function.field.partner", "LINOVA-001");
        addFormField(form, gbc, 1, 1, "function.field.item", "FG-3007");
        addFormField(form, gbc, 1, 2, "function.field.quantity", "120");
        addFormField(form, gbc, 2, 0, "function.field.warehouse", "JP01");
        addFormField(form, gbc, 2, 1, "function.field.owner", text(session.getDisplayNameKey()));
        addFormField(form, gbc, 2, 2, "function.field.memo", text(function.getNameKey()));
        card.add(form, BorderLayout.CENTER);

        String[] columns = {
                t("function.table.line"),
                t("column.item"),
                t("column.qty"),
                t("column.status")
        };
        String[][] rows = {
                {"1", "FG-3007", "120", text("status.open")},
                {"2", "RM-1008", "420", text("status.released")},
                {"3", "PK-2210", "1,800", text("status.ready")}
        };
        JTable table = new JTable(new DefaultTableModel(rows, columns));
        table.setRowHeight(34);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(237, 242, 247));
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(AppTheme.ACCENT_SOFT);
        TableAlignmentSupport.apply(table);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.getTableHeader().setForeground(AppTheme.TEXT_MUTED);
        table.getTableHeader().setFont(AppTheme.font(Font.BOLD, 11));
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(1, 180));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(229, 235, 244)));
        card.add(scrollPane, BorderLayout.SOUTH);
        return card;
    }

    private void addFormField(JPanel form, GridBagConstraints gbc, int row, int column, String labelKey, String value) {
        gbc.gridy = row;
        gbc.gridx = column * 2;
        gbc.weightx = 0;
        JLabel label = new JLabel(t(labelKey));
        label.setForeground(AppTheme.TEXT_MUTED);
        label.setFont(AppTheme.font(Font.BOLD, 11));
        form.add(label, gbc);

        gbc.gridx = column * 2 + 1;
        gbc.weightx = 1;
        JTextField field = new JTextField(value);
        field.setPreferredSize(new Dimension(178, 34));
        styleCompactField(field);
        form.add(field, gbc);
    }

    private void styleCompactField(JTextField field) {
        field.setForeground(AppTheme.TEXT_PRIMARY);
        field.setBackground(new Color(248, 250, 252));
        field.putClientProperty("JComponent.roundRect", Boolean.TRUE);
        field.putClientProperty("FlatLaf.style", "arc: 8; borderWidth: 1; focusWidth: 1");
        field.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 229, 238)),
                AppTheme.emptyBorder(0, 10, 0, 10)
        ));
    }

    private String defaultDocumentNumber(MenuNode function) {
        return function.getCode().replace('_', '-') + "-" + new SimpleDateFormat("MMddHHmm").format(new Date());
    }

    private JPanel createDashboardPage(ModulePageData data) {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setOpaque(false);

        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);
        top.add(createToolbar(data.getActions()), BorderLayout.NORTH);
        top.add(createKpiGrid(data.getMetrics("TOP")), BorderLayout.CENTER);
        page.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.64;
        gbc.insets = new Insets(0, 0, 0, 16);
        center.add(createTablePanel(data, text(data.getTableTitleKey()), localized(data.getTableColumns()), localizedRows(data)), gbc);

        JPanel right = new JPanel(new GridLayout(2, 1, 0, 16));
        right.setOpaque(false);
        right.add(createProcessPanel(text(data.getProcessTitleKey()), localized(data.getProcessSteps())));
        right.add(createFocusPanel(text(data.getFocusTitleKey()), localized(data.getFocusItems())));

        gbc.gridx = 1;
        gbc.weightx = 0.36;
        gbc.insets = new Insets(0, 0, 0, 0);
        center.add(right, gbc);
        page.add(center, BorderLayout.CENTER);

        return page;
    }

    private JPanel createOperationalPage(ModulePageData data) {
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setOpaque(false);
        page.add(createToolbar(data.getActions()), BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 0;
        gbc.weighty = 1;
        gbc.gridx = 0;
        gbc.weightx = 0.66;
        gbc.insets = new Insets(0, 0, 0, 16);
        body.add(createTablePanel(data, text(data.getTableTitleKey()), localized(data.getTableColumns()), localizedRows(data)), gbc);

        JPanel side = new JPanel(new GridLayout(3, 1, 0, 16));
        side.setOpaque(false);
        side.add(createProcessPanel(text(data.getProcessTitleKey()), localized(data.getProcessSteps())));
        side.add(createMetricListPanel(data.getMetrics("SIDE")));
        side.add(createFocusPanel(text(data.getFocusTitleKey()), localized(data.getFocusItems())));

        gbc.gridx = 1;
        gbc.weightx = 0.34;
        gbc.insets = new Insets(0, 0, 0, 0);
        body.add(side, gbc);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JPanel createAiPage(ModulePageData data) {
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setOpaque(false);
        page.add(createToolbar(data.getActions()), BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.58;
        gbc.insets = new Insets(0, 0, 0, 16);

        RoundedPanel promptCard = createCard(text(data.getTableTitleKey()));
        JTextArea prompt = new JTextArea(text(data.getPromptValue()));
        prompt.setLineWrap(true);
        prompt.setWrapStyleWord(true);
        prompt.setFont(AppTheme.font(Font.PLAIN, 14));
        prompt.setForeground(AppTheme.TEXT_PRIMARY);
        prompt.setBackground(new Color(250, 252, 255));
        prompt.setBorder(AppTheme.emptyBorder(14, 14, 14, 14));
        JScrollPane promptScroll = new JScrollPane(prompt);
        promptScroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
        promptCard.add(promptScroll, BorderLayout.CENTER);
        body.add(promptCard, gbc);

        JPanel right = new JPanel(new GridLayout(3, 1, 0, 16));
        right.setOpaque(false);
        right.add(createProcessPanel(text(data.getProcessTitleKey()), localized(data.getProcessSteps())));
        right.add(createFocusPanel(text(data.getFocusTitleKey()), localized(data.getFocusItems())));
        right.add(createMetricListPanel(data.getMetrics("SIDE")));

        gbc.gridx = 1;
        gbc.weightx = 0.42;
        gbc.insets = new Insets(0, 0, 0, 0);
        body.add(right, gbc);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JPanel createMessagePage(String message) {
        JPanel panel = createSoftPanel(new BorderLayout());
        panel.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                AppTheme.emptyBorder(24, 24, 24, 24)
        ));
        JLabel label = new JLabel("<html>" + message + "</html>");
        label.setForeground(AppTheme.ERROR);
        label.setFont(AppTheme.font(Font.PLAIN, 14));
        panel.add(label, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createToolbar(List<String> actionKeys) {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        for (int i = 0; i < actionKeys.size(); i++) {
            final String actionKey = actionKeys.get(i);
            JButton button = new JButton(text(actionKey));
            button.putClientProperty("JButton.buttonType", "roundRect");
            button.putClientProperty("FlatLaf.style", "arc: 10; borderWidth: 1; focusWidth: 1");
            button.setIcon(new ActionIcon(actionKey, i == 0 ? Color.WHITE : AppTheme.ACCENT));
            button.setIconTextGap(8);
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.setOpaque(true);
            if (i == 0) {
                button.setBackground(AppTheme.ACCENT);
                button.setForeground(Color.WHITE);
            } else {
                button.setBackground(Color.WHITE);
                button.setForeground(AppTheme.TEXT_PRIMARY);
            }
            button.setBorder(new CompoundBorder(
                    BorderFactory.createLineBorder(i == 0 ? AppTheme.ACCENT : AppTheme.BORDER),
                    AppTheme.emptyBorder(9, 15, 9, 15)
            ));
            boolean allowed = isToolbarActionAllowed(actionKey);
            button.setEnabled(allowed);
            if (!allowed) {
                button.setToolTipText(t("message.permission.denied"));
            }
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    handleToolbarAction(actionKey);
                }
            });
            toolbar.add(button);
        }
        return toolbar;
    }

    private void handleToolbarAction(String actionKey) {
        logUserAction("TOOLBAR_ACTION_CLICK", "action=" + actionKey + " | actionName=" + english(actionKey));
        if (!ensureToolbarActionAllowed(actionKey)) {
            return;
        }
        if ("action.refresh".equals(actionKey)) {
            refreshFromDatabase();
        } else if ("action.new".equals(actionKey)) {
            openRecordForm(false);
        } else if ("action.edit".equals(actionKey)) {
            openRecordForm(true);
        } else if ("action.export".equals(actionKey)) {
            exportCurrentTable();
        } else if ("action.approve".equals(actionKey)
                || "action.release".equals(actionKey)
                || "action.post".equals(actionKey)) {
            runStatusAction(actionKey);
        } else if ("action.simulate".equals(actionKey)) {
            showSimulationResult();
        } else if ("action.ask".equals(actionKey)) {
            askAiAssistant();
        } else {
            logUserAction("TOOLBAR_ACTION_DENIED", "module=" + moduleCode(currentModule) + " | action=" + actionKey);
            AppMessages.error(this, t("message.error.title"), t("message.permission.denied"));
        }
    }

    private boolean ensureToolbarActionAllowed(String actionKey) {
        if (isToolbarActionAllowed(actionKey)) {
            return true;
        }
        logUserAction("TOOLBAR_ACTION_DENIED", "module=" + moduleCode(currentModule) + " | action=" + actionKey);
        AppMessages.error(this, t("message.error.title"), t("message.permission.denied"));
        return false;
    }

    private boolean isToolbarActionAllowed(String actionKey) {
        return currentModulePermission().allows(actionKey);
    }

    private RoleMenuPermission functionPermission(MenuNode function) {
        if (function == null) {
            return RoleMenuPermission.none("NONE");
        }
        if (!dbConfig.isEnabled()) {
            return RoleMenuPermission.full(function.getCode());
        }
        try {
            return permissionRepository.loadForMenu(session.getRoleCode(), function.getCode());
        } catch (SQLException e) {
            AppLogger.error("Function permission load failed.", e);
            return RoleMenuPermission.none(function.getCode());
        }
    }

    private RoleMenuPermission currentModulePermission() {
        if (currentModule == null) {
            return RoleMenuPermission.viewOnly("NONE");
        }
        String moduleCode = currentModule.getCode();
        if (!dbConfig.isEnabled()) {
            return RoleMenuPermission.full(moduleCode);
        }
        if (isAlwaysVisibleModule(currentModule)) {
            return RoleMenuPermission.viewOnly(moduleCode);
        }
        RoleMenuPermission cached = modulePermissions.get(moduleCode);
        if (cached != null) {
            return cached;
        }
        try {
            RoleMenuPermission permission = permissionRepository.loadForModule(session.getRoleCode(), moduleCode);
            modulePermissions.put(moduleCode, permission);
            return permission;
        } catch (SQLException e) {
            AppLogger.error("Module permission load failed.", e);
            RoleMenuPermission none = RoleMenuPermission.none(moduleCode);
            modulePermissions.put(moduleCode, none);
            return none;
        }
    }

    private void refreshFromDatabase() {
        logUserAction("PAGE_REFRESH_START", "source=database");
        final String currentCode = moduleCode(currentModule);
        BackgroundTasks.run(
                this,
                "Refresh failed.",
                t("message.error.title"),
                t("message.refresh.failed"),
                new BackgroundTasks.Work<List<ModulePageData>>() {
                    @Override
                    public List<ModulePageData> run() throws Exception {
                        return moduleRepository.loadModules();
                    }
                },
                new BackgroundTasks.Success<List<ModulePageData>>() {
                    @Override
                    public void accept(List<ModulePageData> reloaded) {
                        applyReloadedModules(currentCode, reloaded);
                        refreshTexts();
                        logUserAction("PAGE_REFRESH_SUCCESS", "source=database");
                        AppMessages.info(MainFrame.this, t("message.refresh.done"));
                    }
                }
        );
    }

    private void openRecordForm(boolean editMode) {
        if (!ensureToolbarActionAllowed(editMode ? "action.edit" : "action.new")) {
            return;
        }
        if (!hasEditableTable()) {
            logUserAction("FORM_OPEN_BLOCKED", "mode=" + (editMode ? "EDIT" : "CREATE") + " | reason=noEditableTable");
            AppMessages.error(this, t("message.error.title"), t("message.select.row"));
            return;
        }

        int modelRow = -1;
        String[] initialValues;
        if (editMode) {
            modelRow = selectedModelRow();
            if (modelRow < 0) {
                logUserAction("FORM_OPEN_BLOCKED", "mode=EDIT | reason=noSelectedRow");
                AppMessages.error(this, t("message.error.title"), t("message.select.row"));
                return;
            }
            initialValues = copyRowValues(currentModule, modelRow);
        } else {
            initialValues = createInitialValues(currentModule);
        }

        String actionName = editMode ? t("action.edit") : t("action.new");
        logUserAction("FORM_OPEN", "mode=" + (editMode ? "EDIT" : "CREATE")
                + " | row=" + modelRow
                + " | record=" + (editMode ? selectedRecordName(modelRow) : "new"));
        RecordFormDialog dialog = new RecordFormDialog(
                this,
                session.getLanguage(),
                text(currentModule.getTitleKey()),
                actionName,
                editMode ? "action.edit" : "action.new",
                currentModule.getTableColumns().toArray(new String[currentModule.getTableColumns().size()]),
                initialValues
        );
        dialog.setVisible(true);

        if (!dialog.isSaved()) {
            logUserAction("FORM_CANCEL", "mode=" + (editMode ? "EDIT" : "CREATE")
                    + " | row=" + modelRow
                    + " | record=" + (editMode ? selectedRecordName(modelRow) : "new"));
            return;
        }

        String[] values = dialog.getValues();
        if (!hasPrimaryValue(values)) {
            logUserAction("FORM_VALIDATION_FAILURE", "mode=" + (editMode ? "EDIT" : "CREATE") + " | reason=missingPrimaryValue");
            AppMessages.error(this, t("message.error.title"), t("message.id.required"));
            return;
        }

        final ModulePageData targetModule = currentModule;
        final int targetRow = modelRow;
        BackgroundTasks.run(
                this,
                "Save failed.",
                t("message.error.title"),
                t("message.save.failed"),
                new BackgroundTasks.Work<List<ModulePageData>>() {
                    @Override
                    public List<ModulePageData> run() throws Exception {
                        if (editMode) {
                            int sortOrder = targetModule.getTableRowSortOrders().get(targetRow).intValue();
                            logUserAction("FORM_SAVE_SUBMIT", "mode=EDIT | sortOrder=" + sortOrder + " | record=" + values[0]);
                            moduleRepository.updateTableRow(targetModule.getCode(), sortOrder, values);
                            AppLogger.info("Updated ERP row. Module: " + targetModule.getCode() + ", sortOrder: " + sortOrder);
                            logUserAction("FORM_SAVE_SUCCESS", "mode=EDIT | sortOrder=" + sortOrder + " | record=" + values[0]);
                        } else {
                            logUserAction("FORM_SAVE_SUBMIT", "mode=CREATE | record=" + values[0]);
                            int sortOrder = moduleRepository.insertTableRow(targetModule.getCode(), values);
                            AppLogger.info("Created ERP row. Module: " + targetModule.getCode() + ", sortOrder: " + sortOrder);
                            logUserAction("FORM_SAVE_SUCCESS", "mode=CREATE | sortOrder=" + sortOrder + " | record=" + values[0]);
                        }
                        return moduleRepository.loadModules();
                    }
                },
                new BackgroundTasks.Success<List<ModulePageData>>() {
                    @Override
                    public void accept(List<ModulePageData> reloaded) {
                        applyReloadedModules(targetModule.getCode(), reloaded);
                        refreshTexts();
                        AppMessages.success(MainFrame.this, editMode ? t("message.edit.success") : t("message.create.success"));
                    }
                }
        );
    }

    private void runStatusAction(String actionKey) {
        logUserAction("WORKFLOW_ACTION_BLOCKED", "action=" + actionKey + " | reason=moduleProjectionOnly");
        AppMessages.error(this, t("message.error.title"),
                "This dashboard row is a projection. Open the matching business function to release, approve, or post with inventory, finance, and audit checks.");
    }

    private void exportCurrentTable() {
        if (currentModule == null || currentModule.getTableColumns().isEmpty()) {
            logUserAction("EXPORT_FAILURE", "reason=noTable");
            AppMessages.error(this, t("message.error.title"), t("message.export.failed"));
            return;
        }
        final ReportExportSupport.Format format = ReportExportSupport.chooseFormat(this);
        if (format == null) {
            return;
        }
        final ModulePageData module = currentModule;
        final ReportExportSupport.Snapshot snapshot = new ReportExportSupport.Snapshot(
                text(module.getTitleKey()),
                module.getCode().toLowerCase(),
                session.getUsername(),
                "Module=" + module.getCode(),
                localized(module.getTableColumns()),
                localizedRows(module)
        );
        logUserAction("EXPORT_START", "format=" + format.name() + " | filters=Module=" + module.getCode());
        BackgroundTasks.run(
                this,
                "Export failed.",
                t("message.error.title"),
                t("message.export.failed"),
                new BackgroundTasks.Work<File>() {
                    @Override
                    public File run() throws Exception {
                        return ReportExportSupport.export(snapshot, format);
                    }
                },
                new BackgroundTasks.Success<File>() {
                    @Override
                    public void accept(File file) {
                        AppLogger.info("Exported ERP table. Module: " + module.getCode() + ", file: " + file.getAbsolutePath());
                        logUserAction("EXPORT_SUCCESS", "format=" + format.name()
                                + " | file=" + file.getAbsolutePath() + " | rows=" + snapshot.rowCount());
                        AppMessages.success(MainFrame.this, t("message.export.success") + file.getAbsolutePath());
                        ReportExportSupport.confirmOpenFolder(MainFrame.this, session.getLanguage(), file);
                    }
                }
        );
    }

    private void showSimulationResult() {
        logUserAction("SIMULATION_RUN", "pageTitle=" + english(currentModule.getTitleKey()));
        AppMessages.information(
                this,
                t("dialog.simulate.title"),
                t("dialog.simulate.body"),
                t("dialog.confirm.ok")
        );
        AppMessages.info(this, t("message.operation.success"));
    }

    private void askAiAssistant() {
        String prompt = AppMessages.input(this, t("action.ask"), text(currentModule.getPromptValue()),
                t("dialog.confirm.ok"), t("dialog.confirm.cancel"));
        if (prompt == null || prompt.trim().length() == 0) {
            logUserAction("AI_ASK_CANCEL", "reason=emptyPrompt");
            return;
        }
        logUserAction("AI_ASK_SUBMIT", "promptLength=" + prompt.trim().length());
        AppMessages.information(
                this,
                text(currentModule.getTitleKey()),
                text("ai.answer"),
                t("dialog.confirm.ok")
        );
    }

    private void reloadModulesKeepingCurrent() throws SQLException {
        String currentCode = currentModule == null ? null : currentModule.getCode();
        List<ModulePageData> reloaded = moduleRepository.loadModules();
        applyReloadedModules(currentCode, reloaded);
    }

    private void applyReloadedModules(String currentCode, List<ModulePageData> reloaded) {
        modules.clear();
        modules.addAll(reloaded);

        int count = Math.min(navButtons.size(), modules.size());
        for (int i = 0; i < count; i++) {
            navButtons.get(i).putClientProperty("module", modules.get(i));
        }

        currentModule = findModule(currentCode);
        if (currentModule == null && !modules.isEmpty()) {
            currentModule = modules.get(0);
        }
    }

    private ModulePageData findModule(String code) {
        if (code == null) {
            return null;
        }
        for (ModulePageData module : modules) {
            if (code.equals(module.getCode())) {
                return module;
            }
        }
        return null;
    }

    private boolean hasEditableTable() {
        return currentModule != null
                && activeTable != null
                && activeTableModule == currentModule
                && !currentModule.getTableColumns().isEmpty();
    }

    private int selectedModelRow() {
        if (activeTable == null || activeTable.getSelectedRow() < 0) {
            return -1;
        }
        return activeTable.convertRowIndexToModel(activeTable.getSelectedRow());
    }

    private String[] copyRowValues(ModulePageData module, int modelRow) {
        int columnCount = module.getTableColumns().size();
        String[] values = new String[columnCount];
        String[] source = modelRow < module.getTableRows().size() ? module.getTableRows().get(modelRow) : new String[0];
        for (int i = 0; i < columnCount; i++) {
            values[i] = i < source.length && source[i] != null ? source[i] : "";
        }
        return values;
    }

    private String[] createInitialValues(ModulePageData module) {
        int columnCount = module.getTableColumns().size();
        String[] values = new String[columnCount];
        for (int i = 0; i < values.length; i++) {
            values[i] = "";
        }
        if (values.length > 0) {
            values[0] = module.getCode().substring(0, Math.min(3, module.getCode().length()))
                    + "-" + new SimpleDateFormat("MMddHHmm").format(new Date());
        }
        int statusColumnIndex = findColumnIndex(module, "column.status");
        if (statusColumnIndex >= 0 && statusColumnIndex < values.length) {
            values[statusColumnIndex] = "status.open";
        }
        return values;
    }

    private boolean hasPrimaryValue(String[] values) {
        return values != null && values.length > 0 && values[0] != null && values[0].trim().length() > 0;
    }

    private int findColumnIndex(ModulePageData module, String columnKey) {
        for (int i = 0; i < module.getTableColumns().size(); i++) {
            if (columnKey.equals(module.getTableColumns().get(i))) {
                return i;
            }
        }
        return -1;
    }

    private String targetStatusFor(String actionKey) {
        if ("action.post".equals(actionKey)) {
            return "status.posted";
        }
        return "status.released";
    }

    private String successMessageFor(String actionKey) {
        if ("action.approve".equals(actionKey)) {
            return t("message.approve.success");
        }
        if ("action.release".equals(actionKey)) {
            return t("message.release.success");
        }
        if ("action.post".equals(actionKey)) {
            return t("message.post.success");
        }
        return t("message.operation.success");
    }

    private boolean confirmStatusAction(String actionKey, int modelRow, String targetStatus) {
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 8));
        panel.setBorder(AppTheme.emptyBorder(8, 8, 8, 8));
        panel.add(new JLabel(t("dialog.confirm.action") + ": " + text(actionKey)));
        panel.add(new JLabel(t("dialog.confirm.record") + ": " + selectedRecordName(modelRow)));
        panel.add(new JLabel(t("dialog.confirm.status") + ": " + text(targetStatus)));
        logUserAction("WORKFLOW_CONFIRM_OPEN", "action=" + actionKey
                + " | row=" + modelRow
                + " | record=" + selectedRecordName(modelRow)
                + " | targetStatus=" + english(targetStatus));

        boolean confirmed = AppMessages.confirm(
                this,
                t("dialog.confirm.title"),
                confirmText(actionKey, modelRow, targetStatus),
                t("dialog.confirm.ok"),
                t("dialog.confirm.cancel")
        );
        return confirmed;
    }

    private String confirmText(String actionKey, int modelRow, String targetStatus) {
        return t("dialog.confirm.action") + ": " + text(actionKey)
                + "\n" + t("dialog.confirm.record") + ": " + selectedRecordName(modelRow)
                + "\n" + t("dialog.confirm.status") + ": " + text(targetStatus);
    }

    private String selectedRecordName(int modelRow) {
        String[] row = modelRow < currentModule.getTableRows().size() ? currentModule.getTableRows().get(modelRow) : null;
        if (row != null && row.length > 0 && row[0] != null) {
            return english(row[0]);
        }
        return english(currentModule.getTitleKey());
    }

    private void writeCsvLine(Writer writer, String[] values) throws Exception {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                writer.write(",");
            }
            writer.write(csv(values[i]));
        }
        writer.write(System.lineSeparator());
    }

    private String csv(String value) {
        String actual = value == null ? "" : value;
        return "\"" + actual.replace("\"", "\"\"") + "\"";
    }

    private JPanel createKpiGrid(List<ModulePageData.Metric> metrics) {
        int count = Math.max(1, metrics.size());
        JPanel grid = new JPanel(new GridLayout(1, count, 16, 0));
        grid.setOpaque(false);
        for (ModulePageData.Metric metric : metrics) {
            grid.add(createMetricCard(metric));
        }
        if (metrics.isEmpty()) {
            grid.add(createMetricCard(new ModulePageData.Metric("TOP", "panel.kpi", "-", null, "accent")));
        }
        return grid;
    }

    private JPanel createMetricCard(ModulePageData.Metric metric) {
        final ModulePageData.Metric currentMetric = metric;
        RoundedPanel card = createCard(text(metric.getLabelValue()));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                drillIntoMetric(currentMetric);
            }
        });
        JLabel valueLabel = new JLabel(metric.getMetricValue());
        valueLabel.setForeground(colorFor(metric.getAccentCode()));
        valueLabel.setFont(AppTheme.font(Font.BOLD, 32));

        JPanel content = new JPanel(new GridLayout(0, 1, 0, 8));
        content.setOpaque(false);
        content.add(valueLabel);
        if (metric.getNoteValue() != null) {
            JLabel descriptionLabel = new JLabel(text(metric.getNoteValue()));
            descriptionLabel.setForeground(AppTheme.TEXT_MUTED);
            descriptionLabel.setFont(AppTheme.font(Font.PLAIN, 12));
            content.add(descriptionLabel);
        }
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private void drillIntoMetric(ModulePageData.Metric metric) {
        String text = (text(metric.getLabelValue()) + " " + text(metric.getNoteValue())).toLowerCase(I18n.locale(session.getLanguage()));
        String reportCode = text.contains("stock") || text.contains("inventory") || text.contains("库存") || text.contains("在庫")
                ? "REPORT_INVENTORY_DETAIL"
                : text.contains("purchase") || text.contains("采购") || text.contains("購買")
                ? "REPORT_PURCHASE_DETAIL"
                : text.contains("sales") || text.contains("销售") || text.contains("販売")
                ? "REPORT_SALES_DETAIL"
                : text.contains("payable") || text.contains("ap") || text.contains("应付") || text.contains("買掛")
                ? "REPORT_AP_BALANCE"
                : text.contains("finance") || text.contains("cash") || text.contains("receivable") || text.contains("财务") || text.contains("会計")
                ? "REPORT_AR_BALANCE"
                : null;
        MenuNode report = reportCode == null ? null : findMenuByCode(reportCode);
        if (report != null) {
            openFunctionWindow(report);
            AppMessages.info(this, t("message.drilldown.opened") + text(report.getNameKey()));
            return;
        }
        String target = reportCode == null ? "DASHBOARD" : "REPORTS";
        ModulePageData module = findModule(target);
        if (module != null) {
            showModule(module);
            AppMessages.info(this, t("message.drilldown.opened") + text(module.getTitleKey()));
        }
    }

    private MenuNode findMenuByCode(String code) {
        for (List<MenuNode> roots : childMenusByModule.values()) {
            for (MenuNode root : roots) {
                MenuNode match = findMenuByCode(root, code);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private MenuNode findMenuByCode(MenuNode node, String code) {
        if (node == null) {
            return null;
        }
        if (code.equals(node.getCode())) {
            return node;
        }
        for (MenuNode child : node.getChildren()) {
            MenuNode match = findMenuByCode(child, code);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private RoundedPanel createTablePanel(final ModulePageData data, String title, String[] columns, String[][] rows) {
        RoundedPanel card = createCard(title);
        DefaultTableModel model = new DefaultTableModel(rows, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        activeTable = table;
        activeTableModule = data;
        table.setRowHeight(36);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setForeground(AppTheme.TEXT_PRIMARY);
        table.setGridColor(new Color(238, 243, 249));
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(194, 238, 229));
        table.setSelectionForeground(new Color(18, 84, 80));
        table.setBackground(Color.WHITE);
        TableAlignmentSupport.apply(table, new StatusBadgeTableCellRenderer(rawRows(data)));
        table.setComponentPopupMenu(createTablePopup(table));
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    selectPopupRow(table, e);
                    return;
                }
                if (e.getClickCount() == 2 && "OPERATIONAL".equals(data.getPageType())) {
                    int row = selectedModelRow();
                    logUserAction("TABLE_ROW_DOUBLE_CLICK", "row=" + row
                            + " | record=" + (row >= 0 ? selectedRecordName(row) : "none"));
                    if (ensureToolbarActionAllowed("action.edit")) {
                        openRecordForm(true);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    selectPopupRow(table, e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    selectPopupRow(table, e);
                }
            }
        });
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setBackground(new Color(242, 246, 251));
        header.setForeground(AppTheme.TEXT_PRIMARY);
        header.setFont(AppTheme.font(Font.BOLD, 12));
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
        card.add(scrollPane, BorderLayout.CENTER);
        JLabel footer = new JLabel(rows.length == 0 ? t("table.empty.action") : t("table.rows") + rows.length);
        footer.setForeground(AppTheme.TEXT_MUTED);
        footer.setFont(AppTheme.font(Font.PLAIN, 11));
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private JPopupMenu createTablePopup(final JTable table) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem copy = new JMenuItem(t("table.menu.copyCell"));
        copy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copySelectedCell(table);
            }
        });
        JMenuItem copyRow = new JMenuItem(t("table.menu.copyRow"));
        copyRow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copySelectedRow(table);
            }
        });
        JMenuItem exportRow = new JMenuItem(t("table.menu.exportRow"));
        exportRow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportSelectedRow(table);
            }
        });
        JMenuItem details = new JMenuItem(t("table.menu.details"));
        details.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedModelRow() >= 0 && ensureToolbarActionAllowed("action.edit")) {
                    openRecordForm(true);
                }
            }
        });
        menu.add(copy);
        menu.add(copyRow);
        menu.add(exportRow);
        menu.add(details);
        return menu;
    }

    private void copySelectedRow(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) {
            AppMessages.error(this, t("message.error.title"), t("message.select.row"));
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (i > 0) {
                builder.append('\t');
            }
            Object value = table.getValueAt(row, i);
            builder.append(value == null ? "" : value.toString());
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(builder.toString()), null);
        AppMessages.info(this, t("message.copy.done"));
    }

    private void selectPopupRow(JTable table, MouseEvent event) {
        int row = table.rowAtPoint(event.getPoint());
        int column = table.columnAtPoint(event.getPoint());
        if (row >= 0) {
            table.setRowSelectionInterval(row, row);
        }
        if (column >= 0) {
            table.setColumnSelectionInterval(column, column);
        }
    }

    private void copySelectedCell(JTable table) {
        int row = table.getSelectedRow();
        int column = table.getSelectedColumn();
        if (row < 0 || column < 0) {
            AppMessages.error(this, t("message.error.title"), t("message.select.row"));
            return;
        }
        Object value = table.getValueAt(row, column);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value == null ? "" : value.toString()), null);
        AppMessages.info(this, t("message.copy.done"));
    }

    private void exportSelectedRow(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) {
            AppMessages.error(this, t("message.error.title"), t("message.select.row"));
            return;
        }
        final ReportExportSupport.Format format = ReportExportSupport.chooseFormat(this);
        if (format == null) {
            return;
        }
        String[] headers = new String[table.getColumnCount()];
        String[] values = new String[table.getColumnCount()];
        for (int i = 0; i < table.getColumnCount(); i++) {
            headers[i] = table.getColumnName(i);
            Object value = table.getValueAt(row, i);
            values[i] = value == null ? "" : value.toString();
        }
        final ReportExportSupport.Snapshot snapshot = new ReportExportSupport.Snapshot(
                currentModule == null ? "Selected row" : text(currentModule.getTitleKey()),
                "selected-row",
                session.getUsername(),
                "Selected row=" + row,
                headers,
                new String[][]{values}
        );
        logUserAction("EXPORT_ROW_START", "format=" + format.name() + " | row=" + row);
        BackgroundTasks.run(
                this,
                "Selected row export failed.",
                t("message.error.title"),
                t("message.export.failed"),
                new BackgroundTasks.Work<File>() {
                    @Override
                    public File run() throws Exception {
                        return ReportExportSupport.export(snapshot, format);
                    }
                },
                new BackgroundTasks.Success<File>() {
                    @Override
                    public void accept(File file) {
                        logUserAction("EXPORT_ROW_SUCCESS", "format=" + format.name() + " | file=" + file.getAbsolutePath());
                        AppMessages.success(MainFrame.this, t("message.export.success") + file.getAbsolutePath());
                    }
                }
        );
    }

    private JPanel createProcessPanel(String title, String[] steps) {
        RoundedPanel card = createCard(title);
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(4, 0, 4, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        for (int i = 0; i < steps.length; i++) {
            JLabel step = new JLabel(steps[i]);
            step.setOpaque(true);
            step.setBackground(AppTheme.ACCENT_SOFT);
            step.setForeground(AppTheme.ACCENT_DARK);
            step.setFont(AppTheme.font(Font.BOLD, 12));
            step.setBorder(AppTheme.emptyBorder(8, 10, 8, 10));
            gbc.gridx = 0;
            gbc.gridy = i * 2;
            content.add(step, gbc);
            if (i < steps.length - 1) {
                JLabel arrow = new JLabel("↓", SwingConstants.CENTER);
                arrow.setForeground(AppTheme.TEXT_MUTED);
                gbc.gridy = i * 2 + 1;
                content.add(arrow, gbc);
            }
        }
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createMetricListPanel(List<ModulePageData.Metric> metrics) {
        RoundedPanel card = createCard(t("panel.kpi"));
        JPanel list = new JPanel(new GridLayout(0, 1, 0, 9));
        list.setOpaque(false);
        for (ModulePageData.Metric metric : metrics) {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            JLabel name = new JLabel(text(metric.getLabelValue()));
            name.setForeground(AppTheme.TEXT_MUTED);
            name.setFont(AppTheme.font(Font.PLAIN, 12));
            JLabel value = new JLabel(metric.getMetricValue(), SwingConstants.RIGHT);
            value.setForeground(colorFor(metric.getAccentCode()));
            value.setFont(AppTheme.font(Font.BOLD, 16));
            row.add(name, BorderLayout.CENTER);
            row.add(value, BorderLayout.EAST);
            list.add(row);
        }
        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private JPanel createFocusPanel(String title, String[] items) {
        RoundedPanel card = createCard(title);
        JPanel list = new JPanel(new GridLayout(0, 1, 0, 9));
        list.setOpaque(false);
        for (String item : items) {
            JLabel label = new JLabel("<html>" + item + "</html>");
            label.setForeground(AppTheme.TEXT_PRIMARY);
            label.setFont(AppTheme.font(Font.PLAIN, 12));
            label.setBorder(AppTheme.emptyBorder(4, 0, 4, 0));
            list.add(label);
        }
        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private RoundedPanel createCard(String title) {
        RoundedPanel card = new RoundedPanel(AppTheme.PANEL_BACKGROUND, 8);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                AppTheme.emptyBorder(17, 17, 17, 17)
        ));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        titleLabel.setFont(AppTheme.font(Font.BOLD, 14));
        card.add(titleLabel, BorderLayout.NORTH);
        return card;
    }

    private JPanel createSoftPanel(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(true);
        panel.setBackground(Color.WHITE);
        return panel;
    }

    private String[] localized(List<String> values) {
        String[] localized = new String[values.size()];
        for (int i = 0; i < values.size(); i++) {
            localized[i] = text(values.get(i));
        }
        return localized;
    }

    private String[][] localizedRows(ModulePageData data) {
        int columns = data.getTableColumns().size();
        String[][] rows = new String[data.getTableRows().size()][columns];
        for (int rowIndex = 0; rowIndex < data.getTableRows().size(); rowIndex++) {
            String[] source = data.getTableRows().get(rowIndex);
            for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
                if (columnIndex < source.length && source[columnIndex] != null) {
                    rows[rowIndex][columnIndex] = text(source[columnIndex]);
                } else {
                    rows[rowIndex][columnIndex] = "";
                }
            }
        }
        return rows;
    }

    private String[][] rawRows(ModulePageData data) {
        int columns = data.getTableColumns().size();
        String[][] rows = new String[data.getTableRows().size()][columns];
        for (int rowIndex = 0; rowIndex < data.getTableRows().size(); rowIndex++) {
            String[] source = data.getTableRows().get(rowIndex);
            for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
                rows[rowIndex][columnIndex] = columnIndex < source.length && source[columnIndex] != null
                        ? source[columnIndex]
                        : "";
            }
        }
        return rows;
    }

    private Color colorFor(String accentCode) {
        if ("success".equalsIgnoreCase(accentCode)) {
            return AppTheme.SUCCESS;
        }
        if ("warning".equalsIgnoreCase(accentCode)) {
            return AppTheme.WARNING;
        }
        if ("error".equalsIgnoreCase(accentCode)) {
            return AppTheme.ERROR;
        }
        return AppTheme.ACCENT;
    }

    private String t(String key) {
        return I18n.t(session.getLanguage(), key);
    }

    private String text(String value) {
        return I18n.textOrValue(session.getLanguage(), value);
    }

    private String english(String value) {
        return I18n.textOrValue(Language.EN, value);
    }

    private String moduleCode(ModulePageData module) {
        return module == null ? "NONE" : module.getCode();
    }

    private void logUserAction(String event, String details) {
        AppLogger.userAction(event, "user=" + session.getUsername()
                + " | module=" + moduleCode(currentModule)
                + (details == null || details.trim().length() == 0 ? "" : " | " + details));
    }
}
