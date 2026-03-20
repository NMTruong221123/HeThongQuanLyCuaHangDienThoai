package com.phonestore.ui.common;

import java.awt.*;

public final class WindowUtils {

    private WindowUtils() {}

    public static void centerOnScreen(Window window) {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screen.width - window.getWidth()) / 2;
        int y = (screen.height - window.getHeight()) / 2;
        window.setLocation(Math.max(0, x), Math.max(0, y));
    }
}
