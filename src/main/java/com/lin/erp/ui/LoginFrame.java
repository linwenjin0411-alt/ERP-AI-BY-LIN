package com.lin.erp.ui;

import com.lin.erp.auth.AuthException;
import com.lin.erp.auth.AuthService;
import com.lin.erp.auth.UserSession;
import com.lin.erp.config.DbConfig;
import com.lin.erp.db.DbLicenseRepository;
import com.lin.erp.db.LicenseStatus;
import com.lin.erp.db.ModulePageData;
import com.lin.erp.i18n.I18n;
import com.lin.erp.i18n.Language;
import com.lin.erp.logging.AppLogger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingWorker;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.List;
import java.sql.SQLException;

public class LoginFrame extends JFrame {
    private final AuthService authService;
    private final DbLicenseRepository licenseRepository;
    private final DbConfig dbConfig;
    private final List<JLabel> capabilityLabels = new ArrayList<JLabel>();

    private Language language = I18n.DEFAULT_LANGUAGE;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> companyBox;
    private JComboBox<Language> languageBox;
    private JCheckBox rememberBox;
    private JLabel productLabel;
    private JLabel editionLabel;
    private JLabel flowLabel;
    private JLabel noteLabel;
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private JLabel companyLabel;
    private JLabel languageLabel;
    private JLabel messageLabel;
    private JLabel footerLabel;
    private JButton loginButton;

