package com.lin.erp.ui;

import javax.swing.BorderFactory;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AppTheme {
    private static final Logger LOGGER = Logger.getLogger(AppTheme.class.getName());

    public static final Color PAGE_BACKGROUND = new Color(246, 248, 251);
    public static final Color PANEL_BACKGROUND = Color.WHITE;
    public static final Color SIDEBAR_BACKGROUND = new Color(45, 49, 56);
    public static final Color NAV_SELECTED = new Color(23, 148, 137);
    public static final Color NAV_HOVER = new Color(57, 62, 71);
    public static final Color TEXT_PRIMARY = new Color(20, 31, 48);
    public static final Color TEXT_MUTED = new Color(103, 116, 136);
    public static final Color BORDER = new Color(221, 229, 239);
    public static final Color ACCENT = new Color(23, 148, 137);
    public static final Color ACCENT_DARK = new Color(20, 112, 107);
    public static final Color ACCENT_SOFT = new Color(230, 247, 245);
    public static final Color TEAL = new Color(23, 148, 137);
    public static final Color TEAL_SOFT = new Color(230, 247, 245);
    public static final Color SUCCESS = new Color(32, 143, 108);
    public static final Color WARNING = new Color(204, 138, 38);
    public static final Color ERROR = new Color(190, 73, 73);

    private AppTheme() {
    }

    public static void installLookAndFeel() {
        try {
            Class.forName("com.formdev.flatlaf.FlatIntelliJLaf");
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatIntelliJLaf");
            configureFlatLafExtras();
            LOGGER.info("FlatLaf look and feel installed.");
        } catch (Exception flatLafFailure) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                LOGGER.log(Level.WARNING, "FlatLaf is unavailable. System look and feel was installed.", flatLafFailure);
            } catch (Exception systemFailure) {
                LOGGER.log(Level.WARNING, "System look and feel is unavailable. Swing default will be used.", systemFailure);
            }
        }
    }

    private static void configureFlatLafExtras() {
        try {
            Class<?> lafClass = Class.forName("com.formdev.flatlaf.FlatLaf");
            Method setUseNativeWindowDecorations = lafClass.getMethod("setUseNativeWindowDecorations", boolean.class);
            setUseNativeWindowDecorations.invoke(null, Boolean.TRUE);
        } catch (Exception ignored) {
            // Native window decorations are optional.
        }
    }

    public static void applyGlobalDefaults() {
        Font baseFont = new Font("Microsoft YaHei UI", Font.PLAIN, 13);
        UIDefaults defaults = UIManager.getDefaults();
        defaults.put("defaultFont", baseFont);
        defaults.put("Button.font", baseFont);
        defaults.put("CheckBox.font", baseFont);
        defaults.put("ComboBox.font", baseFont);
        defaults.put("Label.font", baseFont);
        defaults.put("PasswordField.font", baseFont);
        defaults.put("TextField.font", baseFont);
        defaults.put("Table.font", baseFont);
        defaults.put("Tree.font", baseFont);
        defaults.put("Component.arc", Integer.valueOf(8));
        defaults.put("Button.arc", Integer.valueOf(10));
        defaults.put("Button.margin", new Insets(8, 14, 8, 14));
        defaults.put("Button.innerFocusWidth", Integer.valueOf(0));
        defaults.put("Button.focusWidth", Integer.valueOf(1));
        defaults.put("Button.borderWidth", Integer.valueOf(1));
        defaults.put("TextComponent.arc", Integer.valueOf(8));
        defaults.put("ComboBox.arc", Integer.valueOf(8));
        defaults.put("ScrollBar.thumbArc", Integer.valueOf(8));
        defaults.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        defaults.put("Table.rowHeight", Integer.valueOf(36));
        defaults.put("Table.showHorizontalLines", Boolean.TRUE);
        defaults.put("Table.showVerticalLines", Boolean.FALSE);
        defaults.put("Table.selectionBackground", ACCENT_SOFT);
        defaults.put("Table.selectionForeground", TEXT_PRIMARY);
        defaults.put("TableHeader.background", new Color(242, 246, 251));
        defaults.put("TableHeader.foreground", TEXT_PRIMARY);
        defaults.put("Component.focusColor", ACCENT);
    }

    public static Font font(int style, int size) {
        return new Font("Microsoft YaHei UI", style, size);
    }

    public static Border emptyBorder(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }
}
