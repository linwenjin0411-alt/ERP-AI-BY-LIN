package com.lin.erp.ui;

import com.lin.erp.auth.UserSession;
import com.lin.erp.i18n.Language;
import com.lin.erp.logging.AppLogger;

import java.io.File;

public final class ExportDiagnostics {
    private ExportDiagnostics() {
    }

    public static void main(String[] args) throws Exception {
        AppLogger.init(false);
        if (args.length == 0 || (!"--self-check".equals(args[0]) && !"--smoke".equals(args[0]))) {
            System.out.println("Usage: java com.lin.erp.ui.ExportDiagnostics --self-check");
            System.out.println("       java com.lin.erp.ui.ExportDiagnostics --smoke");
            return;
        }

        Class.forName("com.lin.erp.ui.ReportExportSupport");
        Class.forName("com.lin.erp.ui.ReportExportSupport$Snapshot");
        Class.forName("com.lin.erp.ui.BackgroundTasks");

        if ("--self-check".equals(args[0])) {
            System.out.println("Export runtime classes are available.");
            return;
        }

        UserSession session = new UserSession(
                "admin",
                "user.admin.name",
                "ADMIN",
                "role.admin",
                "app.company",
                Language.EN
        );
        ReportExportSupport.Snapshot snapshot = new ReportExportSupport.Snapshot(
                "Export diagnostic",
                "export-diagnostic",
                session.getUsername(),
                "Smoke test",
                new String[]{"ID", "Status", "Owner"},
                new String[][]{{"EXP-0001", "Ready", "System"}}
        );
        File file = ReportExportSupport.export(snapshot, ReportExportSupport.Format.CSV);
        if (!file.isFile() || file.length() == 0) {
            throw new IllegalStateException("Export diagnostic did not create a file.");
        }
        System.out.println("Export diagnostic succeeded.");
        System.out.println("File: " + file.getAbsolutePath());
        System.out.println("Bytes: " + file.length());
    }
}
