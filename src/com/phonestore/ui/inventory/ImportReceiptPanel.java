package com.phonestore.ui.inventory;

import com.phonestore.controller.EmployeeController;
import com.phonestore.controller.ImportReceiptController;
import com.phonestore.controller.SupplierController;
import com.phonestore.model.Employee;
import com.phonestore.model.ImportReceipt;
import com.phonestore.model.Supplier;
import com.phonestore.ui.common.images.ImageLoader;
import com.phonestore.ui.common.toast.Toast;
import com.phonestore.util.MoneyVND;
import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.optionalusertools.DateChangeListener;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Map;

public class ImportReceiptPanel extends JPanel {

    private final ImportReceiptController controller = new ImportReceiptController();
    private final SupplierController supplierController = new SupplierController();
    private final EmployeeController employeeController = new EmployeeController();

    private final JComboBox<String> cbStatus = new JComboBox<>(new String[] {"Tất cả", "Hoạt động", "Hủy"});
    private final JTextField txtSearch = new JTextField();

    private final JComboBox<Supplier> cbSupplier = new JComboBox<>();
    private final JComboBox<Employee> cbEmployee = new JComboBox<>();
    private final DatePicker dpFromDate = createDatePicker();
    private final DatePicker dpToDate = createDatePicker();
    private final JTextField txtFromAmount = new JTextField();
    private final JTextField txtToAmount = new JTextField();

    private final ReceiptTableModel tableModel = new ReceiptTableModel();
    private final JTable table = new JTable(tableModel);

