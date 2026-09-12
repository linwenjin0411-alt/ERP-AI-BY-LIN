package com.lin.erp.ui;

import com.lin.erp.auth.UserSession;
import com.lin.erp.config.DbConfig;
import com.lin.erp.db.DbFunctionRecordRepository;
import com.lin.erp.db.DbRoleMenuPermissionRepository;
import com.lin.erp.db.FunctionRecord;
import com.lin.erp.db.MenuNode;
import com.lin.erp.db.RoleMenuPermission;
import com.lin.erp.i18n.I18n;
import com.lin.erp.i18n.Language;
import com.lin.erp.logging.AppLogger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.RowFilter;
import javax.swing.border.CompoundBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
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
import java.util.List;

public class BusinessFunctionPanel extends JPanel {
    private final Window owner;
    private final UserSession session;
    private final MenuNode function;
    private final BusinessFunctionDefinition definition;
    private final DbFunctionRecordRepository repository;
    private final RoleMenuPermission permission;
    private final List<FunctionRecord> records = new ArrayList<FunctionRecord>();

    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> tableSorter;
    private JTextField filterField;
    private JLabel countLabel;
    private JLabel totalLabel;
    private JLabel openLabel;
    private JLabel releasedLabel;

    public BusinessFunctionPanel(Window owner, UserSession session, MenuNode function) {
        super(new BorderLayout(0, 16));
        this.owner = owner;
        this.session = session;
        this.function = function;
        this.definition = BusinessFunctionCatalog.forFunction(function);
        this.repository = new DbFunctionRecordRepository(DbConfig.loadDefault(), session.getUsername(), session.getCompanyNameKey(), session.getRoleCode());
        this.permission = loadPermission(function.getCode());
        setOpaque(false);
        add(createToolbar(), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);
        reload();
        logAction("FUNCTION_PAGE_OPEN", "function=" + function.getCode());
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(16, 0));
        toolbar.setOpaque(false);

