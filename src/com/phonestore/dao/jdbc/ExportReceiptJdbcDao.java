package com.phonestore.dao.jdbc;

import com.phonestore.config.JDBCUtil;
import com.phonestore.dao.ExportReceiptDao;
import com.phonestore.model.ExportReceipt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ExportReceiptJdbcDao implements ExportReceiptDao {
    private static final String COL_PAYMENT_REF = "payment_ref";
    private static final String COL_RECEIPT_CODE = "receipt_code";

    @Override
    public List<ExportReceipt> findAll() {
        String sql = "SELECT px.maphieuxuat, px.thoigian, px.tongtien, "
            + "px.nguoitaophieuxuat, nv.hoten AS nguoitao_ten, "
            + "px.makh, kh.tenkhachhang AS kh_ten, px.trangthai, "
            + "px." + COL_PAYMENT_REF + ", px." + COL_RECEIPT_CODE + " "
            + "FROM phieuxuat px "
            + "LEFT JOIN nhanvien nv ON px.nguoitaophieuxuat = nv.manv "
            + "LEFT JOIN khachhang kh ON px.makh = kh.makh "
            + "ORDER BY px.maphieuxuat ASC";
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException("Chua ket noi duoc DB");
            ensureExtraColumns(c);
            try (PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                List<ExportReceipt> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ExportReceipt> search(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isBlank()) return findAll();

        String sql = "SELECT px.maphieuxuat, px.thoigian, px.tongtien, "
            + "px.nguoitaophieuxuat, nv.hoten AS nguoitao_ten, "
            + "px.makh, kh.tenkhachhang AS kh_ten, px.trangthai, "
            + "px." + COL_PAYMENT_REF + ", px." + COL_RECEIPT_CODE + " "
            + "FROM phieuxuat px "
            + "LEFT JOIN nhanvien nv ON px.nguoitaophieuxuat = nv.manv "
            + "LEFT JOIN khachhang kh ON px.makh = kh.makh "
            + "WHERE CAST(px.maphieuxuat AS CHAR) LIKE ? ORDER BY px.maphieuxuat ASC";

        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException("Chua ket noi duoc DB");
            ensureExtraColumns(c);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, "%" + kw + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    List<ExportReceipt> list = new ArrayList<>();
                    while (rs.next()) list.add(map(rs));
                    return list;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ExportReceipt create(ExportReceipt receipt) {
        String sql = "INSERT INTO phieuxuat (tongtien, nguoitaophieuxuat, makh, trangthai, "
            + COL_PAYMENT_REF + ", " + COL_RECEIPT_CODE + ") VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException("Chua ket noi duoc DB");
            ensureExtraColumns(c);
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(ps, receipt, false);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) receipt.setId(keys.getLong(1));
                }
                return receipt;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ExportReceipt update(ExportReceipt receipt) {
        String sql = "UPDATE phieuxuat SET tongtien=?, nguoitaophieuxuat=?, makh=?, trangthai=?, "
            + COL_PAYMENT_REF + "=?, " + COL_RECEIPT_CODE + "=? WHERE maphieuxuat=?";
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException("Chua ket noi duoc DB");
            ensureExtraColumns(c);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, receipt, true);
                int updated = ps.executeUpdate();
                if (updated <= 0) throw new IllegalArgumentException("Khong tim thay phieu xuat");
                return receipt;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "UPDATE phieuxuat SET trangthai=0 WHERE maphieuxuat=?";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {
            if (c == null) throw new IllegalStateException("Chua ket noi duoc DB");
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Permanently delete receipt and shift subsequent receipt ids down by 1.
     * This also updates common dependent tables (ctphieuxuat and imei_registry) if present.
     */
    public void hardDeleteAndShift(long id) {
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException("Chua ket noi duoc DB");
            ensureExtraColumns(c);
            c.setAutoCommit(false);

            // delete receipt lines if table exists
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM ctphieuxuat WHERE maphieuxuat=?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            } catch (Exception ignored) {}

            // clear imei_registry references that point to this receipt (if column exists)
            try {
                if (JDBCUtil.hasColumn("imei_registry", "export_receipt_id")) {
                    try (PreparedStatement ps = c.prepareStatement("UPDATE imei_registry SET export_receipt_id = NULL WHERE export_receipt_id = ?")) {
                        ps.setLong(1, id);
                        ps.executeUpdate();
                    }
                }
            } catch (Exception ignored) {}

            // delete the receipt row
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM phieuxuat WHERE maphieuxuat=?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }

            // shift receipt ids in ctphieuxuat (if exists)
            try (PreparedStatement ps = c.prepareStatement("UPDATE ctphieuxuat SET maphieuxuat = maphieuxuat - 1 WHERE maphieuxuat > ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            } catch (Exception ignored) {}

            // shift receipt ids in phieuxuat
            try (PreparedStatement ps = c.prepareStatement("UPDATE phieuxuat SET maphieuxuat = maphieuxuat - 1 WHERE maphieuxuat > ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }

            // shift references in imei_registry if column exists
            try {
                if (JDBCUtil.hasColumn("imei_registry", "export_receipt_id")) {
                    try (PreparedStatement ps = c.prepareStatement("UPDATE imei_registry SET export_receipt_id = export_receipt_id - 1 WHERE export_receipt_id > ?")) {
                        ps.setLong(1, id);
                        ps.executeUpdate();
                    }
                }
            } catch (Exception ignored) {}

            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static final class PreviewInfo {
        public int ctDeleteCount;
        public int phieuDeleteCount;
        public int ctShiftCount;
        public int phieuShiftCount;
        public int imeiClearedCount;
        public int imeiShiftCount;
    }

    public PreviewInfo previewHardDeleteAndShift(long id) {
        PreviewInfo info = new PreviewInfo();
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException("Chua ket noi duoc DB");
            // count ctphieuxuat rows that will be deleted
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(1) FROM ctphieuxuat WHERE maphieuxuat=?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) info.ctDeleteCount = rs.getInt(1); }
            } catch (Exception ignored) { info.ctDeleteCount = 0; }

            // phieuxuat row exists?
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(1) FROM phieuxuat WHERE maphieuxuat=?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) info.phieuDeleteCount = rs.getInt(1); }
            } catch (Exception ignored) { info.phieuDeleteCount = 0; }

            // count ctphieuxuat rows that will be shifted
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(1) FROM ctphieuxuat WHERE maphieuxuat > ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) info.ctShiftCount = rs.getInt(1); }
            } catch (Exception ignored) { info.ctShiftCount = 0; }

            // count phieuxuat rows that will be shifted
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(1) FROM phieuxuat WHERE maphieuxuat > ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) info.phieuShiftCount = rs.getInt(1); }
            } catch (Exception ignored) { info.phieuShiftCount = 0; }

            // imei_registry counts
            try {
                if (JDBCUtil.hasColumn("imei_registry", "export_receipt_id")) {
                    try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(1) FROM imei_registry WHERE export_receipt_id = ?")) {
                        ps.setLong(1, id);
                        try (ResultSet rs = ps.executeQuery()) { if (rs.next()) info.imeiClearedCount = rs.getInt(1); }
                    }
                    try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(1) FROM imei_registry WHERE export_receipt_id > ?")) {
                        ps.setLong(1, id);
                        try (ResultSet rs = ps.executeQuery()) { if (rs.next()) info.imeiShiftCount = rs.getInt(1); }
                    }
                }
            } catch (Exception ignored) { info.imeiClearedCount = 0; info.imeiShiftCount = 0; }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return info;
    }

    private ExportReceipt map(ResultSet rs) throws SQLException {
        ExportReceipt r = new ExportReceipt();
        r.setId(rs.getLong("maphieuxuat"));
        Timestamp ts = rs.getTimestamp("thoigian");
        r.setTime(ts == null ? null : ts.toLocalDateTime());

        double total = rs.getDouble("tongtien");
        r.setTotal(rs.wasNull() ? null : total);

        long cb = rs.getLong("nguoitaophieuxuat");
        r.setCreatedBy(rs.wasNull() ? null : cb);
        r.setCreatedByName(rs.getString("nguoitao_ten"));

        long cust = rs.getLong("makh");
        r.setCustomerId(rs.wasNull() ? null : cust);
        r.setCustomerName(rs.getString("kh_ten"));

        int st = rs.getInt("trangthai");
        r.setStatus(rs.wasNull() ? null : st);
        r.setPaymentRef(rs.getString(COL_PAYMENT_REF));
        r.setReceiptCode(rs.getString(COL_RECEIPT_CODE));
        return r;
    }

    private void bind(PreparedStatement ps, ExportReceipt r, boolean includeIdAtEnd) throws SQLException {
        if (!includeIdAtEnd) {
            ps.setDouble(1, r.getTotal() == null ? 0 : r.getTotal());
            if (r.getCreatedBy() == null) ps.setNull(2, Types.INTEGER); else ps.setLong(2, r.getCreatedBy());
            if (r.getCustomerId() == null) ps.setNull(3, Types.INTEGER); else ps.setLong(3, r.getCustomerId());
            if (r.getStatus() == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, r.getStatus());
            if (r.getPaymentRef() == null || r.getPaymentRef().isBlank()) ps.setNull(5, Types.VARCHAR); else ps.setString(5, r.getPaymentRef());
            if (r.getReceiptCode() == null || r.getReceiptCode().isBlank()) ps.setNull(6, Types.VARCHAR); else ps.setString(6, r.getReceiptCode());
        } else {
            if (r.getTotal() == null) ps.setNull(1, Types.DOUBLE); else ps.setDouble(1, r.getTotal());
            if (r.getCreatedBy() == null) ps.setNull(2, Types.INTEGER); else ps.setLong(2, r.getCreatedBy());
            if (r.getCustomerId() == null) ps.setNull(3, Types.INTEGER); else ps.setLong(3, r.getCustomerId());
            if (r.getStatus() == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, r.getStatus());
            if (r.getPaymentRef() == null || r.getPaymentRef().isBlank()) ps.setNull(5, Types.VARCHAR); else ps.setString(5, r.getPaymentRef());
            if (r.getReceiptCode() == null || r.getReceiptCode().isBlank()) ps.setNull(6, Types.VARCHAR); else ps.setString(6, r.getReceiptCode());
            ps.setLong(7, r.getId());
        }
    }

    private void ensureExtraColumns(Connection c) {
        if (c == null) return;
        try (Statement st = c.createStatement()) {
            st.execute("ALTER TABLE phieuxuat ADD COLUMN " + COL_PAYMENT_REF + " VARCHAR(255) NULL");
        } catch (Exception ignored) {
        }
        try (Statement st = c.createStatement()) {
            st.execute("ALTER TABLE phieuxuat ADD COLUMN " + COL_RECEIPT_CODE + " VARCHAR(64) NULL");
        } catch (Exception ignored) {
        }
    }
}
