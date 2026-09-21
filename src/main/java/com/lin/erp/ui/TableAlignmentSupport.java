package com.lin.erp.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Font;

final class TableAlignmentSupport {
    private TableAlignmentSupport() {
    }

    static void apply(JTable table) {
        apply(table, new PaddedTableCellRenderer());
    }

    static void apply(JTable table, TableCellRenderer renderer) {
        if (table == null) {
            return;
        }
        if (renderer != null) {
            table.setDefaultRenderer(Object.class, renderer);
        }
        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setDefaultRenderer(new PaddedHeaderRenderer(header.getDefaultRenderer()));
        }
    }

    static void styleCell(JLabel label) {
        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 8));
    }

    private static final class PaddedTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            styleCell(label);
            label.setFont(AppTheme.font(Font.PLAIN, 12));
            return label;
        }
    }

    private static final class PaddedHeaderRenderer implements TableCellRenderer {
        private final TableCellRenderer delegate;

        private PaddedHeaderRenderer(TableCellRenderer delegate) {
            this.delegate = delegate;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component component = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (component instanceof JLabel) {
                JLabel label = (JLabel) component;
                styleCell(label);
                label.setFont(AppTheme.font(Font.BOLD, 12));
            }
            return component;
        }
    }
}
