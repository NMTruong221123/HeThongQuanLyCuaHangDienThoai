package com.phonestore.util.mail;

import com.phonestore.model.Customer;
import com.phonestore.model.ExportReceipt;
import com.phonestore.util.invoice.ExportInvoiceTextBuilder;
import com.phonestore.util.invoice.ExportReceiptCodeUtil;

public final class ExportInvoiceMailer {

    private ExportInvoiceMailer() {}

    public static void sendExportInvoice(Customer customer, ExportReceipt receipt, String paymentMethodLabel) {
        if (customer == null) throw new IllegalArgumentException("Khach hang khong hop le");
        if (receipt == null) throw new IllegalArgumentException("Phieu xuat khong hop le");

        String to = trim(customer.getEmail());
        if (to.isBlank()) {
            throw new IllegalArgumentException("Khach hang chua co email");
        }

        String code = trim(receipt.getReceiptCode());
        if (code.isBlank()) code = ExportReceiptCodeUtil.fallbackFromId(receipt.getId());
        String subject = "[PhoneStore] Hoa don ban hang - " + code;
        String body = ExportInvoiceTextBuilder.build(customer, receipt, paymentMethodLabel);
        String html = ExportInvoiceEmailTemplate.buildHtml(customer, receipt, paymentMethodLabel);
        try {
            SmtpMailer.sendHtml(to, subject, html);
        } catch (Exception ex) {
            SmtpMailer.sendText(to, subject, body);
        }
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
