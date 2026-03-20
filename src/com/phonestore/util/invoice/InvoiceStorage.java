package com.phonestore.util.invoice;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

public final class InvoiceStorage {

    private InvoiceStorage() {}

    public static Path saveExportReceiptInvoiceText(long exportReceiptId, String invoiceText) {
        try {
            Path dir = Path.of("data", "export", "bills");
            Files.createDirectories(dir);

            cleanupOldBills(dir, 3);

            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String safeId = exportReceiptId <= 0 ? "unknown" : String.valueOf(exportReceiptId);
            Path file = dir.resolve("bill_px_" + safeId + "_" + ts + ".txt");

            String text = invoiceText == null ? "" : invoiceText;
            Files.writeString(file, text, StandardCharsets.UTF_8);
            return file;
        } catch (Exception e) {
            throw new RuntimeException("Không lưu được hóa đơn text: " + (e.getMessage() == null ? e : e.getMessage()), e);
        }
    }

    private static void cleanupOldBills(Path dir, int monthsToKeep) {
        if (dir == null) return;
        int m = Math.max(1, monthsToKeep);
        Instant cutoff = ZonedDateTime.now(ZoneId.systemDefault()).minusMonths(m).toInstant();
        try (Stream<Path> st = Files.list(dir)) {
            st.filter(p -> {
                        String n = p.getFileName() == null ? "" : p.getFileName().toString();
                        return n.startsWith("bill_px_") && n.endsWith(".txt");
                    })
                    .forEach(p -> {
                        try {
                            Instant lm = Files.getLastModifiedTime(p).toInstant();
                            if (lm.isBefore(cutoff)) {
                                Files.deleteIfExists(p);
                            }
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {
        }
    }
}
