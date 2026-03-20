package com.phonestore.ui.inventory;

import com.phonestore.model.WarehouseZone;
import com.phonestore.controller.WarehouseZoneController;
import com.phonestore.ui.common.toast.Toast;
import com.phonestore.ui.common.images.ImageLoader;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WarehouseZonePanel extends JPanel {

    private final WarehouseZoneController zoneController = new WarehouseZoneController();

    private final JTextField txtSearch = new JTextField();
    private final ZoneTableModel tableModel = new ZoneTableModel();
    private final JTable table = new JTable(tableModel);
    private List<WarehouseZone> allRows = new ArrayList<>();
    private final Map<Long, Integer> zoneStock = new HashMap<>();

    private final Color backgroundColor = new Color(240, 247, 250);

    public WarehouseZonePanel() {
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
        JPanel bar = new JPanel(new BorderLayout());
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
        bar.add(left, BorderLayout.WEST);

        btnAdd.addActionListener(e -> onAdd());
        btnEdit.addActionListener(e -> onEdit());
        btnDelete.addActionListener(e -> onDelete());
        btnDetail.addActionListener(e -> onView());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 18));
        right.setOpaque(false);
        right.add(new JLabel("Tìm kiếm:"));
        txtSearch.setPreferredSize(new Dimension(200, 36));
        txtSearch.putClientProperty("JTextField.placeholderText", "Nhập ID/tên/ghi chú...");
        // add internal left padding so text/placeholder isn't overlapped by nearby icons
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
            txtSearch.getBorder(), BorderFactory.createEmptyBorder(0, 8, 0, 0)));
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onSearch(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onSearch(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onSearch(); }
        });
        right.add(txtSearch);

        JButton btnReset = new JButton("Reset", ImageLoader.loadIcon("refresh.svg", 18, 18));
        styleToolbarButton(btnReset);
        btnReset.setPreferredSize(new Dimension(110, 34));
        btnReset.setFont(btnReset.getFont().deriveFont(Font.PLAIN, 13f));
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            reload();
        });

        JButton btnSearch = new JButton("Tìm", ImageLoader.loadIcon("search.svg", 18, 18));
        styleToolbarButton(btnSearch);
        btnSearch.setPreferredSize(new Dimension(100, 34));
        btnSearch.setFont(btnSearch.getFont().deriveFont(Font.PLAIN, 13f));
        btnSearch.addActionListener(e -> onSearch());

        right.add(btnReset);
        right.add(btnSearch);
        bar.add(right, BorderLayout.EAST);
        return bar;
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
        // Custom renderer: center text and dim rows that are inactive (trangthai = 0)
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                int modelRow = table.convertRowIndexToModel(row);
                try {
                    com.phonestore.model.WarehouseZone z = tableModel.getAt(modelRow);
                    if (isSelected) {
                        c.setBackground(table.getSelectionBackground());
                        c.setForeground(table.getSelectionForeground());
                    } else if (z != null && z.getStatus() != null && z.getStatus() == 0) {
                        c.setBackground(new Color(250, 250, 250));
                        c.setForeground(Color.GRAY);
                    } else {
                        c.setBackground(Color.WHITE);
                        c.setForeground(Color.BLACK);
                    }
                } catch (Exception ex) {
                    // fallback to defaults
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        });

        // make ID column narrower and well-centered to avoid visual misalignment
        if (table.getColumnCount() > 0) {
            try {
                table.getColumnModel().getColumn(0).setPreferredWidth(60);
                table.getColumnModel().getColumn(0).setMaxWidth(80);
                table.getColumnModel().getColumn(0).setMinWidth(50);
            } catch (Exception ignored) {
            }
        }

        // make ID column narrower and well-centered to avoid visual misalignment
        if (table.getColumnCount() > 0) {
            try {
                table.getColumnModel().getColumn(0).setPreferredWidth(60);
                table.getColumnModel().getColumn(0).setMaxWidth(80);
                table.getColumnModel().getColumn(0).setMinWidth(50);
            } catch (Exception ignored) {
            }
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
        allRows = zoneController.findAll();
        loadZoneStockCounts();
        applyFilters();
    }

    private void loadZoneStockCounts() {
        zoneStock.clear();
        String sql = "SELECT khuvuckho, SUM(soluongton) AS total FROM sanpham GROUP BY khuvuckho";
        try (java.sql.Connection c = com.phonestore.config.JDBCUtil.getConnectionSilent();
             java.sql.PreparedStatement ps = c == null ? null : c.prepareStatement(sql);
             java.sql.ResultSet rs = ps == null ? null : ps.executeQuery()) {
            if (c == null) return;
            while (rs.next()) {
                int zoneId = rs.getInt("khuvuckho");
                int total = rs.getInt("total");
                zoneStock.put((long) zoneId, total);
            }
        } catch (Exception ex) {
            // ignore and leave counts empty
        }
    }

    private void onSearch() {
        applyFilters();
    }

    private void applyFilters() {
        List<WarehouseZone> src = allRows == null ? new ArrayList<>() : allRows;
        String q = (txtSearch.getText() == null ? "" : txtSearch.getText()).trim().toLowerCase(java.util.Locale.ROOT);
        if (q.isBlank()) {
            tableModel.setRows(src);
            return;
        }
        List<WarehouseZone> out = new ArrayList<>();
        for (WarehouseZone z : src) {
            if (z == null) continue;
            String name = z.getName() == null ? "" : z.getName();
            String note = z.getNote() == null ? "" : z.getNote();
            String id = String.valueOf(z.getId());
            if (com.phonestore.util.TextSearch.matches(q, name, note, id)) out.add(z);
        }
        tableModel.setRows(out);
    }

    private void onAdd() {
        ZoneFormDialog dlg = new ZoneFormDialog(SwingUtilities.getWindowAncestor(this), null);
        dlg.setVisible(true);
        WarehouseZone z = dlg.getResult();
        if (z != null) {
            try {
                zoneController.create(z);
                reload();
                Toast.info(this, "Đã thêm khu vực");
            } catch (Exception ex) {
                Toast.error(this, ex.getMessage());
            }
        }
    }

    private void onEdit() {
        WarehouseZone selected = getSelected();
        if (selected == null) {
            Toast.warn(this, "Vui lòng chọn 1 dòng");
            return;
        }
        ZoneFormDialog dlg = new ZoneFormDialog(SwingUtilities.getWindowAncestor(this), selected);
        dlg.setVisible(true);
        WarehouseZone z = dlg.getResult();
        if (z != null) {
            try {
                zoneController.update(z);
                reload();
                Toast.info(this, "Đã cập nhật");
            } catch (Exception ex) {
                Toast.error(this, ex.getMessage());
            }
        }
    }

    private void onView() {
        WarehouseZone selected = getSelected();
        if (selected == null) {
            Toast.warn(this, "Vui lòng chọn 1 dòng");
            return;
        }
        ZoneFormDialog dlg = new ZoneFormDialog(SwingUtilities.getWindowAncestor(this), selected, true);
        dlg.setVisible(true);
        // do not update anything in view mode
    }

    private void onDelete() {
        WarehouseZone selected = getSelected();
        if (selected == null) {
            Toast.warn(this, "Vui lòng chọn 1 dòng");
            return;
        }
        int c = JOptionPane.showConfirmDialog(this, "Xóa khu vực đã chọn?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;
        zoneController.delete(selected.getId());
        reload();
        Toast.info(this, "Đã xóa");
    }

    private WarehouseZone getSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return tableModel.getAt(row);
    }

    private final class ZoneTableModel extends AbstractTableModel {
        private final String[] cols = {"ID", "Tên", "Ghi chú", "Số lượng", "Trạng thái"};
        private List<WarehouseZone> rows = new ArrayList<>();

        public void setRows(List<WarehouseZone> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        public WarehouseZone getAt(int row) {
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
            WarehouseZone z = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> z.getId();
                case 1 -> z.getName();
                case 2 -> z.getNote();
                case 3 -> {
                    Integer cnt = zoneStock.get(z.getId());
                    yield cnt == null ? 0 : cnt;
                }
                case 4 -> (z.getStatus() != null && z.getStatus() == 1) ? "Hoạt động" : "Ngừng";
                default -> "";
            };
        }
    }
}
