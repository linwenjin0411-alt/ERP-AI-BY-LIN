package com.lin.erp.ui;

import com.lin.erp.auth.UserSession;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

final class ReportExportSupport {
    enum Format {
        CSV("CSV", ".csv"),
        EXCEL("Excel", ".xls"),
        PDF("PDF", ".pdf");

        private final String label;
        private final String extension;

        Format(String label, String extension) {
            this.label = label;
            this.extension = extension;
        }
    }

    static final class Snapshot {
        private final String title;
        private final String filePrefix;
        private final String user;
        private final String generatedAt;
        private final String filters;
        private final String[] headers;
        private final String[][] rows;

        Snapshot(String title, String filePrefix, String user, String filters, String[] headers, String[][] rows) {
            this.title = title == null ? "" : title;
            this.filePrefix = sanitizeFilePrefix(filePrefix);
            this.user = user == null ? "" : user;
            this.generatedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            this.filters = filters == null || filters.trim().length() == 0 ? "None" : filters.trim();
            this.headers = headers == null ? new String[0] : headers;
            this.rows = rows == null ? new String[0][0] : rows;
        }

        int rowCount() {
            return rows.length;
        }
    }

    private ReportExportSupport() {
    }

    static Format chooseFormat(Component owner) {
        Object[] options = new Object[]{Format.CSV.label, Format.EXCEL.label, Format.PDF.label};
        Object selected = JOptionPane.showInputDialog(
                owner,
                "Export format",
                "Export",
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );
        if (selected == null) {
            return null;
        }
        if (Format.EXCEL.label.equals(selected)) {
            return Format.EXCEL;
        }
        if (Format.PDF.label.equals(selected)) {
            return Format.PDF;
        }
        return Format.CSV;
    }

    static Snapshot snapshot(String title, String filePrefix, UserSession session, String filters, JTable table) {
        String[] headers = new String[table.getColumnCount()];
        for (int i = 0; i < table.getColumnCount(); i++) {
            headers[i] = table.getColumnName(i);
        }
        String[][] rows = new String[table.getRowCount()][table.getColumnCount()];
        for (int row = 0; row < table.getRowCount(); row++) {
            for (int column = 0; column < table.getColumnCount(); column++) {
                Object value = table.getValueAt(row, column);
                rows[row][column] = value == null ? "" : value.toString();
            }
        }
        return new Snapshot(title, filePrefix, session == null ? "" : session.getUsername(), filters, headers, rows);
    }

    static File export(Snapshot snapshot, Format format) throws Exception {
        File exportDir = controlledExportDir();
        File file = new File(exportDir, snapshot.filePrefix + "-"
                + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + format.extension);
        if (format == Format.EXCEL) {
            writeExcelHtml(file, snapshot);
        } else if (format == Format.PDF) {
            writePdf(file, snapshot);
        } else {
            writeCsv(file, snapshot);
        }
        return file;
    }

