package com.phonestore.ui.employee;

import com.phonestore.model.Employee;
import com.phonestore.ui.common.toast.Toast;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class EmployeeFormDialog extends JDialog {

    private final JTextField txtFullName = new JTextField();
    private final JLabel lblCode = new JLabel();
    private final JComboBox<String> cbGender = new JComboBox<>(new String[]{"Nam", "Nữ"});
    private final JTextField txtBirthDate = new JTextField();
    private final JButton btnPickDate = new JButton("...");
    private final JTextField txtPhone = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final JComboBox<String> cbStatus = new JComboBox<>(new String[]{"Đang Làm", "Tạm Nghỉ"});

    private Employee result;
    private final Employee editing;
    private JButton btnSave;
    private JButton btnCancel;
    private JLabel lblHeaderTitle;
    private boolean readOnlyMode;

    public EmployeeFormDialog(Window owner, Employee editing) {
        super(owner, "Nhân viên", ModalityType.APPLICATION_MODAL);
        this.editing = editing;
        initUi();
        if (editing != null) bind(editing);
    }

    public Employee getResult() {
        return result;
    }

    private void initUi() {
        setSize(820, 600);
        setResizable(true);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(0, 0));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(12, 120, 190));
        header.setPreferredSize(new Dimension(0, 72));
        lblHeaderTitle = new JLabel(resolveHeaderText(), SwingConstants.CENTER);
        lblHeaderTitle.setForeground(Color.WHITE);
        lblHeaderTitle.setFont(lblHeaderTitle.getFont().deriveFont(Font.BOLD, 20f));
        header.add(lblHeaderTitle, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;
        // show code (read-only) when editing
        if (editing != null) {
            lblCode.setPreferredSize(new Dimension(0, 34));
            y = row(form, gbc, y, "Mã NV", lblCode);
        }
        y = row(form, gbc, y, "Họ tên", txtFullName);
        y = row(form, gbc, y, "Giới Tính", cbGender);
        // birth date field with picker button
        JPanel birthPanel = new JPanel(new BorderLayout(6, 0));
        birthPanel.add(txtBirthDate, BorderLayout.CENTER);
        btnPickDate.setPreferredSize(new Dimension(36, 28));
        birthPanel.add(btnPickDate, BorderLayout.EAST);
        y = row(form, gbc, y, "Ngày sinh", birthPanel);
        y = row(form, gbc, y, "SĐT", txtPhone);
        y = row(form, gbc, y, "Email", txtEmail);
        y = row(form, gbc, y, "Trạng thái", cbStatus);

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        String saveLabel = editing == null ? "Thêm nhân viên" : "Lưu";

        // make buttons fields so we can toggle read-only mode
        btnSave = new JButton(saveLabel);
        Color addColor = new Color(40, 167, 69);
        Color editColor = new Color(23, 142, 201);
        btnSave.setBackground(editing == null ? addColor : editColor);
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setBorderPainted(false);
        btnSave.setOpaque(true);
        btnSave.setPreferredSize(new Dimension(180, 40));
        btnSave.addActionListener(e -> onSave());
        getRootPane().setDefaultButton(btnSave);

        btnCancel = new JButton("Hủy bỏ");
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

        actions.add(btnSave);
        actions.add(btnCancel);
        add(actions, BorderLayout.SOUTH);

        // date picker action
        btnPickDate.addActionListener(ae -> {
            LocalDate initial = null;
            try {
                String t = trim(txtBirthDate.getText());
                if (!t.isBlank()) {
                    if (t.contains("/")) {
                        initial = LocalDate.parse(t, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    } else {
                        initial = LocalDate.parse(t);
                    }
                }
            } catch (Exception ex) {
                initial = LocalDate.now();
            }
            LocalDate picked = DatePickerDialog.showDialog(this, initial);
            if (picked != null) {
                txtBirthDate.setText(picked.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
        });
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

    private void bind(Employee e) {
        if (e.getCode() != null) lblCode.setText(e.getCode());
        txtFullName.setText(e.getFullName() == null ? "" : e.getFullName());
        Integer g = e.getGender();
        // gender mapping: 1 -> Nam, 0 -> Nữ
        if (g == null || g == 1) cbGender.setSelectedIndex(0); else cbGender.setSelectedIndex(1);
        if (e.getBirthDate() == null) txtBirthDate.setText(""); else txtBirthDate.setText(e.getBirthDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        txtPhone.setText(e.getPhone() == null ? "" : e.getPhone());
        txtEmail.setText(e.getEmail() == null ? "" : e.getEmail());
        // status mapping: 1 -> Đang Làm, 0 -> Tạm Nghỉ
        Integer st = e.getStatus();
        if (st == null || st == 1) cbStatus.setSelectedIndex(0); else cbStatus.setSelectedIndex(1);
    }

    private void onSave() {
        String fullName = trim(txtFullName.getText());
        String phone = trim(txtPhone.getText());
        String email = trim(txtEmail.getText());
        String bdText = trim(txtBirthDate.getText());

        if (fullName.isBlank()) {
            Toast.warn(this, "Họ tên là bắt buộc");
            return;
        }
        if (bdText.isBlank()) {
            Toast.warn(this, "Ngày sinh là bắt buộc");
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

        Integer gender = cbGender.getSelectedIndex() == 0 ? 1 : 0;
        LocalDate birthDate;
        try {
            if (bdText.contains("/")) {
                DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                birthDate = LocalDate.parse(bdText, f);
            } else {
                birthDate = LocalDate.parse(bdText);
            }
        } catch (Exception ex) {
            Toast.warn(this, "Ngày sinh không hợp lệ");
            return;
        }

        Integer status = cbStatus.getSelectedIndex() == 0 ? 1 : 0;

        Employee e = new Employee();
        if (editing != null) e.setId(editing.getId());
        e.setFullName(fullName);
        e.setGender(gender);
        e.setBirthDate(birthDate);
        e.setPhone(phone);
        e.setEmail(email);
        e.setStatus(status);

        result = e;
        dispose();
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private String resolveHeaderText() {
        if (readOnlyMode) return "CHI TIẾT NHÂN VIÊN";
        return editing == null ? "THÊM NHÂN VIÊN" : "SỬA NHÂN VIÊN";
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnlyMode = readOnly;
        txtFullName.setEnabled(!readOnly);
        cbGender.setEnabled(!readOnly);
        txtBirthDate.setEnabled(!readOnly);
        btnPickDate.setEnabled(!readOnly);
        txtPhone.setEnabled(!readOnly);
        txtEmail.setEnabled(!readOnly);
        cbStatus.setEnabled(!readOnly);
        if (btnSave != null) btnSave.setVisible(!readOnly);
        if (btnCancel != null) btnCancel.setText(readOnly ? "Đóng" : "Hủy bỏ");
        setTitle(readOnly ? "CHI TIẾT NHÂN VIÊN" : "Nhân viên");
        if (lblHeaderTitle != null) lblHeaderTitle.setText(resolveHeaderText());
    }
}
