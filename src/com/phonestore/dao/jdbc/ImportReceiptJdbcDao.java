package com.phonestore.dao.jdbc;

import com.phonestore.config.JDBCUtil;
import com.phonestore.dao.ImportReceiptDao;
import com.phonestore.model.ImportReceipt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ImportReceiptJdbcDao implements ImportReceiptDao {

    @Override
    public List<ImportReceipt> findAll() {
        String sql = "SELECT pn.maphieunhap, pn.thoigian, pn.manhacungcap, ncc.tennhacungcap AS ncc_ten, "
                + "pn.nguoitao, nv.hoten AS nguoitao_ten, pn.tongtien, pn.trangthai "
                + "FROM phieunhap pn "
                + "LEFT JOIN nhacungcap ncc ON pn.manhacungcap = ncc.manhacungcap "
                + "LEFT JOIN nhanvien nv ON pn.nguoitao = nv.manv "
                + "ORDER BY pn.maphieunhap ASC";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql);
             ResultSet rs = ps == null ? null : ps.executeQuery()) {

            if (c == null) throw new IllegalStateException("ChÆ°a káº¿t ná»‘i Ä‘Æ°á»£c DB");
            List<ImportReceipt> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ImportReceipt> search(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isBlank()) return findAll();

        String sql = "SELECT pn.maphieunhap, pn.thoigian, pn.manhacungcap, ncc.tennhacungcap AS ncc_ten, "
            + "pn.nguoitao, nv.hoten AS nguoitao_ten, pn.tongtien, pn.trangthai "
            + "FROM phieunhap pn "
            + "LEFT JOIN nhacungcap ncc ON pn.manhacungcap = ncc.manhacungcap "
            + "LEFT JOIN nhanvien nv ON pn.nguoitao = nv.manv "
            + "WHERE CAST(pn.maphieunhap AS CHAR) LIKE ? ORDER BY pn.maphieunhap ASC";

        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {
            if (c == null) throw new IllegalStateException("ChÆ°a káº¿t ná»‘i Ä‘Æ°á»£c DB");
            String like = "%" + kw + "%";
            ps.setString(1, like);
            try (ResultSet rs = ps.executeQuery()) {
                List<ImportReceipt> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ImportReceipt create(ImportReceipt receipt) {
        String sql = "INSERT INTO phieunhap (manhacungcap, nguoitao, tongtien, trangthai) VALUES (?, ?, ?, ?)";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (c == null) throw new IllegalStateException("ChÆ°a káº¿t ná»‘i Ä‘Æ°á»£c DB");

            bind(ps, receipt, false);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) receipt.setId(keys.getLong(1));
            }
            return receipt;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ImportReceipt update(ImportReceipt receipt) {
        String sql = "UPDATE phieunhap SET manhacungcap=?, nguoitao=?, tongtien=?, trangthai=? WHERE maphieunhap=?";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {
            if (c == null) throw new IllegalStateException("ChÆ°a káº¿t ná»‘i Ä‘Æ°á»£c DB");

            bind(ps, receipt, true);
            int updated = ps.executeUpdate();
            if (updated <= 0) throw new IllegalArgumentException("KhÃ´ng tÃ¬m tháº¥y phiáº¿u nháº­p");
            return receipt;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

        @Override
    public void delete(long id) {
        String delLines = "DELETE FROM ctphieunhap WHERE maphieunhap=?";
        String delReceipt = "DELETE FROM phieunhap WHERE maphieunhap=?";
        String shiftLines = "UPDATE ctphieunhap SET maphieunhap = maphieunhap - 1 WHERE maphieunhap > ?";
        String shiftReceipt = "UPDATE phieunhap SET maphieunhap = maphieunhap - 1 WHERE maphieunhap > ?";
        String clearImeiReceiptRef = "UPDATE imei_registry SET import_receipt_id = NULL WHERE import_receipt_id=?";
        String shiftImeiReceiptRef = "UPDATE imei_registry SET import_receipt_id = import_receipt_id - 1 WHERE import_receipt_id > ?";
        String maxIdSql = "SELECT COALESCE(MAX(maphieunhap), 0) FROM phieunhap";
        String alterAuto = "ALTER TABLE phieunhap AUTO_INCREMENT = ?";

        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException("Chua ket noi duoc DB");
            c.setAutoCommit(false);

            int affected;
            try (PreparedStatement ps = c.prepareStatement(delLines)) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(delReceipt)) {
                ps.setLong(1, id);
                affected = ps.executeUpdate();
            }
            if (affected <= 0) {
                c.rollback();
                throw new IllegalArgumentException("Khong tim thay phieu nhap");
            }

            try (PreparedStatement ps = c.prepareStatement(shiftLines)) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(shiftReceipt)) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }

            if (JDBCUtil.hasColumn("imei_registry", "import_receipt_id")) {
                try (PreparedStatement ps = c.prepareStatement(clearImeiReceiptRef)) {
                    ps.setLong(1, id);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(shiftImeiReceiptRef)) {
                    ps.setLong(1, id);
                    ps.executeUpdate();
                }
            }

            long nextAuto = 1;
            try (PreparedStatement ps = c.prepareStatement(maxIdSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) nextAuto = Math.max(1, rs.getLong(1) + 1);
            }
            try (PreparedStatement ps = c.prepareStatement(alterAuto)) {
                ps.setLong(1, nextAuto);
                ps.executeUpdate();
            }

            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateStatus(long id, int status) {
        String sql = "UPDATE phieunhap SET trangthai=? WHERE maphieunhap=?";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {
            if (c == null) throw new IllegalStateException("Chua ket noi duoc DB");
            ps.setInt(1, status);
            ps.setLong(2, id);
            int affected = ps.executeUpdate();
            if (affected <= 0) throw new IllegalArgumentException("Khong tim thay phieu nhap");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private ImportReceipt map(ResultSet rs) throws SQLException {
        ImportReceipt r = new ImportReceipt();
        r.setId(rs.getLong("maphieunhap"));
        Timestamp ts = rs.getTimestamp("thoigian");
        r.setTime(ts == null ? null : ts.toLocalDateTime());

        long sup = rs.getLong("manhacungcap");
        r.setSupplierId(rs.wasNull() ? null : sup);
        r.setSupplierName(rs.getString("ncc_ten"));

        long cb = rs.getLong("nguoitao");
        r.setCreatedBy(rs.wasNull() ? null : cb);
        r.setCreatedByName(rs.getString("nguoitao_ten"));

        double total = rs.getDouble("tongtien");
        r.setTotal(rs.wasNull() ? null : total);

        int st = rs.getInt("trangthai");
        r.setStatus(rs.wasNull() ? null : st);
        return r;
    }

    private void bind(PreparedStatement ps, ImportReceipt r, boolean includeIdAtEnd) throws SQLException {
        if (!includeIdAtEnd) {
            ps.setLong(1, r.getSupplierId());
            ps.setLong(2, r.getCreatedBy());
            ps.setDouble(3, r.getTotal());
            ps.setInt(4, r.getStatus() == null ? 1 : r.getStatus());
        } else {
            ps.setLong(1, r.getSupplierId());
            ps.setLong(2, r.getCreatedBy());
            ps.setDouble(3, r.getTotal());
            ps.setInt(4, r.getStatus() == null ? 1 : r.getStatus());
            ps.setLong(5, r.getId());
        }
    }
}
