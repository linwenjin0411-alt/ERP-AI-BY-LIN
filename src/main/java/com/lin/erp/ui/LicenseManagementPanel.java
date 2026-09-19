package com.lin.erp.ui;

import com.lin.erp.auth.UserSession;
import com.lin.erp.config.DbConfig;
import com.lin.erp.db.DbLicenseRepository;
import com.lin.erp.db.DbRoleMenuPermissionRepository;
import com.lin.erp.db.LicenseStatus;
import com.lin.erp.db.MenuNode;
import com.lin.erp.db.RoleMenuPermission;
import com.lin.erp.i18n.I18n;
import com.lin.erp.logging.AppLogger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.CompoundBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;

class LicenseManagementPanel extends JPanel {
    private final Window owner;
    private final UserSession session;
    private final DbLicenseRepository repository;
    private final RoleMenuPermission permission;
    private final JTextField licenseField = new JTextField("LINOVA-yyyyMMdd-signature");
    private final JLabel statusLabel = new JLabel();
    private final JLabel detailLabel = new JLabel();
    private final DefaultTableModel tableModel;

    LicenseManagementPanel(Window owner, UserSession session, MenuNode function) {
        super(new BorderLayout(0, 14));
        this.owner = owner;
        this.session = session;
        this.repository = new DbLicenseRepository(DbConfig.loadDefault());
        this.permission = loadPermission(function);
        this.tableModel = new DefaultTableModel(new String[0][0], columns()) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        setOpaque(false);
        add(createToolbar(), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);
        reload();
        AppLogger.userAction("LICENSE_PAGE_OPEN", "user=" + session.getUsername());
    }

