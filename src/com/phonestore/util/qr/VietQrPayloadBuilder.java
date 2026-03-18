package com.phonestore.util.qr;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Builds VietQR (EMVCo / Napas) payload for bank transfer.
 *
 * This payload is what most Vietnamese banking apps expect when scanning a VietQR.
 * It includes CRC16-CCITT as required by EMV QR.
 */
public final class VietQrPayloadBuilder {

    private VietQrPayloadBuilder() {}

    /**
     * @param bankBin    6-digit bank BIN (e.g. Vietcombank = 970436)
     * @param accountNo  beneficiary account number
     * @param accountName beneficiary name (display purpose, some apps show it)
     * @param amountVnd  amount in VND (integer). 0 or negative means omit.
     * @param purpose    transfer content/purpose (will be trimmed)
     */
    public static String build(
            String bankBin,
            String accountNo,
            String accountName,
            long amountVnd,
            String purpose
    ) {
        String bin = safeDigits(bankBin);
        String acc = safeDigits(accountNo);
        if (bin.length() != 6) {
            throw new IllegalArgumentException("bankBin phải là 6 chữ số (VD: 970436)");
        }
        if (acc.isBlank()) {
            throw new IllegalArgumentException("accountNo không hợp lệ");
        }

        String merchantName = safeText(accountName);
        if (merchantName.isBlank()) merchantName = "NGUOI NHAN";
        merchantName = merchantName.toUpperCase(Locale.ROOT);
        if (merchantName.length() > 25) merchantName = merchantName.substring(0, 25);

        String purposeClean = safeText(purpose);
        if (purposeClean.length() > 50) purposeClean = purposeClean.substring(0, 50);

        // --- Merchant Account Information (Tag 38) ---
        // 00: AID Napas
        // 01: Consumer/merchant account info (nested)
        //    00: bank BIN
        //    01: account number
        // 02: service code (QRIBFTTA = transfer to account)
        String mai01 = tlv("00", bin) + tlv("01", acc);
        String mai = tlv("00", "A000000727")
                + tlv("01", mai01)
                + tlv("02", "QRIBFTTA");

        String payload = ""
                + tlv("00", "01")
                + tlv("01", (amountVnd > 0 ? "12" : "11")) // 12: dynamic (amount), 11: static
                + tlv("38", mai)
            + tlv("52", "4829")
                + tlv("53", "704")
                + (amountVnd > 0 ? tlv("54", Long.toString(amountVnd)) : "")
                + tlv("58", "VN")
                + tlv("59", merchantName)
            + buildAdditionalData(purposeClean);

        // CRC (Tag 63). Value is computed over payload + "6304".
        String toCrc = payload + "6304";
        int crc = crc16Ccitt(toCrc.getBytes(StandardCharsets.UTF_8));
        String crcHex = String.format(Locale.ROOT, "%04X", crc);
        return payload + "63" + "04" + crcHex;
    }

    public static String bankBinFromBankName(String bankName) {
        String name = safeText(bankName).toLowerCase(Locale.ROOT);
        if (name.contains("vietcombank") || name.contains("vcb")) return "970436";
        return "";
    }

    private static String tlv(String id, String value) {
        String v = value == null ? "" : value;
        int len = v.getBytes(StandardCharsets.UTF_8).length;
        if (len > 99) {
            throw new IllegalArgumentException("TLV field too long: " + id + " len=" + len);
        }
        return id + String.format(Locale.ROOT, "%02d", len) + v;
    }

    private static String safeDigits(String s) {
        if (s == null) return "";
        return s.replaceAll("[^0-9]", "");
    }

    private static String safeText(String s) {
        if (s == null) return "";
        return s.trim();
    }

    private static String buildAdditionalData(String purpose) {
        String p = safeText(purpose);
        if (p.isBlank()) return "";

        // Many bank apps use either:
        // - 62/05 (Reference label)
        // - 62/08 (Purpose of transaction)
        // Include both for compatibility.
        String ref = p;
        if (ref.length() > 25) ref = ref.substring(0, 25);
        String full = p;
        if (full.length() > 50) full = full.substring(0, 50);

        String template = tlv("05", ref) + tlv("08", full);
        return tlv("62", template);
    }

    // CRC16-CCITT (poly 0x1021, init 0xFFFF)
    private static int crc16Ccitt(byte[] bytes) {
        int crc = 0xFFFF;
        for (byte b : bytes) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) crc = (crc << 1) ^ 0x1021;
                else crc <<= 1;
                crc &= 0xFFFF;
            }
        }
        return crc & 0xFFFF;
    }
}
