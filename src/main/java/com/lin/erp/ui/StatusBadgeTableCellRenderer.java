package com.lin.erp.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

final class StatusBadgeTableCellRenderer extends DefaultTableCellRenderer {
    private final String[][] semanticRows;

    StatusBadgeTableCellRenderer() {
        this(null);
    }

    StatusBadgeTableCellRenderer(String[][] semanticRows) {
        this.semanticRows = semanticRows;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String text = value == null ? "" : value.toString();
        String semantic = semanticValue(table, row, column);
        String styleValue = semantic.length() == 0 ? text : semantic;
        if (isStatus(styleValue) && !isSelected) {
            label.setOpaque(true);
            label.setHorizontalAlignment(CENTER);
            label.setFont(AppTheme.font(Font.BOLD, 11));
            label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            String lower = styleValue.toLowerCase();
            if (lower.contains("released") || lower.contains("posted") || lower.contains("ready")) {
                label.setBackground(new Color(229, 250, 243));
                label.setForeground(new Color(0, 112, 84));
            } else if (lower.contains("late") || lower.contains("blocked") || lower.contains("cancel")) {
                label.setBackground(new Color(254, 242, 242));
                label.setForeground(new Color(185, 28, 28));
            } else if (lower.contains("waiting") || lower.contains("draft")) {
                label.setBackground(new Color(255, 251, 235));
                label.setForeground(new Color(146, 64, 14));
            } else {
                label.setBackground(new Color(236, 253, 245));
                label.setForeground(new Color(15, 118, 110));
            }
        } else {
            label.setHorizontalAlignment(LEFT);
            label.setBorder(null);
            label.setFont(AppTheme.font(Font.PLAIN, 12));
            if (!isSelected) {
                label.setOpaque(true);
                label.setBackground(table.getBackground());
                label.setForeground(table.getForeground());
            }
        }
        return label;
    }

    private String semanticValue(JTable table, int row, int column) {
        if (semanticRows == null || row < 0 || column < 0) {
            return "";
        }
        int modelRow = table.convertRowIndexToModel(row);
        int modelColumn = table.convertColumnIndexToModel(column);
        if (modelRow < 0 || modelRow >= semanticRows.length) {
            return "";
        }
        String[] values = semanticRows[modelRow];
        if (values == null || modelColumn < 0 || modelColumn >= values.length || values[modelColumn] == null) {
            return "";
        }
        return values[modelColumn];
    }

    private boolean isStatus(String value) {
        String lower = value == null ? "" : value.toLowerCase();
        return lower.startsWith("status.") || "open".equals(lower) || "draft".equals(lower)
                || "ready".equals(lower) || "released".equals(lower) || "posted".equals(lower)
                || "closed".equals(lower) || "cancelled".equals(lower) || "late".equals(lower)
                || "blocked".equals(lower) || lower.contains("approval");
    }
}
