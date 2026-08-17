package com.lin.erp.ui;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public final class ActionIcon implements Icon {
    private final String actionKey;
    private final Color color;
    private final int size;

    public ActionIcon(String actionKey, Color color) {
        this.actionKey = actionKey;
        this.color = color;
        this.size = 16;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(x, y);
        g.setColor(color);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        if ("action.new".equals(actionKey)) {
            g.drawLine(8, 3, 8, 13);
            g.drawLine(3, 8, 13, 8);
        } else if ("action.edit".equals(actionKey)) {
            g.drawLine(4, 12, 12, 4);
            g.drawLine(10, 3, 13, 6);
            g.drawLine(3, 13, 6, 12);
        } else if ("action.export".equals(actionKey)) {
            g.drawRect(3, 9, 10, 4);
            g.drawLine(8, 3, 8, 10);
            g.drawLine(5, 6, 8, 3);
            g.drawLine(11, 6, 8, 3);
        } else if ("action.approve".equals(actionKey)) {
            g.drawLine(3, 8, 6, 12);
            g.drawLine(6, 12, 13, 4);
        } else if ("action.release".equals(actionKey)) {
            g.drawLine(3, 12, 13, 4);
            g.drawLine(9, 4, 13, 4);
            g.drawLine(13, 4, 13, 8);
        } else if ("action.post".equals(actionKey)) {
            g.drawRect(3, 4, 10, 8);
            g.drawLine(5, 7, 11, 7);
            g.drawLine(5, 10, 9, 10);
        } else if ("action.delete".equals(actionKey)) {
            g.drawLine(4, 5, 12, 5);
            g.drawLine(6, 5, 6, 13);
            g.drawLine(10, 5, 10, 13);
            g.drawLine(5, 13, 11, 13);
            g.drawLine(7, 3, 9, 3);
        } else if ("action.refresh".equals(actionKey)) {
            g.drawArc(3, 3, 10, 10, 35, 270);
            g.drawLine(12, 3, 13, 7);
            g.drawLine(12, 3, 9, 5);
        } else if ("action.simulate".equals(actionKey)) {
            g.drawOval(3, 3, 10, 10);
            g.drawLine(8, 3, 8, 13);
            g.drawLine(3, 8, 13, 8);
        } else {
            g.drawOval(3, 3, 10, 10);
            g.drawLine(8, 5, 8, 9);
            g.drawLine(8, 12, 8, 12);
        }

        g.dispose();
    }
}
