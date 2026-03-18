package com.phonestore.dao.jdbc;

import com.phonestore.config.JDBCUtil;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Best-effort IMEI registry.
 * Keeps imported IMEIs and supports sale consumption by variant.
 */
public class ImeiRegistryJdbcDao {

    private static final String TABLE = "imei_registry";
    private static final String COL_IMEI = "imei";
    private static final String COL_VARIANT_ID = "variant_id";
    private static final String COL_PRODUCT_ID = "product_id";
    private static final String COL_IMPORT_RECEIPT_ID = "import_receipt_id";
    private static final String COL_EXPORT_RECEIPT_ID = "export_receipt_id";
    private static final String COL_SOLD = "sold";

    private final SecureRandom rnd = new SecureRandom();

    public void ensureTable() {
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) {
                throw new IllegalStateException(JDBCUtil.buildConnectionError("Chua ket noi duoc DB"));
            }
            ensureTable(c);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    private void ensureTable(Connection c) throws Exception {
        String create = "CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + COL_IMEI + " VARCHAR(15) NOT NULL,"
                + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (" + COL_IMEI + ")"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci";
        try (Statement st = c.createStatement()) {
            st.execute(create);
        }

        if (!JDBCUtil.hasColumn(TABLE, COL_VARIANT_ID)) {
            try (Statement st = c.createStatement()) {
                st.execute("ALTER TABLE " + TABLE + " ADD COLUMN " + COL_VARIANT_ID + " BIGINT NULL");
            }
        }
        if (!JDBCUtil.hasColumn(TABLE, COL_PRODUCT_ID)) {
            try (Statement st = c.createStatement()) {
                st.execute("ALTER TABLE " + TABLE + " ADD COLUMN " + COL_PRODUCT_ID + " BIGINT NULL");
            }
        }
        if (!JDBCUtil.hasColumn(TABLE, COL_SOLD)) {
            try (Statement st = c.createStatement()) {
                st.execute("ALTER TABLE " + TABLE + " ADD COLUMN " + COL_SOLD + " TINYINT(1) NOT NULL DEFAULT 0");
            }
        }
        if (!JDBCUtil.hasColumn(TABLE, COL_IMPORT_RECEIPT_ID)) {
            try (Statement st = c.createStatement()) {
                st.execute("ALTER TABLE " + TABLE + " ADD COLUMN " + COL_IMPORT_RECEIPT_ID + " BIGINT NULL");
            } catch (Exception ignored) {
                // Another process/version may have added it already.
            }
        }
        if (!JDBCUtil.hasColumn(TABLE, COL_EXPORT_RECEIPT_ID)) {
            try (Statement st = c.createStatement()) {
                st.execute("ALTER TABLE " + TABLE + " ADD COLUMN " + COL_EXPORT_RECEIPT_ID + " BIGINT NULL");
            } catch (Exception ignored) {
            }
        }
        // Ignore duplicate index errors.
        try (Statement st = c.createStatement()) {
            st.execute("CREATE INDEX idx_imei_registry_variant_sold ON " + TABLE + " (" + COL_VARIANT_ID + ", " + COL_SOLD + ")");
        } catch (Exception ignored) {
        }
        try (Statement st = c.createStatement()) {
            st.execute("CREATE INDEX idx_imei_registry_product ON " + TABLE + " (" + COL_PRODUCT_ID + ")");
        } catch (Exception ignored) {
        }
        try (Statement st = c.createStatement()) {
            st.execute("CREATE INDEX idx_imei_registry_import_receipt ON " + TABLE + " (" + COL_IMPORT_RECEIPT_ID + ")");
        } catch (Exception ignored) {
        }
        try (Statement st = c.createStatement()) {
            st.execute("CREATE INDEX idx_imei_registry_export_receipt ON " + TABLE + " (" + COL_EXPORT_RECEIPT_ID + ")");
        } catch (Exception ignored) {
        }
    }

