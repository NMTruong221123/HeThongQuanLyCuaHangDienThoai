package com.phonestore.ui.inventory;

import com.phonestore.controller.CustomerController;
import com.phonestore.controller.EmployeeController;
import com.phonestore.model.Customer;
import com.phonestore.model.Employee;
import com.phonestore.model.ExportReceipt;
import com.phonestore.model.ExportReceiptLine;
import com.phonestore.util.MoneyVND;
import com.phonestore.util.invoice.ExportReceiptCodeUtil;
import com.phonestore.util.invoice.InstallmentInfo;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExportReceiptDetailDialog extends JDialog {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final ExportReceipt receipt;
    private final CustomerController customerController = new CustomerController();
    private final EmployeeController employeeController = new EmployeeController();

    private final JTextArea txtCustomerInfo = new JTextArea();
    private final JTextArea txtPaymentInfo = new JTextArea();
    private final JTextArea txtOrderEmployee = new JTextArea();
    private final JTextField txtReceiptCodeDisplay = new JTextField();

    private final JTextField txtProductId = new JTextField();
    private final JTextField txtProductName = new JTextField();
    private final JTextField txtVariant = new JTextField();
    private final JTextField txtUnitPrice = new JTextField();
    private final JTextField txtQty = new JTextField();
    private final JTextArea txtImeis = new JTextArea();

    private final ItemsTableModel itemsTableModel = new ItemsTableModel();
    private final JTable tblItems = new JTable(itemsTableModel);

    public ExportReceiptDetailDialog(Window owner, ExportReceipt receipt) {
        super(owner, "Chi tiet phieu xuat", ModalityType.APPLICATION_MODAL);
        this.receipt = receipt == null ? new ExportReceipt() : receipt;
        initUi();
        bindData();
    }

    private void initUi() {
        setSize(1220, 760);
        setMinimumSize(new Dimension(1080, 700));
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(root, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.add(buildCustomerInfoPanel(), BorderLayout.WEST);
        top.add(buildCenterProductPanel(), BorderLayout.CENTER);
        top.add(buildOrderEmployeePanel(), BorderLayout.EAST);
        root.add(top, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setPreferredSize(new Dimension(0, 290));
        bottom.add(new JScrollPane(tblItems), BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        tblItems.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblItems.setDefaultEditor(Object.class, null);
        if (tblItems.getTableHeader() != null) tblItems.getTableHeader().setReorderingAllowed(false);
    }

    private JComponent buildCustomerInfoPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setPreferredSize(new Dimension(340, 0));
        p.add(new JLabel("Thông tin khách hàng"));
        txtCustomerInfo.setEditable(false);
        txtCustomerInfo.setLineWrap(true);
        txtCustomerInfo.setWrapStyleWord(true);
        p.add(new JScrollPane(txtCustomerInfo));
        p.add(Box.createVerticalStrut(8));
        p.add(new JLabel("Thanh toán"));
        txtPaymentInfo.setEditable(false);
        txtPaymentInfo.setLineWrap(true);
        txtPaymentInfo.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(txtPaymentInfo);
        sp.setPreferredSize(new Dimension(0, 120));
        p.add(sp);
        return p;
    }

    private JComponent buildOrderEmployeePanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setPreferredSize(new Dimension(340, 0));
        p.add(new JLabel("Mã đơn hàng"));
        txtReceiptCodeDisplay.setEditable(false);
        p.add(txtReceiptCodeDisplay);
        p.add(Box.createVerticalStrut(8));
        p.add(new JLabel("Thông tin nhân viên"));
        txtOrderEmployee.setEditable(false);
        txtOrderEmployee.setLineWrap(true);
        txtOrderEmployee.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(txtOrderEmployee);
        sp.setPreferredSize(new Dimension(0, 200));
        p.add(sp);
        return p;
    }

    private JComponent buildCenterProductPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        txtProductId.setEditable(false);
        txtProductName.setEditable(false);
        txtVariant.setEditable(false);
        txtUnitPrice.setEditable(false);
        txtQty.setEditable(false);
        txtImeis.setEditable(false);

        JPanel row1 = new JPanel(new GridLayout(2, 2, 12, 6));
        row1.add(new JLabel("Ma SP"));
        row1.add(txtProductId);
        row1.add(new JLabel("Ten san pham"));
        row1.add(txtProductName);
        p.add(row1);
        p.add(Box.createVerticalStrut(8));

        JPanel row2 = new JPanel(new GridLayout(2, 3, 12, 6));
        row2.add(new JLabel("Cau hinh"));
        row2.add(new JLabel("Don gia"));
        row2.add(new JLabel("So luong"));
        row2.add(txtVariant);
        row2.add(txtUnitPrice);
        row2.add(txtQty);
        p.add(row2);
        p.add(Box.createVerticalStrut(8));

        p.add(new JLabel("Danh sach IMEI"));
        JScrollPane sp = new JScrollPane(txtImeis);
        sp.setPreferredSize(new Dimension(0, 200));
        p.add(sp);
        return p;
    }

    private void bindData() {
        List<ExportReceiptLine> lines = receipt.getLines() == null ? new ArrayList<>() : receipt.getLines();
        itemsTableModel.setRows(lines);

        txtCustomerInfo.setText(buildCustomerInfoText(receipt));
        txtPaymentInfo.setText(buildPaymentText(receipt, lines));
        txtOrderEmployee.setText(buildOrderEmployeeText(receipt));
        txtReceiptCodeDisplay.setText(safe(receipt.getReceiptCode()).isBlank() ? ExportReceiptCodeUtil.fallbackFromId(receipt.getId()) : safe(receipt.getReceiptCode()));

        tblItems.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = tblItems.getSelectedRow();
            if (row < 0) return;
            ExportReceiptLine line = itemsTableModel.getRowAt(row);
            showLine(line);
        });
        if (tblItems.getRowCount() > 0) tblItems.setRowSelectionInterval(0, 0);
    }

    private void showLine(ExportReceiptLine line) {
        if (line == null) return;
        txtProductId.setText(line.getProductId() == null ? "" : String.valueOf(line.getProductId()));
        txtProductName.setText(safe(line.getProductName()));
        txtVariant.setText(safe(line.getVariantLabel()));
        txtUnitPrice.setText(line.getUnitPrice() == null ? "" : MoneyVND.format(line.getUnitPrice()));
        txtQty.setText(line.getQuantity() == null ? "" : String.valueOf(line.getQuantity()));

        if (line.getImeis() != null && !line.getImeis().isEmpty()) {
            txtImeis.setText(String.join("\n", line.getImeis()));
        } else {
            // try to fetch IMEIs from registry if possible
            try {
                com.phonestore.dao.jdbc.ImeiRegistryJdbcDao imeiDao = new com.phonestore.dao.jdbc.ImeiRegistryJdbcDao();
                // First try: fetch IMEIs specifically for this line (no artificial limit)
                java.util.List<String> fetched = imeiDao.findImeisByExportReceipt(
                    receipt.getId(), line.getVariantId(), line.getProductId(), 0
                );
                // Fallback: if nothing found, try fetching all IMEIs linked to the receipt (ignore variant/product)
                if ((fetched == null || fetched.isEmpty())) {
                    fetched = imeiDao.findImeisByExportReceipt(receipt.getId(), null, null, 0);
                }
                if (fetched != null && !fetched.isEmpty()) {
                    txtImeis.setText(String.join("\n", fetched));
                } else {
                    int qty = line.getQuantity() == null ? 0 : Math.max(0, line.getQuantity());
                    txtImeis.setText(qty <= 0 ? "" : "(IMEI x" + qty + ")");
                }
            } catch (Exception ex) {
                int qty = line.getQuantity() == null ? 0 : Math.max(0, line.getQuantity());
                txtImeis.setText(qty <= 0 ? "" : "(IMEI x" + qty + ")");
            }
        }
    }

    private String buildCustomerPaymentText(ExportReceipt r, List<ExportReceiptLine> lines) {
        Customer c = null;
        try {
            if (r.getCustomerId() != null && r.getCustomerId() > 0) c = customerController.findById(r.getCustomerId());
        } catch (Exception ignored) {
        }
        String name = c == null ? safe(r.getCustomerName()) : safe(c.getName());
        String phone = c == null ? "" : safe(c.getPhone());
        String address = c == null ? "" : safe(c.getAddress());
        String email = c == null ? "" : safe(c.getEmail());

        long total = Math.round(r.getTotal() == null ? 0 : r.getTotal());
        String paymentRef = safe(r.getPaymentRef());
        String method = "Tien mat";
        String detail = "";

        if (paymentRef.startsWith("TRANSFER|")) {
            method = "Chuyen khoan";
            long amount = parseRefLong(paymentRef, "amount");
            String ref = parseRefString(paymentRef, "ref");
            long paid = amount <= 0 ? total : amount;
            long change = Math.max(0, paid - total);
            detail = "Thanh toan: " + MoneyVND.format(paid)
                + "\nTra lai: " + MoneyVND.format(change)
                + "\nNoi dung CK: " + (ref.isBlank() ? "-" : ref);
        } else if (paymentRef.startsWith("INSTALLMENT|")) {
            method = "Tra gop";
            InstallmentInfo info = InstallmentInfo.tryParse(paymentRef);
            if (info != null) {
                detail = "Goi: " + info.months() + " thang"
                    + "\nTra truoc: " + MoneyVND.format(info.upfrontAmount())
                    + "\nCon lai: " + MoneyVND.format(info.remainingAmount())
                    + "\nMoi thang: " + MoneyVND.format(info.monthlyAmount())
                    + "\nDu kien tat toan: " + info.months() + " thang";
            }
        } else {
            long given = parseRefLong(paymentRef, "given");
            long paid = given <= 0 ? total : given;
            long change = Math.max(0, paid - total);
            detail = "Khach dua: " + MoneyVND.format(paid)
                + "\nTra lai: " + MoneyVND.format(change);
        }

        return "Ten: " + emptyDash(name)
            + "\nSDT: " + emptyDash(phone)
            + "\nDia chi: " + emptyDash(address)
            + "\nEmail: " + emptyDash(email)
            + "\n\nHinh thuc thanh toan: " + method
            + "\nTong don: " + MoneyVND.format(total)
            + (detail.isBlank() ? "" : ("\n" + detail));
    }

    // Backwards-compatible helpers used by other parts
    private String buildCustomerInfoText(ExportReceipt r) {
        Customer c = null;
        try {
            if (r.getCustomerId() != null && r.getCustomerId() > 0) c = customerController.findById(r.getCustomerId());
        } catch (Exception ignored) {
        }
        String name = c == null ? safe(r.getCustomerName()) : safe(c.getName());
        String phone = c == null ? "" : safe(c.getPhone());
        String address = c == null ? "" : safe(c.getAddress());
        String email = c == null ? "" : safe(c.getEmail());
        return "Tên: " + emptyDash(name)
            + "\nSDT: " + emptyDash(phone)
            + "\nĐịa chỉ: " + emptyDash(address)
            + "\nEmail: " + emptyDash(email);
    }

    private String buildPaymentText(ExportReceipt r, List<ExportReceiptLine> lines) {
        long total = Math.round(r.getTotal() == null ? 0 : r.getTotal());
        String paymentRef = safe(r.getPaymentRef());
        String method = "Tiền mặt";
        String detail = "";
        if (paymentRef.startsWith("TRANSFER|")) {
            method = "Chuyển khoản";
            long amount = parseRefLong(paymentRef, "amount");
            String ref = parseRefString(paymentRef, "ref");
            long paid = amount <= 0 ? total : amount;
            long change = Math.max(0, paid - total);
            detail = "Thanh toán: " + MoneyVND.format(paid)
                + "\nTrả lại: " + MoneyVND.format(change)
                + "\nNội dung CK: " + (ref.isBlank() ? "-" : ref);
        } else if (paymentRef.startsWith("INSTALLMENT|")) {
            method = "Trả góp";
            InstallmentInfo info = InstallmentInfo.tryParse(paymentRef);
            if (info != null) {
                detail = "Gói: " + info.months() + " tháng"
                    + "\nTrả trước: " + MoneyVND.format(info.upfrontAmount())
                    + "\nCòn lại: " + MoneyVND.format(info.remainingAmount())
                    + "\nMỗi tháng: " + MoneyVND.format(info.monthlyAmount())
                    + "\nDự kiến tất toán: " + info.months() + " tháng";
            }
        } else {
            long given = parseRefLong(paymentRef, "given");
            long paid = given <= 0 ? total : given;
            long change = Math.max(0, paid - total);
            detail = "Khách đưa: " + MoneyVND.format(paid)
                + "\nTrả lại: " + MoneyVND.format(change);
        }
        return "Hình thức thanh toán: " + method
            + "\nTổng đơn: " + MoneyVND.format(total)
            + (detail.isBlank() ? "" : "\n" + detail);
    }

    private String buildOrderEmployeeText(ExportReceipt r) {
        String receiptCode = safe(r.getReceiptCode());
        if (receiptCode.isBlank()) receiptCode = ExportReceiptCodeUtil.fallbackFromId(r.getId());

        Employee emp = null;
        try {
            List<Employee> all = employeeController.findAll();
            if (all != null && r.getCreatedBy() != null) {
                for (Employee e : all) {
                    if (e != null && e.getId() == r.getCreatedBy()) {
                        emp = e;
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        String empName = emp == null ? safe(r.getCreatedByName()) : safe(emp.getFullName());
        String empPhone = emp == null ? "" : safe(emp.getPhone());
        String empEmail = emp == null ? "" : safe(emp.getEmail());
        String time = r.getTime() == null ? "" : r.getTime().format(TIME_FMT);

        return "Ma don hang: " + receiptCode
            + "\nMa phieu xuat DB: " + r.getId()
            + "\nNgay ban: " + emptyDash(time)
            + "\n\nNhan vien ban hang: " + emptyDash(empName)
            + "\nSDT: " + emptyDash(empPhone)
            + "\nEmail: " + emptyDash(empEmail);
    }

    private long parseRefLong(String ref, String key) {
        if (ref == null || key == null) return 0;
        String[] parts = ref.split("\\|");
        for (String p : parts) {
            if (p == null) continue;
            String s = p.trim();
            int eq = s.indexOf('=');
            if (eq <= 0) continue;
            String k = s.substring(0, eq).trim();
            String v = s.substring(eq + 1).trim();
            if (!k.equals(key)) continue;
            try {
                return Long.parseLong(v);
            } catch (Exception ignored) {
                return 0;
            }
        }
        return 0;
    }

    private String parseRefString(String ref, String key) {
        if (ref == null || key == null) return "";
        String[] parts = ref.split("\\|");
        for (String p : parts) {
            if (p == null) continue;
            String s = p.trim();
            int eq = s.indexOf('=');
            if (eq <= 0) continue;
            String k = s.substring(0, eq).trim();
            String v = s.substring(eq + 1).trim();
            if (k.equals(key)) return v;
        }
        return "";
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String emptyDash(String s) {
        return safe(s).isBlank() ? "-" : safe(s);
    }

    private static class ItemsTableModel extends AbstractTableModel {
        private final String[] cols = {"STT", "Ma SP", "Ten san pham", "RAM", "ROM", "Mau sac", "Don gia", "So luong", "IMEI"};
        private List<ExportReceiptLine> rows = new ArrayList<>();

        public void setRows(List<ExportReceiptLine> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        public ExportReceiptLine getRowAt(int row) {
            return row < 0 || row >= rows.size() ? null : rows.get(row);
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
            ExportReceiptLine it = rows.get(rowIndex);
            String ram = "";
            String rom = "";
            String color = "";
            String label = it.getVariantLabel() == null ? "" : it.getVariantLabel();
            if (!label.isBlank()) {
                String[] parts = label.split("-", 2);
                String left = parts[0].trim();
                String right = parts.length > 1 ? parts[1].trim() : "";
                String[] rr = left.split("/", -1);
                if (rr.length > 0) ram = rr[0].trim();
                if (rr.length > 1) rom = rr[1].trim();
                color = right;
            }
            String imei = "";
            if (it.getImeis() != null && !it.getImeis().isEmpty()) {
                imei = it.getImeis().size() == 1 ? it.getImeis().get(0) : "(IMEI x" + it.getImeis().size() + ")";
            } else if (it.getQuantity() != null && it.getQuantity() > 0) {
                imei = "(IMEI x" + it.getQuantity() + ")";
            }
            return switch (columnIndex) {
                case 0 -> rowIndex + 1;
                case 1 -> it.getProductId();
                case 2 -> it.getProductName();
                case 3 -> ram;
                case 4 -> rom;
                case 5 -> color;
                case 6 -> it.getUnitPrice() == null ? "" : MoneyVND.format(it.getUnitPrice());
                case 7 -> it.getQuantity();
                case 8 -> imei;
                default -> "";
            };
        }
    }
}
