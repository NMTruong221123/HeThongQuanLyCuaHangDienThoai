package com.phonestore.ui.common;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class UiTheme {

    private UiTheme() {}

    public static void apply() {
        boolean flatApplied = tryApplyFlatLaf();
        if (!flatApplied) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
        } else {
            applyFlatDefaults();
        }

        UIManager.put("OptionPane.okButtonText", "OK");
        UIManager.put("OptionPane.cancelButtonText", "Hủy");
        UIManager.put("OptionPane.yesButtonText", "Có");
        UIManager.put("OptionPane.noButtonText", "Không");
    }

    private static boolean tryApplyFlatLaf() {
        try {
            Class<?> robotoFontClass = Class.forName("com.formdev.flatlaf.fonts.roboto.FlatRobotoFont");
            // FlatRobotoFont.install();
            robotoFontClass.getMethod("install").invoke(null);

            Class<?> flatLafClass = Class.forName("com.formdev.flatlaf.FlatLaf");
            flatLafClass.getMethod("setPreferredFontFamily", String.class)
                    .invoke(null, (String) robotoFontClass.getField("FAMILY").get(null));
            flatLafClass.getMethod("setPreferredLightFontFamily", String.class)
                    .invoke(null, (String) robotoFontClass.getField("FAMILY_LIGHT").get(null));
            flatLafClass.getMethod("setPreferredSemiboldFontFamily", String.class)
                    .invoke(null, (String) robotoFontClass.getField("FAMILY_SEMIBOLD").get(null));

            Class<?> intellijLafClass = Class.forName("com.formdev.flatlaf.FlatIntelliJLaf");
            // FlatIntelliJLaf.registerCustomDefaultsSource("style"); // optional
            try {
                intellijLafClass.getMethod("registerCustomDefaultsSource", String.class).invoke(null, "style");
            } catch (Exception ignored) {
            }
            // FlatIntelliJLaf.setup();
            intellijLafClass.getMethod("setup").invoke(null);

            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Mirrors the reference project UI defaults (table styling, rounded corners, etc.).
     */
    private static void applyFlatDefaults() {
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("TextComponent.arc", 5);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("Button.iconTextGap", 10);
        UIManager.put("PasswordField.showRevealButton", true);
        UIManager.put("Table.selectionBackground", new Color(240, 247, 250));
        UIManager.put("Table.selectionForeground", new Color(0, 0, 0));
        UIManager.put("Table.scrollPaneBorder", new EmptyBorder(0, 0, 0, 0));
        UIManager.put("Table.rowHeight", 40);
        UIManager.put("TabbedPane.selectedBackground", Color.white);
        UIManager.put("TableHeader.height", 40);
        try {
            // UIManager.getFont("h4.font") may not exist in all themes.
            Font h4 = UIManager.getFont("h4.font");
            if (h4 != null) {
                UIManager.put("TableHeader.font", h4);
            }
        } catch (Exception ignored) {
        }
        UIManager.put("TableHeader.background", new Color(242, 242, 242));
        UIManager.put("TableHeader.separatorColor", new Color(242, 242, 242));
        UIManager.put("TableHeader.bottomSeparatorColor", new Color(242, 242, 242));
    }
}
