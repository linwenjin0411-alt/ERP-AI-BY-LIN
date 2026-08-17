package com.lin.erp;

import com.lin.erp.auth.AuthService;
import com.lin.erp.logging.AppLogger;
import com.lin.erp.ui.AppTheme;
import com.lin.erp.ui.LoginFrame;

import javax.swing.SwingUtilities;

public class ErpApp {
    public static void main(String[] args) {
        AppLogger.init();
        AppLogger.info("Starting Linova One ERP.");
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    start();
                } catch (Throwable throwable) {
                    AppLogger.error("Application startup failed.", throwable);
                    throw throwable;
                }
            }
        });
    }

    private static void start() {
        AppTheme.installLookAndFeel();
        AppTheme.applyGlobalDefaults();
        AuthService authService = new AuthService();
        LoginFrame loginFrame = new LoginFrame(authService);
        loginFrame.setVisible(true);
    }
}
