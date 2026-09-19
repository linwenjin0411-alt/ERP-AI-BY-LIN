package com.lin.erp.ui;

import com.lin.erp.auth.UserSession;
import com.lin.erp.config.DbConfig;
import com.lin.erp.db.DbItemMasterRepository;
import com.lin.erp.db.DbRoleMenuPermissionRepository;
import com.lin.erp.db.ItemMasterRecord;
import com.lin.erp.db.RoleMenuPermission;
import com.lin.erp.i18n.I18n;
import com.lin.erp.i18n.Language;
import com.lin.erp.logging.AppLogger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
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

public class ItemMasterPanel extends JPanel {
    private static final String[] COLUMN_KEYS = {
            "column.itemCode", "column.itemName", "column.itemType", "column.uom",
            "column.plant", "column.status", "column.safetyStock", "column.leadTime"
    };

    private final Window owner;
    private final UserSession session;
    private final DbItemMasterRepository repository;
    private final RoleMenuPermission permission;
    private final List<ItemMasterRecord> records = new ArrayList<ItemMasterRecord>();

    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> tableSorter;
    private JTextField filterField;
    private JComboBox<String> statusFilter;
    private JLabel countLabel;
    private JLabel totalItemsLabel;
    private JLabel releasedItemsLabel;
    private JLabel openItemsLabel;

    public ItemMasterPanel(Window owner, UserSession session, DbItemMasterRepository repository) {
        super(new BorderLayout(0, 16));
        this.owner = owner;
        this.session = session;
        this.repository = repository;
        this.permission = loadPermission();
        setOpaque(false);
        add(createToolbar(), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);
        reload();
        logAction("SUBPAGE_OPEN", "page=Item Master Management");
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(16, 0));
        toolbar.setOpaque(false);

