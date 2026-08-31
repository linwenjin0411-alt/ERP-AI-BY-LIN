package com.lin.erp.ui;

import com.lin.erp.auth.UserSession;
import com.lin.erp.config.DbConfig;
import com.lin.erp.db.DbFunctionRecordRepository;
import com.lin.erp.db.FunctionRecord;
import com.lin.erp.db.MenuNode;
import com.lin.erp.i18n.I18n;
import com.lin.erp.i18n.Language;
import com.lin.erp.logging.AppLogger;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BusinessFunctionPanel extends JPanel {
    private static final String MODE_REGISTER = "function.mode.register";
    private static final String MODE_CORRECT = "function.mode.correct";
    private static final String MODE_CANCEL = "function.mode.cancel";
    private static final String MODE_REFERENCE = "function.mode.reference";

    private final Window owner;
    private final UserSession session;
    private final MenuNode function;
    private final BusinessFunctionDefinition definition;
    private final DbFunctionRecordRepository repository;
    private final List<FunctionRecord> records = new ArrayList<FunctionRecord>();
    private JTextField[] fields;
    private final JRadioButton[] modeButtons = new JRadioButton[4];
    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField searchField;

    public BusinessFunctionPanel(Window owner, UserSession session, MenuNode function) {
        super(new BorderLayout(0, 14));
        this.owner = owner;
        this.session = session;
        this.function = function;
        this.definition = BusinessFunctionCatalog.forFunction(function);
        this.repository = new DbFunctionRecordRepository(DbConfig.loadDefault());
        this.fields = new JTextField[definition.getFieldKeys().length];
        setOpaque(false);
        add(createOperationStrip(), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);
        reload();
        logAction("GENERIC_FUNCTION_OPEN", "function=" + function.getCode());
    }

    private JPanel createOperationStrip() {
        RoundedPanel strip = new RoundedPanel(Color.WHITE, 8);
        strip.setLayout(new BorderLayout(16, 0));
        strip.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(231, 236, 244)),
                AppTheme.emptyBorder(10, 12, 10, 12)
        ));

        JPanel modes = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        modes.setOpaque(false);
        JLabel modeLabel = smallLabel(t("function.operation"));
        modes.add(modeLabel);

        ButtonGroup group = new ButtonGroup();
        String[] modeKeys = {MODE_REGISTER, MODE_CORRECT, MODE_CANCEL, MODE_REFERENCE};
        for (int i = 0; i < modeKeys.length; i++) {
            final String modeKey = modeKeys[i];
            JRadioButton radio = new JRadioButton(t(modeKey));
            radio.setOpaque(false);
            radio.setForeground(AppTheme.TEXT_PRIMARY);
            radio.setFont(AppTheme.font(Font.PLAIN, 12));
            radio.putClientProperty("FlatLaf.style", "focusWidth: 0");
            radio.setSelected(i == 0);
            radio.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    applyMode(modeKey);
                }
            });
            modeButtons[i] = radio;
            group.add(radio);
            modes.add(radio);
        }
        strip.add(modes, BorderLayout.WEST);

        JPanel search = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        search.setOpaque(false);
        search.add(smallLabel(t("function.search.target")));
        searchField = new JTextField(defaultDocumentNumber());
        searchField.setPreferredSize(new Dimension(210, 32));
        styleCompactField(searchField);
        search.add(searchField);
        search.add(createStripButton("function.search", "FUNCTION_SEARCH"));
        strip.add(search, BorderLayout.EAST);
        return strip;
    }

    private JPanel createBody() {
        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);
        body.add(createFormCard(), BorderLayout.CENTER);
        body.add(createContextPanel(), BorderLayout.EAST);
        return body;
    }

    private JPanel createFormCard() {
        RoundedPanel card = new RoundedPanel(Color.WHITE, 8);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 235, 244)),
                AppTheme.emptyBorder(16, 16, 16, 16)
        ));

        JPanel band = new JPanel(new BorderLayout());
        band.setOpaque(false);
        JLabel title = new JLabel(t("form.transactionRecord"));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setFont(AppTheme.font(Font.BOLD, 15));
        band.add(title, BorderLayout.WEST);
        JLabel code = new JLabel(function.getCode(), SwingConstants.RIGHT);
        code.setForeground(AppTheme.TEXT_MUTED);
        code.setFont(AppTheme.font(Font.PLAIN, 11));
        band.add(code, BorderLayout.EAST);
        card.add(band, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.setBorder(new MatteBorder(1, 0, 0, 0, new Color(238, 242, 247)));
        center.add(createForm(), BorderLayout.NORTH);
        center.add(createLineTable(), BorderLayout.CENTER);
        center.add(createActionFooter(), BorderLayout.SOUTH);
        card.add(center, BorderLayout.CENTER);
        return card;
    }

    private JPanel createForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(AppTheme.emptyBorder(14, 0, 2, 0));
        String[] labelKeys = definition.getFieldKeys();
        String[] values = definition.getDefaultValues();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 7, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < labelKeys.length; i++) {
            int row = i / 3;
            int column = i % 3;
            gbc.gridy = row;
            gbc.gridx = column * 2;
            gbc.weightx = 0;
            form.add(smallLabel(t(labelKeys[i])), gbc);

            gbc.gridx = column * 2 + 1;
            gbc.weightx = 1;
            fields[i] = new JTextField(i < values.length ? text(values[i]) : "");
            fields[i].setPreferredSize(new Dimension(178, 34));
            styleCompactField(fields[i]);
            form.add(fields[i], gbc);
        }
        return form;
    }

    private JScrollPane createLineTable() {
        String[] columns = localized(definition.getTableColumnKeys());
        tableModel = new DefaultTableModel(new String[0][0], columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
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
        sorter = new TableRowSorter<DefaultTableModel>(tableModel);
        table.setRowSorter(sorter);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(1, 230));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(229, 235, 244)));
        return scrollPane;
    }

    private JPanel createActionFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);
        footer.add(createActionButton("function.mode.reference"));
        footer.add(createActionButton("function.mode.cancel"));
        footer.add(createActionButton("function.mode.correct"));
        footer.add(createPrimaryActionButton("function.mode.register"));
        return footer;
    }

    private JPanel createContextPanel() {
        RoundedPanel side = new RoundedPanel(Color.WHITE, 8);
        side.setPreferredSize(new Dimension(260, 1));
        side.setLayout(new BorderLayout(0, 14));
        side.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 235, 244)),
                AppTheme.emptyBorder(16, 16, 16, 16)
        ));

        JLabel title = new JLabel(t("form.side.title"));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setFont(AppTheme.font(Font.BOLD, 14));
        side.add(title, BorderLayout.NORTH);

        JTextArea body = new JTextArea(functionContext());
        body.setEditable(false);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setOpaque(false);
        body.setForeground(AppTheme.TEXT_MUTED);
        body.setFont(AppTheme.font(Font.PLAIN, 12));
        side.add(body, BorderLayout.CENTER);
        return side;
    }

    private JButton createStripButton(String textKey, final String event) {
        JButton button = new JButton(t(textKey));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("FlatLaf.style", "arc: 8; borderWidth: 0; focusWidth: 0");
        button.setBackground(AppTheme.TEAL);
        button.setForeground(Color.WHITE);
        button.setBorder(AppTheme.emptyBorder(7, 14, 7, 14));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                filterRows();
                logAction(event, "keyword=" + searchField.getText());
                AppMessages.info(owner, t("message.refresh.done"));
            }
        });
        return button;
    }

    private JButton createActionButton(final String actionKey) {
        JButton button = new JButton(t(actionKey));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("FlatLaf.style", "arc: 8; borderWidth: 1; focusWidth: 0");
        button.setBackground(Color.WHITE);
        button.setForeground(AppTheme.TEXT_PRIMARY);
        button.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(213, 222, 235)),
                AppTheme.emptyBorder(8, 15, 8, 15)
        ));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runAction(actionKey);
            }
        });
        return button;
    }

    private JButton createPrimaryActionButton(String actionKey) {
        JButton button = createActionButton(actionKey);
        button.setBackground(AppTheme.ACCENT);
        button.setForeground(Color.WHITE);
        return button;
    }

    private void runAction(String actionKey) {
        selectMode(actionKey);
        if (MODE_REGISTER.equals(actionKey)) {
            createRecord();
        } else if (MODE_CORRECT.equals(actionKey)) {
            updateRecord("status.released", t("message.edit.success"));
        } else if (MODE_CANCEL.equals(actionKey)) {
            updateRecord("status.cancelled", t("message.operation.success"));
        } else {
            loadSelectedRowIntoForm();
            AppMessages.info(owner, t("message.refresh.done"));
        }
        logAction("GENERIC_FUNCTION_ACTION", "function=" + function.getCode() + " | action=" + actionKey);
    }

    private void createRecord() {
        try {
            repository.createRecord(function.getCode(), newDisplayValues("status.open"));
            reload();
            AppMessages.success(owner, t("message.create.success"));
        } catch (SQLException e) {
            AppLogger.error("Function record create failed.", e);
            AppMessages.error(owner, t("message.error.title"), t("message.save.failed"));
        }
    }

    private void updateRecord(String status, String successMessage) {
        int row = selectedModelRow();
        if (row < 0) {
            AppMessages.error(owner, t("message.error.title"), t("message.select.row"));
            return;
        }
        try {
            FunctionRecord record = records.get(row);
            repository.updateRecord(record.getId(), newDisplayValues(status));
            reload();
            AppMessages.success(owner, successMessage);
        } catch (SQLException e) {
            AppLogger.error("Function record update failed.", e);
            AppMessages.error(owner, t("message.error.title"), t("message.save.failed"));
        }
    }

    private void reload() {
        try {
            records.clear();
            records.addAll(repository.loadRecords(function.getCode(), definition.getTableRows()));
            rebuildTable();
        } catch (SQLException e) {
            AppLogger.error("Function record load failed.", e);
            records.clear();
            String[][] seedRows = definition.getTableRows();
            for (int i = 0; i < seedRows.length; i++) {
                records.add(new FunctionRecord(-1, function.getCode(), seedRows[i]));
            }
            rebuildTable();
            AppMessages.error(owner, t("message.error.title"), t("message.refresh.failed"));
        }
    }

    private void rebuildTable() {
        String[][] rows = new String[records.size()][];
        for (int i = 0; i < records.size(); i++) {
            rows[i] = localized(records.get(i).getValues());
        }
        tableModel.setDataVector(rows, localized(definition.getTableColumnKeys()));
    }

    private void filterRows() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        if (keyword.length() == 0) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(keyword)));
        }
    }

    private void applyMode(String modeKey) {
        boolean editable = !MODE_REFERENCE.equals(modeKey);
        for (int i = 0; i < fields.length; i++) {
            fields[i].setEditable(editable);
        }
        loadSelectedRowIntoForm();
        logAction("GENERIC_FUNCTION_MODE", "function=" + function.getCode() + " | mode=" + modeKey);
    }

    private void selectMode(String modeKey) {
        String[] modeKeys = {MODE_REGISTER, MODE_CORRECT, MODE_CANCEL, MODE_REFERENCE};
        for (int i = 0; i < modeKeys.length; i++) {
            modeButtons[i].setSelected(modeKeys[i].equals(modeKey));
        }
        applyMode(modeKey);
    }

    private void loadSelectedRowIntoForm() {
        int row = selectedModelRow();
        if (row < 0) {
            return;
        }
        if (fields.length > 0) {
            fields[0].setText(defaultDocumentNumber());
        }
        if (fields.length > 2 && tableModel.getColumnCount() > 3) {
            fields[2].setText(value(row, 3));
        }
        if (fields.length > 4 && tableModel.getColumnCount() > 1) {
            fields[4].setText(value(row, 1));
        }
        if (fields.length > 5 && tableModel.getColumnCount() > 2) {
            fields[5].setText(value(row, 2));
        }
        if (fields.length > 8 && tableModel.getColumnCount() > 4) {
            fields[8].setText(text(function.getNameKey()) + " / " + value(row, 4));
        }
    }

    private String[] newDisplayValues(String status) {
        String[] columns = definition.getTableColumnKeys();
        String[] values = new String[Math.min(8, columns.length)];
        for (int i = 0; i < values.length; i++) {
            values[i] = valueForColumn(columns[i], i, status);
        }
        return values;
    }

    private String valueForColumn(String columnKey, int index, String status) {
        if ("function.table.line".equals(columnKey)) {
            return String.valueOf(records.size() + 1);
        }
        if ("column.id".equals(columnKey)) {
            return valueAt(0);
        }
        if ("column.date".equals(columnKey) || "column.due".equals(columnKey)) {
            return valueAt(1);
        }
        if ("column.status".equals(columnKey)) {
            return status;
        }
        if ("column.customer".equals(columnKey) || "column.supplier".equals(columnKey) || "column.name".equals(columnKey)) {
            return valueAt(3);
        }
        if ("column.item".equals(columnKey) || "column.type".equals(columnKey)) {
            return valueAt(4);
        }
        if ("column.qty".equals(columnKey) || "column.amount".equals(columnKey)) {
            return valueAt(5);
        }
        if ("column.warehouse".equals(columnKey) || "column.plant".equals(columnKey)) {
            return valueAt(6);
        }
        if ("column.owner".equals(columnKey)) {
            return valueAt(7);
        }
        if ("column.risk".equals(columnKey)) {
            return "risk.low";
        }
        if ("column.next".equals(columnKey)) {
            return nextStepKey();
        }
        return index < definition.getDefaultValues().length ? definition.getDefaultValues()[index] : "";
    }

    private int selectedModelRow() {
        if (table == null || table.getSelectedRow() < 0) {
            return -1;
        }
        return table.convertRowIndexToModel(table.getSelectedRow());
    }

    private String value(int row, int column) {
        Object value = tableModel.getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    private String functionContext() {
        return t("function.context.flow") + "\n"
                + definition.getFlow() + "\n\n"
                + t("function.context.upstream") + "\n"
                + definition.getUpstream() + "\n\n"
                + t("function.context.downstream") + "\n"
                + definition.getDownstream() + "\n\n"
                + t("form.side.audit");
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

    private String valueAt(int index) {
        if (index >= fields.length) {
            return "";
        }
        return fields[index].getText();
    }

    private String[] localized(String[] values) {
        String[] localized = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            localized[i] = text(values[i]);
        }
        return localized;
    }

    private String[][] localizedRows(String[][] rows) {
        String[][] localized = new String[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            localized[i] = localized(rows[i]);
        }
        return localized;
    }

    private JLabel smallLabel(String value) {
        JLabel label = new JLabel(value);
        label.setForeground(AppTheme.TEXT_MUTED);
        label.setFont(AppTheme.font(Font.BOLD, 11));
        return label;
    }

    private void styleCompactField(JTextField field) {
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

    private String text(String value) {
        return I18n.textOrValue(session.getLanguage(), value);
    }

    private void logAction(String event, String details) {
        AppLogger.userAction(event, "user=" + session.getUsername()
                + " | module=" + function.getModuleCode()
                + (details == null || details.trim().length() == 0 ? "" : " | " + details));
    }
}
