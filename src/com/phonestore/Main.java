package com.phonestore;

import com.phonestore.app.AppBootstrap;

public class Main {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                throwable.printStackTrace();
            } catch (Throwable ignored) {
                // ignore
            }

            try {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    String msg = throwable.getClass().getSimpleName();
                    if (throwable.getMessage() != null && !throwable.getMessage().isBlank()) {
                        msg += ": " + throwable.getMessage();
                    }
                    javax.swing.JOptionPane.showMessageDialog(
                            null,
                            msg,
                            "Ứng dụng gặp lỗi",
                            javax.swing.JOptionPane.ERROR_MESSAGE
                    );
                });
            } catch (Throwable ignored) {
                // ignore
            }
        });

        // Set default locale and UI font to support Vietnamese
        try {
            java.util.Locale.setDefault(new java.util.Locale("vi", "VN"));
            java.awt.Font uiFont = new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13);
            javax.swing.UIManager.put("Label.font", uiFont);
            javax.swing.UIManager.put("Button.font", uiFont);
            javax.swing.UIManager.put("TextField.font", uiFont);
            javax.swing.UIManager.put("TextArea.font", uiFont);
            javax.swing.UIManager.put("ComboBox.font", uiFont);
            javax.swing.UIManager.put("Table.font", uiFont);
        } catch (Throwable ignored) {
        }

        AppBootstrap.start();
    }
}
