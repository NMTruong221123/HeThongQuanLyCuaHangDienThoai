package com.phonestore.ui.inventory;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

public class InvoicePreviewDialog extends JDialog {

    private final JTextArea area = new JTextArea();

    public InvoicePreviewDialog(Window owner, String title, String invoiceText, Path savedFile) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        initUi(invoiceText, savedFile);
    }

    private void initUi(String invoiceText, Path savedFile) {
        setSize(720, 560);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));

        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setEditable(false);
        area.setText(invoiceText == null ? "" : invoiceText);
        area.setCaretPosition(0);

        add(new JScrollPane(area), BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout());
        if (savedFile != null) {
            JLabel lbl = new JLabel("Đã lưu hóa đơn: " + savedFile.toString());
            top.add(lbl, BorderLayout.CENTER);
            top.setBorder(BorderFactory.createEmptyBorder(6, 10, 0, 10));
            add(top, BorderLayout.NORTH);
        }

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnPrint = new JButton("In");
        JButton btnClose = new JButton("Đóng");

        btnPrint.addActionListener(e -> onPrint());
        btnClose.addActionListener(e -> dispose());

        actions.add(btnPrint);
        actions.add(btnClose);
        add(actions, BorderLayout.SOUTH);
    }

    private void onPrint() {
        try {
            // This shows system print dialog.
            boolean ok = area.print();
            if (!ok) {
                // user cancelled
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage() == null ? ex.toString() : ex.getMessage(),
                    "Lỗi in hóa đơn",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