    private JPanel createToolbar() {
        RoundedPanel panel = new RoundedPanel(Color.WHITE, 8);
        panel.setLayout(new BorderLayout(16, 0));
        panel.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(231, 236, 244)),
                AppTheme.emptyBorder(12, 14, 12, 14)
        ));

        JPanel input = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        input.setOpaque(false);
        JLabel label = new JLabel(t("license.field.key"));
        label.setForeground(AppTheme.TEXT_MUTED);
        label.setFont(AppTheme.font(Font.BOLD, 11));
        input.add(label);
        licenseField.setPreferredSize(new Dimension(260, 34));
        styleField(licenseField);
        input.add(licenseField);
        input.add(button("action.refresh", false));
        JButton registerButton = button("license.action.register", true);
        registerButton.setEnabled(permission.allows("action.edit"));
        if (!registerButton.isEnabled()) {
            registerButton.setToolTipText(t("message.permission.denied"));
        }
        input.add(registerButton);
        JButton revokeButton = button("license.action.revoke", false);
        revokeButton.setEnabled(permission.allows("action.edit"));
        if (!revokeButton.isEnabled()) {
            revokeButton.setToolTipText(t("message.permission.denied"));
        }
        input.add(revokeButton);
        panel.add(input, BorderLayout.WEST);

        JPanel statusPanel = new JPanel(new GridLayout(2, 1, 0, 3));
        statusPanel.setOpaque(false);
        statusLabel.setForeground(AppTheme.TEXT_PRIMARY);
        statusLabel.setFont(AppTheme.font(Font.BOLD, 12));
        detailLabel.setForeground(AppTheme.TEXT_MUTED);
        detailLabel.setFont(AppTheme.font(Font.PLAIN, 11));
        statusPanel.add(statusLabel);
        statusPanel.add(detailLabel);
        panel.add(statusPanel, BorderLayout.EAST);
        return panel;
    }

    private JPanel createBody() {
        RoundedPanel card = new RoundedPanel(Color.WHITE, 8);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 235, 244)),
                AppTheme.emptyBorder(16, 16, 16, 16)
        ));
        JLabel title = new JLabel(t("menu.admin.license"));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setFont(AppTheme.font(Font.BOLD, 15));
        card.add(title, BorderLayout.NORTH);

        JTable table = new JTable(tableModel);
        table.setRowHeight(34);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(237, 242, 247));
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(AppTheme.ACCENT_SOFT);
        table.setSelectionForeground(AppTheme.TEXT_PRIMARY);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.getTableHeader().setForeground(AppTheme.TEXT_MUTED);
        table.getTableHeader().setFont(AppTheme.font(Font.BOLD, 11));
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(229, 235, 244)));
        card.add(scrollPane, BorderLayout.CENTER);

        JPanel wrap = new JPanel(new GridLayout(1, 1));
        wrap.setOpaque(false);
        wrap.add(card);
        return wrap;
    }

    private JButton button(final String key, boolean primary) {
        JButton button = new JButton(t(key));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("FlatLaf.style", "arc: 8; borderWidth: 1; focusWidth: 0");
        button.setBackground(primary ? AppTheme.ACCENT : Color.WHITE);
        button.setForeground(primary ? Color.WHITE : AppTheme.TEXT_PRIMARY);
        button.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(primary ? AppTheme.ACCENT : new Color(213, 222, 235)),
                AppTheme.emptyBorder(8, 15, 8, 15)
        ));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> {
            if ("action.refresh".equals(key)) {
                reload();
            } else if ("license.action.register".equals(key)) {
                register();
            } else {
                revoke();
            }
        });
        return button;
    }

    private void reload() {
        BackgroundTasks.run(
                owner,
                "License page refresh failed.",
                t("message.error.title"),
                t("license.check.failed"),
                new BackgroundTasks.Work<LicenseStatus>() {
                    @Override
                    public LicenseStatus run() throws Exception {
                        return repository.currentStatus(session.getUsername());
                    }
                },
                new BackgroundTasks.Success<LicenseStatus>() {
                    @Override
                    public void accept(LicenseStatus status) {
                        String validUntil = I18n.formatDate(session.getLanguage(), status.getValidUntil());
                        statusLabel.setText(status.isValid()
                                ? statusMark(true) + " " + t("license.status.valid") + validUntil
                                : statusMark(false) + " " + t("license.status.invalid"));
                        detailLabel.setText(detail(status));
                        tableModel.setDataVector(new String[][]{{
                                maskLicenseKey(status.getLicenseKey()),
                                validUntil,
                                status.isValid() ? t("status.released") : t("status.cancelled"),
                                status.getCustomerName(),
                                status.getModules(),
                                status.getSeatPolicy(),
                                status.getRemainingDays() < 0 ? "" : String.valueOf(status.getRemainingDays()),
                                status.getReasonCode()
                        }}, columns());
                    }
                }
        );
    }

    private void register() {
        if (!permission.allows("action.edit")) {
            AppMessages.error(owner, t("message.error.title"), t("message.permission.denied"));
            return;
        }
        final String key = licenseField.getText();
        BackgroundTasks.run(
                owner,
                "License page save failed.",
                t("message.error.title"),
                t("license.save.failed"),
                new BackgroundTasks.Work<LicenseStatus>() {
                    @Override
                    public LicenseStatus run() throws Exception {
                        return repository.registerLicense(key, session.getUsername());
                    }
                },
                new BackgroundTasks.Success<LicenseStatus>() {
                    @Override
                    public void accept(LicenseStatus status) {
                        if (!status.isValid()) {
                            AppMessages.error(owner, t("message.error.title"), t("license.invalid") + detailSuffix(status));
                            return;
                        }
                        AppMessages.success(owner, t("license.register.success")
                                + I18n.formatDate(session.getLanguage(), status.getValidUntil()));
                        reload();
                    }
                }
        );
    }

    private void revoke() {
        if (!permission.allows("action.edit")) {
            AppMessages.error(owner, t("message.error.title"), t("message.permission.denied"));
            return;
        }
        if (!confirm(t("license.revoke.confirm"))) {
            return;
        }
        BackgroundTasks.run(
                owner,
                "License page revoke failed.",
                t("message.error.title"),
                t("license.revoke.failed"),
                new BackgroundTasks.Work<Void>() {
                    @Override
                    public Void run() throws Exception {
                        repository.revokeCurrentLicense(session.getUsername());
                        return null;
                    }
                },
                new BackgroundTasks.Success<Void>() {
                    @Override
                    public void accept(Void ignored) {
                        AppMessages.success(owner, t("license.revoke.success"));
                        reload();
                    }
                }
        );
    }

    private String[] columns() {
        return new String[]{t("license.field.key"), t("license.field.validUntil"), t("column.status"),
                t("license.field.customer"), t("license.field.modules"), t("license.field.seatPolicy"),
                t("license.field.remainingDays"), t("license.field.reason")};
    }

    private String detail(LicenseStatus status) {
        StringBuilder builder = new StringBuilder();
        append(builder, t("license.field.customer"), status.getCustomerName());
        append(builder, t("license.field.modules"), status.getModules());
        append(builder, t("license.field.seatPolicy"), status.getSeatPolicy());
        if (status.isDeviceBindingEnabled()) {
            append(builder, t("license.field.deviceBinding"), t("status.ready"));
        }
        if (builder.length() == 0 && status.getReasonCode().length() > 0) {
            builder.append(status.getReasonCode());
        }
        return builder.toString();
    }

    private String detailSuffix(LicenseStatus status) {
        if (status.getReasonCode().length() == 0) {
            return "";
        }
        return " (" + status.getReasonCode() + ")";
    }

    private void append(StringBuilder builder, String label, String value) {
        if (value == null || value.trim().length() == 0) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(" | ");
        }
        builder.append(label).append(": ").append(value.trim());
    }

    private String maskLicenseKey(String key) {
        if (key == null || key.trim().length() == 0) {
            return "";
        }
        String value = key.trim();
        if (value.length() <= 12) {
            return "****";
        }
        return value.substring(0, 6) + "..." + value.substring(value.length() - 4);
    }

    private String statusMark(boolean valid) {
        return valid ? "[OK]" : "[!]";
    }

    private boolean confirm(String messageText) {
        final boolean[] result = new boolean[1];
        final JDialog dialog = new JDialog(owner, t("dialog.confirm.title"), java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Color.WHITE);
        root.setBorder(AppTheme.emptyBorder(18, 18, 18, 18));
        JLabel message = new JLabel("<html>" + messageText + "</html>");
        message.setForeground(AppTheme.TEXT_PRIMARY);
        message.setFont(AppTheme.font(Font.PLAIN, 13));
        root.add(message, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton cancel = buttonBase(t("dialog.confirm.cancel"), false);
        JButton ok = buttonBase(t("dialog.confirm.ok"), true);
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                result[0] = true;
                dialog.dispose();
            }
        });
        actions.add(cancel);
        actions.add(ok);
        root.add(actions, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.getRootPane().setDefaultButton(ok);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return result[0];
    }

    private JButton buttonBase(String text, boolean primary) {
        JButton button = new JButton(text);
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("FlatLaf.style", "arc: 8; borderWidth: 1; focusWidth: 0");
        button.setBackground(primary ? AppTheme.ACCENT : Color.WHITE);
        button.setForeground(primary ? Color.WHITE : AppTheme.TEXT_PRIMARY);
        button.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(primary ? AppTheme.ACCENT : new Color(213, 222, 235)),
                AppTheme.emptyBorder(8, 15, 8, 15)
        ));
        button.setFocusPainted(false);
        return button;
    }

    private RoleMenuPermission loadPermission(MenuNode function) {
        String code = function == null ? "ADMIN_LICENSE" : function.getCode();
        try {
            return new DbRoleMenuPermissionRepository(DbConfig.loadDefault()).loadForMenu(session.getRoleCode(), code);
        } catch (SQLException e) {
            AppLogger.error("License management permission load failed.", e);
            return RoleMenuPermission.none(code);
        }
    }

    private void styleField(JTextField field) {
        field.setForeground(AppTheme.TEXT_PRIMARY);
        field.setBackground(new Color(248, 250, 252));
        field.putClientProperty("JComponent.roundRect", Boolean.TRUE);
        field.putClientProperty("FlatLaf.style", "arc: 8; borderWidth: 1; focusWidth: 1");
        field.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 229, 238)),
                AppTheme.emptyBorder(0, 10, 0, 10)
        ));
    }

    private String t(String key) {
        return I18n.t(session.getLanguage(), key);
    }
}
