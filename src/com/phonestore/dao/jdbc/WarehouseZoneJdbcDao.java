package com.phonestore.dao.jdbc;

import com.phonestore.config.JDBCUtil;
import com.phonestore.dao.WarehouseZoneDao;
import com.phonestore.model.WarehouseZone;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WarehouseZoneJdbcDao implements WarehouseZoneDao {

    @Override
    public List<WarehouseZone> findAll() {
        String sql = "SELECT makhuvuc, tenkhuvuc, ghichu, trangthai FROM khuvuckho ORDER BY makhuvuc ASC";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql);
             ResultSet rs = ps == null ? null : ps.executeQuery()) {

            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");

            List<WarehouseZone> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<WarehouseZone> search(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isBlank()) return findAll();

        String sql = "SELECT makhuvuc, tenkhuvuc, ghichu, trangthai "
            + "FROM khuvuckho WHERE tenkhuvuc LIKE ? OR ghichu LIKE ? OR CAST(makhuvuc AS CHAR) LIKE ? "
            + "ORDER BY makhuvuc ASC";

        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {

            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");

            String like = "%" + kw + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);

            try (ResultSet rs = ps.executeQuery()) {
                List<WarehouseZone> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public WarehouseZone create(WarehouseZone zone) {
        String sql = "INSERT INTO khuvuckho (tenkhuvuc, ghichu, trangthai) VALUES (?, ?, ?)";

        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");

            bind(ps, zone, false);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) zone.setId(keys.getLong(1));
            }
            return zone;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public WarehouseZone update(WarehouseZone zone) {
        String sql = "UPDATE khuvuckho SET tenkhuvuc=?, ghichu=?, trangthai=? WHERE makhuvuc=?";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {

            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");

            bind(ps, zone, true);
            int updated = ps.executeUpdate();
            if (updated <= 0) throw new IllegalArgumentException("Không tìm thấy khu vực");
            return zone;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(long id) {
        // Hard-delete and re-sequence IDs so there are no gaps.
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");
            try {
                c.setAutoCommit(false);

                // delete the row
                try (PreparedStatement psDel = c.prepareStatement("DELETE FROM khuvuckho WHERE makhuvuc=?")) {
                    psDel.setLong(1, id);
                    int deleted = psDel.executeUpdate();
                    if (deleted <= 0) throw new IllegalArgumentException("Không tìm thấy khu vực");
                }

                // shift ids down by 1 for rows with id > deleted id
                try (PreparedStatement psShift = c.prepareStatement("UPDATE khuvuckho SET makhuvuc = makhuvuc - 1 WHERE makhuvuc > ?")) {
                    psShift.setLong(1, id);
                    psShift.executeUpdate();
                }

                // reset auto_increment to max(id)+1
                long nextAi = 1;
                try (PreparedStatement psMax = c.prepareStatement("SELECT MAX(makhuvuc) FROM khuvuckho");
                     ResultSet rs = psMax.executeQuery()) {
                    if (rs.next()) {
                        long max = rs.getLong(1);
                        nextAi = max + 1;
                    }
                }
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("ALTER TABLE khuvuckho AUTO_INCREMENT = " + nextAi);
                }

                c.commit();
            } catch (SQLException | RuntimeException ex) {
                try {
                    c.rollback();
                } catch (SQLException ignored) {
                }
                throw new RuntimeException(ex);
            } finally {
                try {
                    c.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private WarehouseZone map(ResultSet rs) throws SQLException {
        WarehouseZone z = new WarehouseZone();
        z.setId(rs.getLong("makhuvuc"));
        z.setName(rs.getString("tenkhuvuc"));
        z.setNote(rs.getString("ghichu"));
        int st = rs.getInt("trangthai");
        z.setStatus(rs.wasNull() ? null : st);
        return z;
    }

    private void bind(PreparedStatement ps, WarehouseZone z, boolean includeIdAtEnd) throws SQLException {
        ps.setString(1, z.getName());
        ps.setString(2, z.getNote() == null ? "" : z.getNote());
        ps.setInt(3, z.getStatus() == null ? 1 : z.getStatus());
        if (includeIdAtEnd) {
            ps.setLong(4, z.getId());
        }
    }
}
