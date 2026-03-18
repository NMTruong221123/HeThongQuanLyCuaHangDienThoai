package com.phonestore.ui.employee;

import com.phonestore.controller.EmployeeController;
import com.phonestore.model.Employee;
import com.phonestore.model.UserAccount;
import com.phonestore.ui.common.toast.Toast;

import javax.swing.*;
import javax.swing.DefaultListCellRenderer;
import java.awt.*;
import java.util.List;

public class AccountFormDialog extends JDialog {

    private final JTextField txtUsername = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final JTextField txtPassword = new JTextField();
    private final JComboBox<String> cbRole = new JComboBox<>();
    private final JComboBox<String> cbStatus = new JComboBox<>();

    private final JComboBox<Employee> cbEmployee = new JComboBox<>();
    private final EmployeeController employeeController = new EmployeeController();
    private final com.phonestore.controller.UserAccountController accountController = new com.phonestore.controller.UserAccountController();

    private UserAccount result;
    private final UserAccount editing;
    private final boolean readOnly;

    // loaded from DB at runtime
    private java.util.List<java.util.Map.Entry<Integer, String>> roleList = new java.util.ArrayList<>();

    public AccountFormDialog(Window owner, UserAccount editing) {
        this(owner, editing, false);
    }

    public AccountFormDialog(Window owner, Employee initialEmployee) {
        this(owner, (UserAccount) null, false);
        if (initialEmployee != null) {
            selectEmployee(initialEmployee.getId());
            cbEmployee.setEnabled(false);
        }
    }

    public AccountFormDialog(Window owner, UserAccount editing, boolean readOnly) {
        super(owner, readOnly ? "CHI TIẾT TÀI KHOẢN" : (editing == null ? "Thêm tài khoản" : "Sửa tài khoản"), ModalityType.APPLICATION_MODAL);
        this.editing = editing;
        this.readOnly = readOnly;
        initUi();
        if (editing != null) bind(editing);
    }

    public UserAccount getResult() {
        return result;
    }

