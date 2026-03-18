package com.phonestore.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyVND {

    private static final Locale LOCALE_VI_VN = Locale.forLanguageTag("vi-VN");

    private MoneyVND() {}

    public static String format(Double amount) {
        if (amount == null) return "";
        long v = Math.round(amount);
        return format(v);
    }

    public static String format(long amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(LOCALE_VI_VN);
        nf.setGroupingUsed(true);
        nf.setMaximumFractionDigits(0);
        nf.setMinimumFractionDigits(0);
        return nf.format(amount) + " ₫";
    }

    /**
     * Parses common VND inputs like: "1234567", "1.234.567", "1 234 567 ₫", "1,234,567đ".
     * Treats all non-digit characters as separators/currency markers.
     */
    public static double parseToDouble(String text) {
        if (text == null) throw new IllegalArgumentException("required");
        String s = text.trim();
        if (s.isEmpty()) throw new IllegalArgumentException("required");

        String lower = s.toLowerCase(LOCALE_VI_VN)
                .replace("vnd", "")
                .replace("vnđ", "")
                .replace("₫", "")
                .replace("đ", "")
                .trim();

        boolean negative = lower.startsWith("-");
        String digitsOnly = lower.replaceAll("[^0-9]", "");
        if (digitsOnly.isEmpty()) throw new IllegalArgumentException("required");

        long value = Long.parseLong(digitsOnly);
        return negative ? -value : value;
    }

    public static Double parseToNullableDouble(String text) {
        if (text == null) return null;
        String s = text.trim();
        if (s.isEmpty()) return null;
        return parseToDouble(s);
    }
}
