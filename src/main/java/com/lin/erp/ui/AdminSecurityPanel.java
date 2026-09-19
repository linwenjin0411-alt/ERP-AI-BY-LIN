package com.lin.erp.ui;

import com.lin.erp.auth.UserSession;
import com.lin.erp.config.DbConfig;
import com.lin.erp.db.DbAdminSecurityRepository;
import com.lin.erp.db.DbRoleMenuPermissionRepository;
import com.lin.erp.db.MenuNode;
import com.lin.erp.db.RoleMenuPermission;
import com.lin.erp.i18n.I18n;
import com.lin.erp.logging.AppLogger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.CompoundBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class AdminSecurityPanel extends JPanel {
    private final Window owner;
    private final UserSession session;
    private final MenuNode function;
    private final DbAdminSecurityRepository repository;
    private final RoleMenuPermission permission;
    private final List<String[]> rows = new ArrayList<String[]>();

    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JLabel countLabel;

    AdminSecurityPanel(Window owner, UserSession session, MenuNode function) {
        super(new BorderLayout(0, 16));
        this.owner = owner;
        this.session = session;
        this.function = function;
        this.repository = new DbAdminSecurityRepository(
                DbConfig.loadDefault(),
                session.getUsername(),
                session.getCompanyNameKey(),
                session.getRoleCode()
        );
        this.permission = loadPermission(function.getCode());
        setOpaque(false);
        add(createToolbar(), BorderLayout.NORTH);
        add(createTable(), BorderLayout.CENTER);
        reload();
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(16, 0));
        toolbar.setOpaque(false);
        JPanel titleBlock = new JPanel(new java.awt.GridLayout(0, 1, 0, 4));
        titleBlock.setOpaque(false);
        JLabel title = new JLabel(t(function.getNameKey()));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setFont(AppTheme.font(Font.BOLD, 20));
        countLabel = new JLabel();
        countLabel.setForeground(AppTheme.TEXT_MUTED);
        countLabel.setFont(AppTheme.font(Font.PLAIN, 12));
        titleBlock.add(title);
        titleBlock.add(countLabel);
        toolbar.add(titleBlock, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(button("action.new"));
        actions.add(button("action.edit"));
        actions.add(button("action.delete"));
        actions.add(button("action.refresh"));
        toolbar.add(actions, BorderLayout.EAST);
        return toolbar;
    }

    private JButton button(final String actionKey) {
        JButton button = new JButton(t(actionKey));
        button.setIcon(new ActionIcon(actionKey, "action.new".equals(actionKey) ? Color.WHITE : AppTheme.ACCENT));
        button.setIconTextGap(8);
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("FlatLaf.style", "arc: 8; borderWidth: 0; focusWidth: 0");
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(AppTheme.emptyBorder(8, 13, 8, 13));
        if ("action.new".equals(actionKey)) {
            button.setBackground(AppTheme.ACCENT);
            button.setForeground(Color.WHITE);
        } else {
            button.setBackground(Color.WHITE);
            button.setForeground(AppTheme.TEXT_PRIMARY);
        }
        boolean allowed = permission.allows(actionKey);
        if ("ADMIN_PERMISSIONS".equals(function.getCode()) && "action.delete".equals(actionKey)) {
            allowed = false;
        }
        button.setEnabled(allowed);
        if (!allowed) {
            button.setToolTipText(t("message.permission.denied"));
        }
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handle(actionKey);
            }
        });
        return button;
    }

    private JPanel createTable() {
        RoundedPanel card = new RoundedPanel(Color.WHITE, 8);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 235, 244)),
                AppTheme.emptyBorder(16, 16, 16, 16)
        ));
        tableModel = new DefaultTableModel(new String[0][0], localizedColumns()) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        sorter = new TableRowSorter<DefaultTableModel>(tableModel);
        table.setRowSorter(sorter);
        table.setRowHeight(34);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(237, 242, 247));
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(AppTheme.ACCENT_SOFT);
        table.setSelectionForeground(AppTheme.TEXT_PRIMARY);
        table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.getTableHeader().setForeground(AppTheme.TEXT_MUTED);
        table.getTableHeader().setFont(AppTheme.font(Font.BOLD, 11));
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        return card;
    }

    private void handle(String actionKey) {
        if (!permission.allows(actionKey)) {
            AppMessages.error(this, t("message.error.title"), t("message.permission.denied"));
            return;
        }
        if ("action.refresh".equals(actionKey)) {
            reload();
        } else if ("action.new".equals(actionKey)) {
            openForm(false);
        } else if ("action.edit".equals(actionKey)) {
            openForm(true);
        } else if ("action.delete".equals(actionKey)) {
            deactivateSelected();
        }
    }

    private void openForm(boolean edit) {
        int row = selectedRow();
        String[] current = edit ? selectedValues(row) : defaultValues();
        if (edit && current == null) {
            AppMessages.error(this, t("message.error.title"), t("message.select.row"));
            return;
        }
        RecordFormDialog dialog = new RecordFormDialog(
                owner,
                session.getLanguage(),
                t(function.getNameKey()),
                edit ? t("action.edit") : t("action.new"),
                edit ? "action.edit" : "action.new",
                formKeys(),
                current
        );
        dialog.setVisible(true);
        if (!dialog.isSaved()) {
            return;
        }
        final String originalKey = edit ? current[0] : null;
        final String[] values = dialog.getValues();
        BackgroundTasks.run(
                owner,
                "Admin security save failed.",
                t("message.error.title"),
                t("message.save.failed"),
                new BackgroundTasks.Work<Void>() {
                    @Override
                    public Void run() throws Exception {
                        if ("ADMIN_USERS".equals(function.getCode())) {
                            repository.saveUser(originalKey, values);
                        } else if ("ADMIN_ROLES".equals(function.getCode())) {
                            repository.saveRole(originalKey, values);
                        } else {
                            repository.savePermission(values);
                        }
                        return null;
                    }
                },
                new BackgroundTasks.Success<Void>() {
                    @Override
                    public void accept(Void value) {
                        AppMessages.success(AdminSecurityPanel.this, t("message.save.success"));
                        reload();
                    }
                }
        );
    }

    private void deactivateSelected() {
        if (!"ADMIN_USERS".equals(function.getCode())) {
            AppMessages.error(this, t("message.error.title"), t("message.permission.denied"));
            return;
        }
        int row = selectedRow();
        final String[] values = selectedValues(row);
        if (values == null) {
            AppMessages.error(this, t("message.error.title"), t("message.select.row"));
            return;
        }
        int result = JOptionPane.showConfirmDialog(
                this,
                t("message.delete.confirm") + " " + values[0] + " / " + values[1] + " ?",
                t("dialog.confirm.title"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        BackgroundTasks.run(
                owner,
                "Admin security delete failed.",
                t("message.error.title"),
                t("message.delete.failed"),
                new BackgroundTasks.Work<Void>() {
                    @Override
                    public Void run() throws Exception {
                        repository.deactivateUser(values[0]);
                        return null;
                    }
                },
                new BackgroundTasks.Success<Void>() {
                    @Override
                    public void accept(Void value) {
                        AppMessages.success(AdminSecurityPanel.this, t("message.delete.success"));
                        reload();
                    }
                }
        );
    }

    private void reload() {
        BackgroundTasks.run(
                owner,
                "Admin security load failed.",
                t("message.error.title"),
                t("message.refresh.failed"),
                new BackgroundTasks.Work<List<String[]>>() {
                    @Override
                    public List<String[]> run() throws Exception {
                        if ("ADMIN_USERS".equals(function.getCode())) {
                            return repository.loadUsers();
                        }
                        if ("ADMIN_ROLES".equals(function.getCode())) {
                            return repository.loadRoles();
                        }
                        return repository.loadPermissions();
                    }
                },
                new BackgroundTasks.Success<List<String[]>>() {
                    @Override
                    public void accept(List<String[]> loaded) {
                        rows.clear();
                        rows.addAll(loaded);
                        tableModel.setDataVector(localizedRows(), localizedColumns());
                        sorter.setModel(tableModel);
                        TableColumnPreferences.install(table, "admin." + function.getCode() + "." + session.getLanguage().name());
                        countLabel.setText(t("item.count.prefix") + rows.size());
                        if (!rows.isEmpty()) {
                            table.setRowSelectionInterval(0, 0);
                        }
                    }
                }
        );
    }

    private int selectedRow() {
        if (table == null || table.getSelectedRow() < 0) {
            return -1;
        }
        return table.convertRowIndexToModel(table.getSelectedRow());
    }

    private String[] selectedValues(int row) {
        if (row < 0 || row >= rows.size()) {
            return null;
        }
        String[] source = rows.get(row);
        String[] form = defaultValues();
        for (int i = 0; i < form.length && i < source.length; i++) {
            form[i] = source[i];
        }
        return form;
    }

    private String[] defaultValues() {
        if ("ADMIN_USERS".equals(function.getCode())) {
            return new String[]{"", "", "", "LINOVA", "PLANNER", "status.open", "", ""};
        }
        if ("ADMIN_ROLES".equals(function.getCode())) {
            return new String[]{"", "", "status.open", ""};
        }
        return new String[]{"PLANNER", "MASTER_ITEM", "", "true", "false", "false", "false"};
    }

    private String[] formKeys() {
        if ("ADMIN_USERS".equals(function.getCode())) {
            return new String[]{"column.id", "column.name", "column.email", "column.company", "column.role", "column.status", "column.failedLogins", "column.resetPassword"};
        }
        if ("ADMIN_ROLES".equals(function.getCode())) {
            return new String[]{"column.id", "column.name", "column.status", "column.userCount"};
        }
        return columns();
    }

    private String[] columns() {
        if ("ADMIN_USERS".equals(function.getCode())) {
            return new String[]{"column.id", "column.name", "column.email", "column.company", "column.role", "column.status", "column.failedLogins", "column.lockedUntil"};
        }
        if ("ADMIN_ROLES".equals(function.getCode())) {
            return new String[]{"column.id", "column.name", "column.status", "column.userCount"};
        }
        return new String[]{"column.role", "column.menu", "column.name", "column.view", "column.create", "column.update", "column.approve"};
    }

    private String[] localizedColumns() {
        String[] columns = columns();
        String[] values = new String[columns.length];
        for (int i = 0; i < columns.length; i++) {
            values[i] = t(columns[i]);
        }
        return values;
    }

    private String[][] localizedRows() {
        String[][] result = new String[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            String[] source = rows.get(i);
            String[] row = new String[source.length];
            for (int j = 0; j < source.length; j++) {
                row[j] = I18n.textOrValue(session.getLanguage(), source[j]);
            }
            result[i] = row;
        }
        return result;
    }

    private RoleMenuPermission loadPermission(String menuCode) {
        try {
            return new DbRoleMenuPermissionRepository(DbConfig.loadDefault()).loadForMenu(session.getRoleCode(), menuCode);
        } catch (SQLException e) {
            AppLogger.error("Admin security permission load failed.", e);
            return RoleMenuPermission.none(menuCode);
        }
    }

    private String t(String key) {
        return I18n.t(session.getLanguage(), key);
    }
}
