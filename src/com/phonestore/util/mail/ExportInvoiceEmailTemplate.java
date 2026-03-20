package com.phonestore.util.mail;

import com.phonestore.model.Customer;
import com.phonestore.model.ExportReceipt;
import com.phonestore.model.ExportReceiptLine;
import com.phonestore.util.MoneyVND;
import com.phonestore.util.invoice.ExportReceiptCodeUtil;
import com.phonestore.util.invoice.InstallmentInfo;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.phonestore.model.Employee;
import com.phonestore.util.service.impl.EmployeeServiceImpl;
import java.util.Locale;

/**
 * HTML email template for export invoice.
 */
public final class ExportInvoiceEmailTemplate {

    private ExportInvoiceEmailTemplate() {
    }

    public static String buildHtml(Customer customer, ExportReceipt receipt, String paymentMethodLabel) {
        if (customer == null) throw new IllegalArgumentException("Khach hang khong hop le");
        if (receipt == null) throw new IllegalArgumentException("Phieu xuat khong hop le");

        String customerName = trim(customer.getName());
        String customerPhone = trim(customer.getPhone());
        String customerEmail = trim(customer.getEmail());

        String when = (receipt.getTime() == null)
                ? LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                : receipt.getTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        String receiptCode = trim(receipt.getReceiptCode());
        if (receiptCode.isBlank()) receiptCode = ExportReceiptCodeUtil.fallbackFromId(receipt.getId());

        String pay = trim(paymentMethodLabel);
        String ref = trim(receipt.getPaymentRef());
        String refUpper = ref.toUpperCase(Locale.ROOT);
        boolean isTransfer = refUpper.startsWith("TRANSFER|") || containsIgnoreCase(pay, "chuyen khoan");
        boolean isInstallment = refUpper.startsWith("INSTALL|") || refUpper.startsWith("INSTALLMENT|") || containsIgnoreCase(pay, "tra gop");
        boolean isCash = !isTransfer && !isInstallment;
        InstallmentInfo installment = isInstallment ? InstallmentInfo.tryParse(ref) : null;

        long totalAmount = computeTotalAmount(receipt);
        String total = totalAmount <= 0 ? "0 VND" : MoneyVND.format(totalAmount);

        String emp = trim(receipt.getCreatedByName());
        String empId = receipt.getCreatedBy() == null ? "" : String.valueOf(receipt.getCreatedBy());

        // Try to fetch full employee details when possible
        String empCode = "";
        String empFullName = "";
        String empPhone = "";
        String empEmail = "";
        if (receipt.getCreatedBy() != null) {
            try {
                Employee e = new EmployeeServiceImpl().getById(receipt.getCreatedBy());
                if (e != null) {
                    empCode = trim(e.getCode());
                    empFullName = trim(e.getFullName());
                    empPhone = trim(e.getPhone());
                    empEmail = trim(e.getEmail());
                }
            } catch (Exception ignored) {
            }
        }
        // fallbacks
        if (empCode.isBlank() && !empId.isBlank()) empCode = empId;
        if (empFullName.isBlank() && !emp.isBlank()) empFullName = emp;

        StringBuilder sb = new StringBuilder(4096);
        sb.append("<html><body style=\"margin:0;padding:0;\">");
        sb.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"border-collapse:collapse;\">")
                .append("<tr><td align=\"center\" style=\"padding:16px;\">");

