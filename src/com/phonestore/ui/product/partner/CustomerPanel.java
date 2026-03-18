package com.phonestore.ui.product.partner;

import com.phonestore.controller.CustomerController;
import com.phonestore.model.Customer;
import com.phonestore.ui.common.images.ImageLoader;
import com.phonestore.ui.common.toast.Toast;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
// Excel functionality removed
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomerPanel extends JPanel {

    private final CustomerController customerController = new CustomerController();

    private final JTextField txtSearch = new JTextField();
    private final CustomerTableModel tableModel = new CustomerTableModel();
    private final JTable table = new JTable(tableModel);
        private List<Customer> allRows = new ArrayList<>();

    private final Color backgroundColor = new Color(240, 247, 250);

    public CustomerPanel() {
        setBackground(backgroundColor);
        setLayout(new BorderLayout(0, 0));
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildPadding(BorderLayout.WEST), BorderLayout.WEST);
        add(buildPadding(BorderLayout.EAST), BorderLayout.EAST);
        add(buildPadding(BorderLayout.SOUTH), BorderLayout.SOUTH);
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
        txtSearch.putClientProperty("JTextField.placeholderText", "Nhập tên/SĐT/email/địa chỉ...");
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
        });

        JButton btnRefresh = new JButton("Làm mới", ImageLoader.loadIcon("refresh.svg", 18, 18));
        styleToolbarButton(btnRefresh);
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

    private void onImport() {
        // Excel import removed
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

    private void configureTable() {
        table.setFillsViewportHeight(true);
        table.setRowHeight(40);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultEditor(Object.class, null);
        if (table.getTableHeader() != null) {
            table.getTableHeader().setReorderingAllowed(false);
        }

        // Single renderer applied to all columns so inactive rows render consistently
        DefaultTableCellRenderer rowRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                try {
                    // alignment: ID and Phone centered; others left
                    if (column == 0 || column == 2 || column == 5) {
                        setHorizontalAlignment(SwingConstants.CENTER);
                    } else {
                        setHorizontalAlignment(SwingConstants.LEFT);
                    }

                    // If selected, keep the selection colors provided by the table look-and-feel
                    if (isSelected) {
                        return c;
                    }

                    int modelRow = table.convertRowIndexToModel(row);
                    Customer cust = tableModel.getAt(modelRow);
                    if (cust != null && cust.getStatus() != null && cust.getStatus() == 0) {
                        c.setBackground(new Color(245, 245, 245));
                        c.setForeground(new Color(110, 110, 110));
                    } else {
                        c.setBackground(table.getBackground());
                        c.setForeground(table.getForeground());
                    }
                } catch (Exception ex) {
                    c.setBackground(table.getBackground());
                    c.setForeground(table.getForeground());
                }
                return c;
            }
        };

        // apply to every column so look is consistent
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(rowRenderer);
        }

        if (table.getColumnCount() > 1) {
            table.getColumnModel().getColumn(1).setPreferredWidth(220);
        }
        if (table.getColumnCount() > 3) table.getColumnModel().getColumn(3).setPreferredWidth(220);
    }

    private void styleToolbarButton(JButton b) {
        if (b == null) return;
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.putClientProperty("JComponent.sizeVariant", "regular");
    }

    private void styleToolbarIconBtn(JButton b) {
        if (b == null) return;
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

    private JComponent buildPadding(String pos) {
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
        allRows = customerController.findAll();
        applyFilters();
    }

    private void onSearch() {
        applyFilters();
    }

    private void applyFilters() {
        List<Customer> src = allRows == null ? new ArrayList<>() : allRows;
        String q = (txtSearch.getText() == null ? "" : txtSearch.getText()).trim().toLowerCase(java.util.Locale.ROOT);
        if (q.isBlank()) {
            tableModel.setRows(src);
            return;
        }
        List<Customer> out = new ArrayList<>();
        for (Customer c : src) {
            if (c == null) continue;
            String name = c.getName() == null ? "" : c.getName();
            String phone = c.getPhone() == null ? "" : c.getPhone();
            String email = c.getEmail() == null ? "" : c.getEmail();
            String addr = c.getAddress() == null ? "" : c.getAddress();
            if (com.phonestore.util.TextSearch.matches(q, name, phone, email, addr)) out.add(c);
        }
        tableModel.setRows(out);
    }

    private void onAdd() {
        CustomerFormDialog dlg = new CustomerFormDialog(SwingUtilities.getWindowAncestor(this), null);
        dlg.setVisible(true);
        Customer c = dlg.getResult();
        if (c != null) {
            try {
                customerController.create(c);
                reload();
                // select the last row (newly added) and scroll to it
                SwingUtilities.invokeLater(() -> {
                    int last = table.getRowCount() - 1;
                    if (last >= 0) {
                        table.setRowSelectionInterval(last, last);
                        table.scrollRectToVisible(table.getCellRect(last, 0, true));
                    }
                });
                Toast.info(this, "Đã thêm khách hàng");
            } catch (Exception ex) {
                Toast.error(this, ex.getMessage());
            }
        }
    }

    private void onEdit() {
        Customer selected = getSelected();
        if (selected == null) {
            Toast.warn(this, "Vui lòng chọn 1 dòng");
            return;
        }

        CustomerFormDialog dlg = new CustomerFormDialog(SwingUtilities.getWindowAncestor(this), selected);
        dlg.setVisible(true);
        Customer c = dlg.getResult();
        if (c != null) {
            try {
                customerController.update(c);
                reload();
                Toast.info(this, "Đã cập nhật");
            } catch (Exception ex) {
                Toast.error(this, ex.getMessage());
            }
        }
    }

    private void onDetail() {
        Customer selected = getSelected();
        if (selected == null) {
            Toast.warn(this, "Vui lòng chọn 1 dòng");
            return;
        }

        CustomerFormDialog dlg = new CustomerFormDialog(SwingUtilities.getWindowAncestor(this), selected, true);
        dlg.setVisible(true);
        // read-only dialog: no changes expected
    }

    private void onDelete() {
        Customer selected = getSelected();
        if (selected == null) {
            Toast.warn(this, "Vui lòng chọn 1 dòng");
            return;
        }
        int c = JOptionPane.showConfirmDialog(this, "Xóa khách hàng đã chọn?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;

        try {
            customerController.delete(selected.getId());
            reload();
            Toast.info(this, "Đã xóa");
        } catch (Exception ex) {
            Toast.error(this, ex.getMessage());
        }
    }

    private Customer getSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return tableModel.getAt(row);
    }

    private static final class CustomerTableModel extends AbstractTableModel {

        private final String[] cols = {"ID", "Tên", "SĐT", "Email", "Địa chỉ", "Trạng thái"};
        private List<Customer> rows = new ArrayList<>();

        public void setRows(List<Customer> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        public Customer getAt(int row) {
            return rows.get(row);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return cols.length;
        }

        @Override
        public String getColumnName(int column) {
            return cols[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Customer c = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> c.getId();
                case 1 -> c.getName();
                case 2 -> c.getPhone();
                case 3 -> c.getEmail();
                case 4 -> c.getAddress();
                case 5 -> {
                    Integer st = c.getStatus();
                    if (st == null) yield "";
                    yield st == 1 ? "Hoạt động" : "Ngưng";
                }
                default -> "";
            };
        }
    }
}
