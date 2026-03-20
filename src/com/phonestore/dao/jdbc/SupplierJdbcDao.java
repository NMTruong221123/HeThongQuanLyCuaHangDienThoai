package com.phonestore.dao.jdbc;

import com.phonestore.config.JDBCUtil;
import com.phonestore.dao.SupplierDao;
import com.phonestore.model.Supplier;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SupplierJdbcDao implements SupplierDao {

    @Override
    public List<Supplier> findAll() {
        String sql = "SELECT manhacungcap, tennhacungcap, diachi, email, sdt, trangthai FROM nhacungcap ORDER BY manhacungcap ASC";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql);
             ResultSet rs = ps == null ? null : ps.executeQuery()) {

            if (c == null) {
                throw new IllegalStateException("Chưa kết nối được DB");
            }

            List<Supplier> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Supplier> search(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isBlank()) {
            return findAll();
        }

        String sql = "SELECT manhacungcap, tennhacungcap, diachi, email, sdt, trangthai "
                + "FROM nhacungcap "
                + "WHERE tennhacungcap LIKE ? OR sdt LIKE ? OR email LIKE ? OR diachi LIKE ? "
            + "ORDER BY manhacungcap ASC";

        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {

            if (c == null) {
                throw new IllegalStateException("Chưa kết nối được DB");
            }

            String like = "%" + kw + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);

            try (ResultSet rs = ps.executeQuery()) {
                List<Supplier> list = new ArrayList<>();
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
    public Supplier create(Supplier supplier) {
        String sql = "INSERT INTO nhacungcap (tennhacungcap, diachi, email, sdt, trangthai) VALUES (?, ?, ?, ?, ?)";

        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (c == null) {
                throw new IllegalStateException("Chưa kết nối được DB");
            }

            bind(ps, supplier, false);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    supplier.setId(keys.getLong(1));
                }
            }
            return supplier;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Supplier update(Supplier supplier) {
        String sql = "UPDATE nhacungcap SET tennhacungcap=?, diachi=?, email=?, sdt=?, trangthai=? WHERE manhacungcap=?";

        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {

            if (c == null) {
                throw new IllegalStateException("Chưa kết nối được DB");
            }

            bind(ps, supplier, true);
            int updated = ps.executeUpdate();
            if (updated <= 0) {
                throw new IllegalArgumentException("Không tìm thấy nhà cung cấp");
            }
            return supplier;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "UPDATE nhacungcap SET trangthai=0 WHERE manhacungcap=?";

        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {

            if (c == null) {
                throw new IllegalStateException("Chưa kết nối được DB");
            }

            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Supplier map(ResultSet rs) throws SQLException {
        Supplier s = new Supplier();
        s.setId(rs.getLong("manhacungcap"));
        s.setName(rs.getString("tennhacungcap"));
        s.setAddress(rs.getString("diachi"));
        s.setEmail(rs.getString("email"));
        s.setPhone(rs.getString("sdt"));

        int status = rs.getInt("trangthai");
        s.setStatus(rs.wasNull() ? null : status);
        return s;
    }

    private void bind(PreparedStatement ps, Supplier s, boolean includeIdAtEnd) throws SQLException {
        ps.setString(1, s.getName());
        ps.setString(2, s.getAddress());
        ps.setString(3, s.getEmail());
        ps.setString(4, s.getPhone());

        Integer status = s.getStatus();
        if (status == null) {
            status = 1;
        }
        ps.setInt(5, status);

        if (includeIdAtEnd) {
            ps.setLong(6, s.getId());
        }
    }
}
