package com.phonestore.ui.employee;

import com.phonestore.controller.EmployeeController;
import com.phonestore.controller.UserAccountController;
import com.phonestore.model.Employee;
import com.phonestore.ui.common.toast.Toast;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeChooserDialog extends JDialog {

    private final EmployeeController employeeController = new EmployeeController();
    private final JTable table = new JTable();
    private final JTextField txtSearch = new JTextField();
    private final EmployeeTableModel model = new EmployeeTableModel();
    private final UserAccountController accountController = new UserAccountController();
    private Employee selected;
    private final long currentEmployeeId;

    public EmployeeChooserDialog(Window owner, long currentEmployeeId) {
        super(owner, "Chọn nhân viên", ModalityType.APPLICATION_MODAL);
        this.currentEmployeeId = currentEmployeeId;
        initUi();
        load();
    }

    private void initUi() {
        setSize(640, 420);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(8,8));

        JPanel top = new JPanel(new BorderLayout(6,0));
        top.setBorder(BorderFactory.createEmptyBorder(8,8,0,8));
        top.add(new JLabel("Tìm kiếm:"), BorderLayout.WEST);
        txtSearch.setPreferredSize(new Dimension(480, 34));
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm kiếm nhân viên....");
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
        });
        top.add(txtSearch, BorderLayout.CENTER);
        JButton btnTopChoose = new JButton("Chọn nhân viên");
        btnTopChoose.addActionListener(e -> onChoose());
        top.add(btnTopChoose, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        table.setModel(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(36);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) onChoose();
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCancel = new JButton("Hủy");
        JButton btnChoose = new JButton("Chọn");
        btnCancel.addActionListener(e -> {
            selected = null;
            dispose();
        });
        btnChoose.addActionListener(e -> onChoose());
        actions.add(btnCancel);
        actions.add(btnChoose);
        add(actions, BorderLayout.SOUTH);
    }

    private void load() {
        try {
            List<Employee> all = employeeController.findAll();
            java.util.List<com.phonestore.model.UserAccount> accounts = accountController.findAll();
            java.util.Set<Long> hasAccount = new java.util.HashSet<>();
            for (com.phonestore.model.UserAccount a : accounts) {
                if (a == null) continue;
                hasAccount.add(a.getEmployeeId());
            }
            // filter: only active (status==1) and without account (unless it's the currentEmployeeId)
            java.util.List<Employee> out = new ArrayList<>();
            for (Employee e : all) {
                if (e == null) continue;
                if (e.getStatus() == null || e.getStatus() != 1) continue;
                if (hasAccount.contains(e.getId()) && e.getId() != currentEmployeeId) continue;
                out.add(e);
            }
            model.setRows(out);
        } catch (Throwable ex) {
            Toast.error(this, ex.getMessage() == null ? ex.toString() : ex.getMessage());
            model.setRows(new ArrayList<>());
        }
    }

    private void applyFilter() {
        String q = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase(java.util.Locale.ROOT);
        model.filter(q);
    }

    private void onChoose() {
        int r = table.getSelectedRow();
        if (r < 0) {
            Toast.warn(this, "Vui lòng chọn 1 nhân viên");
            return;
        }
        selected = model.getAt(r);
        dispose();
    }

    public Employee getSelected() {
        return selected;
    }

    private static final class EmployeeTableModel extends AbstractTableModel {
        private final String[] cols = {"MNV", "Họ tên", "Giới tính", "Ngày Sinh", "SDT", "Email"};
        private java.util.List<Employee> rows = new ArrayList<>();
        private java.util.List<Employee> original = new ArrayList<>();

        public void setRows(java.util.List<Employee> rows) {
            this.original = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            this.rows = new ArrayList<>(this.original);
            fireTableDataChanged();
        }

        public void filter(String q) {
            if (q == null || q.isBlank()) {
                rows = new ArrayList<>(original);
            } else {
                java.util.List<Employee> out = new ArrayList<>();
                for (Employee e : original) {
                    String name = e.getFullName() == null ? "" : e.getFullName().toLowerCase(java.util.Locale.ROOT);
                    String id = String.valueOf(e.getId());
                    String phone = e.getPhone() == null ? "" : e.getPhone().toLowerCase(java.util.Locale.ROOT);
                    String mail = e.getEmail() == null ? "" : e.getEmail().toLowerCase(java.util.Locale.ROOT);
                    if (name.contains(q) || id.contains(q) || phone.contains(q) || mail.contains(q)) out.add(e);
                }
                rows = out;
            }
            fireTableDataChanged();
        }

        public Employee getAt(int row) { return rows.get(row); }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int column) { return cols[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Employee e = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> e.getId();
                case 1 -> e.getFullName();
                case 2 -> (e.getGender() != null && e.getGender() == 1) ? "Nam" : "Nữ";
                case 3 -> (e.getBirthDate() == null) ? "" : e.getBirthDate().toString();
                case 4 -> e.getPhone();
                case 5 -> e.getEmail();
                default -> "";
            };
        }
    }
}
