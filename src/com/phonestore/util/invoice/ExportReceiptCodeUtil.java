package com.phonestore.util.invoice;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

public final class ExportReceiptCodeUtil {

    private ExportReceiptCodeUtil() {
    }

    public static String build(LocalDateTime time, String orderCode) {
        LocalDateTime t = time == null ? LocalDateTime.now() : time;
        int day = t.getDayOfMonth();
        int month = t.getMonthValue();
        int year2 = t.getYear() % 100;
        String dow = toDow2(t.getDayOfWeek());
        String ord = (orderCode == null || orderCode.trim().isEmpty()) ? "NEW" : orderCode.trim();
        return "PXPS-" + day + month + String.format("%02d", year2) + dow + "#" + ord;
    }

    public static String fallbackFromId(long id) {
        return "PX#" + id;
    }

    private static String toDow2(DayOfWeek d) {
        if (d == null) return "UN";
        return switch (d) {
            case MONDAY -> "MO";
            case TUESDAY -> "TU";
            case WEDNESDAY -> "WE";
            case THURSDAY -> "TH";
            case FRIDAY -> "FR";
            case SATURDAY -> "SA";
            case SUNDAY -> "SU";
        };
    }
}
