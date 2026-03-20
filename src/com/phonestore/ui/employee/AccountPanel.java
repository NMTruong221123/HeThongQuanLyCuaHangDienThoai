package com.phonestore.ui.employee;

import com.phonestore.controller.UserAccountController;
import com.phonestore.model.UserAccount;
import com.phonestore.ui.common.images.ImageLoader;
import com.phonestore.ui.common.toast.Toast;
import com.phonestore.ui.common.session.SessionContext;
import com.phonestore.ui.common.session.UserSession;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
// Excel functionality removed
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AccountPanel extends JPanel {

    private final UserAccountController accountController = new UserAccountController();
    private final com.phonestore.controller.EmployeeController employeeController = new com.phonestore.controller.EmployeeController();
    private final JTextField txtSearch = new JTextField();
    private final AccountTableModel tableModel = new AccountTableModel();
    private final JTable table = new JTable(tableModel);
    private List<UserAccount> allRows = new ArrayList<>();

    private final Color backgroundColor = new Color(240, 247, 250);

    public AccountPanel() {
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

        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.setOpaque(false);
        JPanel searchWrap = new JPanel(new BorderLayout(6, 0));
        searchWrap.setOpaque(false);
        // push the search box lower within the toolbar
        searchWrap.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
        searchWrap.add(new JLabel("Tìm kiếm:"), BorderLayout.WEST);
        txtSearch.setPreferredSize(new Dimension(300, 34));
        txtSearch.putClientProperty("JTextField.placeholderText", "Nhập tên đăng nhập/manv...");
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onSearch(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onSearch(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onSearch(); }
        });
        searchWrap.add(txtSearch, BorderLayout.CENTER);
        JButton btnSearch = new JButton("Tìm", ImageLoader.loadIcon("search.svg", 18, 18));
        JButton btnReset = new JButton("Reset", ImageLoader.loadIcon("refresh.svg", 18, 18));
        styleBtn(btnSearch);
        styleBtn(btnReset);
        btnSearch.setPreferredSize(new Dimension(100, 34));
        btnSearch.setFont(btnSearch.getFont().deriveFont(Font.PLAIN, 13f));
        btnReset.setPreferredSize(new Dimension(100, 34));
        btnReset.setFont(btnReset.getFont().deriveFont(Font.PLAIN, 13f));
        btnSearch.addActionListener(e -> onSearch());
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            reload();
        });

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightButtons.setOpaque(false);
        rightButtons.add(btnReset);
        rightButtons.add(btnSearch);

        right.add(searchWrap, BorderLayout.CENTER);
        right.add(rightButtons, BorderLayout.EAST);

        bar.add(left);
        bar.add(right);
        return bar;
    }

    private void onExport() {
        // Excel export removed
    }

    private void onImport() {
        // Excel import removed
    }

    private void upsertAccount(UserAccount a) {
        if (a == null) throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        // Try update first (manv is the key). If not found, fallback to create.
        try {
            accountController.update(a);
            return;
        } catch (IllegalArgumentException notFound) {
            // continue
        }
        accountController.create(a);
    }

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

    private long parseRequiredLong(String s, String key) {
        String t = trim(s);
        if (t.isBlank()) throw new IllegalArgumentException("Thiếu cột: " + key);
        try {
            return Long.parseLong(t);
        } catch (Exception e) {
            throw new IllegalArgumentException("Giá trị không hợp lệ cho cột: " + key);
        }
    }

    private String req(String s, String key) {
        String t = trim(s);
        if (t.isBlank()) throw new IllegalArgumentException("Thiếu cột: " + key);
        return t;
    }

    private JComponent buildCenter() {
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        configureTable();
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        return center;
    }

    private java.util.Map<Integer, String> roleMap = new java.util.HashMap<>();
    private java.util.Map<Long, com.phonestore.model.Employee> employeeMap = new java.util.HashMap<>();

    private void configureTable() {
        table.setFillsViewportHeight(true);
        table.setRowHeight(40);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultEditor(Object.class, null);
        if (table.getTableHeader() != null) table.getTableHeader().setReorderingAllowed(false);

        // custom renderer to color rows where linked employee is missing (deleted) or account locked
        DefaultTableCellRenderer rowRenderer = new DefaultTableCellRenderer() {
            private final Color deletedBg = new Color(255, 230, 230);
            private final Color lockedBg = new Color(240, 240, 240);
            private final Color activeBg = Color.WHITE;
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                try {
                    AccountTableModel m = (AccountTableModel) table.getModel();
                    UserAccount a = m.getAt(row);
                    boolean empExists = employeeMap != null && employeeMap.containsKey(a.getEmployeeId());
                    boolean accountLocked = a != null && a.getStatus() != null && a.getStatus() == 0;
                    if (!isSelected) {
                        if (accountLocked) c.setBackground(lockedBg);
                        else c.setBackground(empExists ? activeBg : deletedBg);
                    }
                } catch (Exception ignored) {
                    if (!isSelected) c.setBackground(backgroundColor);
                }
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
        // set as default renderer (covers cases where columns aren't created yet)
        table.setDefaultRenderer(Object.class, rowRenderer);
        // also apply to existing columns if present
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(rowRenderer);
        }
        if (table.getColumnCount() > 2) table.getColumnModel().getColumn(2).setPreferredWidth(240);
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
            allRows = accountController.findAll();
            // load role names from DB and pass to table model
            try {
                com.phonestore.dao.NhomQuyenDao dq = new com.phonestore.dao.jdbc.NhomQuyenJdbcDao();
                java.util.List<com.phonestore.model.NhomQuyen> roles = dq.findAll();
                roleMap.clear();
                for (com.phonestore.model.NhomQuyen r : roles) {
                    if (r == null) continue;
                    String n = r.getName();
                    if (n != null) n = n.trim();
                    roleMap.put(r.getId(), n);
                }
                if (roleMap.isEmpty()) {
                    // retry once
                    roles = dq.findAll();
                    for (com.phonestore.model.NhomQuyen r : roles) {
                        if (r == null) continue;
                        roleMap.put(r.getId(), r.getName());
                    }
                }
            } catch (Throwable ex) {
                roleMap.clear();
                Toast.warn(this, "Không tải được nhóm quyền từ DB: " + (ex.getMessage() == null ? ex.toString() : ex.getMessage()));
            }
            // load employees to map (for email and display data)
            try {
                java.util.List<com.phonestore.model.Employee> emps = employeeController.findAll();
                employeeMap.clear();
                for (com.phonestore.model.Employee e : emps) {
                    if (e == null) continue;
                    employeeMap.put(e.getId(), e);
                }
                if (employeeMap.isEmpty()) {
                    // retry once
                    emps = employeeController.findAll();
                    for (com.phonestore.model.Employee e : emps) {
                        if (e == null) continue;
                        employeeMap.put(e.getId(), e);
                    }
                }
            } catch (Throwable ex) {
                employeeMap.clear();
                Toast.warn(this, "Không tải được danh sách nhân viên: " + (ex.getMessage() == null ? ex.toString() : ex.getMessage()));
            }
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
        List<UserAccount> src = allRows == null ? new ArrayList<>() : allRows;
        String q = (txtSearch.getText() == null ? "" : txtSearch.getText()).trim().toLowerCase(java.util.Locale.ROOT);
        // ensure model always has role/employee maps so names show even when no search is active
        tableModel.setRoleMap(roleMap);
        tableModel.setEmployeeMap(employeeMap);
        if (q.isBlank()) {
            tableModel.setRows(src);
            return;
        }
        List<UserAccount> out = new ArrayList<>();
        for (UserAccount a : src) {
            if (a == null) continue;
            String name = a.getEmployeeName() == null ? "" : a.getEmployeeName();
            String username = a.getUsername() == null ? "" : a.getUsername();
            String role = a.getRoleId() == null ? "" : String.valueOf(a.getRoleId());
            if (com.phonestore.util.TextSearch.matches(q, name, username, role)) out.add(a);
        }
        tableModel.setRows(out);
    }

    private void onAdd() {
        // First choose an employee (only active without accounts)
        EmployeeChooserDialog chooser = new EmployeeChooserDialog(SwingUtilities.getWindowAncestor(this), -1);
        chooser.setVisible(true);
        com.phonestore.model.Employee picked = chooser.getSelected();
        if (picked == null) return;
        AccountFormDialog dlg = new AccountFormDialog(SwingUtilities.getWindowAncestor(this), picked);
        dlg.setVisible(true);
        UserAccount a = dlg.getResult();
        if (a == null) return;
        try {
            accountController.create(a);
            reload();
            Toast.info(this, "Đã thêm tài khoản");
        } catch (Exception ex) {
            Toast.error(this, ex.getMessage());
        }
    }

    private void onEdit() {
        UserAccount selected = getSelected();
        if (selected == null) {
            Toast.warn(this, "Vui lòng chọn 1 dòng");
            return;
        }
        // enforce role-based restrictions: managers cannot edit other manager accounts
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
            // Only admin may edit other users. Non-admins may edit only their own account.
            if (!curIsAdmin && selected.getEmployeeId() != curId) {
                Toast.warn(this, "Chỉ Admin được sửa tài khoản của người khác");
                return;
            }
        } catch (Throwable ignored) {}
        AccountFormDialog dlg = new AccountFormDialog(SwingUtilities.getWindowAncestor(this), selected);
        dlg.setVisible(true);
        UserAccount a = dlg.getResult();
        if (a == null) return;
        try {
            accountController.update(a);
            reload();
            Toast.info(this, "Đã cập nhật");
        } catch (Exception ex) {
            Toast.error(this, ex.getMessage());
        }
    }

    private void onDetail() {
        UserAccount selected = getSelected();
        if (selected == null) {
            Toast.warn(this, "Vui lòng chọn 1 dòng");
            return;
        }
        AccountFormDialog dlg = new AccountFormDialog(SwingUtilities.getWindowAncestor(this), selected, true);
        dlg.setVisible(true);
        // read-only: no update performed
    }

    private void onDelete() {
        UserAccount selected = getSelected();
        if (selected == null) {
            Toast.warn(this, "Vui lòng chọn 1 dòng");
            return;
        }
        // prevent deleting own account and managers deleting other managers
        try {
            UserSession s = SessionContext.getSession();
            long curId = s == null ? -1L : (s.getUserId() == null ? -1L : s.getUserId());
            // prevent deleting own account
            if (selected.getEmployeeId() == curId) {
                Toast.warn(this, "Bạn không thể xóa tài khoản của chính mình");
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
            // Only admin can delete other accounts
            if (!curIsAdmin) {
                Toast.warn(this, "Chỉ Admin được xóa tài khoản khác");
                return;
            }
            // prevent deleting admin account (there should be a single admin)
            boolean targetIsAdmin = false;
            try {
                if (selected.getRoleId() != null) {
                    String rn = roleMap == null ? null : roleMap.get(selected.getRoleId());
                    if (rn != null && rn.toLowerCase().contains("admin")) targetIsAdmin = true;
                }
            } catch (Throwable ignored) {}
            if (targetIsAdmin) {
                Toast.warn(this, "Không thể xóa tài khoản Admin");
                return;
            }
        } catch (Throwable ignored) {}
        int c = JOptionPane.showConfirmDialog(this, "Hủy tài khoản đã chọn?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;
        try {
            accountController.delete(selected.getEmployeeId());
            reload();
            Toast.info(this, "Đã hủy");
        } catch (Exception ex) {
            String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            // If delete failed due to FK constraint, offer to disable the account instead
            if (msg.toLowerCase().contains("không thể xóa tài khoản")
                    || msg.toLowerCase().contains("foreign key")
                    || msg.toLowerCase().contains("constraint")) {
                int r = JOptionPane.showConfirmDialog(this,
                        "Không thể xóa tài khoản vì có dữ liệu tham chiếu (ví dụ: phiếu nhập/phiếu xuất).\nBạn có muốn KHÓA (vô hiệu hóa) tài khoản này thay vì xóa?",
                        "Không thể xóa", JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) {
                    try {
                        selected.setStatus(0);
                        accountController.update(selected);
                        reload();
                        Toast.info(this, "Đã khóa tài khoản thay cho xóa");
                    } catch (Exception e2) {
                        Toast.error(this, e2.getMessage());
                    }
                }
                return;
            }
            Toast.error(this, msg);
        }
    }

    private UserAccount getSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return tableModel.getAt(row);
    }

    private static final class AccountTableModel extends AbstractTableModel {
        private final String[] cols = {"Mã nhân viên", "Tên nhân viên", "Tên đăng nhập", "Email", "Nhóm quyền", "Trạng thái"};
        private List<UserAccount> rows = new ArrayList<>();
        private java.util.Map<Integer, String> roleMap = new java.util.HashMap<>();
        private java.util.Map<Long, com.phonestore.model.Employee> employeeMap = new java.util.HashMap<>();

        public void setRoleMap(java.util.Map<Integer, String> m) {
            this.roleMap = m == null ? new java.util.HashMap<>() : new java.util.HashMap<>(m);
        }

        public void setEmployeeMap(java.util.Map<Long, com.phonestore.model.Employee> m) {
            this.employeeMap = m == null ? new java.util.HashMap<>() : new java.util.HashMap<>(m);
        }

        public void setRows(List<UserAccount> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        public UserAccount getAt(int row) {
            return rows.get(row);
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int column) { return cols[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            UserAccount a = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> {
                    // employee code
                    com.phonestore.model.Employee e = employeeMap == null ? null : employeeMap.get(a.getEmployeeId());
                    if (e != null) {
                        String code = e.getCode() == null ? String.valueOf(e.getId()) : e.getCode();
                        yield code;
                    }
                    // fallback: try to format a code from id
                    yield String.format("NVPS-%02d", a.getEmployeeId() <= 0 ? 0 : a.getEmployeeId());
                }
                case 1 -> {
                    // employee name
                    com.phonestore.model.Employee e = employeeMap == null ? null : employeeMap.get(a.getEmployeeId());
                    if (e != null) {
                        String name = e.getFullName() == null ? "" : e.getFullName();
                        yield name;
                    }
                    yield a.getEmployeeName() == null ? "" : a.getEmployeeName();
                }
                case 2 -> a.getUsername();
                case 3 -> {
                    // prefer employee email if available, fallback to account email
                    com.phonestore.model.Employee e = employeeMap == null ? null : employeeMap.get(a.getEmployeeId());
                    if (e != null) {
                        String em = e.getEmail();
                        if (em != null && !em.isBlank()) yield em;
                    }
                    yield a.getEmail() == null ? "" : a.getEmail();
                }
                case 4 -> {
                    Integer rid = a.getRoleId();
                    if (rid == null) yield "";
                    String name = roleMap == null ? null : roleMap.get(rid);
                    yield name == null ? String.valueOf(rid) : name;
                }
                case 5 -> {
                    // If linked employee record is missing, show special status
                    com.phonestore.model.Employee e = employeeMap == null ? null : employeeMap.get(a.getEmployeeId());
                    if (e == null) yield "ĐÃ XÓA NV";
                    yield (a.getStatus() != null && a.getStatus() == 1) ? "Hoạt động" : "Khóa";
                }
                default -> "";
            };
        }
    }
}