    private void initUi() {
        setSize(640, 430);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(0, 0));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(12, 120, 190));
        header.setPreferredSize(new Dimension(0, 72));
        String headerText = readOnly
            ? "CHI TIẾT TÀI KHOẢN"
            : (editing == null ? "THÊM TÀI KHOẢN" : "SỬA TÀI KHOẢN");
        JLabel title = new JLabel(headerText, SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        header.add(title, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        // add extra top padding so the input fields sit lower on the dialog
        form.setBorder(BorderFactory.createEmptyBorder(24, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;
        y = row(form, gbc, y, "Nhân viên (manv)", cbEmployee);
        y = row(form, gbc, y, "Tên đăng nhập", txtUsername);
        y = row(form, gbc, y, "Email", txtEmail);
        y = row(form, gbc, y, "Mật khẩu", txtPassword);
        y = row(form, gbc, y, "Nhóm quyền", cbRole);
        y = row(form, gbc, y, "Trạng thái", cbStatus);

        add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        if (!readOnly) {
            String saveLabel = editing == null ? "Thêm tài khoản" : "Lưu";
            JButton btnSave = new JButton(saveLabel);
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

            actions.add(btnSave);
            actions.add(btnCancel);
        } else {
            JButton btnClose = new JButton("Đóng");
            btnClose.setPreferredSize(new Dimension(140, 40));
            btnClose.addActionListener(e -> dispose());
            actions.add(btnClose);
        }
        add(actions, BorderLayout.SOUTH);

        configureEmployeeCombo();
        configureStatusCombo();
        configureRoleCombo();
        loadEmployees();

        // Email is sourced from the selected Employee and must not be editable
        txtEmail.setEditable(false);

        if (editing != null) {
            cbEmployee.setEnabled(false);
        }
        if (editing != null && !readOnly) {
            // when editing, password should not be changeable from this form
            txtPassword.setEditable(false);
            txtPassword.setText("");
            txtPassword.setToolTipText("Mật khẩu không thể thay đổi ở chế độ sửa");
        }
        if (readOnly) {
            // disable all inputs
            cbEmployee.setEnabled(false);
            txtUsername.setEditable(false);
            txtPassword.setEditable(false);
            cbRole.setEnabled(false);
            cbStatus.setEnabled(false);
        }
    }

    private void updateEmailFromSelectedEmployee() {
        Object sel = cbEmployee.getSelectedItem();
        if (sel instanceof Employee e) {
            txtEmail.setText(e.getEmail() == null ? "" : e.getEmail());
        } else {
            txtEmail.setText("");
        }
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

    private void bind(UserAccount a) {
        selectEmployee(a.getEmployeeId());
        txtUsername.setText(a.getUsername() == null ? "" : a.getUsername());
        // email should come from the selected Employee and not be editable
        updateEmailFromSelectedEmployee();
        // If selected employee not available (deleted), create a placeholder so UI shows info
        if (!(cbEmployee.getSelectedItem() instanceof Employee)) {
            Employee placeholder = new Employee();
            placeholder.setId(a.getEmployeeId());
            String fallbackName = a.getEmployeeName() == null ? "(đã xóa)" : a.getEmployeeName();
            placeholder.setFullName(fallbackName);
            // try to build a fallback code
            try {
                placeholder.setCode(String.format("NVPS-%02d", a.getEmployeeId() <= 0 ? 0 : a.getEmployeeId()));
            } catch (Exception ignored) {
            }
            cbEmployee.setModel(new DefaultComboBoxModel<>(new Employee[]{placeholder}));
            cbEmployee.setSelectedIndex(0);
            updateEmailFromSelectedEmployee();
        }
        if (editing != null) {
            txtPassword.setText("");
            txtPassword.setEditable(false);
            txtPassword.setToolTipText("Mật khẩu không thể thay đổi ở chế độ sửa");
        } else {
            txtPassword.setText(a.getPassword() == null ? "" : a.getPassword());
        }
        // select role by id (loaded from DB)
        Integer rid = a.getRoleId();
        if (rid == null) rid = 1;
        for (java.util.Map.Entry<Integer, String> it : roleList) {
            if (it.getKey().equals(rid)) {
                cbRole.setSelectedItem(it.getValue());
                break;
            }
        }
        int st = a.getStatus() == null ? 1 : a.getStatus();
        // If linked employee is missing and we are in read-only/detail mode, show special status
        Object empSel = cbEmployee.getSelectedItem();
        boolean empMissing = !(empSel instanceof Employee);
        if (empMissing && readOnly) {
            cbStatus.removeAllItems();
            cbStatus.addItem("ĐÃ XÓA NV");
            cbStatus.setSelectedIndex(0);
        } else {
            cbStatus.setSelectedItem(st == 1 ? "Hoạt động" : "Ngưng hoạt động");
        }
    }

    private void onSave() {
        Employee emp = (Employee) cbEmployee.getSelectedItem();
        if (emp == null || emp.getId() <= 0) {
            Toast.warn(this, "Vui lòng chọn nhân viên");
            return;
        }

        String username = trim(txtUsername.getText());
        String email = trim(txtEmail.getText());
        String password;
        if (username.isBlank()) {
            Toast.warn(this, "Tên đăng nhập là bắt buộc");
            return;
        }
        if (editing != null) {
            // preserve existing password when editing
            password = editing.getPassword() == null ? "" : editing.getPassword();
        } else {
            password = trim(txtPassword.getText());
            if (password.isBlank()) {
                Toast.warn(this, "Mật khẩu là bắt buộc");
                return;
            }
        }

        Integer roleId = null;
        Object roleSel = cbRole.getSelectedItem();
        if (roleSel != null) {
            String name = roleSel.toString();
            for (java.util.Map.Entry<Integer, String> it : roleList) {
                if (it.getValue().equals(name)) { roleId = it.getKey(); break; }
            }
        }
        if (roleId == null) {
            Toast.warn(this, "Vui lòng chọn nhóm quyền");
            return;
        }

        Integer status = null;
        Object sel = cbStatus.getSelectedItem();
        if (sel != null) {
            status = "Hoạt động".equals(sel) ? 1 : 0;
        }

        UserAccount a = new UserAccount();
        a.setEmployeeId(emp.getId());
        a.setUsername(username);
        if (!email.isBlank()) a.setEmail(email);
        a.setPassword(password);
        a.setRoleId(roleId);
        a.setStatus(status);
        result = a;
        dispose();
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private void configureEmployeeCombo() {
        cbEmployee.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Employee e) {
                    String code = e.getCode();
                    if (code == null || code.isBlank()) code = String.valueOf(e.getId());
                    String name = e.getFullName() == null ? "" : e.getFullName();
                    setText(code + " -- " + name);
                }
                return this;
            }
        });
        cbEmployee.addActionListener(e -> updateEmailFromSelectedEmployee());
    }

    private void loadEmployees() {
        try {
            List<Employee> employees = employeeController.findAll();
            java.util.List<com.phonestore.model.UserAccount> accounts = accountController.findAll();
            java.util.Set<Long> hasAccount = new java.util.HashSet<>();
            for (com.phonestore.model.UserAccount a : accounts) {
                if (a == null) continue;
                hasAccount.add(a.getEmployeeId());
            }
            java.util.List<Employee> out = new java.util.ArrayList<>();
            for (Employee e : employees) {
                if (e == null) continue;
                if (e.getStatus() == null || e.getStatus() != 1) continue; // only active
                if (editing == null) {
                    if (hasAccount.contains(e.getId())) continue; // skip if already has account
                } else {
                    // when editing, keep the currently edited employee available even if it already has an account
                    if (e.getId() != editing.getEmployeeId() && hasAccount.contains(e.getId())) continue;
                }
                out.add(e);
            }
            cbEmployee.setModel(new DefaultComboBoxModel<>(out.toArray(new Employee[0])));
        } catch (Throwable ex) {
            Toast.error(this, ex.getMessage() == null ? ex.toString() : ex.getMessage());
        }
    }

    private void configureStatusCombo() {
        cbStatus.removeAllItems();
        cbStatus.addItem("Hoạt động");
        cbStatus.addItem("Ngưng hoạt động");
        cbStatus.setSelectedIndex(0);
    }

    private void configureRoleCombo() {
        cbRole.removeAllItems();
        roleList.clear();
        try {
            com.phonestore.dao.NhomQuyenDao dao = new com.phonestore.dao.jdbc.NhomQuyenJdbcDao();
            java.util.List<com.phonestore.model.NhomQuyen> rs = dao.findAll();

            // Determine which role IDs are considered ADMIN (name contains 'admin')
            java.util.Set<Integer> adminRoleIds = new java.util.HashSet<>();
            for (com.phonestore.model.NhomQuyen r : rs) {
                if (r == null) continue;
                Integer id = r.getId();
                String name = r.getName();
                if (id != null && name != null && name.toLowerCase().contains("admin")) {
                    adminRoleIds.add(id);
                }
            }

            int adminCount = -1;
            if (!adminRoleIds.isEmpty()) {
                try {
                    java.util.List<com.phonestore.model.UserAccount> accs = accountController.findAll();
                    int cnt = 0;
                    for (com.phonestore.model.UserAccount a : accs) {
                        if (a == null) continue;
                        Integer rid = a.getRoleId();
                        if (rid != null && adminRoleIds.contains(rid)) cnt++;
                    }
                    adminCount = cnt;
                } catch (Throwable ignored) {
                }
            }
            boolean adminFull = adminCount >= 2;
            boolean editingIsAdmin = editing != null && editing.getRoleId() != null && adminRoleIds.contains(editing.getRoleId());

            for (com.phonestore.model.NhomQuyen r : rs) {
                if (r == null) continue;
                if (r.getStatus() == null || r.getStatus() != 1) continue; // only active roles
                Integer id = r.getId();
                String name = r.getName() == null ? ("Role " + id) : r.getName();

                // When ADMIN is already at max (2 accounts), do not allow selecting ADMIN for new account
                // or for editing a non-admin account. Keep ADMIN available only when editing an existing admin.
                if (adminFull && id != null && adminRoleIds.contains(id) && !editingIsAdmin) {
                    continue;
                }
                roleList.add(java.util.Map.entry(id, name));
                cbRole.addItem(name);
            }
        } catch (Throwable ex) {
            // fallback: keep combo empty and let user know via toast
            com.phonestore.ui.common.toast.Toast.error(this, ex.getMessage() == null ? ex.toString() : ex.getMessage());
        }
        if (cbRole.getItemCount() > 0) cbRole.setSelectedIndex(0);
    }

    private void selectEmployee(long employeeId) {
        ComboBoxModel<Employee> model = cbEmployee.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            Employee it = model.getElementAt(i);
            if (it != null && it.getId() == employeeId) {
                cbEmployee.setSelectedItem(it);
                updateEmailFromSelectedEmployee();
                return;
            }
        }
    }
}
