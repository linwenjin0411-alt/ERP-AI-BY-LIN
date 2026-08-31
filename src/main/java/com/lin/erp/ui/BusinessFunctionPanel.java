package com.lin.erp.ui;

import com.lin.erp.auth.UserSession;
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
import java.text.SimpleDateFormat;
import java.util.Date;

public class BusinessFunctionPanel extends JPanel {
    private static final String MODE_REGISTER = "function.mode.register";
    private static final String MODE_CORRECT = "function.mode.correct";
    private static final String MODE_CANCEL = "function.mode.cancel";
    private static final String MODE_REFERENCE = "function.mode.reference";

    private final Window owner;
    private final UserSession session;
    private final MenuNode function;
    private final JTextField[] fields = new JTextField[9];
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
        setOpaque(false);
        add(createOperationStrip(), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);
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
        String[] labelKeys = {
                "function.field.documentNo", "function.field.businessDate", "function.field.status",
                "function.field.partner", "function.field.item", "function.field.quantity",
                "function.field.warehouse", "function.field.owner", "function.field.memo"
        };
        String[] values = {
                defaultDocumentNumber(), "2026/08/17", text("status.open"),
                defaultPartner(), defaultItem(), defaultQuantity(),
                "JP01", text(session.getDisplayNameKey()), text(function.getNameKey())
        };

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
            fields[i] = new JTextField(values[i]);
            fields[i].setPreferredSize(new Dimension(178, 34));
            styleCompactField(fields[i]);
            form.add(fields[i], gbc);
        }
        return form;
    }

    private JScrollPane createLineTable() {
        String[] columns = {
                t("function.table.line"),
                t("column.item"),
                t("column.qty"),
                t("column.status"),
                t("column.next")
        };
        tableModel = new DefaultTableModel(sampleRows(), columns) {
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
            tableModel.addRow(new String[]{
                    String.valueOf(tableModel.getRowCount() + 1),
                    fields[4].getText(),
                    fields[5].getText(),
                    text("status.open"),
                    text(nextStepKey())
            });
            AppMessages.success(owner, t("message.create.success"));
        } else if (MODE_CORRECT.equals(actionKey)) {
            int row = selectedModelRow();
            if (row < 0) {
                AppMessages.error(owner, t("message.error.title"), t("message.select.row"));
                return;
            }
            tableModel.setValueAt(fields[4].getText(), row, 1);
            tableModel.setValueAt(fields[5].getText(), row, 2);
            tableModel.setValueAt(text("status.released"), row, 3);
            AppMessages.success(owner, t("message.edit.success"));
        } else if (MODE_CANCEL.equals(actionKey)) {
            int row = selectedModelRow();
            if (row < 0) {
                AppMessages.error(owner, t("message.error.title"), t("message.select.row"));
                return;
            }
            tableModel.setValueAt(text("status.cancelled"), row, 3);
            AppMessages.success(owner, t("message.operation.success"));
        } else {
            loadSelectedRowIntoForm();
            AppMessages.info(owner, t("message.refresh.done"));
        }
        logAction("GENERIC_FUNCTION_ACTION", "function=" + function.getCode() + " | action=" + actionKey);
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
        fields[0].setText(defaultDocumentNumber());
        fields[2].setText(value(row, 3));
        fields[4].setText(value(row, 1));
        fields[5].setText(value(row, 2));
        fields[8].setText(text(function.getNameKey()) + " / " + value(row, 4));
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

    private String[][] sampleRows() {
        return new String[][]{
                {"1", defaultItem(), defaultQuantity(), text("status.open"), text(nextStepKey())},
                {"2", "RM-1008", "420", text("status.released"), text("term.stockOverview")},
                {"3", "PK-2210", "1,800", text("status.ready"), text("term.receipt")}
        };
    }

    private String functionContext() {
        return t("function.context.flow") + "\n"
                + moduleFlow() + "\n\n"
                + t("function.context.upstream") + "\n"
                + upstreamText() + "\n\n"
                + t("function.context.downstream") + "\n"
                + downstreamText() + "\n\n"
                + t("form.side.audit");
    }

    private String moduleFlow() {
        String module = function.getModuleCode();
        if ("SALES".equals(module)) {
            return "Customer / Item -> Sales Order -> Shipment -> Sales Confirmation -> AR -> Collection";
        }
        if ("PROCUREMENT".equals(module)) {
            return "Supplier / Item -> Purchase Order -> Receipt -> Purchase Confirmation -> AP -> Payment";
        }
        if ("MANUFACTURING".equals(module)) {
            return "Item / BOM -> Production Order -> Issue -> Completion -> Finished Goods Stock";
        }
        if ("INVENTORY".equals(module)) {
            return "Receipt / Completion / Shipment / Issue / Transfer / Count -> Stock Ledger";
        }
        if ("FINANCE".equals(module)) {
            return "Sales / Purchase Confirmation -> Receivable / Payable -> Collection / Payment";
        }
        if ("ADMIN".equals(module)) {
            return "User -> Role -> Menu Permission -> Operation Log";
        }
        return "Master Data -> Business Transaction -> Status / Audit";
    }

    private String upstreamText() {
        String code = function.getCode();
        if (code.indexOf("SHIPMENT") >= 0) {
            return "Sales order and available stock";
        }
        if (code.indexOf("RECEIPT") >= 0) {
            return "Purchase order and warehouse";
        }
        if (code.indexOf("ISSUE") >= 0) {
            return "Production order, BOM, and raw material stock";
        }
        if (code.indexOf("AR") >= 0) {
            return "Sales confirmation and billing";
        }
        if (code.indexOf("AP") >= 0) {
            return "Purchase confirmation and invoice verification";
        }
        return text(function.getNameKey());
    }

    private String downstreamText() {
        String code = function.getCode();
        if (code.indexOf("SHIPMENT") >= 0) {
            return "Sales confirmation and accounts receivable";
        }
        if (code.indexOf("RECEIPT") >= 0) {
            return "Inventory increase and purchase confirmation";
        }
        if (code.indexOf("ISSUE") >= 0) {
            return "Inventory decrease and production progress";
        }
        if (code.indexOf("TRANSFER") >= 0 || code.indexOf("COUNT") >= 0) {
            return "Stock ledger and current inventory";
        }
        if (code.indexOf("PAYMENT") >= 0) {
            return "Payable balance reduction";
        }
        if (code.indexOf("COLLECTION") >= 0) {
            return "Receivable balance reduction";
        }
        return "Next business page and operation audit";
    }

    private String defaultDocumentNumber() {
        String code = function.getCode();
        String prefix = code.length() <= 3 ? code : code.substring(0, 3);
        return prefix + "-" + new SimpleDateFormat("MMddHHmm").format(new Date());
    }

    private String defaultPartner() {
        if ("PROCUREMENT".equals(function.getModuleCode())) {
            return "SUP-2007";
        }
        if ("SALES".equals(function.getModuleCode()) || "FINANCE".equals(function.getModuleCode())) {
            return "CUS-3001";
        }
        return "LINOVA-001";
    }

    private String defaultItem() {
        if ("PROCUREMENT".equals(function.getModuleCode())) {
            return "RM-1008";
        }
        return "FG-3007";
    }

    private String defaultQuantity() {
        if ("PROCUREMENT".equals(function.getModuleCode())) {
            return "3,000";
        }
        return "120";
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
