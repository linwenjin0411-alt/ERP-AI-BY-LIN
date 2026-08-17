package com.lin.erp.ui;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.CompoundBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public final class AppMessages {
    private static final int AUTO_CLOSE_DELAY_MS = 3000;

    private AppMessages() {
    }

    public static void success(Component parent, String message) {
        showToast(parent, message, AppTheme.SUCCESS, new Color(237, 250, 245));
    }

    public static void info(Component parent, String message) {
        showToast(parent, message, AppTheme.ACCENT, AppTheme.ACCENT_SOFT);
    }

    public static void error(Component parent, String title, String message) {
        JOptionPane pane = new JOptionPane(message, JOptionPane.ERROR_MESSAGE, JOptionPane.DEFAULT_OPTION);
        JDialog dialog = pane.createDialog(resolveWindow(parent), title);
        dialog.setIconImages(AppIcon.images());
        dialog.setModal(true);
        dialog.setVisible(true);
        dialog.dispose();
    }

    private static void showToast(Component parent, String message, Color accent, Color background) {
        final Window owner = resolveWindow(parent);
        final JWindow toast = owner == null ? new JWindow() : new JWindow(owner);
        toast.setAlwaysOnTop(true);
        toast.setFocusableWindowState(false);
        toast.setBackground(new Color(0, 0, 0, 0));

        JPanel panel = new RoundedPanel(background, 8);
        panel.setLayout(new BorderLayout(12, 0));
        panel.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 90)),
                AppTheme.emptyBorder(12, 14, 12, 14)
        ));

        JLabel mark = new JLabel("", SwingConstants.CENTER);
        mark.setOpaque(true);
        mark.setBackground(accent);
        mark.setPreferredSize(new Dimension(8, 28));
        panel.add(mark, BorderLayout.WEST);

        JLabel label = new JLabel("<html>" + escape(message) + "</html>");
        label.setForeground(AppTheme.TEXT_PRIMARY);
        label.setFont(AppTheme.font(Font.BOLD, 13));
        panel.add(label, BorderLayout.CENTER);

        toast.setContentPane(panel);
        toast.pack();
        toast.setLocation(calculateLocation(owner, toast));
        toast.setVisible(true);

        Timer timer = new Timer(AUTO_CLOSE_DELAY_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toast.setVisible(false);
                toast.dispose();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private static Point calculateLocation(Window owner, JWindow toast) {
        if (owner != null && owner.isShowing()) {
            Point ownerLocation = owner.getLocationOnScreen();
            int x = ownerLocation.x + owner.getWidth() - toast.getWidth() - 28;
            int y = ownerLocation.y + 46;
            return new Point(Math.max(ownerLocation.x + 16, x), y);
        }
        Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        return new Point(screen.width - toast.getWidth() - 32, 48);
    }

    private static Window resolveWindow(Component parent) {
        if (parent == null) {
            return null;
        }
        if (parent instanceof Window) {
            return (Window) parent;
        }
        return SwingUtilities.getWindowAncestor(parent);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
