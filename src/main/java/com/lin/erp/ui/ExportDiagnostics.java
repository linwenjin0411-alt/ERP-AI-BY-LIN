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
                "Export diagnostic 导出诊断 エクスポート診断",
                "export-diagnostic",
                session.getUsername(),
                "Smoke test / 中文 / 日本語",
                new String[]{"ID", "Status 状态 ステータス", "Owner 负责人 担当"},
                new String[][]{
                        {"EXP-0001", "Ready 就绪 準備完了", "System 系统 システム"},
                        {"EXP-0002", "Late 延期 遅延", "Procurement 采购 購買"}
                }
        );
        File csvFile = ReportExportSupport.export(snapshot, ReportExportSupport.Format.CSV);
        File pdfFile = ReportExportSupport.export(snapshot, ReportExportSupport.Format.PDF);
        if (!csvFile.isFile() || csvFile.length() == 0 || !pdfFile.isFile() || pdfFile.length() == 0) {
            throw new IllegalStateException("Export diagnostic did not create expected files.");
        }
        System.out.println("Export diagnostic succeeded.");
        System.out.println("CSV: " + csvFile.getAbsolutePath() + " (" + csvFile.length() + " bytes)");
        System.out.println("PDF: " + pdfFile.getAbsolutePath() + " (" + pdfFile.length() + " bytes)");
    }
}