    private final Color backgroundColor = new Color(240, 247, 250);
    private List<ImportReceipt> allRows = new ArrayList<>();

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ImportReceiptPanel() {
        setBackground(backgroundColor);
        setLayout(new BorderLayout(0, 0));
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(pad(BorderLayout.WEST), BorderLayout.WEST);
        add(pad(BorderLayout.EAST), BorderLayout.EAST);
        add(pad(BorderLayout.SOUTH), BorderLayout.SOUTH);

        configureFilters();
        loadFilterData();
        wireFilterEvents();
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
        JButton btnDetail = new JButton("CHI TIẾT", ImageLoader.loadIcon("detail.svg", 42, 42));
        JButton btnCancel = new JButton("HỦY PHIẾU", ImageLoader.loadIcon("cancel.svg", 42, 42));

        styleToolbarIconBtn(btnAdd);
        styleToolbarIconBtn(btnDetail);
        styleToolbarIconBtn(btnCancel);

        left.add(btnAdd);
        left.add(btnDetail);
        left.add(btnCancel);
        bar.add(left, BorderLayout.WEST);

        btnAdd.addActionListener(e -> onAdd());
        btnDetail.addActionListener(e -> onDetail());
        btnCancel.addActionListener(e -> onCancel());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 18));
        right.setOpaque(false);
        cbStatus.setPreferredSize(new Dimension(140, 34));
        txtSearch.setPreferredSize(new Dimension(320, 34));
        txtSearch.putClientProperty("JTextField.placeholderText", "Nhập nội dung tìm kiếm...");

        JButton btnRefresh = new JButton("Làm mới", ImageLoader.loadIcon("refresh.svg", 18, 18));
        styleBtn(btnRefresh);
        btnRefresh.setPreferredSize(new Dimension(120, 34));
        btnRefresh.setFont(btnRefresh.getFont().deriveFont(Font.PLAIN, 13f));
        btnRefresh.addActionListener(e -> onRefresh());

        right.add(cbStatus);
        right.add(txtSearch);
        right.add(btnRefresh);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void onExport() {
        // Excel export removed
    }

    private void onImport() {
        // Excel import removed
    }

    private void onDetail() {
        ImportReceipt selected = getSelected();
        if (selected == null) {
            Toast.warn(this, "Vui long chon 1 dong");
            return;
        }
        try {
            selected.setLines(controller.findLinesByReceiptId(selected.getId()));
        } catch (Exception ignored) {
        }
        ImportReceiptFormDialog dlg = new ImportReceiptFormDialog(SwingUtilities.getWindowAncestor(this), selected, true);
        dlg.setVisible(true);
    }

    private void onCancel() {
        onDelete();
    }

    private void onRefresh() {
        txtSearch.setText("");
        cbStatus.setSelectedIndex(0);
        cbSupplier.setSelectedIndex(0);
        cbEmployee.setSelectedIndex(0);
        dpFromDate.clear();
        dpToDate.clear();
        txtFromAmount.setText("");
        txtToAmount.setText("");
        reload();
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

    private Long parseRequiredLongObj(String s, String key) {
        String t = trim(s);
        if (t.isBlank()) throw new IllegalArgumentException("Thiếu cột: " + key);
        try {
            long v = Long.parseLong(t);
            if (v <= 0) throw new NumberFormatException();
            return v;
        } catch (Exception e) {
            throw new IllegalArgumentException("Giá trị không hợp lệ cho cột: " + key);
        }
    }

    private Double parseRequiredDoubleObj(String s, String key) {
        String t = trim(s);
        if (t.isBlank()) throw new IllegalArgumentException("Thiếu cột: " + key);
        try {
            String normalized = t.replace("₫", "").replace(",", "").replace(" ", "");
            return Double.parseDouble(normalized);
        } catch (Exception e) {
            throw new IllegalArgumentException("Giá trị không hợp lệ cho cột: " + key);
        }
    }

    private JComponent buildCenter() {
        JPanel wrap = new JPanel(new BorderLayout(10, 0));
        wrap.setOpaque(false);

        wrap.add(buildFilterPanel(), BorderLayout.WEST);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        configureTable();
        center.add(new JScrollPane(table), BorderLayout.CENTER);

        wrap.add(center, BorderLayout.CENTER);
        return wrap;
    }

    private JComponent buildFilterPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        p.setPreferredSize(new Dimension(240, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;
        gbc.gridx = 0; gbc.gridy = y++; gbc.weightx = 1.0; gbc.gridwidth = 1;
        p.add(compactFilter("Nhà cung cấp", cbSupplier), gbc);

        gbc.gridx = 0; gbc.gridy = y++;
        p.add(compactFilter("Nhân viên nhập", cbEmployee), gbc);

        gbc.gridx = 0; gbc.gridy = y++;
        p.add(compactFilter("Từ ngày", dpFromDate), gbc);

        gbc.gridx = 0; gbc.gridy = y++;
        p.add(compactFilter("Đến ngày", dpToDate), gbc);

        gbc.gridx = 0; gbc.gridy = y++;
        p.add(compactFilter("Từ số tiền (VND)", txtFromAmount), gbc);

        gbc.gridx = 0; gbc.gridy = y++;
        p.add(compactFilter("Đến số tiền (VND)", txtToAmount), gbc);

        // filler to push to top
        gbc.gridx = 0; gbc.gridy = y; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        p.add(Box.createGlue(), gbc);
        return p;
    }

    private JComponent compactFilter(String label, JComponent field) {
        JPanel c = new JPanel(new BorderLayout(0, 6));
        c.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 12f));
        l.setForeground(new Color(60, 60, 60));
        c.add(l, BorderLayout.NORTH);
        // put field inside a small wrapper to control height
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        field.setPreferredSize(new Dimension(0, 28));
        wrap.add(field, BorderLayout.CENTER);
        c.add(wrap, BorderLayout.CENTER);
        return c;
    }

    private JComponent filterBlock(String label, JComponent field) {
        JPanel b = new JPanel(new BorderLayout(0, 6));
        b.setOpaque(false);
        JLabel l = new JLabel(label);
        b.add(l, BorderLayout.NORTH);
        b.add(field, BorderLayout.CENTER);
        field.setPreferredSize(new Dimension(0, 30));
        return b;
    }

    private void configureTable() {
        table.setFillsViewportHeight(true);
        table.setRowHeight(40);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultEditor(Object.class, null);
        if (table.getTableHeader() != null) table.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer centerDimRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                try {
                    ImportReceipt r = tableModel.getAt(t.convertRowIndexToModel(row));
                    if (r != null && r.getStatus() != null && r.getStatus() == 0) {
                        c.setForeground(Color.GRAY);
                    } else {
                        c.setForeground(Color.BLACK);
                    }
                } catch (Exception ignored) {
                    c.setForeground(Color.BLACK);
                }
                if (isSelected) c.setBackground(t.getSelectionBackground()); else c.setBackground(Color.WHITE);
                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerDimRenderer);
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
            allRows = controller.findAll();
            applyFilters();
        } catch (Throwable ex) {
            Toast.error(this, ex.getMessage() == null ? ex.toString() : ex.getMessage());
            allRows = new ArrayList<>();
            tableModel.setRows(allRows);
        }
    }

    private void applyFilters() {
        List<ImportReceipt> src = allRows == null ? new ArrayList<>() : allRows;

        String kw = trim(txtSearch.getText()).toLowerCase(java.util.Locale.ROOT);

        Supplier sup = (Supplier) cbSupplier.getSelectedItem();
        long supId = (sup == null ? 0 : sup.getId());

        Employee emp = (Employee) cbEmployee.getSelectedItem();
        long empId = (emp == null ? 0 : emp.getId());

        String status = Objects.toString(cbStatus.getSelectedItem(), "Tất cả");

        LocalDate fromDate = dpFromDate.getDate();
        LocalDate toDate = dpToDate.getDate();

        Double fromAmt = parseMoneyOrNull(txtFromAmount.getText());
        Double toAmt = parseMoneyOrNull(txtToAmount.getText());

        List<ImportReceipt> out = new ArrayList<>();
        for (ImportReceipt r : src) {
            if (r == null) continue;

            int st = (r.getStatus() == null ? 1 : r.getStatus());
            if ("Hoạt động".equals(status) && st != 1) continue;
            if ("Hủy".equals(status) && st != 0) continue;

            if (supId > 0) {
                Long rid = r.getSupplierId();
                if (rid == null || rid != supId) continue;
            }

            if (empId > 0) {
                Long rid = r.getCreatedBy();
                if (rid == null || rid != empId) continue;
            }

            if (fromDate != null || toDate != null) {
                LocalDateTime t = r.getTime();
                LocalDate d = (t == null ? null : t.toLocalDate());
                if (d == null) continue;
                if (fromDate != null && d.isBefore(fromDate)) continue;
                if (toDate != null && d.isAfter(toDate)) continue;
            }

            if (fromAmt != null || toAmt != null) {
                Double total = r.getTotal();
                double v = (total == null ? 0 : total);
                if (fromAmt != null && v < fromAmt) continue;
                if (toAmt != null && v > toAmt) continue;
            }

            if (!kw.isBlank()) {
                String idText = String.valueOf(r.getId());
                String supplierName = (r.getSupplierName() == null ? "" : r.getSupplierName());
                String employeeName = (r.getCreatedByName() == null ? "" : r.getCreatedByName());
                if (!com.phonestore.util.TextSearch.matches(kw, idText, supplierName, employeeName)) continue;
            }

            out.add(r);
        }
        tableModel.setRows(out);
    }

    private void onAdd() {
        ImportReceiptFormDialog dlg = new ImportReceiptFormDialog(SwingUtilities.getWindowAncestor(this), null);
        dlg.setVisible(true);
        ImportReceipt r = dlg.getResult();
        if (r == null) return;
        try {
            controller.create(r);
            reload();
            Toast.info(this, "Đã thêm phiếu nhập");
        } catch (Exception ex) {
            Toast.error(this, ex.getMessage());
        }
    }

    private void onEdit() {
        ImportReceipt selected = getSelected();
        if (selected == null) {
            Toast.warn(this, "Vui lòng chọn 1 dòng");
            return;
        }
        ImportReceiptFormDialog dlg = new ImportReceiptFormDialog(SwingUtilities.getWindowAncestor(this), selected);
        dlg.setVisible(true);
        ImportReceipt r = dlg.getResult();
        if (r == null) return;
        try {
            controller.update(r);
            reload();
            Toast.info(this, "Đã cập nhật");
        } catch (Exception ex) {
            Toast.error(this, ex.getMessage());
        }
    }

    private void onDelete() {
        ImportReceipt selected = getSelected();
        if (selected == null) {
            Toast.warn(this, "Vui lòng chọn 1 dòng");
            return;
        }
        int c = JOptionPane.showConfirmDialog(this, "Hủy phiếu nhập đã chọn?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;
        try {
            controller.delete(selected.getId());
            reload();
            Toast.info(this, "Đã xóa");
        } catch (Exception ex) {
            Toast.error(this, ex.getMessage());
        }
    }

    private ImportReceipt getSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return tableModel.getAt(row);
    }

    private static final class ReceiptTableModel extends AbstractTableModel {
        private final String[] cols = {"STT", "Mã phiếu nhập", "Nhà cung cấp", "Nhân viên nhập", "Thời gian", "Trạng thái", "Tổng tiền"};
        private List<ImportReceipt> rows = new ArrayList<>();

        public void setRows(List<ImportReceipt> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        public ImportReceipt getAt(int row) {
            return rows.get(row);
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int column) { return cols[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ImportReceipt r = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> rowIndex + 1;
                case 1 -> r.getId();
                case 2 -> (r.getSupplierName() != null && !r.getSupplierName().isBlank()) ? r.getSupplierName() : nvl(r.getSupplierId());
                case 3 -> (r.getCreatedByName() != null && !r.getCreatedByName().isBlank()) ? r.getCreatedByName() : nvl(r.getCreatedBy());
                case 4 -> formatTime(r.getTime());
                case 5 -> (r.getStatus() == null) ? "" : (r.getStatus() == 0 ? "Đã hủy" : "Hoạt động");
                case 6 -> MoneyVND.format(r.getTotal());
                default -> "";
            };
        }

        private static String nvl(Object o) {
            return o == null ? "" : String.valueOf(o);
        }

        private static String formatTime(LocalDateTime t) {
            if (t == null) return "";
            try {
                return t.format(TIME_FMT);
            } catch (Exception e) {
                return t.toString();
            }
        }
    }

    private void configureFilters() {
        dpFromDate.getComponentDateTextField().putClientProperty("JTextField.placeholderText", "Nhập Ngày vào");
        dpToDate.getComponentDateTextField().putClientProperty("JTextField.placeholderText", "Nhập Ngày vào");
        txtFromAmount.putClientProperty("JTextField.placeholderText", "Từ...");
        txtToAmount.putClientProperty("JTextField.placeholderText", "Đến...");

        cbSupplier.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Supplier s) setText(s.getName() == null ? "" : s.getName());
                return this;
            }
        });
        cbEmployee.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Employee e) setText(e.getFullName() == null ? "" : e.getFullName());
                return this;
            }
        });
    }

    private void loadFilterData() {
        try {
            List<Supplier> suppliers = supplierController.findAll();
            List<Supplier> model = new ArrayList<>();
            Supplier all = new Supplier();
            all.setId(0);
            all.setName("Tất cả");
            model.add(all);
            if (suppliers != null) model.addAll(suppliers);
            cbSupplier.setModel(new DefaultComboBoxModel<>(model.toArray(new Supplier[0])));
            cbSupplier.setSelectedIndex(0);
        } catch (Throwable ex) {
            cbSupplier.setModel(new DefaultComboBoxModel<>(new Supplier[0]));
        }

        try {
            List<Employee> employees = employeeController.findAll();
            List<Employee> model = new ArrayList<>();
            Employee all = new Employee();
            all.setId(0);
            all.setFullName("Tất cả");
            model.add(all);
            if (employees != null) model.addAll(employees);
            cbEmployee.setModel(new DefaultComboBoxModel<>(model.toArray(new Employee[0])));
            cbEmployee.setSelectedIndex(0);
        } catch (Throwable ex) {
            cbEmployee.setModel(new DefaultComboBoxModel<>(new Employee[0]));
        }
    }

    private void wireFilterEvents() {
        cbStatus.addActionListener(e -> applyFilters());
        cbSupplier.addActionListener(e -> applyFilters());
        cbEmployee.addActionListener(e -> applyFilters());

        DateChangeListener dl = e -> applyFilters();
        dpFromDate.addDateChangeListener(dl);
        dpToDate.addDateChangeListener(dl);

        txtSearch.addActionListener(e -> applyFilters());
        installDocApply(txtSearch);

        txtFromAmount.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { applyFilters(); }
        });
        txtToAmount.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { applyFilters(); }
        });
    }

    private void installDocApply(JTextField f) {
        f.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });
    }

    private Double parseMoneyOrNull(String s) {
        String t = trim(s);
        if (t.isBlank()) return null;
        try {
            return MoneyVND.parseToDouble(t);
        } catch (Exception e) {
            return null;
        }
    }

    private static DatePicker createDatePicker() {
        DatePickerSettings settings = new DatePickerSettings(java.util.Locale.forLanguageTag("vi-VN"));
        settings.setAllowEmptyDates(true);
        settings.setFormatForDatesCommonEra("dd/MM/yyyy");
        settings.setFormatForDatesBeforeCommonEra("dd/MM/yyyy");
        // force use of popup calendar for selection (keyboard editing off)
        settings.setAllowKeyboardEditing(false);
        DatePicker dp = new DatePicker(settings);
        com.phonestore.ui.common.DatePickerYearScroller.enable(dp);
        dp.getComponentDateTextField().setPreferredSize(new Dimension(0, 30));
        dp.setPreferredSize(new Dimension(0, 30));
        return dp;
    }
}
