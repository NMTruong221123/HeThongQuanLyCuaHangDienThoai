package com.phonestore.ui.product.partner;

import com.phonestore.model.Supplier;
import com.phonestore.ui.common.toast.Toast;

import javax.swing.*;
import java.awt.*;

public class SupplierFormDialog extends JDialog {

    private final JTextField txtName = new JTextField();
    private final JTextField txtPhone = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final JTextField txtAddress = new JTextField();
    private final JComboBox<String> cbStatus = new JComboBox<>(new String[]{"Hoạt động", "Ngưng"});

    private Supplier result;
    private final Supplier editing;
    private final boolean readOnly;

    public SupplierFormDialog(Window owner, Supplier editing) {
        this(owner, editing, false);
    }

    public SupplierFormDialog(Window owner, Supplier editing, boolean readOnly) {
        super(owner, "Nhà cung cấp", ModalityType.APPLICATION_MODAL);
        this.editing = editing;
        this.readOnly = readOnly;
        initUi();
        if (editing != null) {
            bind(editing);
        }
    }

    public Supplier getResult() {
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
        String headerText = readOnly ? "CHI TIẾT NHÀ CUNG CẤP" : (editing == null ? "THÊM NHÀ CUNG CẤP" : "SỬA NHÀ CUNG CẤP");
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
        y = row(form, gbc, y, "Tên nhà cung cấp", txtName);
        y = row(form, gbc, y, "SĐT", txtPhone);
        y = row(form, gbc, y, "Email", txtEmail);
        y = row(form, gbc, y, "Địa chỉ", txtAddress);
        y = row(form, gbc, y, "Trạng thái", cbStatus);

        add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        String saveLabel = editing == null ? "Thêm Nhà Cung Cấp" : "Lưu";

        JButton btnSave = null;
        if (!readOnly) {
            btnSave = new JButton(saveLabel);
            Color addColor = new Color(40, 167, 69);
            Color editColor = new Color(23, 142, 201);
            Color okColor = (editing == null) ? addColor : editColor;
            btnSave.setBackground(okColor);
            btnSave.setForeground(Color.WHITE);
            btnSave.setFocusPainted(false);
            btnSave.setBorderPainted(false);
            btnSave.setOpaque(true);
            btnSave.setPreferredSize(new Dimension(180, 40));
            btnSave.addActionListener(e -> onSave());
            getRootPane().setDefaultButton(btnSave);
            actions.add(btnSave);

            JButton btnCancel = new JButton("Hủy bỏ");
            btnCancel.setBackground(new Color(229, 57, 53));
            btnCancel.setForeground(Color.WHITE);
            btnCancel.setFocusPainted(false);
            btnCancel.setBorderPainted(false);
            btnCancel.setOpaque(true);
            btnCancel.setPreferredSize(new Dimension(140, 40));
            btnCancel.addActionListener(e -> {
                result = null;
                dispose();
            });
            actions.add(btnCancel);
        } else {
            JButton btnCancel = new JButton("Hủy bỏ");
            btnCancel.setBackground(new Color(229, 57, 53));
            btnCancel.setForeground(Color.WHITE);
            btnCancel.setFocusPainted(false);
            btnCancel.setBorderPainted(false);
            btnCancel.setOpaque(true);
            btnCancel.setPreferredSize(new Dimension(140, 40));
            btnCancel.addActionListener(e -> {
                result = null;
                dispose();
            });
            actions.add(btnCancel);
        }
        add(actions, BorderLayout.SOUTH);

        // set editable state
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

    private void bind(Supplier s) {
        txtName.setText(s.getName() == null ? "" : s.getName());
        txtPhone.setText(s.getPhone() == null ? "" : s.getPhone());
        txtEmail.setText(s.getEmail() == null ? "" : s.getEmail());
        txtAddress.setText(s.getAddress() == null ? "" : s.getAddress());
        if (s.getStatus() == null || s.getStatus() == 1) cbStatus.setSelectedIndex(0);
        else cbStatus.setSelectedIndex(1);
    }

    private void onSave() {
        String name = trim(txtName.getText());
        String phone = trim(txtPhone.getText());
        String email = trim(txtEmail.getText());
        String address = trim(txtAddress.getText());

        if (name.isBlank()) {
            Toast.warn(this, "Tên nhà cung cấp là bắt buộc");
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

        Integer status = cbStatus.getSelectedIndex() == 0 ? 1 : 0;

        Supplier s = new Supplier();
        if (editing != null) {
            s.setId(editing.getId());
        }
        s.setName(name);
        s.setPhone(phone);
        s.setEmail(email);
        s.setAddress(address);
        s.setStatus(status);

        result = s;
        dispose();
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
