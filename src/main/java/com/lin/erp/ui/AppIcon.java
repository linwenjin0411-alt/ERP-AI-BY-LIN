package com.lin.erp.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public final class AppIcon {
    private AppIcon() {
    }

    public static List<Image> images() {
        List<Image> images = new ArrayList<Image>();
        images.add(create(16));
        images.add(create(24));
        images.add(create(32));
        images.add(create(48));
        images.add(create(64));
        images.add(create(128));
        return images;
    }

    private static BufferedImage create(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int pad = Math.max(1, size / 10);
        int arc = Math.max(4, size / 4);
        RoundRectangle2D.Float shape = new RoundRectangle2D.Float(pad, pad, size - pad * 2, size - pad * 2, arc, arc);
        g.setPaint(new GradientPaint(0, 0, new Color(20, 112, 107), size, size, new Color(23, 148, 137)));
        g.fill(shape);

        g.setColor(new Color(255, 255, 255, 55));
        g.setStroke(new BasicStroke(Math.max(1f, size / 28f)));
        int left = pad + size / 5;
        int right = size - pad - size / 5;
        int top = pad + size / 4;
        int middle = size / 2;
        int bottom = size - pad - size / 4;
        g.drawLine(left, middle, middle, top);
        g.drawLine(middle, top, right, middle);
        g.drawLine(left, middle, middle, bottom);
        g.drawLine(middle, bottom, right, middle);

        g.setColor(Color.WHITE);
        int node = Math.max(3, size / 8);
        fillNode(g, left, middle, node);
        fillNode(g, middle, top, node);
        fillNode(g, right, middle, node);
        fillNode(g, middle, bottom, node);

        g.setColor(new Color(255, 255, 255, 235));
        int barWidth = Math.max(2, size / 11);
        int base = size - pad - size / 5;
        int x = pad + size / 4;
        g.fillRoundRect(x, base - size / 5, barWidth, size / 5, barWidth, barWidth);
        g.fillRoundRect(x + barWidth * 2, base - size / 3, barWidth, size / 3, barWidth, barWidth);
        g.fillRoundRect(x + barWidth * 4, base - size / 4, barWidth, size / 4, barWidth, barWidth);

        g.dispose();
        return image;
    }

    private static void fillNode(Graphics2D g, int cx, int cy, int size) {
        g.fillOval(cx - size / 2, cy - size / 2, size, size);
    }
}
