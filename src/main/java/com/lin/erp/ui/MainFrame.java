package com.lin.erp.ui;

import com.lin.erp.auth.UserSession;
import com.lin.erp.config.DbConfig;
import com.lin.erp.db.DbItemMasterRepository;
import com.lin.erp.db.DbMenuRepository;
import com.lin.erp.db.DbModuleRepository;
import com.lin.erp.db.MenuNode;
import com.lin.erp.db.ModulePageData;
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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
    private final Map<String, List<MenuNode>> childMenusByModule = new LinkedHashMap<String, List<MenuNode>>();

    private ModulePageData currentModule;
    private MenuNode currentSubMenu;
    private ModulePageData activeTableModule;
    private JTable activeTable;
    private JPanel contentPanel;
    private JPanel secondaryMenuPanel;
    private JPanel secondaryMenuListPanel;
    private JLabel secondaryMenuTitleLabel;
    private JLabel brandTaglineLabel;
    private JLabel screenTitleLabel;
    private JLabel screenSubtitleLabel;
    private JLabel companyLabel;
    private JLabel userLabel;
    private JLabel roleLabel;
    private JLabel periodLabel;
    private JTextField searchField;
    private JComboBox<Language> languageBox;
    private boolean refreshingLanguage;

    public MainFrame(UserSession session) {
        this.session = session;
        this.modules = loadModulesForStartup();
        this.moduleRepository = new DbModuleRepository(DbConfig.loadDefault());
        this.menuRepository = new DbMenuRepository(DbConfig.loadDefault());
        this.itemMasterRepository = new DbItemMasterRepository(DbConfig.loadDefault());
        initializeFrame();
    }

    public MainFrame(UserSession session, List<ModulePageData> modules) {
        this.session = session;
        this.modules = modules == null ? loadModulesForStartup() : modules;
        this.moduleRepository = new DbModuleRepository(DbConfig.loadDefault());
        this.menuRepository = new DbMenuRepository(DbConfig.loadDefault());
        this.itemMasterRepository = new DbItemMasterRepository(DbConfig.loadDefault());
        initializeFrame();
    }

    private void initializeFrame() {
        if (!modules.isEmpty()) {
            currentModule = modules.get(0);
        }
        loadSecondaryMenus();
        currentSubMenu = firstVisibleSubMenu(currentModule);
        AppLogger.userAction("WORKSPACE_OPEN", "user=" + session.getUsername() + " | module=" + moduleCode(currentModule));

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImages(AppIcon.images());
        setMinimumSize(new Dimension(1240, 780));
        setLocationByPlatform(true);
        setContentPane(createContent());
        refreshTexts();
        pack();
        setLocationRelativeTo(null);
    }

    public static List<ModulePageData> loadModulesForStartup() {
        try {
            DbConfig config = DbConfig.loadDefault();
            List<ModulePageData> modules = new DbModuleRepository(config).loadModules();
            AppLogger.info("Loaded ERP module data from database. Module count: " + modules.size());
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
        try {
            childMenusByModule.putAll(menuRepository.loadChildrenByModule());
            AppLogger.info("Loaded ERP secondary menus from database. Root count: " + childMenusByModule.size());
        } catch (SQLException e) {
            AppLogger.error("Failed to load ERP secondary menus. Fallback menus will be used.", e);
        }
        mergeFallbackSecondaryMenus();
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
        brandTaglineLabel = new JLabel();
        brandTaglineLabel.setForeground(new Color(188, 198, 214));
        brandTaglineLabel.setFont(AppTheme.font(Font.PLAIN, 12));
        brand.add(name);
        brand.add(brandTaglineLabel);
        sidebar.add(brand, BorderLayout.NORTH);

        JPanel menu = new JPanel(new GridLayout(0, 1, 0, 7));
        menu.setOpaque(false);
        menu.setBorder(AppTheme.emptyBorder(28, 0, 20, 0));
        for (ModulePageData module : modules) {
            JButton button = createNavButton(module);
            navButtons.add(button);
            menu.add(button);
        }
        sidebar.add(menu, BorderLayout.CENTER);

        JPanel userPanel = new JPanel(new GridLayout(0, 1, 0, 3));
        userPanel.setOpaque(true);
        userPanel.setBackground(new Color(55, 60, 68));
        userPanel.setBorder(AppTheme.emptyBorder(12, 12, 12, 12));
        userLabel = new JLabel();
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(AppTheme.font(Font.BOLD, 13));
        roleLabel = new JLabel();
        roleLabel.setForeground(new Color(188, 198, 214));
        roleLabel.setFont(AppTheme.font(Font.PLAIN, 12));
        userPanel.add(userLabel);
        userPanel.add(roleLabel);
        sidebar.add(userPanel, BorderLayout.SOUTH);

        return sidebar;
    }

    private JButton createNavButton(final ModulePageData module) {
        JButton button = new JButton();
        button.putClientProperty("module", module);
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("FlatLaf.style", "arc: 10; borderWidth: 0; focusWidth: 1");
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
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
        tools.add(searchField);

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
        brandTaglineLabel.setText(t("app.tagline"));
        companyLabel.setText(text(session.getCompanyNameKey()));
        periodLabel.setText(t("top.period"));
        searchField.setText(t("top.search"));
        userLabel.setText(text(session.getDisplayNameKey()));
        roleLabel.setText(text(session.getRoleNameKey()));

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
        for (MenuNode child : children) {
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
                gbc.gridy = row++;
                sections.add(createFunctionSection(child, child.getChildren()), gbc);
            } else {
                directFunctions.add(child);
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
        button.setPreferredSize(new Dimension(210, 34));
        button.setMinimumSize(new Dimension(170, 34));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("FlatLaf.style", "arc: 4; borderWidth: 1; focusWidth: 1");
        button.setBackground(new Color(229, 232, 238));
        button.setForeground(AppTheme.TEXT_PRIMARY);
        button.setFont(AppTheme.font(Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(207, 214, 224)),
                AppTheme.emptyBorder(7, 8, 7, 8)
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

        JFrame window = new JFrame(text(function.getNameKey()) + " - " + I18n.APP_NAME);
        window.setIconImages(AppIcon.images());
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        window.setMinimumSize(new Dimension(1080, 660));
        window.setPreferredSize(new Dimension(1180, 720));

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

        JLabel title = new JLabel(text(function.getNameKey()), SwingConstants.RIGHT);
        title.setForeground(new Color(218, 226, 238));
        title.setFont(AppTheme.font(Font.BOLD, 13));
        header.add(title, BorderLayout.EAST);
        return header;
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
            AppMessages.success(this, t("message.operation.success"));
        }
    }

    private void refreshFromDatabase() {
        logUserAction("PAGE_REFRESH_START", "source=database");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            reloadModulesKeepingCurrent();
            refreshTexts();
            logUserAction("PAGE_REFRESH_SUCCESS", "source=database");
            AppMessages.info(this, t("message.refresh.done"));
        } catch (SQLException e) {
            AppLogger.error("Refresh failed.", e);
            logUserAction("PAGE_REFRESH_FAILURE", "errorType=" + e.getClass().getSimpleName());
            AppMessages.error(this, t("message.error.title"), t("message.refresh.failed"));
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void openRecordForm(boolean editMode) {
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

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            if (editMode) {
                int sortOrder = currentModule.getTableRowSortOrders().get(modelRow).intValue();
                logUserAction("FORM_SAVE_SUBMIT", "mode=EDIT | sortOrder=" + sortOrder + " | record=" + values[0]);
                moduleRepository.updateTableRow(currentModule.getCode(), sortOrder, values);
                AppLogger.info("Updated ERP row. Module: " + currentModule.getCode() + ", sortOrder: " + sortOrder);
                logUserAction("FORM_SAVE_SUCCESS", "mode=EDIT | sortOrder=" + sortOrder + " | record=" + values[0]);
                AppMessages.success(this, t("message.edit.success"));
            } else {
                logUserAction("FORM_SAVE_SUBMIT", "mode=CREATE | record=" + values[0]);
                int sortOrder = moduleRepository.insertTableRow(currentModule.getCode(), values);
                AppLogger.info("Created ERP row. Module: " + currentModule.getCode() + ", sortOrder: " + sortOrder);
                logUserAction("FORM_SAVE_SUCCESS", "mode=CREATE | sortOrder=" + sortOrder + " | record=" + values[0]);
                AppMessages.success(this, t("message.create.success"));
            }
            reloadModulesKeepingCurrent();
            refreshTexts();
        } catch (SQLException e) {
            AppLogger.error("Save failed.", e);
            logUserAction("FORM_SAVE_FAILURE", "mode=" + (editMode ? "EDIT" : "CREATE")
                    + " | errorType=" + e.getClass().getSimpleName());
            AppMessages.error(this, t("message.error.title"), t("message.save.failed"));
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void runStatusAction(String actionKey) {
        if (!hasEditableTable()) {
            logUserAction("WORKFLOW_ACTION_BLOCKED", "action=" + actionKey + " | reason=noEditableTable");
            AppMessages.error(this, t("message.error.title"), t("message.select.row"));
            return;
        }
        int modelRow = selectedModelRow();
        if (modelRow < 0) {
            logUserAction("WORKFLOW_ACTION_BLOCKED", "action=" + actionKey + " | reason=noSelectedRow");
            AppMessages.error(this, t("message.error.title"), t("message.select.row"));
            return;
        }

        int statusColumnIndex = findColumnIndex(currentModule, "column.status");
        if (statusColumnIndex < 0) {
            logUserAction("WORKFLOW_ACTION_BLOCKED", "action=" + actionKey + " | reason=noStatusColumn");
            AppMessages.error(this, t("message.error.title"), t("message.no.status"));
            return;
        }

        String targetStatus = targetStatusFor(actionKey);
        if (!confirmStatusAction(actionKey, modelRow, targetStatus)) {
            logUserAction("WORKFLOW_ACTION_CANCEL", "action=" + actionKey
                    + " | row=" + modelRow
                    + " | record=" + selectedRecordName(modelRow)
                    + " | targetStatus=" + english(targetStatus));
            return;
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            int sortOrder = currentModule.getTableRowSortOrders().get(modelRow).intValue();
            logUserAction("WORKFLOW_ACTION_SUBMIT", "action=" + actionKey
                    + " | sortOrder=" + sortOrder
                    + " | record=" + selectedRecordName(modelRow)
                    + " | targetStatus=" + english(targetStatus));
            moduleRepository.updateTableRowStatus(currentModule.getCode(), sortOrder, statusColumnIndex, targetStatus);
            AppLogger.info("Updated ERP row status. Module: " + currentModule.getCode()
                    + ", sortOrder: " + sortOrder + ", status: " + targetStatus);
            reloadModulesKeepingCurrent();
            refreshTexts();
            logUserAction("WORKFLOW_ACTION_SUCCESS", "action=" + actionKey
                    + " | sortOrder=" + sortOrder
                    + " | targetStatus=" + english(targetStatus));
            AppMessages.success(this, successMessageFor(actionKey));
        } catch (SQLException e) {
            AppLogger.error("Status update failed.", e);
            logUserAction("WORKFLOW_ACTION_FAILURE", "action=" + actionKey + " | errorType=" + e.getClass().getSimpleName());
            AppMessages.error(this, t("message.error.title"), t("message.status.failed"));
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void exportCurrentTable() {
        logUserAction("EXPORT_START", "format=csv");
        if (currentModule == null || currentModule.getTableColumns().isEmpty()) {
            logUserAction("EXPORT_FAILURE", "reason=noTable");
            AppMessages.error(this, t("message.error.title"), t("message.export.failed"));
            return;
        }

        File exportDir = new File("exports");
        if (!exportDir.isDirectory() && !exportDir.mkdirs()) {
            logUserAction("EXPORT_FAILURE", "reason=cannotCreateExportDirectory");
            AppMessages.error(this, t("message.error.title"), t("message.export.failed"));
            return;
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File file = new File(exportDir, currentModule.getCode().toLowerCase() + "-" + timestamp + ".csv");
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            writer.write('\ufeff');
            writeCsvLine(writer, localized(currentModule.getTableColumns()));
            String[][] rows = localizedRows(currentModule);
            for (int i = 0; i < rows.length; i++) {
                writeCsvLine(writer, rows[i]);
            }
            AppLogger.info("Exported ERP table. Module: " + currentModule.getCode() + ", file: " + file.getAbsolutePath());
            logUserAction("EXPORT_SUCCESS", "file=" + file.getAbsolutePath() + " | rows=" + currentModule.getTableRows().size());
            AppMessages.success(this, t("message.export.success") + file.getAbsolutePath());
        } catch (Exception e) {
            AppLogger.error("Export failed.", e);
            logUserAction("EXPORT_FAILURE", "errorType=" + e.getClass().getSimpleName());
            AppMessages.error(this, t("message.error.title"), t("message.export.failed"));
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignored) {
                    // Nothing else to do if the export stream fails while closing.
                }
            }
        }
    }

    private void showSimulationResult() {
        logUserAction("SIMULATION_RUN", "pageTitle=" + english(currentModule.getTitleKey()));
        JOptionPane.showMessageDialog(
                this,
                t("dialog.simulate.body"),
                t("dialog.simulate.title"),
                JOptionPane.INFORMATION_MESSAGE
        );
        AppMessages.info(this, t("message.operation.success"));
    }

    private void askAiAssistant() {
        String prompt = JOptionPane.showInputDialog(this, text(currentModule.getPromptValue()), t("action.ask"));
        if (prompt == null || prompt.trim().length() == 0) {
            logUserAction("AI_ASK_CANCEL", "reason=emptyPrompt");
            return;
        }
        logUserAction("AI_ASK_SUBMIT", "promptLength=" + prompt.trim().length());
        JOptionPane.showMessageDialog(
                this,
                text("ai.answer"),
                text(currentModule.getTitleKey()),
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void reloadModulesKeepingCurrent() throws SQLException {
        String currentCode = currentModule == null ? null : currentModule.getCode();
        List<ModulePageData> reloaded = moduleRepository.loadModules();
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

        Object[] options = new Object[]{t("dialog.confirm.ok"), t("dialog.confirm.cancel")};
        int result = JOptionPane.showOptionDialog(
                this,
                panel,
                t("dialog.confirm.title"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
        return result == 0;
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
        RoundedPanel card = createCard(text(metric.getLabelValue()));
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
        table.setSelectionBackground(AppTheme.ACCENT_SOFT);
        table.setSelectionForeground(AppTheme.TEXT_PRIMARY);
        table.setBackground(Color.WHITE);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && "OPERATIONAL".equals(data.getPageType())) {
                    int row = selectedModelRow();
                    logUserAction("TABLE_ROW_DOUBLE_CLICK", "row=" + row
                            + " | record=" + (row >= 0 ? selectedRecordName(row) : "none"));
                    openRecordForm(true);
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
        return card;
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
