package com.lin.erp.ui;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

final class TableColumnPreferences {
    private static final Preferences PREFS = Preferences.userNodeForPackage(TableColumnPreferences.class);

    private TableColumnPreferences() {
    }

    static void install(final JTable table, final String scope) {
        if (table == null || scope == null) {
            return;
        }
        table.getTableHeader().setReorderingAllowed(true);
        final Map<String, TableColumn> allColumns = columnsByName(table);
        table.putClientProperty("columnPreferences.allColumns", allColumns);
        applyHiddenColumns(table, scope, allColumns);
        applyColumnWidths(table, scope);
        applyColumnOrder(table, scope);
        if (Boolean.TRUE.equals(table.getClientProperty("columnPreferences.installed"))) {
            return;
        }
        table.putClientProperty("columnPreferences.installed", Boolean.TRUE);
        table.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
            @Override
            public void columnAdded(TableColumnModelEvent e) {
                save(table, scope);
            }

            @Override
            public void columnRemoved(TableColumnModelEvent e) {
                save(table, scope);
            }

            @Override
            public void columnMoved(TableColumnModelEvent e) {
                save(table, scope);
            }

            @Override
            public void columnMarginChanged(javax.swing.event.ChangeEvent e) {
                save(table, scope);
            }

            @Override
            public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {
            }
        });
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShow(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShow(e);
            }

            private void maybeShow(MouseEvent e) {
                if (!e.isPopupTrigger()) {
                    return;
                }
                createPopup(table, scope, allColumns(table)).show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, TableColumn> allColumns(JTable table) {
        Object value = table.getClientProperty("columnPreferences.allColumns");
        if (value instanceof Map) {
            return (Map<String, TableColumn>) value;
        }
        Map<String, TableColumn> columns = columnsByName(table);
        table.putClientProperty("columnPreferences.allColumns", columns);
        return columns;
    }

    private static Map<String, TableColumn> columnsByName(JTable table) {
        Map<String, TableColumn> columns = new LinkedHashMap<String, TableColumn>();
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            String name = table.getColumnName(i);
            column.setIdentifier(name);
            columns.put(name, column);
        }
        return columns;
    }

    private static JPopupMenu createPopup(final JTable table, final String scope, final Map<String, TableColumn> allColumns) {
        JPopupMenu menu = new JPopupMenu();
        for (final Map.Entry<String, TableColumn> entry : allColumns.entrySet()) {
            final String name = entry.getKey();
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(name, isVisible(table, name));
            item.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (isVisible(table, name)) {
                        if (table.getColumnModel().getColumnCount() > 1) {
                            table.removeColumn(entry.getValue());
                        }
                    } else {
                        table.addColumn(entry.getValue());
                        applyColumnOrder(table, scope);
                    }
                    save(table, scope);
                }
            });
            menu.add(item);
        }
        menu.addSeparator();
        JMenuItem restore = new JMenuItem("Reset columns");
        restore.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                PREFS.remove(scope + ".hidden");
                PREFS.remove(scope + ".order");
                for (String name : allColumns.keySet()) {
                    PREFS.remove(scope + ".width." + name);
                    if (!isVisible(table, name)) {
                        table.addColumn(allColumns.get(name));
                    }
                }
                applyColumnOrder(table, scope, new ArrayList<String>(allColumns.keySet()));
                save(table, scope);
            }
        });
        menu.add(restore);
        return menu;
    }

    private static void applyHiddenColumns(JTable table, String scope, Map<String, TableColumn> allColumns) {
        String hidden = PREFS.get(scope + ".hidden", "");
        for (String name : hidden.split("\\|")) {
            if (name.length() > 0 && isVisible(table, name)) {
                table.removeColumn(allColumns.get(name));
            }
        }
    }

    private static void applyColumnWidths(JTable table, String scope) {
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            int width = PREFS.getInt(scope + ".width." + column.getIdentifier(), -1);
            if (width > 24) {
                column.setPreferredWidth(width);
            }
        }
    }

    private static void applyColumnOrder(JTable table, String scope) {
        String order = PREFS.get(scope + ".order", "");
        if (order.length() == 0) {
            return;
        }
        List<String> names = new ArrayList<String>();
        for (String name : order.split("\\|")) {
            if (name.length() > 0) {
                names.add(name);
            }
        }
        applyColumnOrder(table, scope, names);
    }

    private static void applyColumnOrder(JTable table, String scope, List<String> names) {
        for (int target = 0; target < names.size(); target++) {
            int current = visibleIndex(table, names.get(target));
            if (current >= 0 && current != target && target < table.getColumnModel().getColumnCount()) {
                table.moveColumn(current, target);
            }
        }
    }

    private static void save(final JTable table, final String scope) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                StringBuilder order = new StringBuilder();
                StringBuilder hidden = new StringBuilder();
                Map<String, TableColumn> visible = columnsByName(table);
                for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
                    TableColumn column = table.getColumnModel().getColumn(i);
                    if (order.length() > 0) {
                        order.append('|');
                    }
                    order.append(column.getIdentifier());
                    PREFS.putInt(scope + ".width." + column.getIdentifier(), column.getWidth());
                }
                PREFS.put(scope + ".order", order.toString());
                for (int i = 0; i < table.getModel().getColumnCount(); i++) {
                    String name = table.getModel().getColumnName(i);
                    if (!visible.containsKey(name)) {
                        if (hidden.length() > 0) {
                            hidden.append('|');
                        }
                        hidden.append(name);
                    }
                }
                PREFS.put(scope + ".hidden", hidden.toString());
            }
        });
    }

    private static boolean isVisible(JTable table, String name) {
        return visibleIndex(table, name) >= 0;
    }

    private static int visibleIndex(JTable table, String name) {
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            Object identifier = table.getColumnModel().getColumn(i).getIdentifier();
            if (name.equals(identifier)) {
                return i;
            }
        }
        return -1;
    }
}
