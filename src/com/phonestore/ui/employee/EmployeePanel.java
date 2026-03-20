package com.phonestore.ui.employee;

import com.phonestore.controller.EmployeeController;
import com.phonestore.controller.UserAccountController;
import com.phonestore.ui.common.session.SessionContext;
import com.phonestore.ui.common.session.UserSession;
import com.phonestore.model.Employee;
import com.phonestore.ui.common.images.ImageLoader;
import com.phonestore.ui.common.toast.Toast;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EmployeePanel extends JPanel {

    private final EmployeeController employeeController = new EmployeeController();
    private final UserAccountController accountController = new UserAccountController();
    private final JTextField txtSearch = new JTextField();
    private final EmployeeTableModel tableModel = new EmployeeTableModel();
    private final JTable table = new JTable(tableModel);
    private List<Employee> allRows = new ArrayList<>();

    private final Color backgroundColor = new Color(240, 247, 250);

    public EmployeePanel() {
        setBackground(backgroundColor);
        setLayout(new BorderLayout(0, 0));
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(pad(BorderLayout.WEST), BorderLayout.WEST);
        add(pad(BorderLayout.EAST), BorderLayout.EAST);
        add(pad(BorderLayout.SOUTH), BorderLayout.SOUTH);
        reload();
    }

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new GridLayout(1, 2, 50, 0));
        bar.setBackground(backgroundColor);
        bar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        bar.setPreferredSize(new Dimension(0, 110));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        left.setOpaque(false);
        JButton btnAdd = new JButton("THÊM", ImageLoader.loadIcon("add.svg", 42, 42));
        JButton btnEdit = new JButton("SỬA", ImageLoader.loadIcon("edit.svg", 42, 42));
        JButton btnDelete = new JButton("XÓA", ImageLoader.loadIcon("delete.svg", 42, 42));
        JButton btnDetail = new JButton("CHI TIẾT", ImageLoader.loadIcon("detail.svg", 42, 42));

        styleToolbarIconBtn(btnAdd);
        styleToolbarIconBtn(btnEdit);
        styleToolbarIconBtn(btnDelete);
        styleToolbarIconBtn(btnDetail);

        left.add(btnAdd);
        left.add(btnEdit);
        left.add(btnDelete);
        left.add(btnDetail);

        btnAdd.addActionListener(e -> onAdd());
        btnEdit.addActionListener(e -> onEdit());
        btnDelete.addActionListener(e -> onDelete());
        btnDetail.addActionListener(e -> onDetail());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 18));
        right.setOpaque(false);
        txtSearch.setPreferredSize(new Dimension(320, 34));
        txtSearch.putClientProperty("JTextField.placeholderText", "Nhập tên/SĐT...");
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onSearch(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onSearch(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onSearch(); }
        });

        JButton btnRefresh = new JButton("Làm mới", ImageLoader.loadIcon("refresh.svg", 18, 18));
        styleBtn(btnRefresh);
        btnRefresh.setPreferredSize(new Dimension(120, 34));
        btnRefresh.setFont(btnRefresh.getFont().deriveFont(Font.PLAIN, 13f));
        btnRefresh.addActionListener(e -> {
            txtSearch.setText("");
            reload();
        });

        right.add(txtSearch);
        right.add(btnRefresh);

        bar.add(left);
        bar.add(right);
        return bar;
    }

    private void onExport() {
        // Excel export removed
    }

    // Excel import removed

    private String n(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private long parseLong(String s) {
        try {
            String t = trim(s);
            if (t.isBlank()) return 0;
            return Long.parseLong(t);
        } catch (Exception e) {
            return 0;
        }
    }

    private String req(String s, String key) {
        String t = trim(s);
        if (t.isBlank()) throw new IllegalArgumentException("Thiếu cột: " + key);
        return t;
    }

    private Integer parseRequiredGender(String s, String key) {
        String t = trim(s).toLowerCase();
        if (t.isBlank()) throw new IllegalArgumentException("Thiếu cột: " + key);
        if (t.equals("1") || t.equals("nam") || t.equals("male")) return 1;
        if (t.equals("0") || t.equals("nu") || t.equals("nữ") || t.equals("female")) return 0;
        throw new IllegalArgumentException("Giới tính chỉ nhận 0/1 hoặc Nam/Nữ");
    }

    private LocalDate parseRequiredDate(String s, String key) {
        String t = trim(s);
        if (t.isBlank()) throw new IllegalArgumentException("Thiếu cột: " + key);
        try {
            return LocalDate.parse(t);
        } catch (DateTimeParseException ignored) {
            // continue
        }
        for (String p : new String[] {"d/M/yyyy", "dd/MM/yyyy", "d-M-yyyy", "dd-MM-yyyy"}) {
            try {
                return LocalDate.parse(t, DateTimeFormatter.ofPattern(p));
            } catch (DateTimeParseException ignored) {
                // continue
            }
        }
        throw new IllegalArgumentException("Ngày sinh không hợp lệ: " + t);
    }

    private JComponent buildCenter() {
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        // add top spacing so toolbar icon labels don't overlap table header
        center.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        configureTable();
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        return center;
    }

    private void configureTable() {
        table.setFillsViewportHeight(true);
        table.setRowHeight(40);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultEditor(Object.class, null);
        if (table.getTableHeader() != null) table.getTableHeader().setReorderingAllowed(false);

        if (table.getColumnCount() > 1) table.getColumnModel().getColumn(1).setPreferredWidth(220);

        // Custom renderer to dim entire row for status = 0 (Tạm Nghỉ) while preserving selection highlight
        DefaultTableCellRenderer rowRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (row < 0) return c;
                Employee e = tableModel.getAt(row);
                if (!isSelected) {
                    if (e != null && e.getStatus() != null && e.getStatus() == 0) {
                        c.setBackground(new Color(245, 245, 245));
                        c.setForeground(Color.DARK_GRAY);
                    } else {
                        c.setBackground(Color.WHITE);
                        c.setForeground(Color.BLACK);
                    }
                } else {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(table.getSelectionForeground());
                }
                // alignment: name (col 1) left, others center
                if (column == 1) {
                    setHorizontalAlignment(SwingConstants.LEFT);
                } else {
                    setHorizontalAlignment(SwingConstants.CENTER);
                }
                return c;
            }
        };

        // Apply renderer to all columns so the entire row is consistently styled
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(rowRenderer);
        }
    }

    private void styleBtn(JButton b) {
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.putClientProperty("JComponent.sizeVariant", "regular");
    }

    private void styleToolbarIconBtn(JButton b) {
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setHorizontalTextPosition(SwingConstants.CENTER);
        b.setVerticalTextPosition(SwingConstants.BOTTOM);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setPreferredSize(new Dimension(92, 92));
        b.setFont(b.getFont().deriveFont(Font.BOLD, 12f));
        b.setForeground(new Color(7, 125, 218));
    }

    private JComponent pad(String pos) {
        JPanel p = new JPanel();
        p.setBackground(backgroundColor);
        return switch (pos) {
            case BorderLayout.WEST, BorderLayout.EAST -> {
                p.setPreferredSize(new Dimension(10, 0));
                yield p;
            }
            case BorderLayout.SOUTH -> {
                p.setPreferredSize(new Dimension(0, 10));
                yield p;
            }
            default -> p;
        };
    }

    private void reload() {
        try {
            allRows = employeeController.findAll();
            applyFilters();
        } catch (Throwable ex) {
            Toast.error(this, ex.getMessage() == null ? ex.toString() : ex.getMessage());
            allRows = new ArrayList<>();
            tableModel.setRows(allRows);
        }
    }

    private void onSearch() {
        applyFilters();
    }

    private void applyFilters() {
        List<Employee> src = allRows == null ? new ArrayList<>() : allRows;
        String q = (txtSearch.getText() == null ? "" : txtSearch.getText()).trim().toLowerCase(java.util.Locale.ROOT);
        if (q.isBlank()) {
            tableModel.setRows(src);
            return;
        }
        List<Employee> out = new ArrayList<>();
        for (Employee e : src) {
            if (e == null) continue;
            String name = e.getFullName() == null ? "" : e.getFullName();
            String phone = e.getPhone() == null ? "" : e.getPhone();
            String id = String.valueOf(e.getId());
            if (com.phonestore.util.TextSearch.matches(q, name, phone, id)) out.add(e);
        }
        tableModel.setRows(out);
    }

    private void onAdd() {
        EmployeeFormDialog dlg = new EmployeeFormDialog(SwingUtilities.getWindowAncestor(this), null);
        dlg.setVisible(true);
        Employee e = dlg.getResult();
        if (e == null) return;
        try {
            employeeController.create(e);
            reload();
            Toast.info(this, "Đã thêm nhân viên");
        } catch (Exception ex) {
            Toast.error(this, ex.getMessage());
        }
    }

    private void onEdit() {
        Employee selected = getSelected();
        if (selected == null) {
            Toast.warn(this, "Vui lòng chọn 1 dòng");
            return;
        }
        // enforce role-based restrictions: only Admin may edit other employees
        try {
            UserSession s = SessionContext.getSession();
            long curId = s == null ? -1L : (s.getUserId() == null ? -1L : s.getUserId());
            boolean curIsAdmin = false;
            if (s != null) {
                for (String p : s.getPermissions()) {
                    if (p != null && p.startsWith("role:")) {
                        String rn = p.substring("role:".length()).toLowerCase();
                        if (rn.contains("admin")) { curIsAdmin = true; break; }
                    }
                }
            }
            if (!curIsAdmin && selected.getId() != curId) {
                Toast.warn(this, "Chỉ Admin được sửa thông tin nhân viên khác");
                return;
            }
        } catch (Throwable ignored) {}

        EmployeeFormDialog dlg = new EmployeeFormDialog(SwingUtilities.getWindowAncestor(this), selected);
        dlg.setVisible(true);
        Employee e = dlg.getResult();
        if (e == null) return;
        try {
            employeeController.update(e);
            reload();
            Toast.info(this, "Đã cập nhật");
        } catch (Exception ex) {
            Toast.error(this, ex.getMessage());
        }
    }

    private void onDelete() {
        Employee selected = getSelected();
        if (selected == null) {
            Toast.warn(this, "Vui lòng chọn 1 dòng");
            return;
        }
        // enforce permission: prevent deleting self; only Admin may delete other employees; prevent deleting Admin accounts
        try {
            UserSession s = SessionContext.getSession();
            long curId = s == null ? -1L : (s.getUserId() == null ? -1L : s.getUserId());
            if (selected.getId() == curId) {
                Toast.warn(this, "Bạn không thể xóa chính mình");
                return;
            }
            boolean curIsAdmin = false;
            if (s != null) {
                for (String p : s.getPermissions()) {
                    if (p != null && p.startsWith("role:")) {
                        String rn = p.substring("role:".length()).toLowerCase();
                        if (rn.contains("admin")) { curIsAdmin = true; break; }
                    }
                }
            }
            if (!curIsAdmin) {
                Toast.warn(this, "Chỉ Admin được xóa nhân viên khác");
                return;
            }
            // prevent deleting employee who has Admin account
            try {
                java.util.List<com.phonestore.model.UserAccount> accs = accountController.findAll();
                for (com.phonestore.model.UserAccount a : accs) {
                    if (a == null) continue;
                    if (a.getEmployeeId() != selected.getId()) continue;
                    if (a.getRoleId() != null) {
                        try {
                            com.phonestore.dao.NhomQuyenDao dq = new com.phonestore.dao.jdbc.NhomQuyenJdbcDao();
                            java.util.List<com.phonestore.model.NhomQuyen> roles = dq.findAll();
                            for (com.phonestore.model.NhomQuyen r : roles) {
                                if (r == null) continue;
                                if (r.getId() == a.getRoleId()) {
                                    String rn = r.getName();
                                    if (rn != null && rn.toLowerCase().contains("admin")) {
                                        Toast.warn(this, "Không thể xóa nhân viên có quyền Admin");
                                        return;
                                    }
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}

        int c = JOptionPane.showConfirmDialog(this, "Xóa nhân viên đã chọn?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;
        try {
            employeeController.delete(selected.getId());
            // remove from local cache so it disappears immediately
            allRows.removeIf(r -> r.getId() == selected.getId());
            applyFilters();
            Toast.info(this, "Đã xóa");
        } catch (Exception ex) {
            Toast.error(this, ex.getMessage());
        }
    }

    private void onDetail() {
        Employee selected = getSelected();
        if (selected == null) {
            Toast.warn(this, "Vui lòng chọn 1 dòng");
            return;
        }
        EmployeeFormDialog dlg = new EmployeeFormDialog(SwingUtilities.getWindowAncestor(this), selected);
        dlg.setReadOnly(true);
        dlg.setVisible(true);
    }

    private Employee getSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return tableModel.getAt(row);
    }

    private static final class EmployeeTableModel extends AbstractTableModel {
        private final String[] cols = {"STT", "Mã NV", "Họ tên", "Giới tính", "Ngày sinh", "SĐT", "Email", "Trạng thái"};
        private List<Employee> rows = new ArrayList<>();

        public void setRows(List<Employee> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        public Employee getAt(int row) {
            return rows.get(row);
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int column) { return cols[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Employee e = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> Integer.valueOf(rowIndex + 1); // STT (1-based)
                case 1 -> (e.getCode() == null || e.getCode().isBlank()) ? String.format("NVPS-%02d", e.getId() <= 0 ? 1 : e.getId()) : e.getCode();
                case 2 -> e.getFullName();
                case 3 -> e.getGender() == null ? "" : (e.getGender() == 1 ? "Nam" : "Nữ");
                case 4 -> e.getBirthDate();
                case 5 -> e.getPhone();
                case 6 -> e.getEmail();
                case 7 -> (e.getStatus() != null && e.getStatus() == 1) ? "Đang làm" : "Tạm Nghỉ";
                default -> "";
            };
        }
    }
}
