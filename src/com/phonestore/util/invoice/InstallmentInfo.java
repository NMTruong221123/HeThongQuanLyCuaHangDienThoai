package com.phonestore.util.invoice;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record InstallmentInfo(int months, long upfrontAmount, long remainingAmount, long monthlyAmount) {

    private static final String PREFIX = "INSTALLMENT|";

    public static InstallmentInfo of(int months, long totalAmount, long upfrontAmount) {
        int m = Math.max(0, months);
        long total = Math.max(0, totalAmount);
        long upfront = clamp(upfrontAmount, 0, total);
        long remaining = Math.max(0, total - upfront);
        long monthly = 0;
        if (m > 0) {
            monthly = BigDecimal.valueOf(remaining)
                    .divide(BigDecimal.valueOf(m), 0, RoundingMode.CEILING)
                    .longValue();
        }
        return new InstallmentInfo(m, upfront, remaining, monthly);
    }

    public String encodeToPaymentRef() {
        return PREFIX
                + "months=" + months
                + "|upfront=" + upfrontAmount
                + "|remaining=" + remainingAmount
                + "|monthly=" + monthlyAmount;
    }

    public static InstallmentInfo tryParse(String paymentRef) {
        if (paymentRef == null) return null;
        String s = paymentRef.trim();
        if (!s.startsWith(PREFIX)) return null;
        String body = s.substring(PREFIX.length());

        int months = 0;
        long upfront = 0;
        long remaining = 0;
        long monthly = 0;

        String[] parts = body.split("\\|");
        for (String p : parts) {
            if (p == null) continue;
            String part = p.trim();
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            String k = part.substring(0, eq).trim();
            String v = part.substring(eq + 1).trim();
            try {
                switch (k) {
                    case "months" -> months = Integer.parseInt(v);
                    case "upfront" -> upfront = Long.parseLong(v);
                    case "remaining" -> remaining = Long.parseLong(v);
                    case "monthly" -> monthly = Long.parseLong(v);
                }
            } catch (Exception ignored) {
            }
        }

        if (months <= 0) return null;
        return new InstallmentInfo(months, Math.max(0, upfront), Math.max(0, remaining), Math.max(0, monthly));
    }

    private static long clamp(long v, long min, long max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}
