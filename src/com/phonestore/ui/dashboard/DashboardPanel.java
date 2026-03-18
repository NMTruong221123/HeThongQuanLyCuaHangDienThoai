package com.phonestore.ui.dashboard;

import javax.swing.*;
import java.awt.*;

public class DashboardPanel extends JPanel {

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 0));
        // Show a single large illustration (reuse the login image resource)
        // Use project-provided homepage image named 'trangchu' in resources/images/default
        JPanel imagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                try {
                    javax.swing.Icon icon = com.phonestore.ui.common.images.ImageLoader.loadDefaultImageFit("trangchu.png", getWidth(), getHeight());
                    if (icon instanceof javax.swing.ImageIcon) {
                        java.awt.Image img = ((javax.swing.ImageIcon) icon).getImage();
                        g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
                    } else if (icon != null) {
                        int iw = icon.getIconWidth();
                        int ih = icon.getIconHeight();
                        int x = Math.max(0, (getWidth() - iw) / 2);
                        int y = Math.max(0, (getHeight() - ih) / 2);
                        icon.paintIcon(this, g, x, y);
                    }
                } catch (Throwable ignored) {
                }
            }
        };
        imagePanel.setLayout(new BorderLayout());
        add(imagePanel, BorderLayout.CENTER);
    }
    // Dashboard intentionally simplified to a single illustration per user request.
}
