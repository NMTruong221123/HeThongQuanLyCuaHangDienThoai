package com.phonestore.ui.product;

import com.phonestore.model.AttributeItem;
import com.phonestore.model.AttributeType;
import com.phonestore.controller.AttributeController;
import com.phonestore.ui.common.toast.Toast;
import com.phonestore.ui.common.images.ImageLoader;
// Excel functionality removed

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
// Excel functionality removed
import java.util.Map;

public class AttributePanel extends JPanel {

    private final AttributeController attributeController = new AttributeController();

    private final JComboBox<AttributeType> cboType = new JComboBox<>(AttributeType.values());
    private final AttributeTableModel tableModel = new AttributeTableModel();
    private final JTable table = new JTable(tableModel);

    private final Color backgroundColor = new Color(240, 247, 250);

    public AttributePanel() {
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

        // left: type selector
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        left.setOpaque(false);
        left.add(new JLabel("Loại:"));
        cboType.setPreferredSize(new Dimension(220, 32));
        left.add(cboType);
        bar.add(left, BorderLayout.WEST);

        // center: large icon buttons (center aligned)
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        center.setOpaque(false);
        JButton btnAdd = new JButton("THÊM", ImageLoader.loadIcon("add.svg", 42, 42));
        JButton btnEdit = new JButton("SỬA", ImageLoader.loadIcon("edit.svg", 42, 42));
        JButton btnDelete = new JButton("XÓA", ImageLoader.loadIcon("delete.svg", 42, 42));

        styleToolbarIconBtn(btnAdd);
        styleToolbarIconBtn(btnEdit);
        styleToolbarIconBtn(btnDelete);

        center.add(btnAdd);
        center.add(btnEdit);
        center.add(btnDelete);
        bar.add(center, BorderLayout.CENTER);

        cboType.addActionListener(e -> reload());
        btnAdd.addActionListener(e -> onAdd());
        btnEdit.addActionListener(e -> onEdit());
        btnDelete.addActionListener(e -> onDelete());

        // right: small controls (refresh)
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 18));
        right.setOpaque(false);
        JButton btnRefresh = new JButton("Làm mới", ImageLoader.loadIcon("refresh.svg", 18, 18));
        styleToolbarButton(btnRefresh);
        btnRefresh.setPreferredSize(new Dimension(120, 34));
        btnRefresh.setFont(btnRefresh.getFont().deriveFont(Font.PLAIN, 13f));
        btnRefresh.addActionListener(e -> reload());
        right.add(btnRefresh);
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

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(center);
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
        AttributeType type = (AttributeType) cboType.getSelectedItem();
        tableModel.setRows(attributeController.findByType(type));
    }

    private static String headerDisplayName(AttributeType type) {
        if (type == null) return "";
        // use more formal "Thương hiệu" instead of label "Hãng"
        if (type == AttributeType.BRAND) return "Thương hiệu";
        return type.getLabel();
    }

    private void onAdd() {
        AttributeType type = (AttributeType) cboType.getSelectedItem();
        String headerName = headerDisplayName(type);
        String title = "Quản lý " + headerName;
        String header = headerName + " Sản Phẩm";
        String name = new NamedInputDialog(SwingUtilities.getWindowAncestor(this), title, header, "Tên " + type.getLabel(), null).showDialog();
        if (name == null) return;
        try {
            attributeController.create(type, name);
            reload();
            Toast.info(this, "Đã thêm");
        } catch (Exception ex) {
            Toast.error(this, ex.getMessage());
        }
    }

    private void onEdit() {
        AttributeType type = (AttributeType) cboType.getSelectedItem();
        AttributeItem item = getSelected();
        if (item == null) {
            Toast.warn(this, "Vui lòng chọn 1 dòng");
            return;
        }
        String headerName = headerDisplayName(type);
        String title = "Quản lý " + headerName;
        String header = headerName + " Sản Phẩm";
        String name = new NamedInputDialog(SwingUtilities.getWindowAncestor(this), title, header, "Tên " + type.getLabel(), item.getName()).showDialog();
        if (name == null) return;
        try {
            attributeController.update(item.getId(), type, name);
            reload();
            Toast.info(this, "Đã cập nhật");
        } catch (Exception ex) {
            Toast.error(this, ex.getMessage());
        }
    }

    private void onDelete() {
        AttributeType type = (AttributeType) cboType.getSelectedItem();
        AttributeItem item = getSelected();
        if (item == null) {
            Toast.warn(this, "Vui lòng chọn 1 dòng");
            return;
        }
        int c = JOptionPane.showConfirmDialog(this, "Xóa mục đã chọn?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;
        attributeController.delete(item.getId(), type);
        reload();
        Toast.info(this, "Đã xóa");
    }

    private void onExport() {
        // Excel export removed
    }

    private void onImport() {
        // Excel import removed
    }

    private AttributeItem getSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return tableModel.getAt(row);
    }

    private static final class AttributeTableModel extends AbstractTableModel {
        private final String[] cols = {"STT", "Tên"};
        private List<AttributeItem> rows = new ArrayList<>();

        public void setRows(List<AttributeItem> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        public AttributeItem getAt(int row) {
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
            AttributeItem i = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> rowIndex + 1; // STT (1-based index)
                case 1 -> i.getName();
                default -> "";
            };
        }
    }

    private static final class NamedInputDialog extends JDialog {
        private String result = null;
        private final JTextField tf = new JTextField(30);

        public NamedInputDialog(Window owner, String title, String headerText, String label, String initial) {
            super(owner, title, ModalityType.APPLICATION_MODAL);
            init(headerText, label, initial);
            pack();
            setLocationRelativeTo(owner);
        }

        private void init(String headerText, String labelText, String initial) {
            setLayout(new BorderLayout());

            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(new Color(0, 123, 192));
            header.setPreferredSize(new Dimension(400, 70));
            JLabel title = new JLabel(headerText == null ? "" : headerText.toUpperCase(), SwingConstants.CENTER);
            title.setForeground(Color.white);
            title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
            header.add(title, BorderLayout.CENTER);
            add(header, BorderLayout.NORTH);

            JPanel body = new JPanel(new GridBagLayout());
            body.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(6, 6, 6, 6);
            JLabel lbl = new JLabel(labelText == null ? "" : labelText);
            body.add(lbl, gbc);

            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            tf.setText(initial == null ? "" : initial);
            body.add(tf, gbc);

            add(body, BorderLayout.CENTER);

            JPanel foot = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
            JButton ok = new JButton("OK");
            JButton cancel = new JButton("Hủy");
            ok.addActionListener(e -> onOk());
            cancel.addActionListener(e -> onCancel());


            // style buttons: confirm color depends on mode (add = green, edit = blue), cancel = red
            ok.setPreferredSize(new Dimension(180, 40));
            Color okColor = (initial == null || initial.isBlank()) ? new Color(40, 167, 69) : new Color(23, 142, 201); // green for add, blue for edit
            ok.setBackground(okColor);
            ok.setForeground(Color.WHITE);
            ok.setOpaque(true);
            ok.setBorderPainted(false);
            ok.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            cancel.setPreferredSize(new Dimension(140, 40));
            cancel.setBackground(new Color(229, 57, 53));
            cancel.setForeground(Color.WHITE);
            cancel.setOpaque(true);
            cancel.setBorderPainted(false);
            cancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            foot.add(ok);
            foot.add(cancel);
            add(foot, BorderLayout.SOUTH);

            getRootPane().setDefaultButton(ok);
        }

        private void onOk() {
            String v = tf.getText();
            if (v == null || v.isBlank()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập tên", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
            result = v.trim();
            setVisible(false);
            dispose();
        }

        private void onCancel() {
            result = null;
            setVisible(false);
            dispose();
        }

        public String showDialog() {
            setVisible(true);
            return result;
        }
    }
}