    /**
     * Reserve one IMEI.
     */
    public boolean tryReserve(String imei) {
        String v = normalizeImei(imei);
        if (!isValidImei15Digits(v)) throw new IllegalArgumentException("IMEI khong hop le");
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException(JDBCUtil.buildConnectionError("Chua ket noi duoc DB"));
            ensureTable(c);
            return tryReserve(c, v, null, null);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    /**
     * Reserve all IMEIs for one variant/product.
     */
    public void reserveAllForVariant(List<String> imeis, Long productId, Long variantId) {
        if (imeis == null || imeis.isEmpty()) throw new IllegalArgumentException("Danh sach IMEI trong");
        List<Long> productIds = new ArrayList<>(imeis.size());
        List<Long> variantIds = new ArrayList<>(imeis.size());
        for (int i = 0; i < imeis.size(); i++) {
            productIds.add(productId);
            variantIds.add(variantId);
        }
        reserveAllDetailed(imeis, productIds, variantIds);
    }

    /**
     * Reserve all IMEIs with per-IMEI product/variant metadata in one transaction.
     */
    public void reserveAllDetailed(List<String> imeis, List<Long> productIds, List<Long> variantIds) {
        if (imeis == null || imeis.isEmpty()) throw new IllegalArgumentException("Danh sach IMEI trong");
        int n = imeis.size();
        if (productIds != null && productIds.size() != n) throw new IllegalArgumentException("Danh sach productId khong khop");
        if (variantIds != null && variantIds.size() != n) throw new IllegalArgumentException("Danh sach variantId khong khop");

        List<String> normalized = new ArrayList<>(n);
        Set<String> seen = new HashSet<>();
        for (String it : imeis) {
            String v = normalizeImei(it);
            if (!isValidImei15Digits(v)) throw new IllegalArgumentException("IMEI khong hop le: " + v);
            if (!seen.add(v)) throw new IllegalArgumentException("IMEI bi trung trong danh sach: " + v);
            normalized.add(v);
        }

        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException(JDBCUtil.buildConnectionError("Chua ket noi duoc DB"));
            ensureTable(c);
            c.setAutoCommit(false);
            for (int i = 0; i < normalized.size(); i++) {
                String imei = normalized.get(i);
                Long pid = productIds == null ? null : productIds.get(i);
                Long vid = variantIds == null ? null : variantIds.get(i);
                boolean ok = tryReserve(c, imei, pid, vid);
                if (!ok) {
                    c.rollback();
                    throw new IllegalStateException("IMEI da ton tai trong CSDL: " + imei);
                }
            }
            c.commit();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    /**
     * Backward compatible reserve-all API.
     */
    public void reserveAll(List<String> imeis) {
        reserveAllForVariant(imeis, null, null);
    }

    /**
     * Returns available IMEIs by variant.
     */
    public List<String> findAvailableImeisByVariantId(long variantId) {
        if (variantId <= 0) return new ArrayList<>();
        String sql = "SELECT " + COL_IMEI + " FROM " + TABLE
                + " WHERE " + COL_VARIANT_ID + "=? AND " + COL_SOLD + "=0"
                + " ORDER BY " + COL_IMEI + " ASC";
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException(JDBCUtil.buildConnectionError("Chua ket noi duoc DB"));
            ensureTable(c);
            List<String> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, variantId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String v = rs.getString(1);
                        if (v != null && !v.isBlank()) out.add(v.trim());
                    }
                }
            }
            return out;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    /**
     * Fallback for old imported IMEIs without variant mapping.
     */
    public List<String> findAvailableImeisByProductId(long productId) {
        if (productId <= 0) return new ArrayList<>();
        String sql = "SELECT " + COL_IMEI + " FROM " + TABLE
                + " WHERE " + COL_PRODUCT_ID + "=? AND " + COL_SOLD + "=0"
                + " ORDER BY " + COL_IMEI + " ASC";
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException(JDBCUtil.buildConnectionError("Chua ket noi duoc DB"));
            ensureTable(c);
            List<String> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String v = rs.getString(1);
                        if (v != null && !v.isBlank()) out.add(v.trim());
                    }
                }
            }
            return out;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    /**
     * Returns all unsold IMEIs.
     */
    public List<String> findAllImeis() {
        String sql = "SELECT " + COL_IMEI + " FROM " + TABLE
                + " WHERE " + COL_SOLD + "=0 ORDER BY " + COL_IMEI + " ASC";
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException(JDBCUtil.buildConnectionError("Chua ket noi duoc DB"));
            ensureTable(c);
            List<String> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String v = rs.getString(1);
                    if (v != null && !v.isBlank()) out.add(v.trim());
                }
            }
            return out;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    /**
     * Mark sold after payment success.
     */
    public int markSoldImeis(List<String> imeis) {
        if (imeis == null || imeis.isEmpty()) return 0;
        List<String> normalized = normalizeValidList(imeis);
        if (normalized.isEmpty()) return 0;
        String sql = "UPDATE " + TABLE + " SET " + COL_SOLD + "=1 WHERE " + COL_IMEI + "=? AND " + COL_SOLD + "=0";
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException(JDBCUtil.buildConnectionError("Chua ket noi duoc DB"));
            ensureTable(c);
            c.setAutoCommit(false);
            int updated = 0;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (String imei : normalized) {
                    ps.setString(1, imei);
                    updated += ps.executeUpdate();
                }
            }
            c.commit();
            return updated;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    /**
     * Keep API for existing callers.
     */
    public int removeAllImeis(List<String> imeis) {
        return markSoldImeis(imeis);
    }

    public boolean removeImei(String imei) {
        return markSoldImeis(java.util.List.of(imei)) > 0;
    }

    public int markSoldImeisForReceipt(List<String> imeis, long exportReceiptId) {
        if (imeis == null || imeis.isEmpty()) return 0;
        List<String> normalized = normalizeValidList(imeis);
        if (normalized.isEmpty()) return 0;
        String sql = "UPDATE " + TABLE + " SET " + COL_SOLD + "=1, " + COL_EXPORT_RECEIPT_ID + "=? WHERE " + COL_IMEI + "=? AND " + COL_SOLD + "=0";
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException(JDBCUtil.buildConnectionError("Chua ket noi duoc DB"));
            ensureTable(c);
            c.setAutoCommit(false);
            int updated = 0;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (String imei : normalized) {
                    ps.setLong(1, Math.max(0, exportReceiptId));
                    ps.setString(2, imei);
                    updated += ps.executeUpdate();
                }
            }
            c.commit();
            return updated;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    public void bindImeisToImportReceipt(long receiptId, List<String> imeis) {
        if (receiptId <= 0 || imeis == null || imeis.isEmpty()) return;
        List<String> normalized = normalizeValidList(imeis);
        if (normalized.isEmpty()) return;
        String sql = "UPDATE " + TABLE + " SET " + COL_IMPORT_RECEIPT_ID + "=? WHERE " + COL_IMEI + "=?";
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException(JDBCUtil.buildConnectionError("Chua ket noi duoc DB"));
            ensureTable(c);
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (String imei : normalized) {
                    ps.setLong(1, receiptId);
                    ps.setString(2, imei);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            c.commit();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    /**
     * Bind IMEIs to an import receipt with per-IMEI product/variant mapping.
     * For each IMEI, insert if missing or update existing row to set product/variant if not present,
     * then set the import_receipt_id to the given receiptId.
     */
    public void bindDetailedImeisToImportReceipt(long receiptId, List<String> imeis, List<Long> productIds, List<Long> variantIds) {
        if (receiptId <= 0 || imeis == null || imeis.isEmpty()) return;
        int n = imeis.size();
        if (productIds != null && productIds.size() != n) throw new IllegalArgumentException("productIds length mismatch");
        if (variantIds != null && variantIds.size() != n) throw new IllegalArgumentException("variantIds length mismatch");

        List<String> normalized = normalizeValidList(imeis);
        if (normalized.isEmpty()) return;

        String insertSql = "INSERT INTO " + TABLE + " (" + COL_IMEI + ", " + COL_PRODUCT_ID + ", " + COL_VARIANT_ID + ", " + COL_IMPORT_RECEIPT_ID + ") VALUES (?, ?, ?, ?)";
        String updateSql = "UPDATE " + TABLE + " SET " + COL_PRODUCT_ID + "=COALESCE(" + COL_PRODUCT_ID + ", ?), " + COL_VARIANT_ID + "=COALESCE(" + COL_VARIANT_ID + ", ?), " + COL_IMPORT_RECEIPT_ID + "=? WHERE " + COL_IMEI + "=?";

        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException(JDBCUtil.buildConnectionError("Chua ket noi duoc DB"));
            ensureTable(c);
            c.setAutoCommit(false);
            try (PreparedStatement ins = c.prepareStatement(insertSql); PreparedStatement upd = c.prepareStatement(updateSql)) {
                for (int i = 0; i < normalized.size(); i++) {
                    String imei = normalized.get(i);
                    Long pid = productIds == null ? null : productIds.get(i);
                    Long vid = variantIds == null ? null : variantIds.get(i);
                    try {
                        ins.setString(1, imei);
                        if (pid == null) ins.setNull(2, Types.BIGINT); else ins.setLong(2, pid);
                        if (vid == null) ins.setNull(3, Types.BIGINT); else ins.setLong(3, vid);
                        ins.setLong(4, receiptId);
                        ins.executeUpdate();
                    } catch (Exception ex) {
                        // duplicate key or other insert failure -> fallback to update
                        upd.setObject(1, pid == null ? null : pid, pid == null ? Types.BIGINT : Types.BIGINT);
                        upd.setObject(2, vid == null ? null : vid, vid == null ? Types.BIGINT : Types.BIGINT);
                        upd.setLong(3, receiptId);
                        upd.setString(4, imei);
                        upd.executeUpdate();
                    }
                }
            }
            c.commit();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    public List<String> findImeisByImportReceipt(long receiptId, Long variantId, Long productId, int limit) {
        if (receiptId <= 0) return new ArrayList<>();
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException(JDBCUtil.buildConnectionError("Chua ket noi duoc DB"));
            ensureTable(c);

            StringBuilder sb = new StringBuilder();
            sb.append("SELECT ").append(COL_IMEI)
              .append(" FROM ").append(TABLE)
              .append(" WHERE ").append(COL_IMPORT_RECEIPT_ID).append("=?");

            boolean useVariant = variantId != null && variantId > 0;
            boolean useProduct = !useVariant && productId != null && productId > 0;
            if (useVariant) {
                sb.append(" AND ").append(COL_VARIANT_ID).append("=?");
            } else if (useProduct) {
                sb.append(" AND ").append(COL_PRODUCT_ID).append("=?");
            }

            sb.append(" ORDER BY created_at ASC, ").append(COL_IMEI).append(" ASC");
            if (limit > 0) sb.append(" LIMIT ?");

            List<String> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sb.toString())) {
                int idx = 1;
                ps.setLong(idx++, receiptId);
                if (useVariant) {
                    ps.setLong(idx++, variantId);
                } else if (useProduct) {
                    ps.setLong(idx++, productId);
                }
                if (limit > 0) ps.setInt(idx, limit);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String v = rs.getString(1);
                        if (v != null && !v.isBlank()) out.add(v.trim());
                    }
                }
            }
            return out;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    public List<String> findImeisByExportReceipt(long receiptId, Long variantId, Long productId, int limit) {
        if (receiptId <= 0) return new ArrayList<>();
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException(JDBCUtil.buildConnectionError("Chua ket noi duoc DB"));
            ensureTable(c);

            StringBuilder sb = new StringBuilder();
            sb.append("SELECT ").append(COL_IMEI)
                .append(" FROM ").append(TABLE)
                .append(" WHERE ").append(COL_EXPORT_RECEIPT_ID).append("=?");

            boolean useVariant = variantId != null && variantId > 0;
            boolean useProduct = !useVariant && productId != null && productId > 0;
            if (useVariant) {
                sb.append(" AND ").append(COL_VARIANT_ID).append("=?");
            } else if (useProduct) {
                sb.append(" AND ").append(COL_PRODUCT_ID).append("=?");
            }

            sb.append(" ORDER BY created_at ASC, ").append(COL_IMEI).append(" ASC");
            if (limit > 0) sb.append(" LIMIT ?");

            List<String> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sb.toString())) {
                int idx = 1;
                ps.setLong(idx++, receiptId);
                if (useVariant) {
                    ps.setLong(idx++, variantId);
                } else if (useProduct) {
                    ps.setLong(idx++, productId);
                }
                if (limit > 0) ps.setInt(idx, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String v = rs.getString(1);
                        if (v != null && !v.isBlank()) out.add(v.trim());
                    }
                }
            }
            return out;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    /**
     * Returns subset of IMEIs already imported.
     */
    public Set<String> findExisting(List<String> imeis) {
        if (imeis == null || imeis.isEmpty()) return new HashSet<>();
        List<String> normalized = normalizeValidList(imeis);
        if (normalized.isEmpty()) return new HashSet<>();
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException(JDBCUtil.buildConnectionError("Chua ket noi duoc DB"));
            ensureTable(c);
            Set<String> out = new HashSet<>();
            int chunkSize = 400;
            for (int from = 0; from < normalized.size(); from += chunkSize) {
                int to = Math.min(normalized.size(), from + chunkSize);
                List<String> chunk = normalized.subList(from, to);
                StringBuilder sb = new StringBuilder();
                sb.append("SELECT ").append(COL_IMEI).append(" FROM ").append(TABLE).append(" WHERE ").append(COL_IMEI).append(" IN (");
                for (int i = 0; i < chunk.size(); i++) {
                    if (i > 0) sb.append(',');
                    sb.append('?');
                }
                sb.append(')');
                try (PreparedStatement ps = c.prepareStatement(sb.toString())) {
                    for (int i = 0; i < chunk.size(); i++) ps.setString(i + 1, chunk.get(i));
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String v = rs.getString(1);
                            if (v != null && !v.isBlank()) out.add(v.trim());
                        }
                    }
                }
            }
            return out;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    /**
     * Reserves a sequential range.
     */
    public List<String> reserveSequential(String startImei, int qty) {
        String start = normalizeImei(startImei);
        if (!isValidImei15Digits(start)) throw new IllegalArgumentException("IMEI bat dau phai dung 15 chu so");
        if (qty <= 0) throw new IllegalArgumentException("So luong phai > 0");
        List<String> list = buildSequential(start, qty);
        reserveAll(list);
        return list;
    }

    /**
     * Reserves random IMEIs.
     */
    public List<String> reserveRandom(int qty) {
        if (qty <= 0) throw new IllegalArgumentException("So luong phai > 0");
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException(JDBCUtil.buildConnectionError("Chua ket noi duoc DB"));
            ensureTable(c);
            c.setAutoCommit(false);

            List<String> out = new ArrayList<>();
            Set<String> inBatch = new HashSet<>();
            int maxTries = Math.max(2000, qty * 200);
            for (int tries = 0; tries < maxTries && out.size() < qty; tries++) {
                String imei = randomImei15();
                if (!inBatch.add(imei)) continue;
                boolean ok = tryReserve(c, imei, null, null);
                if (ok) out.add(imei);
            }
            if (out.size() != qty) {
                c.rollback();
                throw new IllegalStateException("Khong the sinh du IMEI ngau nhien khong trung (qty=" + qty + ")");
            }
            c.commit();
            return out;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    private boolean tryReserve(Connection c, String imei, Long productId, Long variantId) throws Exception {
        String sql = "INSERT IGNORE INTO " + TABLE + "(" + COL_IMEI + ", " + COL_PRODUCT_ID + ", " + COL_VARIANT_ID + ", " + COL_SOLD + ") VALUES (?, ?, ?, 0)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, imei);
            if (productId == null || productId <= 0) ps.setNull(2, Types.BIGINT); else ps.setLong(2, productId);
            if (variantId == null || variantId <= 0) ps.setNull(3, Types.BIGINT); else ps.setLong(3, variantId);
            int updated = ps.executeUpdate();
            return updated > 0;
        }
    }

    private List<String> normalizeValidList(List<String> imeis) {
        List<String> out = new ArrayList<>();
        for (String it : imeis) {
            String v = normalizeImei(it);
            if (v.isBlank()) continue;
            if (!isValidImei15Digits(v)) throw new IllegalArgumentException("IMEI khong hop le: " + v);
            out.add(v);
        }
        return out;
    }

    private List<String> buildSequential(String start, int qty) {
        long base;
        try {
            base = Long.parseUnsignedLong(start);
        } catch (Exception e) {
            base = Long.parseLong(start);
        }
        long max = 999_999_999_999_999L;
        if (base < 0 || base > max || base + (long) qty - 1L > max) {
            throw new IllegalArgumentException("Dai IMEI vuot gioi han 15 chu so");
        }
        List<String> out = new ArrayList<>(qty);
        for (int i = 0; i < qty; i++) {
            long v = base + i;
            out.add(String.format(Locale.ROOT, "%015d", v));
        }
        return out;
    }

    private String randomImei15() {
        long v = Math.floorMod(rnd.nextLong(), 1_000_000_000_000_000L);
        return String.format(Locale.ROOT, "%015d", v);
    }

    private String normalizeImei(String imei) {
        return imei == null ? "" : imei.trim();
    }

    private boolean isValidImei15Digits(String text) {
        if (text == null || text.length() != 15) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }
}