        sb.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"640\" style=\"border-collapse:separate;border-spacing:0;border:1px solid #000;font-family:Arial,Helvetica,sans-serif;font-size:13px;color:#000;\">");

        sb.append(centerLine("CUA HANG DIEN THOAI PHONESTORE", true));
        sb.append(centerLine("Dia chi: Can Tho", false));
        sb.append(centerLine("Hotline: 0367678723", false));
        sb.append(formTitleRow("HOA DON BAN HANG"));
        sb.append(formBlankRow());

        sb.append(formLine("Ngay ban: ", when));
        sb.append(formLine("Ma don hang: ", receiptCode));
        sb.append(formSectionRow("THONG TIN NHAN VIEN"));
        sb.append(formLine("Ma nhan vien: ", empCode.isBlank() ? "(chua co)" : empCode));
        sb.append(formLine("Ten nhan vien: ", empFullName.isBlank() ? "(chua co)" : empFullName));
        sb.append(formLine("SDT: ", empPhone.isBlank() ? "(chua co)" : empPhone));
        sb.append(formLine("EMAIL: ", empEmail.isBlank() ? "(chua co)" : empEmail));

        sb.append(formSectionRow("THONG TIN KHACH HANG"));
        sb.append(formLine("TEN KH: ", customerName.isBlank() ? "(chua co)" : customerName));
        sb.append(formLine("SDT: ", customerPhone.isBlank() ? "(chua co)" : customerPhone));
        if (customerEmail.isBlank()) {
            sb.append(formLine("EMAIL: ", "(chua co)"));
        } else {
            sb.append(formFullRowRaw("<div style=\"margin:0 0 2px 0;\"><span style=\"font-weight:bold;\">EMAIL:</span> <a href=\"mailto:" + escapeHtml(customerEmail) + "\">" + escapeHtml(customerEmail) + "</a></div>"));
        }

        sb.append(formSectionRow("THONG TIN SAN PHAM"));
        sb.append(itemsTable(receipt.getLines(), totalAmount));

        sb.append(totalsRightBlock(total, "0 VND", total));

        String paymentDetail = buildPaymentDetailHtml(isCash, isTransfer, isInstallment, totalAmount, ref, installment, pay);
        sb.append(formSectionRow("PHUONG THUC THANH TOAN"));
        sb.append(formFullRowRaw(paymentDetail));

        sb.append(formSectionRow("CHINH SACH"));
        sb.append(formFullRow("Chinh sach doi tra: Doi trong 30 ngay ke tu ngay mua (loi nha san xuat)."));
        sb.append(formFullRow("Dieu kien: San pham con nguyen ven, day du hop/phu kien; khong roi vo, vao nuoc, can thiep sua chua."));
        sb.append(formFullRow("Xin chan thanh cam on Quy khach da tin tuong va lua chon PhoneStore!"));
        sb.append(formFullRow("Moi thac mac vui long lien he Hotline: 0367678723"));

        sb.append("</table>");
        sb.append("</td></tr></table>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String itemsTable(List<ExportReceiptLine> lines, long totalAmount) {
        StringBuilder sb = new StringBuilder(2048);

        sb.append("<tr><td style=\"padding:0 8px 8px 8px;\">");
        sb.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"border-collapse:separate;border-spacing:0;table-layout:fixed;\">");

        sb.append("<tr>")
                .append(th("STT", "width:44px;"))
                .append(th("TEN SP", ""))
                .append(th("CAU HINH SP | IMEI", ""))
                .append(th("SL SP", "width:64px;"))
                .append(th("THANH TIEN", "width:140px;"))
                .append("</tr>");

        if (lines == null || lines.isEmpty()) {
            String amount = totalAmount <= 0 ? "" : MoneyVND.format(totalAmount);
            sb.append("<tr>")
                    .append(td("1", "text-align:center;"))
                    .append(td("(Hang hoa theo phieu)", ""))
                    .append(td("-", ""))
                    .append(td("1", "text-align:center;"))
                    .append(td(amount, "text-align:right;"))
                    .append("</tr>");
        } else {
            int idx = 1;
            for (ExportReceiptLine it : lines) {
                if (it == null) continue;
                String name = trim(it.getProductName());
                if (name.isBlank()) name = "(Hang hoa theo phieu)";
                String cfg = trim(it.getVariantLabel());

                String cfgImei = cfg.isBlank() ? "-" : escapeHtml(cfg);
                String imeiHtml = buildImeiHtml(it);
                cfgImei = cfgImei + "<br>IMEI: " + (imeiHtml.isBlank() ? "-" : imeiHtml);

                int qty = it.getQuantity() == null ? 0 : Math.max(0, it.getQuantity());
                String qtyStr = qty <= 0 ? "" : String.valueOf(qty);
                long lineTotal = it.getLineTotal();
                String amount = lineTotal <= 0 ? (totalAmount <= 0 ? "" : MoneyVND.format(totalAmount)) : MoneyVND.format(lineTotal);

                sb.append("<tr>")
                        .append(td(String.valueOf(idx++), "text-align:center;"))
                        .append(td(name, "word-break:break-word;"))
                        .append(tdRaw(cfgImei, "word-break:break-word;"))
                        .append(td(qtyStr, "text-align:center;"))
                        .append(td(amount, "text-align:right;"))
                        .append("</tr>");
            }
        }

        sb.append("</table>");
        sb.append("</td></tr>");
        return sb.toString();
    }

    private static String buildImeiHtml(ExportReceiptLine line) {
        if (line == null || line.getImeis() == null || line.getImeis().isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String it : line.getImeis()) {
            String v = it == null ? "" : it.trim();
            if (v.isBlank()) continue;
            if (sb.length() > 0) sb.append("<br>");
            sb.append(escapeHtml(v));
        }
        return sb.toString();
    }

    private static String buildPaymentDetailHtml(boolean isCash,
                                                 boolean isTransfer,
                                                 boolean isInstallment,
                                                 long totalAmount,
                                                 String paymentRef,
                                                 InstallmentInfo installment,
                                                 String paymentLabel) {
        long total = Math.max(0, totalAmount);
        String totalStr = total <= 0 ? "0 VND" : MoneyVND.format(total);

        if (isCash) {
            Long given = tryParseCashGiven(paymentRef);
            long cashGiven = (given == null || given <= 0) ? total : given;
            long change = Math.max(0, cashGiven - total);
            return paymentBlock(
                    "TIEN MAT",
                    line("So tien khach dua", MoneyVND.format(cashGiven))
                            + line("Tien thua lai", MoneyVND.format(change))
            );
        }

        if (isTransfer) {
            TransferInfo info = tryParseTransferInfo(paymentRef);
            long transferred = (info == null || info.amount <= 0) ? total : info.amount;
            long change = Math.max(0, transferred - total);
            String details = line("So tien khach chuyen khoan", MoneyVND.format(transferred))
                    + line("Tien thua lai", MoneyVND.format(change));
            if (info != null && info.ref != null && !info.ref.isBlank()) {
                details += lineRaw("Noi dung CK", "Thanh toan PhoneStore " + escapeHtml(info.ref));
            }
            return paymentBlock("NGAN HANG", details);
        }

        if (isInstallment && installment != null) {
            String details = line("So tien phai tra", totalStr)
                    + line("Goi", installment.months() + " thang")
                    + line("So tien da dua (tien coc)", MoneyVND.format(installment.upfrontAmount()))
                    + line("Con lai", MoneyVND.format(installment.remainingAmount()))
                    + line("Moi thang phai tra", MoneyVND.format(installment.monthlyAmount()));
            return paymentBlock("TRA GOP", details);
        }

        String label = trim(paymentLabel);
        if (label.isBlank()) {
            return paymentBlock("(CHUA CO)", "");
        }
        return paymentBlock(label.toUpperCase(Locale.ROOT), "");
    }

    private static String paymentBlock(String title, String lines) {
        String t = escapeHtml(trim(title));
        String body = lines == null ? "" : lines;
        if (body.isBlank()) {
            return "<div style=\"font-weight:bold;\">" + t + "</div>";
        }
        return "<div style=\"font-weight:bold;margin-bottom:4px;\">" + t + "</div>" + body;
    }

    private static String line(String label, String value) {
        return lineRaw(escapeHtml(label), escapeHtml(value));
    }

    private static String lineRaw(String labelHtml, String valueHtml) {
        String l = labelHtml == null ? "" : labelHtml;
        String v = valueHtml == null ? "" : valueHtml;
        return "<div style=\"margin:0 0 2px 0;\"><span style=\"font-weight:bold;\">" + l + ":</span> " + v + "</div>";
    }

    private static Long tryParseCashGiven(String paymentRef) {
        if (paymentRef == null) return null;
        String s = paymentRef.trim();
        if (!s.startsWith("CASH|")) return null;
        String[] parts = s.substring("CASH|".length()).split("\\|");
        for (String p : parts) {
            if (p == null) continue;
            String part = p.trim();
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            String k = part.substring(0, eq).trim();
            String v = part.substring(eq + 1).trim();
            if (!k.equals("given")) continue;
            try {
                return Long.parseLong(v);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private static TransferInfo tryParseTransferInfo(String paymentRef) {
        if (paymentRef == null) return null;
        String s = paymentRef.trim();
        if (!s.startsWith("TRANSFER|")) return null;

        String ref = "";
        long amount = 0;

        String[] parts = s.substring("TRANSFER|".length()).split("\\|");
        for (String p : parts) {
            if (p == null) continue;
            String part = p.trim();
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            String k = part.substring(0, eq).trim();
            String v = part.substring(eq + 1).trim();
            try {
                switch (k) {
                    case "ref" -> ref = v;
                    case "amount" -> amount = Long.parseLong(v);
                }
            } catch (Exception ignored) {
            }
        }

        return new TransferInfo(trim(ref), Math.max(0, amount));
    }

    private static final class TransferInfo {
        private final String ref;
        private final long amount;

        private TransferInfo(String ref, long amount) {
            this.ref = ref == null ? "" : ref;
            this.amount = amount;
        }
    }

    private static String formLine(String prefix, String value) {
        String p = trim(prefix);
        String v = trim(value);
        return "<tr><td style=\"padding:6px 8px;\">" + escapeHtml(p + v) + "</td></tr>";
    }

    private static String formFullRow(String value) {
        return "<tr><td style=\"padding:8px;\">" + escapeHtml(value) + "</td></tr>";
    }

    private static String formFullRowRaw(String html) {
        String safe = html == null ? "" : html;
        return "<tr><td style=\"padding:8px;\">" + safe + "</td></tr>";
    }

    private static String formTitleRow(String title) {
        return "<tr><td style=\"padding:10px 8px;text-align:center;font-weight:bold;font-size:15px;\">"
                + escapeHtml(title)
                + "</td></tr>";
    }

    private static String centerLine(String text, boolean bold) {
        String weight = bold ? "font-weight:bold;" : "font-weight:normal;";
        return "<tr><td style=\"padding:6px 8px;text-align:center;" + weight + "\">" + escapeHtml(text) + "</td></tr>";
    }

    private static String formSectionRow(String title) {
        return "<tr><td style=\"padding:10px 8px 4px 8px;font-weight:bold;\">" + escapeHtml(title) + "</td></tr>";
    }

    private static String formBlankRow() {
        return "<tr><td style=\"padding:8px 8px 2px 8px;\">&nbsp;</td></tr>";
    }

    private static String totalsRightBlock(String total, String discount, String payable) {
        String t = trim(total);
        String d = trim(discount);
        String p = trim(payable);

        return "<tr><td style=\"padding:4px 8px 10px 8px;\">"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"border-collapse:separate;border-spacing:0;\">"
                + "<tr>"
                + "<td style=\"width:60%;\">&nbsp;</td>"
                + "<td style=\"width:40%;\">"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"border-collapse:separate;border-spacing:0;\">"
                + totalsRowRight("TONG TIEN", t)
                + totalsRowRight("GIAM GIA", d)
                + totalsRowRight("TIEN PHAI TRA", p)
                + "</table>"
                + "</td>"
                + "</tr></table>"
                + "</td></tr>";
    }

    private static String totalsRowRight(String label, String value) {
        return "<tr>"
                + "<td style=\"padding:4px 0;font-weight:bold;\">" + escapeHtml(label) + ":</td>"
                + "<td style=\"padding:4px 0;text-align:right;\">" + escapeHtml(value) + "</td>"
                + "</tr>";
    }

    private static String th(String text, String extraStyle) {
        String style = "padding:6px 6px;font-weight:bold;text-align:center;" + (extraStyle == null ? "" : extraStyle);
        return "<th style=\"" + style + "\">" + escapeHtml(text) + "</th>";
    }

    private static String td(String text, String extraStyle) {
        String style = "padding:6px 6px;vertical-align:top;" + (extraStyle == null ? "" : extraStyle);
        return "<td style=\"" + style + "\">" + escapeHtml(text) + "</td>";
    }

    private static String tdRaw(String html, String extraStyle) {
        String style = "padding:6px 6px;vertical-align:top;" + (extraStyle == null ? "" : extraStyle);
        return "<td style=\"" + style + "\">" + (html == null ? "" : html) + "</td>";
    }

    private static long computeTotalAmount(ExportReceipt receipt) {
        if (receipt == null) return 0;
        if (receipt.getLines() != null && !receipt.getLines().isEmpty()) {
            long sum = 0;
            for (ExportReceiptLine it : receipt.getLines()) {
                if (it == null) continue;
                sum += it.getLineTotal();
            }
            if (sum > 0) return sum;
        }
        if (receipt.getTotal() == null) return 0;
        return Math.round(receipt.getTotal());
    }

    private static boolean containsIgnoreCase(String s, String needle) {
        if (s == null || needle == null) return false;
        return s.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private static String escapeHtml(String s) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder out = new StringBuilder(s.length() + 32);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(ch);
            }
        }
        return out.toString();
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
