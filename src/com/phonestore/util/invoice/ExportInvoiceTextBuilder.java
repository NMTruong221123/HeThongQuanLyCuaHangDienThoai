package com.phonestore.util.invoice;

import com.phonestore.model.Customer;
import com.phonestore.model.ExportReceipt;
import com.phonestore.model.ExportReceiptLine;
import com.phonestore.util.MoneyVND;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ExportInvoiceTextBuilder {

    private ExportInvoiceTextBuilder() {
    }

    public static String build(Customer customer, ExportReceipt receipt, String paymentMethodLabel) {
        final int OUTER_W = 50;
        final int INNER_W = OUTER_W - 2;
        final String BORDER = repeat('*', OUTER_W);

        String customerName = customer == null ? "" : trim(customer.getName());
        String customerPhone = customer == null ? "" : trim(customer.getPhone());
        String customerEmail = customer == null ? "" : trim(customer.getEmail());

        String receiptCode = "";
        if (receipt != null) {
            receiptCode = trim(receipt.getReceiptCode());
            if (receiptCode.isBlank()) receiptCode = ExportReceiptCodeUtil.fallbackFromId(receipt.getId());
        }

        String when = (receipt == null || receipt.getTime() == null)
                ? LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                : receipt.getTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        long totalAmount = computeTotalAmount(receipt);

        // Employee info
        String empCode = "";
        String empName = "";
        String empPhone = "";
        String empEmail = "";
        if (receipt != null && receipt.getCreatedBy() != null) {
            try {
                com.phonestore.model.Employee e = new com.phonestore.util.service.impl.EmployeeServiceImpl().getById(receipt.getCreatedBy());
                if (e != null) {
                    empCode = trim(e.getCode());
                    empName = trim(e.getFullName());
                    empPhone = trim(e.getPhone());
                    empEmail = trim(e.getEmail());
                }
            } catch (Exception ignored) {
            }
        }
        String total = totalAmount <= 0 ? "0 VND" : MoneyVND.format(totalAmount);

        String pay = trim(paymentMethodLabel);
        String ref = receipt == null ? "" : trim(receipt.getPaymentRef());
        String refUpper = ref.toUpperCase(Locale.ROOT);
        boolean isTransfer = refUpper.startsWith("TRANSFER|") || pay.toLowerCase(Locale.ROOT).contains("chuyen khoan");
        boolean isInstallment = refUpper.startsWith("INSTALL|") || pay.toLowerCase(Locale.ROOT).contains("tra gop");
        boolean isCash = !isTransfer && !isInstallment;

        InstallmentInfo installment = isInstallment ? InstallmentInfo.tryParse(ref) : null;
        Long cashGiven = isCash ? tryParseCashGiven(ref) : null;
        TransferInfo transferInfo = isTransfer ? tryParseTransferInfo(ref) : null;

        StringBuilder sb = new StringBuilder();

        sb.append(BORDER).append("\n");
        sb.append(boxCenter("HOA DON BAN HANG", INNER_W)).append("\n");
        sb.append(boxCenter("CUA HANG DIEN THOAI PHONESTORE", INNER_W)).append("\n");
        sb.append(boxCenter("Dia chi: Can Tho", INNER_W)).append("\n");
        sb.append(boxCenter("Hotline: 0367678723", INNER_W)).append("\n");
        sb.append(BORDER).append("\n");

        sb.append(boxCenter("Ma don hang: " + receiptCode, INNER_W)).append("\n");
        sb.append(boxCenter("Ngay ban: " + when, INNER_W)).append("\n");
        sb.append(BORDER).append("\n");
        sb.append(boxCenter("THONG TIN NHAN VIEN", INNER_W)).append("\n");
        sb.append(boxIndentLine(2, "Ma nhan vien: " + (empCode.isBlank() ? "(chua co)" : empCode), INNER_W)).append("\n");
        sb.append(boxIndentLine(2, "Ten nhan vien: " + (empName.isBlank() ? "(chua co)" : empName), INNER_W)).append("\n");
        sb.append(boxIndentLine(2, "SDT: " + (empPhone.isBlank() ? "(chua co)" : empPhone), INNER_W)).append("\n");
        sb.append(boxIndentLine(2, "Email: " + (empEmail.isBlank() ? "(chua co)" : empEmail), INNER_W)).append("\n");
        sb.append(BORDER).append("\n");

        sb.append(boxCenter("THONG TIN KHACH HANG", INNER_W)).append("\n");
        sb.append(BORDER).append("\n");
        sb.append(boxIndentLine(2, "Ho ten: " + (customerName.isBlank() ? "(chua co)" : customerName), INNER_W)).append("\n");
        sb.append(boxIndentLine(2, "SDT: " + (customerPhone.isBlank() ? "(chua co)" : customerPhone), INNER_W)).append("\n");
        sb.append(boxIndentLine(2, "Email: " + (customerEmail.isBlank() ? "(chua co)" : customerEmail), INNER_W)).append("\n");
        sb.append(BORDER).append("\n");

        sb.append(boxCenter("THONG TIN SAN PHAM", INNER_W)).append("\n");
        sb.append(BORDER).append("\n");

        final String tableHeader = " STT | TEN SP/CAU HINH         | SL | THANH TIEN ";
        final String tableSep = " ----|-------------------------|----|----------- ";
        sb.append(boxFit(tableHeader, INNER_W)).append("\n");
        sb.append(boxFit(tableSep, INNER_W)).append("\n");

        List<ExportReceiptLine> items = receipt == null ? null : receipt.getLines();
        if (items != null && !items.isEmpty()) {
            int idx = 1;
            for (ExportReceiptLine it : items) {
                if (it == null) continue;
                appendTableItem(sb, idx++, it, totalAmount, tableHeader, INNER_W);
            }
        } else {
            String row = formatTableRow(tableHeader, "1", "(Hang hoa theo phieu)", "1", totalAmount <= 0 ? "" : MoneyVND.format(totalAmount));
            sb.append(boxFit(row, INNER_W)).append("\n");
        }

        appendImeisBlock(sb, items, INNER_W);
        sb.append(BORDER).append("\n");

        sb.append(boxCenter("THANH TOAN", INNER_W)).append("\n");
        sb.append(BORDER).append("\n");
        sb.append(boxKvRight(2, "Tong tien hang", total, 2, INNER_W)).append("\n");
        sb.append(boxKvRight(2, "Giam gia", "0 VND", 2, INNER_W)).append("\n");
        sb.append(boxKvRight(2, "Tien phai tra", total, 2, INNER_W)).append("\n");
        sb.append(boxIndentLine(2, "Hinh thuc thanh toan:", INNER_W)).append("\n");
        sb.append(boxIndentLine(4, "[" + (isCash ? "x" : " ") + "] Tien mat", INNER_W)).append("\n");
        sb.append(boxIndentLine(4, "[" + (isTransfer ? "x" : " ") + "] Chuyen khoan", INNER_W)).append("\n");
        sb.append(boxIndentLine(4, "[" + (isInstallment ? "x" : " ") + "] Tra gop", INNER_W)).append("\n");

        if (isCash) {
            long given = (cashGiven == null || cashGiven <= 0) ? totalAmount : cashGiven;
            long change = Math.max(0, given - Math.max(0, totalAmount));
            sb.append(boxIndentLine(4, "Tien khach dua: " + MoneyVND.format(given), INNER_W)).append("\n");
            sb.append(boxIndentLine(4, "Tien thua lai: " + MoneyVND.format(change), INNER_W)).append("\n");
        }

        if (isTransfer) {
            long transferred = transferInfo == null ? Math.max(0, totalAmount) : Math.max(0, transferInfo.amount);
            long change = Math.max(0, transferred - Math.max(0, totalAmount));
            sb.append(boxIndentLine(4, "Tien chuyen khoan: " + MoneyVND.format(transferred), INNER_W)).append("\n");
            sb.append(boxIndentLine(4, "Tien thua lai: " + MoneyVND.format(change), INNER_W)).append("\n");
            String transferRef = (transferInfo == null || transferInfo.ref.isBlank()) ? ref : transferInfo.ref;
            if (!transferRef.isBlank()) {
                sb.append(boxIndentLine(4, "Noi dung CK: Thanh toan PhoneStore " + transferRef, INNER_W)).append("\n");
            }
        }

        if (isInstallment && installment != null) {
            sb.append(boxIndentLine(4, "So tien phai tra: " + total, INNER_W)).append("\n");
            sb.append(boxIndentLine(4, "Goi: " + installment.months() + " thang", INNER_W)).append("\n");
            sb.append(boxIndentLine(4, "Tra truoc: " + MoneyVND.format(installment.upfrontAmount()), INNER_W)).append("\n");
            sb.append(boxIndentLine(4, "Con lai: " + MoneyVND.format(installment.remainingAmount()), INNER_W)).append("\n");
            sb.append(boxIndentLine(4, "Moi thang: " + MoneyVND.format(installment.monthlyAmount()), INNER_W)).append("\n");
        }

        sb.append(BORDER).append("\n");
        sb.append(boxCenter("Xin cam on Quy khach da mua hang!", INNER_W)).append("\n");
        sb.append(BORDER).append("\n");
        return sb.toString();
    }

    private static void appendImeisBlock(StringBuilder sb, List<ExportReceiptLine> items, int innerW) {
        List<String> imeis = new ArrayList<>();
        if (items != null) {
            for (ExportReceiptLine it : items) {
                if (it == null || it.getImeis() == null) continue;
                for (String v : it.getImeis()) {
                    String s = v == null ? "" : v.trim();
                    if (!s.isBlank()) imeis.add(s);
                }
            }
        }

        if (imeis.isEmpty()) {
            sb.append(boxIndentLine(2, "IMEI: -", innerW)).append("\n");
            return;
        }

        sb.append(boxIndentLine(2, "IMEI:", innerW)).append("\n");
        for (String imei : imeis) {
            sb.append(boxIndentLine(4, "- " + imei, innerW)).append("\n");
        }
    }

    private static void appendTableItem(StringBuilder sb,
                                        int idx,
                                        ExportReceiptLine it,
                                        long totalAmount,
                                        String tableHeader,
                                        int innerW) {
        String name = trim(it.getProductName());
        if (name.isBlank()) name = "(Hang hoa theo phieu)";
        String cfg = trim(it.getVariantLabel());
        if (!cfg.isBlank()) name = name + " " + cfg;

        int qty = it.getQuantity() == null ? 0 : it.getQuantity();
        long lineTotal = it.getLineTotal();
        String qtyStr = qty <= 0 ? "" : String.valueOf(qty);
        String totalStr = lineTotal <= 0 ? (totalAmount <= 0 ? "" : MoneyVND.format(totalAmount)) : MoneyVND.format(lineTotal);

        List<String> nameLines = wrap(name, nameColumnWidthFromHeader(tableHeader));
        if (nameLines.isEmpty()) nameLines = List.of("(Hang hoa theo phieu)");

        for (int i = 0; i < nameLines.size(); i++) {
            String stt = (i == 0) ? String.valueOf(idx) : "";
            String sl = (i == 0) ? qtyStr : "";
            String amount = (i == 0) ? totalStr : "";
            String row = formatTableRow(tableHeader, stt, nameLines.get(i), sl, amount);
            sb.append(boxFit(row, innerW)).append("\n");
        }
    }

    private static int nameColumnWidthFromHeader(String header) {
        if (header == null) return 24;
        int p1 = header.indexOf('|');
        int p2 = header.indexOf('|', p1 + 1);
        if (p1 < 0 || p2 < 0) return 24;
        int segLen = p2 - (p1 + 1);
        return Math.max(6, segLen - 2);
    }

    private static String formatTableRow(String tableHeader, String stt, String name, String sl, String amount) {
        String header = tableHeader == null ? "" : tableHeader;
        int p1 = header.indexOf('|');
        int p2 = header.indexOf('|', p1 + 1);
        int p3 = header.indexOf('|', p2 + 1);
        if (p1 < 0 || p2 < 0 || p3 < 0) {
            return " " + trim(stt) + " | " + trim(name) + " | " + trim(sl) + " | " + trim(amount);
        }

        int sttLen = p1;
        int nameLen = p2 - (p1 + 1);
        int slLen = p3 - (p2 + 1);
        int amtLen = header.length() - (p3 + 1);

        String segStt = fit(trim(stt), sttLen, true);
        String segName = fit(" " + trim(name), nameLen, false);
        String segSl = fit(trim(sl), slLen, true);
        String segAmt = fit(trim(amount), amtLen, true);

        return segStt + "|" + segName + "|" + segSl + "|" + segAmt;
    }

    private static String boxFit(String content, int innerW) {
        return "*" + fit(content, innerW, false) + "*";
    }

    private static String boxCenter(String content, int innerW) {
        String t = trim(content);
        if (t.length() > innerW) t = t.substring(0, innerW);
        int left = Math.max(0, (innerW - t.length()) / 2);
        String out = spaces(left) + t;
        return "*" + fit(out, innerW, false) + "*";
    }

    private static String boxIndentLine(int indent, String text, int innerW) {
        String out = spaces(Math.max(0, indent)) + trim(text);
        return "*" + fit(out, innerW, false) + "*";
    }

    private static String boxKvRight(int indent, String key, String value, int trailingSpaces, int innerW) {
        String k = trim(key);
        String v = trim(value);
        String prefix = spaces(Math.max(0, indent)) + k + ":";
        int tail = Math.max(0, trailingSpaces);
        int valueStart = Math.max(prefix.length() + 1, innerW - tail - v.length());
        String out = prefix + spaces(valueStart - prefix.length()) + v + spaces(tail);
        return "*" + fit(out, innerW, false) + "*";
    }

    private static String fit(String s, int width, boolean rightAlign) {
        String t = s == null ? "" : s;
        if (t.length() == width) return t;
        if (t.length() > width) return t.substring(0, Math.max(0, width));
        int pad = width - t.length();
        return rightAlign ? spaces(pad) + t : t + spaces(pad);
    }

    private static String repeat(char ch, int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) sb.append(ch);
        return sb.toString();
    }

    private static String spaces(int count) {
        return repeat(' ', count);
    }

    private static List<String> wrap(String text, int width) {
        String t = trim(text);
        List<String> out = new ArrayList<>();
        if (t.isBlank() || width <= 0) return out;

        int pos = 0;
        while (pos < t.length()) {
            int end = Math.min(t.length(), pos + width);
            if (end < t.length()) {
                int lastSpace = t.lastIndexOf(' ', end);
                if (lastSpace > pos + 3) end = lastSpace;
            }
            String part = t.substring(pos, end).trim();
            if (!part.isBlank()) out.add(part);
            pos = end;
            while (pos < t.length() && t.charAt(pos) == ' ') pos++;
        }
        return out;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
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
}