    static void showPrintPreview(Window owner, Snapshot snapshot) {
        JTextArea area = new JTextArea(previewText(snapshot));
        area.setEditable(false);
        area.setFont(AppTheme.font(java.awt.Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setPreferredSize(new Dimension(760, 520));
        JOptionPane.showMessageDialog(owner, scrollPane, "Print Preview", JOptionPane.PLAIN_MESSAGE);
        try {
            area.print();
        } catch (Exception ignored) {
            // User may cancel printing; preview has already been shown.
        }
    }

    private static File controlledExportDir() throws Exception {
        File exportDir = new File("exports").getCanonicalFile();
        File workspace = new File(".").getCanonicalFile();
        if (!exportDir.getPath().startsWith(workspace.getPath())) {
            throw new SecurityException("Export directory must stay inside the application workspace.");
        }
        if (!exportDir.isDirectory() && !exportDir.mkdirs()) {
            throw new IllegalStateException("Cannot create export directory.");
        }
        return exportDir;
    }

    private static void writeCsv(File file, Snapshot snapshot) throws Exception {
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            writer.write('\ufeff');
            writeCsvLine(writer, new String[]{"Report", snapshot.title});
            writeCsvLine(writer, new String[]{"Generated by", snapshot.user});
            writeCsvLine(writer, new String[]{"Generated at", snapshot.generatedAt});
            writeCsvLine(writer, new String[]{"Filters", snapshot.filters});
            writeCsvLine(writer, new String[0]);
            writeCsvLine(writer, snapshot.headers);
            for (int i = 0; i < snapshot.rows.length; i++) {
                writeCsvLine(writer, snapshot.rows[i]);
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private static void writeExcelHtml(File file, Snapshot snapshot) throws Exception {
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            writer.write("<html><head><meta charset=\"UTF-8\"></head><body>");
            writer.write("<h2>" + html(snapshot.title) + "</h2>");
            writer.write("<p>Generated by: " + html(snapshot.user) + "<br>");
            writer.write("Generated at: " + html(snapshot.generatedAt) + "<br>");
            writer.write("Filters: " + html(snapshot.filters) + "</p>");
            writer.write("<table border=\"1\"><tr>");
            for (int i = 0; i < snapshot.headers.length; i++) {
                writer.write("<th>" + html(snapshot.headers[i]) + "</th>");
            }
            writer.write("</tr>");
            for (int i = 0; i < snapshot.rows.length; i++) {
                writer.write("<tr>");
                for (int j = 0; j < snapshot.headers.length; j++) {
                    writer.write("<td>" + html(j < snapshot.rows[i].length ? snapshot.rows[i][j] : "") + "</td>");
                }
                writer.write("</tr>");
            }
            writer.write("</table></body></html>");
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private static void writePdf(File file, Snapshot snapshot) throws Exception {
        List<String> lines = new ArrayList<String>();
        String[] preview = previewText(snapshot).split("\\r?\\n");
        for (int i = 0; i < preview.length && i < 52; i++) {
            lines.add(preview[i]);
        }
        StringBuilder content = new StringBuilder();
        content.append("BT /F1 10 Tf 36 806 Td 14 TL\n");
        for (String line : lines) {
            content.append("(").append(pdf(line)).append(") Tj T*\n");
        }
        content.append("ET\n");
        byte[] stream = content.toString().getBytes(StandardCharsets.US_ASCII);
        StringBuilder pdf = new StringBuilder();
        List<Integer> offsets = new ArrayList<Integer>();
        pdf.append("%PDF-1.4\n");
        offsets.add(pdf.length());
        pdf.append("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n");
        offsets.add(pdf.length());
        pdf.append("2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n");
        offsets.add(pdf.length());
        pdf.append("3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj\n");
        offsets.add(pdf.length());
        pdf.append("4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n");
        offsets.add(pdf.length());
        pdf.append("5 0 obj << /Length ").append(stream.length).append(" >> stream\n");
        pdf.append(content).append("endstream endobj\n");
        int xref = pdf.length();
        pdf.append("xref\n0 6\n0000000000 65535 f \n");
        for (Integer offset : offsets) {
            pdf.append(String.format("%010d 00000 n \n", offset.intValue()));
        }
        pdf.append("trailer << /Size 6 /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF");
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(file);
            output.write(pdf.toString().getBytes(StandardCharsets.US_ASCII));
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    private static String previewText(Snapshot snapshot) {
        StringBuilder text = new StringBuilder();
        text.append(snapshot.title).append('\n');
        text.append("Generated by: ").append(snapshot.user).append('\n');
        text.append("Generated at: ").append(snapshot.generatedAt).append('\n');
        text.append("Filters: ").append(snapshot.filters).append("\n\n");
        text.append(join(snapshot.headers)).append('\n');
        for (int i = 0; i < snapshot.rows.length; i++) {
            text.append(join(snapshot.rows[i])).append('\n');
        }
        return text.toString();
    }

    private static void writeCsvLine(Writer writer, String[] values) throws Exception {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                writer.write(",");
            }
            String value = protectCsv(values[i] == null ? "" : values[i]);
            writer.write("\"" + value.replace("\"", "\"\"") + "\"");
        }
        writer.write(System.lineSeparator());
    }

    private static String protectCsv(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("=") || trimmed.startsWith("+") || trimmed.startsWith("-") || trimmed.startsWith("@")) {
            return "'" + value;
        }
        return value;
    }

    private static String join(String[] values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(" | ");
            }
            builder.append(values[i] == null ? "" : values[i]);
        }
        return builder.toString();
    }

    private static String html(String value) {
        return (value == null ? "" : value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String pdf(String value) {
        return (value == null ? "" : value).replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                .replaceAll("[^\\x20-\\x7E]", "?");
    }

    private static String sanitizeFilePrefix(String value) {
        String normalized = value == null ? "report" : value.trim().toLowerCase();
        normalized = normalized.replaceAll("[^a-z0-9._-]+", "-");
        return normalized.length() == 0 ? "report" : normalized;
    }
}
