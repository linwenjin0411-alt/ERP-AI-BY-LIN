package com.lin.erp;

import com.lin.erp.auth.AuthService;
import com.lin.erp.auth.UserSession;
import com.lin.erp.i18n.Language;
import com.lin.erp.logging.AppLogger;
import com.lin.erp.ui.MainFrame;

public class Diagnostics {
    public static void main(String[] args) throws Exception {
        AppLogger.init(false);

        if (args.length == 0 || !"--login".equals(args[0])) {
            System.out.println("Usage: java com.lin.erp.Diagnostics --login <username> <password>");
            return;
        }

        String username = args.length > 1 ? args[1] : "admin";
        String password = args.length > 2 ? args[2] : "admin123";

        long start = System.currentTimeMillis();
        AuthService authService = new AuthService();
        UserSession session = authService.authenticate(username, password.toCharArray(), Language.EN);
        int moduleCount = MainFrame.loadModulesForStartup().size();
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("Login diagnostic succeeded.");
        System.out.println("User: " + session.getUsername());
        System.out.println("Modules loaded: " + moduleCount);
        System.out.println("Elapsed: " + elapsed + " ms");
    }
}
