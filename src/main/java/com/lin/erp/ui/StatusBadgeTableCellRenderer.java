package com.lin.erp.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

final class StatusBadgeTableCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String text = value == null ? "" : value.toString();
        if (isStatus(text) && !isSelected) {
            label.setOpaque(true);
            label.setHorizontalAlignment(CENTER);
            label.setFont(AppTheme.font(Font.BOLD, 11));
            label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            if (text.toLowerCase().contains("released") || text.toLowerCase().contains("posted")
                    || text.toLowerCase().contains("ready")) {
                label.setBackground(new Color(229, 250, 243));
                label.setForeground(new Color(0, 112, 84));
            } else if (text.toLowerCase().contains("late") || text.toLowerCase().contains("blocked")
                    || text.toLowerCase().contains("cancel")) {
                label.setBackground(new Color(254, 242, 242));
                label.setForeground(new Color(185, 28, 28));
            } else if (text.toLowerCase().contains("waiting") || text.toLowerCase().contains("draft")) {
                label.setBackground(new Color(255, 251, 235));
                label.setForeground(new Color(146, 64, 14));
            } else {
                label.setBackground(new Color(236, 253, 245));
                label.setForeground(new Color(15, 118, 110));
            }
        } else {
            label.setHorizontalAlignment(LEFT);
            label.setBorder(null);
        }
        return label;
    }

    private boolean isStatus(String value) {
        String lower = value == null ? "" : value.toLowerCase();
        return lower.startsWith("status.") || "open".equals(lower) || "draft".equals(lower)
                || "ready".equals(lower) || "released".equals(lower) || "posted".equals(lower)
                || "closed".equals(lower) || "cancelled".equals(lower) || "late".equals(lower)
                || "blocked".equals(lower) || lower.contains("approval");
    }
}
