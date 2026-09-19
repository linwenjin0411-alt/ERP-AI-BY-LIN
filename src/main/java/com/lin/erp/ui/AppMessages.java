package com.lin.erp.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.CompoundBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
        showDialog(parent, title, message, null, null, AppTheme.ERROR, new Color(254, 242, 242));
    }

    public static void information(Component parent, String title, String message, String okText) {
        showDialog(parent, title, message, okText, null, AppTheme.ACCENT, AppTheme.ACCENT_SOFT);
    }

    public static boolean confirm(Component parent, String title, String message, String okText, String cancelText) {
        final boolean[] result = new boolean[1];
        showDialog(parent, title, message, okText, cancelText, AppTheme.WARNING, new Color(255, 251, 235), result);
        return result[0];
    }

    public static String input(Component parent, String title, String message, String okText, String cancelText) {
        final String[] value = new String[1];
        final Window owner = resolveWindow(parent);
        final JDialog dialog = new JDialog(owner, title, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setIconImages(AppIcon.images());

        JPanel root = dialogPanel(AppTheme.ACCENT, AppTheme.ACCENT_SOFT);
        root.add(messageLabel(message), BorderLayout.NORTH);
        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(420, 34));
        field.putClientProperty("JComponent.roundRect", Boolean.TRUE);
        root.add(field, BorderLayout.CENTER);

        JPanel actions = actionPanel();
        JButton cancel = dialogButton(cancelText, false, AppTheme.ACCENT);
        JButton ok = dialogButton(okText, true, AppTheme.ACCENT);
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                value[0] = field.getText();
                dialog.dispose();
            }
        });
        actions.add(cancel);
        actions.add(ok);
        root.add(actions, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.getRootPane().setDefaultButton(ok);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return value[0];
    }

    private static void showDialog(Component parent, String title, String message, String okText, String cancelText,
            Color accent, Color background) {
        showDialog(parent, title, message, okText, cancelText, accent, background, null);
    }

    private static void showDialog(Component parent, String title, String message, String okText, String cancelText,
            Color accent, Color background, final boolean[] result) {
        final Window owner = resolveWindow(parent);
        final JDialog dialog = new JDialog(owner, title, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setIconImages(AppIcon.images());

        JPanel root = dialogPanel(accent, background);
        root.add(messageLabel(message), BorderLayout.CENTER);
        JPanel actions = actionPanel();
        boolean hasCancel = cancelText != null && cancelText.trim().length() > 0;
        JButton ok = dialogButton(okText == null || okText.trim().length() == 0 ? "OK" : okText, true, accent);
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (result != null) {
                    result[0] = true;
                }
                dialog.dispose();
            }
        });
        if (hasCancel) {
            JButton cancel = dialogButton(cancelText, false, accent);
            cancel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                }
            });
            actions.add(cancel);
        }
        actions.add(ok);
        root.add(actions, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.getRootPane().setDefaultButton(ok);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
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

    private static JPanel dialogPanel(Color accent, Color background) {
        JPanel root = new RoundedPanel(background, 8);
        root.setLayout(new BorderLayout(12, 14));
        root.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 90)),
                AppTheme.emptyBorder(18, 18, 18, 18)
        ));
        return root;
    }

    private static JLabel messageLabel(String message) {
        JLabel label = new JLabel("<html>" + escape(message).replace("\n", "<br>") + "</html>");
        label.setForeground(AppTheme.TEXT_PRIMARY);
        label.setFont(AppTheme.font(Font.PLAIN, 13));
        label.setPreferredSize(new Dimension(420, Math.max(42, label.getPreferredSize().height)));
        return label;
    }

    private static JPanel actionPanel() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        return actions;
    }

    private static JButton dialogButton(String text, boolean primary, Color accent) {
        JButton button = new JButton(text);
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("FlatLaf.style", "arc: 8; borderWidth: 1; focusWidth: 0");
        button.setBackground(primary ? accent : Color.WHITE);
        button.setForeground(primary ? Color.WHITE : AppTheme.TEXT_PRIMARY);
        button.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(primary ? accent : new Color(213, 222, 235)),
                AppTheme.emptyBorder(8, 15, 8, 15)
        ));
        button.setFocusPainted(false);
        return button;
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
