package com.phonestore.ui.product.partner;

import com.phonestore.model.Customer;
import com.phonestore.ui.common.toast.Toast;

import javax.swing.*;
import java.awt.*;

public class CustomerFormDialog extends JDialog {

    private final JTextField txtName = new JTextField();
    private final JTextField txtPhone = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final JTextField txtAddress = new JTextField();
    private final JComboBox<String> cbStatus = new JComboBox<>(new String[] {"Hoạt động", "Ngưng"});

    private Customer result;
    private final Customer editing;
    private final boolean readOnly;

    public CustomerFormDialog(Window owner, Customer editing) {
        this(owner, editing, false);
    }

    public CustomerFormDialog(Window owner, Customer editing, boolean readOnly) {
        super(owner, "Khách hàng", ModalityType.APPLICATION_MODAL);
        this.editing = editing;
        this.readOnly = readOnly;
        initUi();
        if (editing != null) {
            bind(editing);
        }
    }

    public Customer getResult() {
        return result;
    }

    private void initUi() {
        setSize(640, 420);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(0, 0));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(12, 120, 190));
        header.setPreferredSize(new Dimension(0, 72));
        String headerText = readOnly ? "CHI TIẾT KHÁCH HÀNG" : (editing == null ? "THÊM KHÁCH HÀNG" : "SỬA KHÁCH HÀNG");
        JLabel title = new JLabel(headerText, SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        header.add(title, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;
        y = row(form, gbc, y, "Tên khách hàng", txtName);
        y = row(form, gbc, y, "SĐT", txtPhone);
        y = row(form, gbc, y, "Email", txtEmail);
        y = row(form, gbc, y, "Địa chỉ", txtAddress);
        y = row(form, gbc, y, "Trạng thái", cbStatus);

        add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        String saveLabel = editing == null ? "Thêm Khách Hàng" : "Lưu";
        String cancelLabel;
        if (readOnly) cancelLabel = "Hủy bỏ";
        else if (editing == null) cancelLabel = "Hủy Bỏ";
        else cancelLabel = "Hủy bỏ";

        JButton btnSave = null;
        if (!readOnly) {
            btnSave = new JButton(saveLabel);
            btnSave.setBackground(new Color(23, 142, 201));
            btnSave.setForeground(Color.WHITE);
            btnSave.setFocusPainted(false);
            btnSave.setPreferredSize(new Dimension(180, 40));
            btnSave.addActionListener(e -> onSave());
            getRootPane().setDefaultButton(btnSave);
            actions.add(btnSave);
        }

        JButton btnCancel = new JButton(cancelLabel);
        btnCancel.setBackground(new Color(217, 83, 79));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFocusPainted(false);
        btnCancel.setPreferredSize(new Dimension(140, 40));
        btnCancel.addActionListener(e -> {
            result = null;
            dispose();
        });

        actions.add(btnCancel);
        add(actions, BorderLayout.SOUTH);

        // If readOnly mode, make fields non-editable
        txtName.setEditable(!readOnly);
        txtPhone.setEditable(!readOnly);
        txtEmail.setEditable(!readOnly);
        txtAddress.setEditable(!readOnly);
        cbStatus.setEnabled(!readOnly);
    }

    private int row(JPanel panel, GridBagConstraints gbc, int y, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
        return y + 1;
    }

    private void bind(Customer c) {
        txtName.setText(c.getName() == null ? "" : c.getName());
        txtPhone.setText(c.getPhone() == null ? "" : c.getPhone());
        txtEmail.setText(c.getEmail() == null ? "" : c.getEmail());
        txtAddress.setText(c.getAddress() == null ? "" : c.getAddress());
        Integer st = c.getStatus();
        if (st == null || st == 1) cbStatus.setSelectedIndex(0); else cbStatus.setSelectedIndex(1);
    }

    private void onSave() {
        String name = trim(txtName.getText());
        String phone = trim(txtPhone.getText());
        String email = trim(txtEmail.getText());
        String address = trim(txtAddress.getText());

        if (name.isBlank()) {
            Toast.warn(this, "Tên khách hàng là bắt buộc");
            return;
        }
        if (phone.isBlank()) {
            Toast.warn(this, "SĐT là bắt buộc");
            return;
        }
        if (email.isBlank()) {
            Toast.warn(this, "Email là bắt buộc");
            return;
        }
        if (address.isBlank()) {
            Toast.warn(this, "Địa chỉ là bắt buộc");
            return;
        }

        // status from selector (Hoạt động=1, Ngưng=0)
        Integer status = cbStatus.getSelectedIndex() == 1 ? 0 : 1;

        Customer c = new Customer();
        if (editing != null) {
            c.setId(editing.getId());
        }
        c.setName(name);
        c.setPhone(phone);
        c.setEmail(email.isBlank() ? null : email);
        c.setAddress(address);
        c.setStatus(status);

        result = c;
        dispose();
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
