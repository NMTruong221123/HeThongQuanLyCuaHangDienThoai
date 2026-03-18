package com.phonestore.ui.common;

import javax.swing.*;
import java.awt.*;

public class PlaceholderPanel extends JPanel {

    public PlaceholderPanel(String title) {
        setLayout(new BorderLayout());
        JLabel label = new JLabel(title);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 18f));
        add(label, BorderLayout.CENTER);
    }
}
