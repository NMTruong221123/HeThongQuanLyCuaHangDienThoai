package com.phonestore.ui.inventory;

import com.phonestore.controller.ExportReceiptController;
import com.phonestore.controller.CustomerController;
import com.phonestore.controller.EmployeeController;
import com.phonestore.controller.ProductController;
import com.phonestore.controller.ProductVariantController;
import com.phonestore.dao.jdbc.ImeiRegistryJdbcDao;
import com.phonestore.model.ExportReceipt;
import com.phonestore.model.ExportReceiptLine;
import com.phonestore.model.Customer;
import com.phonestore.model.Employee;
import com.phonestore.ui.common.images.ImageLoader;
import com.phonestore.ui.common.toast.Toast;
import com.phonestore.util.MoneyVND;
import com.phonestore.util.invoice.ExportReceiptCodeUtil;
import com.phonestore.util.invoice.ExportInvoiceTextBuilder;
import com.phonestore.util.invoice.InvoiceStorage;
import com.phonestore.util.mail.ExportInvoiceMailer;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.optionalusertools.DateChangeListener;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ExportReceiptPanel extends JPanel {

    private final ExportReceiptController controller = new ExportReceiptController();
    private final CustomerController customerController = new CustomerController();
    private final EmployeeController employeeController = new EmployeeController();
    private final ProductController productController = new ProductController();
    private final ProductVariantController variantController = new ProductVariantController();
    private final ImeiRegistryJdbcDao imeiDao = new ImeiRegistryJdbcDao();

    private final JComboBox<String> cbStatus = new JComboBox<>(new String[] {"Tất cả", "Hoạt động", "Hủy"});
    private final JTextField txtSearch = new JTextField();

    private final JComboBox<Customer> cbCustomer = new JComboBox<>();
    private final JComboBox<Employee> cbEmployee = new JComboBox<>();
    private final DatePicker dpFromDate = createDatePicker();
    private final DatePicker dpToDate = createDatePicker();
    private final JTextField txtFromAmount = new JTextField();
    private final JTextField txtToAmount = new JTextField();

    private final ReceiptTableModel tableModel = new ReceiptTableModel();
    private final JTable table = new JTable(tableModel);

    private final Color backgroundColor = new Color(240, 247, 250);
    private List<ExportReceipt> allRows = new ArrayList<>();

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ExportReceiptPanel() {
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
        configureTable();
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
        JButton btnDelete = new JButton("HỦY PHIẾU", ImageLoader.loadIcon("cancel.svg", 42, 42));
        JButton btnDetail = new JButton("CHI TIẾT", ImageLoader.loadIcon("detail.svg", 42, 42));

        styleToolbarIconBtn(btnAdd);
        styleToolbarIconBtn(btnDelete);
        styleToolbarIconBtn(btnDetail);

        left.add(btnAdd);
        left.add(btnDelete);
        left.add(btnDetail);

        btnAdd.addActionListener(e -> onAdd());
        btnDelete.addActionListener(e -> onDelete());
        btnDetail.addActionListener(e -> onDetail());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        right.setOpaque(false);
        cbStatus.setPreferredSize(new Dimension(140, 34));
        txtSearch.setPreferredSize(new Dimension(320, 34));
        txtSearch.putClientProperty("JTextField.placeholderText", "Nhập nội dung tìm kiếm...");

        JButton btnRefresh = new JButton("Làm mới", ImageLoader.loadIcon("refresh.svg", 18, 18));
        styleBtn(btnRefresh);
        btnRefresh.setPreferredSize(new Dimension(120, 34));
        btnRefresh.setFont(btnRefresh.getFont().deriveFont(Font.PLAIN, 13f));
        btnRefresh.addActionListener(e -> onRefresh());
        btnRefresh.setAlignmentY(Component.TOP_ALIGNMENT);

        right.add(cbStatus);
        right.add(txtSearch);
        right.add(btnRefresh);

        bar.add(left);
        bar.add(right);
        return bar;
    }

    private void onDetail() {
        onEdit();
    }

    private void onCancel() {
        onDelete();
    }

    private void onRefresh() {
        txtSearch.setText("");
        cbStatus.setSelectedIndex(0);
        if (cbCustomer.getItemCount() > 0) cbCustomer.setSelectedIndex(0);
        if (cbEmployee.getItemCount() > 0) cbEmployee.setSelectedIndex(0);
        dpFromDate.clear();
        dpToDate.clear();
        txtFromAmount.setText("");
        txtToAmount.setText("");
        reload();
    }

    private void onExport() {
        // Excel export removed
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
        if (t.isBlank()) throw new IllegalArgumentException("Thiáº¿u cá»™t: " + key);
        try {
            String normalized = t.replace("â‚«", "").replace(",", "").replace(" ", "");
            return Double.parseDouble(normalized);
        } catch (Exception e) {
            throw new IllegalArgumentException("GiÃ¡ trá»‹ khÃ´ng há»£p lá»‡ cho cá»™t: " + key);
        }
    }

    private Long parseLongObj(String s) {
        try {
            String t = trim(s);
            if (t.isBlank()) return null;
            return Long.parseLong(t);
        } catch (Exception e) {
            return null;
        }
    }

    private Double parseDoubleObj(String s) {
        try {
            String t = trim(s);
            if (t.isBlank()) return null;
            String normalized = t.replace("â‚«", "").replace(",", "").replace(" ", "");
            return Double.parseDouble(normalized);
        } catch (Exception e) {
            return null;
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
        gbc.gridx = 0; gbc.gridy = y++; gbc.weightx = 1.0;
        p.add(compactFilter("Khách hàng", cbCustomer), gbc);

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
                    ExportReceipt r = tableModel.getAt(t.convertRowIndexToModel(row));
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

    // import/export functionality removed

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
        List<ExportReceipt> src = allRows == null ? new ArrayList<>() : allRows;

        String kw = trim(txtSearch.getText()).toLowerCase(java.util.Locale.ROOT);

        Customer cus = (Customer) cbCustomer.getSelectedItem();
        long cusId = (cus == null ? 0 : cus.getId());

        Employee emp = (Employee) cbEmployee.getSelectedItem();
        long empId = (emp == null ? 0 : emp.getId());

        String status = Objects.toString(cbStatus.getSelectedItem(), "Tất cả");

        LocalDate fromDate = dpFromDate.getDate();
        LocalDate toDate = dpToDate.getDate();

        Double fromAmt = parseMoneyOrNull(txtFromAmount.getText());
        Double toAmt = parseMoneyOrNull(txtToAmount.getText());

        List<ExportReceipt> out = new ArrayList<>();
        for (ExportReceipt r : src) {
            if (r == null) continue;

            int st = (r.getStatus() == null ? 1 : r.getStatus());
            if ("Hoạt động".equals(status) && st != 1) continue;
            if ("Hủy".equals(status) && st != 0) continue;

            if (cusId > 0) {
                Long rid = r.getCustomerId();
                if (rid == null || rid != cusId) continue;
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
                String customerName = (r.getCustomerName() == null ? "" : r.getCustomerName());
                String receiptCode = (r.getReceiptCode() == null ? "" : r.getReceiptCode());
                if (!com.phonestore.util.TextSearch.matches(kw, idText, customerName, receiptCode)) continue;
            }

            out.add(r);
        }

        tableModel.setRows(out);
    }

    private void onAdd() {
        ExportReceiptFormDialog dlg = new ExportReceiptFormDialog(SwingUtilities.getWindowAncestor(this), null);
        dlg.setVisible(true);
        ExportReceipt r = dlg.getResult();
        if (r == null) return;
        try {
            if (r.getReceiptCode() == null || r.getReceiptCode().contains("#NEW")) {
                long nextId = estimateNextReceiptId();
                String nextCode = ExportReceiptCodeUtil.build(
                        r.getTime() == null ? LocalDateTime.now() : r.getTime(),
                        String.valueOf(nextId));
                r.setReceiptCode(nextCode);
            }

            Customer customer = null;
            if (r.getCustomerId() != null && r.getCustomerId() > 0) {
                customer = customerController.findById(r.getCustomerId());
            }

            ExportReceiptPaymentDialog payDlg = new ExportReceiptPaymentDialog(SwingUtilities.getWindowAncestor(this), r, customer);
            payDlg.setVisible(true);
            ExportReceiptPaymentDialog.Result pay = payDlg.getResult();
            if (pay == ExportReceiptPaymentDialog.Result.CANCELLED) return;

            // Apply edited total/paymentRef from payment wizard
            long editedTotal = payDlg.getEditedTotalAmount();
            if (editedTotal >= 0) {
                r.setTotal((double) editedTotal);
            }
            r.setPaymentRef(payDlg.getPaymentRefToPersist());

            ExportReceipt created = controller.create(r);
            String finalReceiptCode = ExportReceiptCodeUtil.build(
                    created.getTime() == null ? LocalDateTime.now() : created.getTime(),
                    String.valueOf(created.getId()));
            r.setReceiptCode(finalReceiptCode);
            created.setReceiptCode(finalReceiptCode);
            // Keep transient lines so invoice prints correct product details
            created.setLines(r.getLines());
            // Ensure transient paymentRef is preserved for invoice/email rendering.
            created.setPaymentRef(rewriteTransferRefWithFinalCode(r.getPaymentRef(), finalReceiptCode));
            try {
                controller.update(created);
            } catch (Exception ignored) {
            }

            Customer customerForInvoice = payDlg.getEditedCustomer();
            if (customerForInvoice != null && customer != null) {
                customerForInvoice.setId(customer.getId());
                customerForInvoice.setStatus(customer.getStatus());
            }
            if (customerForInvoice == null) customerForInvoice = customer;

            // If user didn't fill email on payment screen, fallback to DB email
            if (customerForInvoice != null) {
                String e1 = customerForInvoice.getEmail() == null ? "" : customerForInvoice.getEmail().trim();
                String e2 = customer == null || customer.getEmail() == null ? "" : customer.getEmail().trim();
                if (e1.isBlank() && !e2.isBlank()) {
                    customerForInvoice.setEmail(e2);
                }
            }

            String paymentLabel = payDlg.getPaymentMethodLabel();
            String invoiceText = ExportInvoiceTextBuilder.build(customerForInvoice, created, paymentLabel);
            InvoiceStorage.saveExportReceiptInvoiceText(created.getId(), invoiceText);

            // Send invoice email (best-effort, async)
            Customer mailCustomer = customerForInvoice;
            if (mailCustomer != null && mailCustomer.getEmail() != null && !mailCustomer.getEmail().trim().isBlank()) {
                Customer finalCustomer = mailCustomer;
                ExportReceipt finalReceipt = created;
                String finalPaymentLabel = paymentLabel;
                Thread t = new Thread(() -> {
                    try {
                        ExportInvoiceMailer.sendExportInvoice(finalCustomer, finalReceipt, finalPaymentLabel);
                    } catch (Exception ex) {
                        System.err.println("[MAIL] Gửi hóa đơn bán hàng thất bại: " + ex);
                        SwingUtilities.invokeLater(() -> Toast.warn(this,
                                "Gửi email hóa đơn thất bại: " + (ex.getMessage() == null ? ex.toString() : ex.getMessage())));
                    }
                }, "export-invoice-mail");
                t.setDaemon(true);
                t.start();
            } else {
                Toast.warn(this, "Không có email khách hàng để gửi hóa đơn");
            }

            if (payDlg.isPrintRequested()) {
                printInvoiceText(invoiceText);
            }

            reload();
            Toast.info(this, "Đã thêm phiếu xuất");
        } catch (Exception ex) {
            Toast.error(this, ex.getMessage());
        }
    }

    private long estimateNextReceiptId() {
        try {
            List<ExportReceipt> rows = controller.findAll();
            long max = 0;
            if (rows != null) {
                for (ExportReceipt it : rows) {
                    if (it == null) continue;
                    if (it.getId() > max) max = it.getId();
                }
            }
            return max + 1;
        } catch (Exception ex) {
            return System.currentTimeMillis() % 100000;
        }
    }

    private String rewriteTransferRefWithFinalCode(String paymentRef, String finalCode) {
        String ref = paymentRef == null ? "" : paymentRef.trim();
        String code = finalCode == null ? "" : finalCode.trim();
        if (ref.isBlank() || code.isBlank()) return ref;
        if (!ref.startsWith("TRANSFER|")) return ref;

        String[] parts = ref.split("\\|");
        String amount = "";
        for (String part : parts) {
            if (part == null) continue;
            String p = part.trim();
            if (p.startsWith("amount=")) amount = p.substring("amount=".length()).trim();
        }
        if (amount.isBlank()) return "TRANSFER|ref=" + code;
        return "TRANSFER|ref=" + code + "|amount=" + amount;
    }

    private void consumeSoldImeis(long receiptId, List<ExportReceiptLine> lines) {
        if (lines == null || lines.isEmpty()) return;
        Set<String> sold = new LinkedHashSet<>();
        for (ExportReceiptLine line : lines) {
            if (line == null || line.getImeis() == null) continue;
            for (String imei : line.getImeis()) {
                if (imei == null) continue;
                String v = imei.trim();
                if (!v.isBlank()) sold.add(v);
            }
        }
        if (sold.isEmpty()) return;
        try {
            imeiDao.markSoldImeisForReceipt(new ArrayList<>(sold), receiptId);
        } catch (Exception ex) {
            Toast.warn(this, "Không thể cập nhật IMEI đã bán: " + (ex.getMessage() == null ? ex.toString() : ex.getMessage()));
        }
    }

    private void updateStocksAfterSale(List<ExportReceiptLine> lines) {
        if (lines == null || lines.isEmpty()) return;
        try {
            // Variant stock
            for (ExportReceiptLine line : lines) {
                if (line == null) continue;
                Long productId = line.getProductId();
                Long variantId = line.getVariantId();
                int qty = line.getQuantity() == null ? 0 : Math.max(0, line.getQuantity());
                if (qty <= 0 || productId == null || productId <= 0 || variantId == null || variantId <= 0) continue;

                List<com.phonestore.model.ProductVariant> vars = variantController.findByProductId(productId);
                if (vars == null) continue;
                for (com.phonestore.model.ProductVariant v : vars) {
                    if (v == null || v.getId() != variantId) continue;
                    int current = v.getStock() == null ? 0 : v.getStock();
                    v.setStock(Math.max(0, current - qty));
                    try {
                        variantController.update(v);
                    } catch (Exception ignored) {
                    }
                    break;
                }
            }

            // Product stock
            List<com.phonestore.model.Product> products = productController.findAll();
            if (products == null || products.isEmpty()) return;
            java.util.Map<Long, Integer> soldByProduct = new java.util.HashMap<>();
            for (ExportReceiptLine line : lines) {
                if (line == null || line.getProductId() == null || line.getProductId() <= 0) continue;
                int qty = line.getQuantity() == null ? 0 : Math.max(0, line.getQuantity());
                if (qty <= 0) continue;
                soldByProduct.merge(line.getProductId(), qty, Integer::sum);
            }
            if (soldByProduct.isEmpty()) return;

            for (com.phonestore.model.Product p : products) {
                if (p == null) continue;
                Integer sold = soldByProduct.get(p.getId());
                if (sold == null || sold <= 0) continue;
                int current = p.getStock() == null ? 0 : p.getStock();
                p.setStock(Math.max(0, current - sold));
                try {
                    productController.update(p);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ex) {
            Toast.warn(this, "Khong the cap nhat ton kho sau khi ban: " + (ex.getMessage() == null ? ex.toString() : ex.getMessage()));
        }
    }

    private void printInvoiceText(String text) throws PrinterException {
        JTextArea area = new JTextArea();
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setText(text == null ? "" : text);
        area.setCaretPosition(0);
        area.print();
    }

    private void onEdit() {
        ExportReceipt selected = getSelected();
        if (selected == null) {
            Toast.warn(this, "Vui lòng chọn 1 dòng");
            return;
        }

        // Load line items (best-effort) so the editor shows the real bill.
        try {
            selected.setLines(controller.findLinesByReceiptId(selected.getId()));
        } catch (Exception ignored) {
        }

        ExportReceiptDetailDialog dlg = new ExportReceiptDetailDialog(SwingUtilities.getWindowAncestor(this), selected);
        dlg.setVisible(true);
    }

    private void onDelete() {
        ExportReceipt selected = getSelected();
        if (selected == null) {
            Toast.warn(this, "Vui lÃ²ng chá»n 1 dÃ²ng");
            return;
        }
        try {
            com.phonestore.dao.jdbc.ExportReceiptJdbcDao.PreviewInfo info = controller.previewDeleteAndCompress(selected.getId());
            StringBuilder sb = new StringBuilder();
            sb.append("Hành động sẽ thực hiện:\n");
            sb.append("- Xóa phiếu xuất (phieuxuat) : ").append(info.phieuDeleteCount).append(" hàng\n");
            sb.append("- Xóa chi tiết phiếu (ctphieuxuat) : ").append(info.ctDeleteCount).append(" hàng\n");
            sb.append("- Dồn (giảm 1) cho phieuxuat với maphieuxuat > id : ").append(info.phieuShiftCount).append(" hàng\n");
            sb.append("- Dồn cho ctphieuxuat với maphieuxuat > id : ").append(info.ctShiftCount).append(" hàng\n");
            sb.append("- Xóa tham chiếu IMEI có export_receipt_id = id : ").append(info.imeiClearedCount).append(" hàng\n");
            sb.append("- Dồn export_receipt_id > id : ").append(info.imeiShiftCount).append(" hàng\n\n");
            sb.append("Tiến hành backup trước khi xóa được khuyến nghị. Chọn 'Backup & Xóa' để sao lưu và xóa, hoặc 'Hủy' để abort.");

            Object[] options = {"Backup & Xóa", "Hủy"};
            int c = JOptionPane.showConfirmDialog(this, "Hủy phiếu xuất đã chọn?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (c != JOptionPane.YES_OPTION) return;
            try {
                // perform soft-delete (mark as canceled) like ImportReceipt
                controller.delete(selected.getId());
                reload();
                Toast.info(this, "Đã hủy phiếu (đánh dấu Hủy)");
            } catch (Exception ex) {
                Toast.error(this, ex.getMessage() == null ? ex.toString() : ex.getMessage());
            }
        } catch (Exception ex) {
            Toast.error(this, ex.getMessage() == null ? ex.toString() : ex.getMessage());
        }
    }

    private ExportReceipt getSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return tableModel.getAt(row);
    }

    private static final class ReceiptTableModel extends AbstractTableModel {
        private final String[] cols = {"STT", "Mã phiếu xuất", "Khách hàng", "Nhân viên nhập", "Thời gian", "Trạng thái", "Tổng tiền"};
        private List<ExportReceipt> rows = new ArrayList<>();

        public void setRows(List<ExportReceipt> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        public ExportReceipt getAt(int row) {
            return rows.get(row);
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int column) { return cols[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ExportReceipt r = rows.get(rowIndex);
                return switch (columnIndex) {
                    case 0 -> rowIndex + 1;
                    case 1 -> r.getId();
                    case 2 -> (r.getCustomerName() != null && !r.getCustomerName().isBlank()) ? r.getCustomerName() : nvl(r.getCustomerId());
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

        cbCustomer.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Customer c) setText(c.getName() == null ? "" : c.getName());
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
            List<Customer> customers = customerController.findAll();
            List<Customer> model = new ArrayList<>();
            Customer all = new Customer();
            all.setId(0);
            all.setName("Tất cả");
            model.add(all);
            if (customers != null) {
                for (Customer c : customers) {
                    if (c != null && c.getStatus() != null && c.getStatus() == 1) {
                        model.add(c);
                    }
                }
            }
            cbCustomer.setModel(new DefaultComboBoxModel<>(model.toArray(new Customer[0])));
            cbCustomer.setSelectedIndex(0);
        } catch (Throwable ex) {
            cbCustomer.setModel(new DefaultComboBoxModel<>(new Customer[0]));
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
        cbCustomer.addActionListener(e -> applyFilters());
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
        settings.setAllowKeyboardEditing(false);
        DatePicker dp = new DatePicker(settings);
        com.phonestore.ui.common.DatePickerYearScroller.enable(dp);
        dp.getComponentDateTextField().setPreferredSize(new Dimension(0, 30));
        dp.setPreferredSize(new Dimension(0, 30));
        return dp;
    }
}
