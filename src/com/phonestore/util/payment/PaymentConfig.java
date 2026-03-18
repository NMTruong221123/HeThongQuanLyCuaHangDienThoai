package com.phonestore.util.payment;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class PaymentConfig {

    private PaymentConfig() {}

    public record Bank(String bankName, String accountNo, String accountName) {}

    public static Bank loadBank() {
        String bankName = trimToNull(System.getProperty("pay.bankName"));
        String accountNo = trimToNull(System.getProperty("pay.accountNo"));
        String accountName = trimToNull(System.getProperty("pay.accountName"));

        if (bankName == null) bankName = trimToNull(System.getenv("PHONESTORE_PAY_BANK_NAME"));
        if (accountNo == null) accountNo = trimToNull(System.getenv("PHONESTORE_PAY_ACCOUNT_NO"));
        if (accountName == null) accountName = trimToNull(System.getenv("PHONESTORE_PAY_ACCOUNT_NAME"));

        Properties fp = new Properties();
        loadFromFile(Path.of("payment.properties"), fp);
        loadFromFile(Path.of("config").resolve("payment.properties"), fp);
        loadFromFile(Path.of("resources").resolve("payment.properties"), fp);

        if (bankName == null) bankName = trimToNull(fp.getProperty("pay.bankName"));
        if (accountNo == null) accountNo = trimToNull(fp.getProperty("pay.accountNo"));
        if (accountName == null) accountName = trimToNull(fp.getProperty("pay.accountName"));

        if (bankName == null) bankName = "";
        if (accountNo == null) accountNo = "";
        if (accountName == null) accountName = "";

        return new Bank(bankName, accountNo, accountName);
    }

    private static void loadFromFile(Path path, Properties into) {
        try {
            if (path == null || into == null) return;
            if (!Files.exists(path)) return;
            try (InputStream in = Files.newInputStream(path)) {
                into.load(in);
            }
        } catch (Exception ignored) {
        }
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