    public LoginFrame(AuthService authService) {
        this.authService = authService;
        this.dbConfig = DbConfig.loadDefault();
        this.licenseRepository = new DbLicenseRepository(dbConfig);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImages(AppIcon.images());
        setMinimumSize(new Dimension(1040, 650));
        setLocationByPlatform(true);
        setContentPane(createContent());
        registerEnterShortcut();
        updateTexts();
        pack();
        setLocationRelativeTo(null);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                usernameField.requestFocusInWindow();
            }
        });
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(24, 0));
        root.setBackground(AppTheme.PAGE_BACKGROUND);
        root.setBorder(AppTheme.emptyBorder(28, 28, 28, 28));
        root.add(createBrandPanel(), BorderLayout.CENTER);
        root.add(createLoginPanel(), BorderLayout.EAST);
        return root;
    }

    private JPanel createBrandPanel() {
        JPanel panel = new BrandPanel();
        panel.setLayout(new GridBagLayout());
        panel.setPreferredSize(new Dimension(610, 590));
        panel.setBorder(AppTheme.emptyBorder(48, 52, 48, 52));

        GridBagConstraints gbc = baseConstraints();
        gbc.weightx = 1;

        productLabel = new JLabel();
        productLabel.setForeground(Color.WHITE);
        productLabel.setFont(AppTheme.font(Font.BOLD, 36));
        panel.add(productLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(10, 0, 0, 0);
        editionLabel = new JLabel();
        editionLabel.setForeground(new Color(224, 237, 250));
        editionLabel.setFont(AppTheme.font(Font.PLAIN, 18));
        panel.add(editionLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(36, 0, 0, 0);
        flowLabel = new JLabel();
        flowLabel.setForeground(Color.WHITE);
        flowLabel.setFont(AppTheme.font(Font.BOLD, 18));
        panel.add(flowLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(22, 0, 0, 0);
        panel.add(createCapabilityGrid(), gbc);

        gbc.gridy++;
        gbc.weighty = 1;
        gbc.insets = new Insets(32, 0, 0, 0);
        noteLabel = new JLabel();
        noteLabel.setForeground(new Color(212, 226, 242));
        noteLabel.setFont(AppTheme.font(Font.PLAIN, 13));
        panel.add(noteLabel, gbc);

        return panel;
    }

    private JPanel createCapabilityGrid() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);

        for (int i = 0; i < 9; i++) {
            JLabel item = new JLabel("", SwingConstants.CENTER);
            capabilityLabels.add(item);
            item.setOpaque(true);
            item.setForeground(Color.WHITE);
            item.setBackground(new Color(255, 255, 255, 32));
            item.setFont(AppTheme.font(Font.BOLD, 13));
            item.setBorder(new CompoundBorder(
                    BorderFactory.createLineBorder(new Color(255, 255, 255, 70)),
                    AppTheme.emptyBorder(12, 10, 12, 10)
            ));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = i % 3;
            gbc.gridy = i / 3;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.insets = new Insets(5, 5, 5, 5);
            grid.add(item, gbc);
        }
        return grid;
    }

    private JPanel createLoginPanel() {
        RoundedPanel panel = new RoundedPanel(AppTheme.PANEL_BACKGROUND, 8);
        panel.setPreferredSize(new Dimension(372, 590));
        panel.setLayout(new GridBagLayout());
        panel.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                AppTheme.emptyBorder(34, 34, 30, 34)
        ));

        GridBagConstraints gbc = baseConstraints();
        gbc.weightx = 1;

        titleLabel = new JLabel();
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        titleLabel.setFont(AppTheme.font(Font.BOLD, 26));
        panel.add(titleLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(8, 0, 26, 0);
        subtitleLabel = new JLabel();
        subtitleLabel.setForeground(AppTheme.TEXT_MUTED);
        subtitleLabel.setFont(AppTheme.font(Font.PLAIN, 14));
        panel.add(subtitleLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 8, 0);
        usernameLabel = fieldLabel();
        panel.add(usernameLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        usernameField = new JTextField(dbConfig.isEnabled() ? "" : "admin");
        styleTextField(usernameField);
        panel.add(usernameField, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(18, 0, 8, 0);
        passwordLabel = fieldLabel();
        panel.add(passwordLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        passwordField = new JPasswordField(dbConfig.isEnabled() ? "" : "admin123");
        styleTextField(passwordField);
        panel.add(passwordField, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(18, 0, 8, 0);
        companyLabel = fieldLabel();
        panel.add(companyLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        companyBox = new JComboBox<String>();
        styleComboBox(companyBox);
        panel.add(companyBox, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(18, 0, 8, 0);
        languageLabel = fieldLabel();
        panel.add(languageLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        languageBox = new JComboBox<Language>(Language.values());
        languageBox.setSelectedItem(language);
        styleComboBox(languageBox);
        languageBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Language selected = (Language) languageBox.getSelectedItem();
                if (selected != null && selected != language) {
                    language = selected;
                    updateTexts();
                }
            }
        });
        panel.add(languageBox, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(18, 0, 0, 0);
        rememberBox = new JCheckBox();
        rememberBox.setOpaque(false);
        rememberBox.setForeground(AppTheme.TEXT_MUTED);
        panel.add(rememberBox, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(24, 0, 0, 0);
        loginButton = new JButton();
        stylePrimaryButton(loginButton);
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                attemptLogin();
            }
        });
        panel.add(loginButton, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(16, 0, 0, 0);
        messageLabel = new JLabel();
        messageLabel.setForeground(AppTheme.TEXT_MUTED);
        messageLabel.setFont(AppTheme.font(Font.PLAIN, 12));
        panel.add(messageLabel, gbc);

        gbc.gridy++;
        gbc.weighty = 1;
        gbc.insets = new Insets(18, 0, 0, 0);
        footerLabel = new JLabel();
        footerLabel.setForeground(new Color(128, 139, 155));
        footerLabel.setFont(AppTheme.font(Font.PLAIN, 12));
        panel.add(footerLabel, gbc);

        return panel;
    }

    private JLabel fieldLabel() {
        JLabel label = new JLabel();
        label.setForeground(AppTheme.TEXT_PRIMARY);
        label.setFont(AppTheme.font(Font.BOLD, 13));
        return label;
    }

    private void updateTexts() {
        setTitle(I18n.t(language, "login.window.title"));
        productLabel.setText(I18n.APP_NAME);
        editionLabel.setText(I18n.t(language, "brand.edition"));
        flowLabel.setText(I18n.t(language, "brand.flow"));
        noteLabel.setText("<html>" + I18n.t(language, "brand.note") + "</html>");

        String[] capabilityKeys = {
                "cap.master", "cap.procurement", "cap.sales",
                "cap.inventory", "cap.manufacturing", "cap.finance",
                "cap.admin", "cap.ai", "cap.analytics"
        };
        for (int i = 0; i < capabilityLabels.size(); i++) {
            capabilityLabels.get(i).setText(I18n.t(language, capabilityKeys[i]));
        }

        titleLabel.setText(I18n.t(language, "login.title"));
        subtitleLabel.setText(I18n.t(language, "login.subtitle"));
        usernameLabel.setText(I18n.t(language, "login.user"));
        passwordLabel.setText(I18n.t(language, "login.password"));
        companyLabel.setText(I18n.t(language, "login.company"));
        languageLabel.setText(I18n.t(language, "login.language"));
        rememberBox.setText(I18n.t(language, "login.remember"));
        loginButton.setText(I18n.t(language, "login.button"));
        messageLabel.setText(I18n.t(language, "login.test.account"));
        messageLabel.setForeground(AppTheme.TEXT_MUTED);
        footerLabel.setText("<html>" + I18n.t(language, "login.footer") + "</html>");

        companyBox.removeAllItems();
        companyBox.addItem(I18n.t(language, "app.company"));
        languageBox.setSelectedItem(language);
    }

    private void styleTextField(JTextField field) {
        field.setPreferredSize(new Dimension(304, 42));
        field.setForeground(AppTheme.TEXT_PRIMARY);
        field.setCaretColor(AppTheme.ACCENT);
        field.putClientProperty("JComponent.roundRect", Boolean.TRUE);
        field.putClientProperty("FlatLaf.style", "arc: 10; borderWidth: 1; focusWidth: 1");
        field.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                AppTheme.emptyBorder(0, 12, 0, 12)
        ));
    }

    private void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setPreferredSize(new Dimension(304, 42));
        comboBox.setForeground(AppTheme.TEXT_PRIMARY);
        comboBox.setBackground(Color.WHITE);
        comboBox.putClientProperty("JComponent.roundRect", Boolean.TRUE);
        comboBox.putClientProperty("FlatLaf.style", "arc: 10; borderWidth: 1; focusWidth: 1");
        comboBox.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
    }

    private void stylePrimaryButton(JButton button) {
        button.setPreferredSize(new Dimension(304, 44));
        button.setBackground(AppTheme.ACCENT);
        button.setForeground(Color.WHITE);
        button.setFont(AppTheme.font(Font.BOLD, 15));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("FlatLaf.style", "arc: 12; borderWidth: 0; focusWidth: 1");
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
    }

    private void attemptLogin() {
        setLoginEnabled(false);
        messageLabel.setText(I18n.t(language, "login.signingIn"));
        messageLabel.setForeground(AppTheme.SUCCESS);

        final String username = usernameField.getText();
        final Language selectedLanguage = language;
        final char[] password = passwordField.getPassword();
        AppLogger.info("Sign-in requested for user: " + username);
        AppLogger.userAction("LOGIN_SUBMIT", "username=" + username + " | language=" + selectedLanguage.name());

        SwingWorker<UserSession, Void> worker = new SwingWorker<UserSession, Void>() {
            @Override
            protected UserSession doInBackground() throws Exception {
                try {
                    return authService.authenticate(username, password, selectedLanguage);
                } finally {
                    Arrays.fill(password, '\0');
                }
            }

            @Override
            protected void done() {
                try {
                    UserSession session = get();
                    AppLogger.info("Account authentication succeeded for user: " + session.getUsername());
                    AppLogger.userAction("ACCOUNT_AUTH_SUCCESS", "username=" + session.getUsername());
                    startLicenseCheck(session);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showLoginFailure(I18n.t(selectedLanguage, "login.interrupted"), ex);
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    if (cause instanceof AuthException) {
                        showLoginFailure(cause.getMessage(), cause);
                    } else {
                        showLoginFailure(I18n.t(selectedLanguage, "login.failed.generic"), cause);
                    }
                }
            }
        };
        worker.execute();
    }

    private void showLoginFailure(String message, Throwable throwable) {
        AppLogger.error("Sign-in failed.", throwable);
        AppLogger.userAction("LOGIN_FAILURE", "username=" + usernameField.getText()
                + " | errorType=" + throwable.getClass().getSimpleName());
        messageLabel.setText(message);
        messageLabel.setForeground(AppTheme.ERROR);
        AppMessages.error(this, I18n.t(language, "message.error.title"), message);
        passwordField.selectAll();
        passwordField.requestFocusInWindow();
        setLoginEnabled(true);
    }

    private void startLicenseCheck(final UserSession session) {
        messageLabel.setText(I18n.t(language, "license.checking"));
        messageLabel.setForeground(AppTheme.SUCCESS);
        SwingWorker<LicenseStatus, Void> worker = new SwingWorker<LicenseStatus, Void>() {
            @Override
            protected LicenseStatus doInBackground() throws Exception {
                return licenseRepository.currentStatus(session.getUsername());
            }

            @Override
            protected void done() {
                try {
                    LicenseStatus status = get();
                    if (status.isValid()) {
                        AppLogger.userAction("LICENSE_VALID", "username=" + session.getUsername()
                                + " | validUntil=" + status.getValidUntil());
                        showExpiryWarning(status);
                        loadWorkspace(session);
                    } else {
                        promptAndRegisterLicense(session);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showLicenseFailure(I18n.t(language, "login.interrupted"), ex);
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    showLicenseFailure(I18n.t(language, "license.check.failed"), cause);
                }
            }
        };
        worker.execute();
    }

    private void promptAndRegisterLicense(final UserSession session) {
        final String key = showLicensePrompt();
        if (key == null) {
            AppLogger.userAction("LICENSE_INPUT_CANCEL", "username=" + session.getUsername());
            messageLabel.setText(I18n.t(language, "license.required"));
            messageLabel.setForeground(AppTheme.ERROR);
            setLoginEnabled(true);
            return;
        }

        messageLabel.setText(I18n.t(language, "license.registering"));
        messageLabel.setForeground(AppTheme.SUCCESS);
        SwingWorker<LicenseStatus, Void> worker = new SwingWorker<LicenseStatus, Void>() {
            @Override
            protected LicenseStatus doInBackground() throws Exception {
                return licenseRepository.registerLicense(key, session.getUsername());
            }

            @Override
            protected void done() {
                try {
                    LicenseStatus registered = get();
                    if (registered.isValid()) {
                        AppLogger.userAction("LICENSE_REGISTER_SUCCESS", "username=" + session.getUsername()
                                + " | validUntil=" + registered.getValidUntil());
                        AppMessages.success(LoginFrame.this, I18n.t(language, "license.register.success")
                                + I18n.formatDate(language, registered.getValidUntil()));
                        loadWorkspace(session);
                    } else {
                        AppMessages.error(LoginFrame.this, I18n.t(language, "message.error.title"),
                                I18n.t(language, "license.invalid") + licenseReason(registered));
                        promptAndRegisterLicense(session);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showLicenseFailure(I18n.t(language, "login.interrupted"), ex);
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    showLicenseFailure(I18n.t(language, "license.save.failed"), cause);
                }
            }
        };
        worker.execute();
    }

    private String showLicensePrompt() {
        final JDialog dialog = new JDialog(this, I18n.t(language, "license.prompt.title"), true);
        final JTextField keyField = new JTextField();
        final String[] result = new String[1];

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(AppTheme.PAGE_BACKGROUND);
        root.setBorder(AppTheme.emptyBorder(18, 18, 18, 18));

        JLabel message = new JLabel("<html>" + I18n.t(language, "license.prompt.message") + "</html>");
        message.setForeground(AppTheme.TEXT_PRIMARY);
        message.setFont(AppTheme.font(Font.PLAIN, 13));
        root.add(message, BorderLayout.NORTH);

        keyField.setPreferredSize(new Dimension(420, 34));
        keyField.putClientProperty("JComponent.roundRect", Boolean.TRUE);
        root.add(keyField, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton cancel = new JButton(I18n.t(language, "dialog.confirm.cancel"));
        JButton confirm = new JButton(I18n.t(language, "dialog.confirm.ok"));
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        confirm.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                result[0] = keyField.getText();
                dialog.dispose();
            }
        });
        actions.add(cancel);
        actions.add(confirm);
        root.add(actions, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.getRootPane().setDefaultButton(confirm);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        if (result[0] == null) {
            return null;
        }
        return result[0].trim();
    }

    private void loadWorkspace(final UserSession session) {
        messageLabel.setText(I18n.t(language, "workspace.loading"));
        messageLabel.setForeground(AppTheme.SUCCESS);
        SwingWorker<List<ModulePageData>, Void> worker = new SwingWorker<List<ModulePageData>, Void>() {
            @Override
            protected List<ModulePageData> doInBackground() throws Exception {
                return MainFrame.loadModulesForStartup();
            }

            @Override
            protected void done() {
                try {
                    List<ModulePageData> modules = get();
                    AppLogger.userAction("LOGIN_SUCCESS", "username=" + session.getUsername());
                    MainFrame mainFrame = new MainFrame(session, modules);
                    mainFrame.setVisible(true);
                    AppMessages.success(mainFrame, I18n.t(session.getLanguage(), "message.login.success"));
                    dispose();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showLoginFailure(I18n.t(language, "login.interrupted"), ex);
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    showLoginFailure(I18n.t(language, "login.failed.generic"), cause);
                }
            }
        };
        worker.execute();
    }

    private void showExpiryWarning(LicenseStatus status) {
        long days = status.getRemainingDays();
        if (days == 30 || days == 14 || (days >= 0 && days <= 7)) {
            AppMessages.info(this, I18n.t(language, "license.expiry.warning") + days);
        }
    }

    private String licenseReason(LicenseStatus status) {
        if (status == null || status.getReasonCode().length() == 0) {
            return "";
        }
        return " (" + status.getReasonCode() + ")";
    }

    private void showLicenseFailure(String message, Throwable throwable) {
        AppLogger.error("License step failed.", throwable);
        messageLabel.setText(message);
        messageLabel.setForeground(AppTheme.ERROR);
        AppMessages.error(this, I18n.t(language, "message.error.title"), message);
        setLoginEnabled(true);
    }

    private void setLoginEnabled(boolean enabled) {
        loginButton.setEnabled(enabled);
        usernameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        companyBox.setEnabled(enabled);
        languageBox.setEnabled(enabled);
    }

    private void registerEnterShortcut() {
        JComponent root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                "login"
        );
        root.getActionMap().put("login", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                attemptLogin();
            }
        });
    }

    private GridBagConstraints baseConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, 0, 0);
        return gbc;
    }

    private static final class BrandPanel extends JPanel {
        private BrandPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint paint = new GradientPaint(
                    0, 0, AppTheme.ACCENT_DARK,
                    getWidth(), getHeight(), new Color(20, 112, 107)
            );
            g2.setPaint(paint);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.setColor(new Color(255, 255, 255, 28));
            for (int x = 42; x < getWidth(); x += 54) {
                g2.drawLine(x, 36, x, getHeight() - 36);
            }
            for (int y = 42; y < getHeight(); y += 54) {
                g2.drawLine(36, y, getWidth() - 36, y);
            }
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

}