        JPanel titleBlock = new JPanel(new GridLayout(0, 1, 0, 4));
        titleBlock.setOpaque(false);
        JLabel title = new JLabel(text(function.getNameKey()));
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
        filterField = new JTextField(18);
        filterField.putClientProperty("JTextField.placeholderText", "Filter");
        filterField.setBorder(AppTheme.emptyBorder(8, 10, 8, 10));
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
        actions.add(filterField);
        actions.add(createActionButton("action.new", "action.new"));
        actions.add(createActionButton("action.edit", "action.edit"));
        actions.add(createActionButton("action.delete", "action.delete"));
        actions.add(createActionButton("action.refresh", "action.refresh"));
        actions.add(createActionButton("action.export", "action.export"));
        toolbar.add(actions, BorderLayout.EAST);
        return toolbar;
    }

    private JButton createActionButton(final String actionKey, String iconKey) {
        JButton button = new JButton(t(actionKey));
        button.setIcon(new ActionIcon(iconKey, "action.new".equals(actionKey) ? Color.WHITE : AppTheme.ACCENT));
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
        boolean mutableAction = "action.new".equals(actionKey) || "action.edit".equals(actionKey) || "action.delete".equals(actionKey);
        boolean allowed = permission.allows(actionKey) && !(definition.isReadOnly() && mutableAction);
        button.setEnabled(allowed);
        if (!allowed) {
            button.setToolTipText(definition.isReadOnly() && mutableAction
                    ? "Read-only page"
                    : t("message.permission.denied"));
        }
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleAction(actionKey);
            }
        });
        return button;
    }

    private JPanel createBody() {
        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);
        body.add(createTableCard(), BorderLayout.CENTER);
        body.add(createSidePanel(), BorderLayout.EAST);
        return body;
    }

    private JPanel createTableCard() {
        RoundedPanel card = new RoundedPanel(Color.WHITE, 8);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 235, 244)),
                AppTheme.emptyBorder(16, 16, 16, 16)
        ));

        tableModel = new DefaultTableModel(new String[0][0], localized(definition.getTableColumnKeys())) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        tableSorter = new TableRowSorter<DefaultTableModel>(tableModel);
        table.setRowSorter(tableSorter);
        table.setRowHeight(34);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(237, 242, 247));
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(AppTheme.ACCENT_SOFT);
        table.setSelectionForeground(AppTheme.TEXT_PRIMARY);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openForm(true);
                }
            }
        });
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setBackground(new Color(248, 250, 252));
        header.setForeground(AppTheme.TEXT_MUTED);
        header.setFont(AppTheme.font(Font.BOLD, 11));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(229, 235, 244)));
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    private JPanel createSidePanel() {
        RoundedPanel side = new RoundedPanel(Color.WHITE, 8);
        side.setPreferredSize(new Dimension(300, 1));
        side.setLayout(new BorderLayout(0, 14));
        side.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 235, 244)),
                AppTheme.emptyBorder(16, 16, 16, 16)
        ));

        JLabel title = new JLabel(t("form.side.title"));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setFont(AppTheme.font(Font.BOLD, 14));
        side.add(title, BorderLayout.NORTH);

        JPanel list = new JPanel(new GridLayout(0, 1, 0, 10));
        list.setOpaque(false);
        totalLabel = metricValueLabel();
        releasedLabel = metricValueLabel();
        openLabel = metricValueLabel();
        list.add(metricRow(t("item.side.total"), totalLabel));
        list.add(metricRow(t("item.side.released"), releasedLabel));
        list.add(metricRow(t("item.side.open"), openLabel));
        list.add(contextRow(t("function.context.flow"), definition.getFlow()));
        list.add(contextRow(t("function.context.upstream"), definition.getUpstream()));
        list.add(contextRow(t("function.context.downstream"), definition.getDownstream()));
        list.add(contextRow("Numbering rule", definition.getNumberingRule()));
        list.add(contextRow("Organization / Period", definition.getOrganizationPeriod()));
        if (definition.isReadOnly()) {
            list.add(contextRow("Mode", "Read-only query/export page"));
        }
        side.add(list, BorderLayout.CENTER);
        return side;
    }

    private JPanel metricRow(String label, JLabel number) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel name = new JLabel(label);
        name.setForeground(AppTheme.TEXT_MUTED);
        name.setFont(AppTheme.font(Font.PLAIN, 12));
        row.add(name, BorderLayout.CENTER);
        row.add(number, BorderLayout.EAST);
        return row;
    }

    private JPanel contextRow(String label, String value) {
        JPanel row = new JPanel(new GridLayout(0, 1, 0, 4));
        row.setOpaque(false);
        JLabel name = new JLabel(label);
        name.setForeground(AppTheme.TEXT_PRIMARY);
        name.setFont(AppTheme.font(Font.BOLD, 12));
        JLabel body = new JLabel("<html><body style='width:230px'>" + escapeHtml(value) + "</body></html>");
        body.setForeground(AppTheme.TEXT_MUTED);
        body.setFont(AppTheme.font(Font.PLAIN, 12));
        row.add(name);
        row.add(body);
        return row;
    }

    private JLabel metricValueLabel() {
        JLabel label = new JLabel("0", SwingConstants.RIGHT);
        label.setForeground(AppTheme.ACCENT);
        label.setFont(AppTheme.font(Font.BOLD, 16));
        return label;
    }

    private void handleAction(String actionKey) {
        logAction("FUNCTION_ACTION_CLICK", "function=" + function.getCode() + " | action=" + english(actionKey));
        if (!ensureActionAllowed(actionKey)) {
            return;
        }
        if ("action.new".equals(actionKey)) {
            openForm(false);
        } else if ("action.edit".equals(actionKey)) {
            openForm(true);
        } else if ("action.delete".equals(actionKey)) {
            deleteSelected();
        } else if ("action.refresh".equals(actionKey)) {
            reload();
            AppMessages.info(owner, t("message.refresh.done"));
        } else if ("action.export".equals(actionKey)) {
            export();
        }
    }

    private boolean ensureActionAllowed(String actionKey) {
        if (permission.allows(actionKey)) {
            if (definition.isReadOnly()
                    && ("action.new".equals(actionKey) || "action.edit".equals(actionKey) || "action.delete".equals(actionKey))) {
                AppMessages.error(owner, t("message.error.title"), "This page is read-only. Use filter, sort, refresh, or export.");
                return false;
            }
            return true;
        }
        logAction("FUNCTION_ACTION_DENIED", "function=" + function.getCode()
                + " | action=" + actionKey
                + " | permissionScope=" + permission.getScopeCode());
        AppMessages.error(owner, t("message.error.title"), t("message.permission.denied"));
        return false;
    }

    private RoleMenuPermission loadPermission(String menuCode) {
        try {
            return new DbRoleMenuPermissionRepository(DbConfig.loadDefault()).loadForMenu(session.getRoleCode(), menuCode);
        } catch (SQLException e) {
            AppLogger.error("Function permission load failed.", e);
            return RoleMenuPermission.viewOnly(menuCode);
        }
    }

    private void openForm(boolean editMode) {
        int row = selectedRow();
        FunctionRecord selected = null;
        if (editMode) {
            if (row < 0) {
                AppMessages.error(owner, t("message.error.title"), t("message.select.row"));
                return;
            }
            selected = records.get(row);
        }

        String[] initialValues = selected == null ? createDefaultFormValues() : recordValuesForForm(selected);
        RecordFormDialog dialog = new RecordFormDialog(
                owner,
                session.getLanguage(),
                text(function.getNameKey()),
                editMode ? t("action.edit") : t("action.new"),
                editMode ? "action.edit" : "action.new",
                definition.getFieldKeys(),
                initialValues
        );
        dialog.setVisible(true);

        if (!dialog.isSaved()) {
            return;
        }
        String[] formValues = dialog.getValues();
        if (formValues.length == 0 || formValues[0] == null || formValues[0].trim().length() == 0) {
            AppMessages.error(owner, t("message.error.title"), t("message.id.required"));
            return;
        }

        final FunctionRecord selectedRecord = selected;
        BackgroundTasks.run(
                owner,
                "Function record save failed.",
                t("message.error.title"),
                t("message.save.failed"),
                new BackgroundTasks.Work<Void>() {
                    @Override
                    public Void run() throws Exception {
                        if (editMode) {
                            repository.updateRecord(selectedRecord.getId(), valuesForTable(formValues, currentStatusValue(formValues)));
                        } else {
                            repository.createRecord(function.getCode(), valuesForTable(formValues, "status.open"));
                        }
                        return null;
                    }
                },
                new BackgroundTasks.Success<Void>() {
                    @Override
                    public void accept(Void value) {
                        AppMessages.success(owner, editMode ? t("message.edit.success") : t("message.create.success"));
                        reload();
                    }
                }
        );
    }

    private void deleteSelected() {
        int row = selectedRow();
        if (row < 0) {
            AppMessages.error(owner, t("message.error.title"), t("message.select.row"));
            return;
        }
        FunctionRecord record = records.get(row);
        int result = JOptionPane.showConfirmDialog(
                owner,
                t("message.delete.confirm") + " " + displayRecordId(record) + " ?",
                t("dialog.confirm.title"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        BackgroundTasks.run(
                owner,
                "Function record delete failed.",
                t("message.error.title"),
                t("message.delete.failed"),
                new BackgroundTasks.Work<Void>() {
                    @Override
                    public Void run() throws Exception {
                        repository.deleteRecord(record.getId());
                        return null;
                    }
                },
                new BackgroundTasks.Success<Void>() {
                    @Override
                    public void accept(Void value) {
                        AppMessages.success(owner, t("message.delete.success"));
                        reload();
                    }
                }
        );
    }

    private void reload() {
        BackgroundTasks.run(
                owner,
                "Function record load failed.",
                t("message.error.title"),
                t("message.refresh.failed"),
                new BackgroundTasks.Work<List<FunctionRecord>>() {
                    @Override
                    public List<FunctionRecord> run() throws Exception {
                        return repository.loadRecords(function.getCode(), definition.getTableRows());
                    }
                },
                new BackgroundTasks.Success<List<FunctionRecord>>() {
                    @Override
                    public void accept(List<FunctionRecord> loaded) {
                        records.clear();
                        records.addAll(loaded);
                        rebuildTable();
                    }
                }
        );
    }

    private void rebuildTable() {
        String[][] rows = new String[records.size()][];
        for (int i = 0; i < records.size(); i++) {
            rows[i] = localized(records.get(i).getValues());
        }
        tableModel.setDataVector(rows, localized(definition.getTableColumnKeys()));
        tableSorter.setModel(tableModel);
        applyFilter();
        countLabel.setText(t("item.count.prefix") + records.size());
        if (records.size() > 0) {
            table.setRowSelectionInterval(0, 0);
        }
        if (totalLabel != null) {
            totalLabel.setText(String.valueOf(records.size()));
            releasedLabel.setText(String.valueOf(countStatus("status.released")));
            openLabel.setText(String.valueOf(countStatus("status.open")));
        }
    }

    private int countStatus(String status) {
        int count = 0;
        for (FunctionRecord record : records) {
            for (String value : record.getValues()) {
                if (status.equals(value)) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private void export() {
        File exportDir = new File("exports");
        if (!exportDir.isDirectory() && !exportDir.mkdirs()) {
            AppMessages.error(owner, t("message.error.title"), t("message.export.failed"));
            return;
        }

        File file = new File(exportDir, function.getCode().toLowerCase() + "-"
                + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".csv");
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            writer.write('\ufeff');
            writeCsvLine(writer, localized(definition.getTableColumnKeys()));
            for (FunctionRecord record : records) {
                writeCsvLine(writer, localized(record.getValues()));
            }
            AppMessages.success(owner, t("message.export.success") + file.getAbsolutePath());
        } catch (Exception e) {
            AppLogger.error("Function record export failed.", e);
            AppMessages.error(owner, t("message.error.title"), t("message.export.failed"));
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignored) {
                    // Export already completed or failed.
                }
            }
        }
    }

    private int selectedRow() {
        if (table == null || table.getSelectedRow() < 0) {
            return -1;
        }
        int viewRow = table.getSelectedRow();
        return table.convertRowIndexToModel(viewRow);
    }

    private void applyFilter() {
        if (tableSorter == null || filterField == null) {
            return;
        }
        String filter = filterField.getText();
        if (filter == null || filter.trim().length() == 0) {
            tableSorter.setRowFilter(null);
            return;
        }
        tableSorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(filter.trim())));
    }

    private String[] createDefaultFormValues() {
        String[] defaults = definition.getDefaultValues();
        String[] values = new String[definition.getFieldKeys().length];
        for (int i = 0; i < values.length; i++) {
            values[i] = i < defaults.length ? text(defaults[i]) : "";
        }
        if (values.length > 0) {
            values[0] = defaultDocumentNumber();
        }
        return values;
    }

    private String[] recordValuesForForm(FunctionRecord record) {
        String[] formValues = createDefaultFormValues();
        String[] columns = definition.getTableColumnKeys();
        String[] values = record.getValues();
        for (int i = 0; i < columns.length && i < values.length; i++) {
            int index = fieldIndexForColumn(columns[i]);
            if (index >= 0 && index < formValues.length) {
                formValues[index] = text(values[i]);
            }
        }
        return formValues;
    }

    private String[] valuesForTable(String[] formValues, String status) {
        String[] columns = definition.getTableColumnKeys();
        String[] values = new String[Math.min(8, columns.length)];
        for (int i = 0; i < values.length; i++) {
            values[i] = valueForColumn(columns[i], formValues, i, status);
        }
        return values;
    }

    private String valueForColumn(String columnKey, String[] formValues, int index, String status) {
        if ("function.table.line".equals(columnKey)) {
            String line = formValue(formValues, 0);
            return line.matches("\\d+") ? line : String.valueOf(records.size() + 1);
        }
        if ("column.status".equals(columnKey)) {
            return status;
        }
        if ("column.next".equals(columnKey)) {
            return nextStepKey();
        }
        if ("column.risk".equals(columnKey)) {
            return "risk.low";
        }
        int fieldIndex = fieldIndexForColumn(columnKey);
        if (fieldIndex >= 0) {
            return formValue(formValues, fieldIndex);
        }
        return index < formValues.length ? formValue(formValues, index) : "";
    }

    private int fieldIndexForColumn(String columnKey) {
        String[] fieldKeys = definition.getFieldKeys();
        for (int i = 0; i < fieldKeys.length; i++) {
            if (columnKey.equals(fieldKeys[i])) {
                return i;
            }
        }
        if ("column.id".equals(columnKey) || "license.field.key".equals(columnKey)) {
            return 0;
        }
        if ("column.name".equals(columnKey)) {
            return Math.min(1, fieldKeys.length - 1);
        }
        if ("column.role".equals(columnKey) || "column.type".equals(columnKey) || "column.permission".equals(columnKey)) {
            return Math.min(2, fieldKeys.length - 1);
        }
        if ("column.department".equals(columnKey) || "column.scope".equals(columnKey)) {
            return Math.min(3, fieldKeys.length - 1);
        }
        if ("column.email".equals(columnKey) || "column.language".equals(columnKey)
                || "column.item".equals(columnKey) || "license.field.validUntil".equals(columnKey)) {
            return Math.min(4, fieldKeys.length - 1);
        }
        if ("column.customer".equals(columnKey) || "column.supplier".equals(columnKey)) {
            return Math.min(3, fieldKeys.length - 1);
        }
        if ("column.qty".equals(columnKey) || "column.amount".equals(columnKey)) {
            return Math.min(5, fieldKeys.length - 1);
        }
        if ("column.warehouse".equals(columnKey) || "column.plant".equals(columnKey)) {
            return Math.min(6, fieldKeys.length - 1);
        }
        if ("column.owner".equals(columnKey)) {
            return Math.min(7, fieldKeys.length - 1);
        }
        if ("column.date".equals(columnKey) || "column.due".equals(columnKey) || "license.field.validFrom".equals(columnKey)) {
            return Math.min(1, fieldKeys.length - 1);
        }
        return -1;
    }

    private String currentStatusValue(String[] formValues) {
        String[] fieldKeys = definition.getFieldKeys();
        for (int i = 0; i < fieldKeys.length; i++) {
            if (("function.field.status".equals(fieldKeys[i]) || "column.status".equals(fieldKeys[i]))
                    && formValue(formValues, i).trim().length() > 0) {
                return formValue(formValues, i);
            }
        }
        return "status.released";
    }

    private String formValue(String[] values, int index) {
        if (values == null || index < 0 || index >= values.length || values[index] == null) {
            return "";
        }
        return values[index].trim();
    }

    private String[] localized(String[] values) {
        String[] localized = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            localized[i] = text(values[i]);
        }
        return localized;
    }

    private void writeCsvLine(Writer writer, String[] values) throws Exception {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                writer.write(",");
            }
            String value = values[i] == null ? "" : values[i];
            writer.write("\"" + value.replace("\"", "\"\"") + "\"");
        }
        writer.write(System.lineSeparator());
    }

    private String displayRecordId(FunctionRecord record) {
        String[] values = record.getValues();
        return values.length == 0 || values[0] == null || values[0].trim().length() == 0 ? String.valueOf(record.getId()) : text(values[0]);
    }

    private String defaultDocumentNumber() {
        String code = function.getCode();
        String prefix = code.length() <= 3 ? code : code.substring(0, 3);
        return prefix + "-" + new SimpleDateFormat("MMddHHmm").format(new Date());
    }

    private String nextStepKey() {
        String module = function.getModuleCode();
        if ("SALES".equals(module)) {
            return "term.shipment";
        }
        if ("PROCUREMENT".equals(module)) {
            return "term.receipt";
        }
        if ("MANUFACTURING".equals(module)) {
            return "term.materialIssue";
        }
        if ("FINANCE".equals(module)) {
            return "action.post";
        }
        return "action.details";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String t(String key) {
        return I18n.t(session.getLanguage(), key);
    }

    private String text(String value) {
        return I18n.textOrValue(session.getLanguage(), value);
    }

    private String english(String key) {
        return I18n.textOrValue(Language.EN, key);
    }

    private void logAction(String event, String details) {
        AppLogger.userAction(event, "user=" + session.getUsername()
                + " | module=" + function.getModuleCode()
                + (details == null || details.trim().length() == 0 ? "" : " | " + details));
    }
}
