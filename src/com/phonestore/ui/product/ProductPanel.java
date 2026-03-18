package com.phonestore.ui.product;

import com.phonestore.model.Product;
import com.phonestore.controller.ProductController;
import com.phonestore.ui.common.toast.Toast;
import com.phonestore.ui.common.images.ImageLoader;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
// Excel functionality removed
import java.util.Map;

public class ProductPanel extends JPanel {

    private final ProductController productController = new ProductController();

    private final JTextField txtSearch = new JTextField();
    private final ProductTableModel tableModel = new ProductTableModel();
    private final JTable table = new JTable(tableModel);
    private List<Product> allRows = new ArrayList<>();

    private final Color backgroundColor = new Color(240, 247, 250);

    public ProductPanel() {
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

        JComboBox<String> cbAll = new JComboBox<>(new String[] {"Tất cả"});
        cbAll.setPreferredSize(new Dimension(140, 34));

        txtSearch.setPreferredSize(new Dimension(320, 34));
        txtSearch.putClientProperty("JTextField.placeholderText", "Nhập tên sản phẩm...");
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

        right.add(cbAll);
        right.add(txtSearch);
        right.add(btnRefresh);

        bar.add(left);
        bar.add(right);
        return bar;
    }

    private JComponent buildCenter() {
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);

        configureTable();
        JScrollPane sp = new JScrollPane(table);
        center.add(sp, BorderLayout.CENTER);
        return center;
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

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (i != 1) {
                table.getColumnModel().getColumn(i).setCellRenderer(center);
            }
        }
        if (table.getColumnCount() > 1) {
            table.getColumnModel().getColumn(1).setPreferredWidth(220);
        }
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

    private void reload() {
        try {
            allRows = productController.findAll();
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
        List<Product> src = allRows == null ? new ArrayList<>() : allRows;
        String q = (txtSearch.getText() == null ? "" : txtSearch.getText()).trim().toLowerCase(java.util.Locale.ROOT);
        if (q.isBlank()) {
            tableModel.setRows(src);
            return;
        }
        List<Product> out = new ArrayList<>();
        for (Product p : src) {
            if (p == null) continue;
            String name = p.getName() == null ? "" : p.getName();
            String id = String.valueOf(p.getId());
            if (com.phonestore.util.TextSearch.matches(q, name, id)) out.add(p);
        }
        tableModel.setRows(out);
    }

    private void onAdd() {
        ProductFormDialog dlg = new ProductFormDialog(SwingUtilities.getWindowAncestor(this), null);
        dlg.setVisible(true);
        if (dlg.isChanged()) {
            reload();
        }
    }

    private void onDetail() {
        Product selected = getSelected();
        if (selected == null) {
            Toast.warn(this, "Vui lòng chọn 1 dòng");
            return;
        }
        ProductFormDialog dlg = new ProductFormDialog(SwingUtilities.getWindowAncestor(this), selected);
        dlg.setReadOnly(true);
        dlg.setVisible(true);
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
        String v = trim(s);
        if (v.isBlank()) return 0;
        try {
            return Long.parseLong(v);
        } catch (Exception e) {
            return 0;
        }
    }

    private long parseRequiredLong(String s, String key) {
        String v = trim(s);
        if (v.isBlank()) throw new IllegalArgumentException(key + " required");
        return Long.parseLong(v);
    }

    private Double parseRequiredDouble(String s, String key) {
        String v = trim(s);
        if (v.isBlank()) throw new IllegalArgumentException(key + " required");
        return Double.parseDouble(v);
    }

    private String req(String s, String key) {
        String v = trim(s);
        if (v.isBlank()) throw new IllegalArgumentException(key + " required");
        return v;
    }

    private void onEdit() {
        Product selected = getSelected();
        if (selected == null) {
            Toast.warn(this, "Vui lòng chọn 1 dòng");
            return;
        }

        ProductFormDialog dlg = new ProductFormDialog(SwingUtilities.getWindowAncestor(this), selected);
        dlg.setVisible(true);
        if (dlg.isChanged()) {
            reload();
        }
    }

    private void onDelete() {
        Product selected = getSelected();
        if (selected == null) {
            Toast.warn(this, "Vui lòng chọn 1 dòng");
            return;
        }
        int c = JOptionPane.showConfirmDialog(this, "Xóa sản phẩm đã chọn?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            productController.delete(selected.getId());
            reload();
            Toast.info(this, "Đã xóa");
        } catch (Exception ex) {
            Toast.error(this, ex.getMessage());
        }
    }

    private Product getSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return tableModel.getAt(row);
    }

    private static final class ProductTableModel extends AbstractTableModel {

        private final String[] cols = {"ID", "Tên", "Tồn", "Xuất xứ", "Hãng", "Khu vực", "Trạng thái"};

        private List<Product> rows = new ArrayList<>();

        public void setRows(List<Product> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        public Product getAt(int row) {
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
            Product p = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> p.getId();
                case 1 -> p.getName();
                case 2 -> p.getStock() == null ? 0 : p.getStock();
                case 3 -> (p.getOriginName() != null && !p.getOriginName().isBlank()) ? p.getOriginName() : p.getOriginId();
                case 4 -> (p.getBrandName() != null && !p.getBrandName().isBlank()) ? p.getBrandName() : p.getBrandId();
                case 5 -> (p.getZoneName() != null && !p.getZoneName().isBlank()) ? p.getZoneName() : p.getZoneId();
                case 6 -> (p.getStatus() != null && p.getStatus() == 1) ? "Đang kinh doanh" : "Ngừng";
                default -> "";
            };
        }
    }
}
