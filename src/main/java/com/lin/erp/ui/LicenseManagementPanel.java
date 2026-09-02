package com.lin.erp.ui;

import com.lin.erp.auth.UserSession;
import com.lin.erp.config.DbConfig;
import com.lin.erp.db.DbLicenseRepository;
import com.lin.erp.db.LicenseStatus;
import com.lin.erp.db.MenuNode;
import com.lin.erp.i18n.I18n;
import com.lin.erp.logging.AppLogger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import java.sql.SQLException;

class LicenseManagementPanel extends JPanel {
    private final Window owner;
    private final UserSession session;
    private final DbLicenseRepository repository;
    private final JTextField licenseField = new JTextField("LINOVA-yyyyMMdd-signature");
    private final JLabel statusLabel = new JLabel();
    private final DefaultTableModel tableModel;

    LicenseManagementPanel(Window owner, UserSession session, MenuNode function) {
        super(new BorderLayout(0, 14));
        this.owner = owner;
        this.session = session;
        this.repository = new DbLicenseRepository(DbConfig.loadDefault());
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
        input.add(button("license.action.register", true));
        panel.add(input, BorderLayout.WEST);

        statusLabel.setForeground(AppTheme.TEXT_PRIMARY);
        statusLabel.setFont(AppTheme.font(Font.BOLD, 12));
        panel.add(statusLabel, BorderLayout.EAST);
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
            } else {
                register();
            }
        });
        return button;
    }

    private void reload() {
        try {
            LicenseStatus status = repository.currentStatus();
            String validUntil = status.getValidUntil() == null ? "" : status.getValidUntil().toString();
            statusLabel.setText(status.isValid() ? t("license.status.valid") + validUntil : t("license.status.invalid"));
            tableModel.setDataVector(new String[][]{{
                    status.getLicenseKey() == null ? "" : status.getLicenseKey(),
                    validUntil,
                    status.isValid() ? t("status.released") : t("status.cancelled")
            }}, columns());
        } catch (SQLException e) {
            AppLogger.error("License page refresh failed.", e);
            AppMessages.error(owner, t("message.error.title"), t("license.check.failed"));
        }
    }

    private void register() {
        try {
            LicenseStatus status = repository.registerLicense(licenseField.getText());
            if (!status.isValid()) {
                AppMessages.error(owner, t("message.error.title"), t("license.invalid"));
                return;
            }
            AppMessages.success(owner, t("license.register.success") + status.getValidUntil());
            reload();
        } catch (SQLException e) {
            AppLogger.error("License page save failed.", e);
            AppMessages.error(owner, t("message.error.title"), t("license.save.failed"));
        }
    }

    private String[] columns() {
        return new String[]{t("license.field.key"), t("license.field.validUntil"), t("column.status")};
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