        JPanel titleBlock = new JPanel(new GridLayout(0, 1, 0, 4));
        titleBlock.setOpaque(false);
        JLabel title = new JLabel(t("menu.master.item"));
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
        statusFilter = new JComboBox<String>(new String[]{"", "status.open", "status.released", "status.ready", "status.cancelled"});
        statusFilter.setPreferredSize(new Dimension(150, 34));
        statusFilter.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                                                                  boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String text = value == null ? "" : value.toString();
                label.setText(text.length() == 0 ? "All status" : I18n.textOrValue(session.getLanguage(), text));
                return label;
            }
        });
        statusFilter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyFilter();
            }
        });
        actions.add(statusFilter);
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
        boolean allowed = permission.allows(actionKey);
        button.setEnabled(allowed);
        if (!allowed) {
            button.setToolTipText(t("message.permission.denied"));
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

        tableModel = new DefaultTableModel(new String[0][0], localizedColumns()) {
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
                    logAction("ITEM_TABLE_DOUBLE_CLICK", "row=" + selectedRow());
                    if (ensureActionAllowed("action.edit")) {
                        openForm(true);
                    }
                }
            }
        });
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(true);
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
        side.setPreferredSize(new Dimension(260, 1));
        side.setLayout(new BorderLayout(0, 14));
        side.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 235, 244)),
                AppTheme.emptyBorder(16, 16, 16, 16)
        ));

        JLabel title = new JLabel(t("item.side.title"));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setFont(AppTheme.font(Font.BOLD, 14));
        side.add(title, BorderLayout.NORTH);

        JPanel list = new JPanel(new GridLayout(0, 1, 0, 10));
        list.setOpaque(false);
        totalItemsLabel = metricValueLabel();
        releasedItemsLabel = metricValueLabel();
        openItemsLabel = metricValueLabel();
        list.add(metricRow(t("item.side.total"), totalItemsLabel));
        list.add(metricRow(t("item.side.released"), releasedItemsLabel));
        list.add(metricRow(t("item.side.open"), openItemsLabel));
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

    private JLabel metricValueLabel() {
        JLabel label = new JLabel("0", SwingConstants.RIGHT);
        label.setForeground(AppTheme.ACCENT);
        label.setFont(AppTheme.font(Font.BOLD, 16));
        return label;
    }

    private void handleAction(String actionKey) {
        logAction("ITEM_MASTER_ACTION_CLICK", "action=" + actionKey + " | actionName=" + english(actionKey));
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
        } else if ("action.export".equals(actionKey)) {
            export();
        }
    }

    private boolean ensureActionAllowed(String actionKey) {
        if (permission.allows(actionKey)) {
            return true;
        }
        logAction("ITEM_ACTION_DENIED", "action=" + actionKey);
        AppMessages.error(this, t("message.error.title"), t("message.permission.denied"));
        return false;
    }

    private RoleMenuPermission loadPermission() {
        try {
            return new DbRoleMenuPermissionRepository(DbConfig.loadDefault()).loadForMenu(session.getRoleCode(), "MASTER_ITEM");
        } catch (SQLException e) {
            AppLogger.error("Item master permission load failed.", e);
            return RoleMenuPermission.none("MASTER_ITEM");
        }
    }

    private void openForm(boolean editMode) {
        if (!ensureActionAllowed(editMode ? "action.edit" : "action.new")) {
            return;
        }
        int row = selectedRow();
        ItemMasterRecord selected = null;
        if (editMode) {
            if (row < 0) {
                AppMessages.error(this, t("message.error.title"), t("message.select.row"));
                logAction("ITEM_FORM_OPEN_BLOCKED", "mode=EDIT | reason=noSelectedRow");
                return;
            }
            selected = records.get(row);
        }

        String[] initialValues = selected == null ? defaultValues() : selected.toValues();
        RecordFormDialog dialog = new RecordFormDialog(
                owner,
                session.getLanguage(),
                t("menu.master.item"),
                editMode ? t("action.edit") : t("action.new"),
                editMode ? "action.edit" : "action.new",
                COLUMN_KEYS,
                initialValues
        );
        logAction("ITEM_FORM_OPEN", "mode=" + (editMode ? "EDIT" : "CREATE")
                + " | itemCode=" + (selected == null ? "new" : selected.getItemCode()));
        dialog.setVisible(true);

        if (!dialog.isSaved()) {
            logAction("ITEM_FORM_CANCEL", "mode=" + (editMode ? "EDIT" : "CREATE"));
            return;
        }

        ItemMasterRecord record = ItemMasterRecord.fromValues(dialog.getValues());
        if (record.getItemCode().length() == 0 || record.getItemName().length() == 0) {
            AppMessages.error(this, t("message.error.title"), t("message.item.required"));
            logAction("ITEM_FORM_VALIDATION_FAILURE", "reason=missingCodeOrName");
            return;
        }

        final ItemMasterRecord selectedRecord = selected;
        final String[] savedItemCode = new String[]{record.getItemCode()};
        BackgroundTasks.run(
                this,
                "Item master save failed.",
                t("message.error.title"),
                t("message.save.failed"),
                new BackgroundTasks.Work<Void>() {
                    @Override
                    public Void run() throws Exception {
                        if (editMode) {
                            repository.updateItem(selectedRecord.getItemCode(), record);
                        } else {
                            repository.createItem(record);
                        }
                        return null;
                    }
                },
                new BackgroundTasks.Success<Void>() {
                    @Override
                    public void accept(Void value) {
                        AppMessages.success(ItemMasterPanel.this, editMode ? t("message.edit.success") : t("message.create.success"));
                        logAction("ITEM_SAVE_SUCCESS", "mode=" + (editMode ? "EDIT" : "CREATE")
                                + " | itemCode=" + record.getItemCode());
                        reload(savedItemCode[0]);
                    }
                }
        );
    }

    private void deleteSelected() {
        int row = selectedRow();
        if (row < 0) {
            AppMessages.error(this, t("message.error.title"), t("message.select.row"));
            logAction("ITEM_DELETE_BLOCKED", "reason=noSelectedRow");
            return;
        }

        ItemMasterRecord record = records.get(row);
        int result = JOptionPane.showConfirmDialog(
                this,
                t("message.delete.confirm") + " " + record.getItemCode() + " / " + record.getItemName() + " / " + I18n.textOrValue(session.getLanguage(), record.getStatus()) + " ?",
                t("dialog.confirm.title"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            logAction("ITEM_DELETE_CANCEL", "itemCode=" + record.getItemCode());
            return;
        }

        BackgroundTasks.run(
                this,
                "Item master delete failed.",
                t("message.error.title"),
                t("message.delete.failed"),
                new BackgroundTasks.Work<Void>() {
                    @Override
                    public Void run() throws Exception {
                        repository.deleteItem(record.getItemCode());
                        return null;
                    }
                },
                new BackgroundTasks.Success<Void>() {
                    @Override
                    public void accept(Void value) {
                        AppMessages.success(ItemMasterPanel.this, t("message.delete.success"));
                        logAction("ITEM_DELETE_SUCCESS", "itemCode=" + record.getItemCode());
                        reload();
                    }
                }
        );
    }

    private void reload() {
        reload(selectedItemCode());
    }

    private void reload(final String preferredItemCode) {
        BackgroundTasks.run(
                this,
                "Item master load failed.",
                t("message.error.title"),
                t("message.refresh.failed"),
                new BackgroundTasks.Work<List<ItemMasterRecord>>() {
                    @Override
                    public List<ItemMasterRecord> run() throws Exception {
                        return repository.loadItems();
                    }
                },
                new BackgroundTasks.Success<List<ItemMasterRecord>>() {
                    @Override
                    public void accept(List<ItemMasterRecord> loaded) {
                        records.clear();
                        records.addAll(loaded);
                        rebuildTable(preferredItemCode);
                        AppMessages.info(ItemMasterPanel.this, t("message.refresh.done"));
                        logAction("ITEM_PAGE_REFRESH_SUCCESS", "rows=" + records.size());
                    }
                }
        );
    }

    private void rebuildTable(String preferredItemCode) {
        tableModel.setDataVector(localizedRows(), localizedColumns());
        tableSorter.setModel(tableModel);
        TableColumnPreferences.install(table, "item-master." + session.getLanguage().name());
        applyFilter();
        countLabel.setText(t("item.count.prefix") + records.size());
        selectItem(preferredItemCode);
        if (totalItemsLabel != null) {
            totalItemsLabel.setText(String.valueOf(records.size()));
            releasedItemsLabel.setText(String.valueOf(countStatus("status.released")));
            openItemsLabel.setText(String.valueOf(countStatus("status.open")));
        }
    }

    private int countStatus(String status) {
        int count = 0;
        for (ItemMasterRecord record : records) {
            if (status.equals(record.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private void export() {
        File exportDir = new File("exports");
        if (!exportDir.isDirectory() && !exportDir.mkdirs()) {
            AppMessages.error(this, t("message.error.title"), t("message.export.failed"));
            logAction("ITEM_EXPORT_FAILURE", "reason=cannotCreateExportDirectory");
            return;
        }

        File file = new File(exportDir, "item-master-" + safeFilterSuffix()
                + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".csv");
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            writer.write('\ufeff');
            writeCsvLine(writer, visibleColumnHeaders());
            for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
                String[] row = new String[table.getColumnCount()];
                for (int column = 0; column < table.getColumnCount(); column++) {
                    Object value = table.getValueAt(viewRow, column);
                    row[column] = value == null ? "" : value.toString();
                }
                writeCsvLine(writer, row);
            }
            AppMessages.success(this, t("message.export.success") + file.getAbsolutePath());
            logAction("ITEM_EXPORT_SUCCESS", "file=" + file.getAbsolutePath() + " | rows=" + records.size());
        } catch (Exception e) {
            AppLogger.error("Item master export failed.", e);
            AppMessages.error(this, t("message.error.title"), t("message.export.failed"));
            logAction("ITEM_EXPORT_FAILURE", "errorType=" + e.getClass().getSimpleName());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignored) {
                    // Export already finished or failed; close errors are not actionable for the user.
                }
            }
        }
    }

    private int selectedRow() {
        if (table == null || table.getSelectedRow() < 0) {
            return -1;
        }
        return table.convertRowIndexToModel(table.getSelectedRow());
    }

    private void applyFilter() {
        if (tableSorter == null || filterField == null) {
            return;
        }
        String filter = filterField.getText();
        String status = statusFilter == null || statusFilter.getSelectedItem() == null ? "" : statusFilter.getSelectedItem().toString();
        List<RowFilter<Object, Object>> filters = new ArrayList<RowFilter<Object, Object>>();
        if (filter != null && filter.trim().length() > 0) {
            filters.add(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(filter.trim())));
        }
        if (status.length() > 0) {
            filters.add(RowFilter.regexFilter("(?i)^" + java.util.regex.Pattern.quote(I18n.textOrValue(session.getLanguage(), status)) + "$"));
        }
        if (filters.isEmpty()) {
            tableSorter.setRowFilter(null);
        } else {
            tableSorter.setRowFilter(RowFilter.andFilter(filters));
        }
    }

    private String selectedItemCode() {
        int row = selectedRow();
        return row < 0 || row >= records.size() ? null : records.get(row).getItemCode();
    }

    private void selectItem(String preferredItemCode) {
        if (records.isEmpty()) {
            return;
        }
        int modelRow = 0;
        if (preferredItemCode != null && preferredItemCode.trim().length() > 0) {
            for (int i = 0; i < records.size(); i++) {
                if (preferredItemCode.equals(records.get(i).getItemCode())) {
                    modelRow = i;
                    break;
                }
            }
        }
        int viewRow = table.convertRowIndexToView(modelRow);
        if (viewRow >= 0 && viewRow < table.getRowCount()) {
            table.setRowSelectionInterval(viewRow, viewRow);
        } else if (table.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        }
    }

    private String safeFilterSuffix() {
        String filter = filterField == null ? "" : filterField.getText();
        String status = statusFilter == null || statusFilter.getSelectedItem() == null ? "" : statusFilter.getSelectedItem().toString();
        String raw = (filter == null ? "" : filter.trim()) + (status.length() == 0 ? "" : "-" + status.replace("status.", ""));
        if (raw.trim().length() == 0) {
            return "";
        }
        return raw.replaceAll("[^A-Za-z0-9_-]+", "_") + "-";
    }

    private String[] defaultValues() {
        return new String[]{
                "ITM-" + new SimpleDateFormat("MMddHHmm").format(new Date()),
                "",
                "Purchased material",
                "EA",
                "JP01",
                "status.open",
                "0",
                "7"
        };
    }

    private String[] localizedColumns() {
        String[] columns = new String[COLUMN_KEYS.length];
        for (int i = 0; i < COLUMN_KEYS.length; i++) {
            columns[i] = t(COLUMN_KEYS[i]);
        }
        return columns;
    }

    private String[][] localizedRows() {
        String[][] rows = new String[records.size()][COLUMN_KEYS.length];
        for (int i = 0; i < records.size(); i++) {
            String[] values = records.get(i).toValues();
            for (int j = 0; j < values.length; j++) {
                rows[i][j] = I18n.textOrValue(session.getLanguage(), values[j]);
            }
        }
        return rows;
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

    private String[] visibleColumnHeaders() {
        String[] headers = new String[table.getColumnCount()];
        for (int i = 0; i < table.getColumnCount(); i++) {
            headers[i] = table.getColumnName(i);
        }
        return headers;
    }

    private String t(String key) {
        return I18n.t(session.getLanguage(), key);
    }

    private String english(String key) {
        return I18n.textOrValue(Language.EN, key);
    }

    private void logAction(String event, String details) {
        AppLogger.userAction(event, "user=" + session.getUsername()
                + " | module=MASTER | submenu=MASTER_ITEM"
                + (details == null || details.trim().length() == 0 ? "" : " | " + details));
    }
}
