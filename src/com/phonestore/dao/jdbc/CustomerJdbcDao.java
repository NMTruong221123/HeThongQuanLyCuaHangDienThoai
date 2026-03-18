package com.phonestore.dao.jdbc;

import com.phonestore.config.JDBCUtil;
import com.phonestore.dao.CustomerDao;
import com.phonestore.model.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerJdbcDao implements CustomerDao {

    private static final String TABLE = "khachhang";
    private static final String COL_EMAIL = "email";

    private boolean hasEmailColumn() {
        return JDBCUtil.hasColumn(TABLE, COL_EMAIL);
    }

    @Override
    public List<Customer> findAll() {
        boolean hasEmail = hasEmailColumn();
        // Return both active/inactive customers; UI can decide how to display/filter.
        String sql = hasEmail
            ? "SELECT makh, tenkhachhang, diachi, email, sdt, trangthai FROM khachhang ORDER BY makh ASC"
            : "SELECT makh, tenkhachhang, diachi, sdt, trangthai FROM khachhang ORDER BY makh ASC";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql);
             ResultSet rs = ps == null ? null : ps.executeQuery()) {

            if (c == null) {
                throw new IllegalStateException("Chưa kết nối được DB");
            }

            List<Customer> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs, hasEmail));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Customer findById(long id) {
        if (id <= 0) return null;
        boolean hasEmail = hasEmailColumn();
        String sql = hasEmail
                ? "SELECT makh, tenkhachhang, diachi, email, sdt, trangthai FROM khachhang WHERE makh=?"
                : "SELECT makh, tenkhachhang, diachi, sdt, trangthai FROM khachhang WHERE makh=?";

        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {

            if (c == null) {
                throw new IllegalStateException("Chưa kết nối được DB");
            }

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs, hasEmail);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Customer> search(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isBlank()) {
            return findAll();
        }

        boolean hasEmail = hasEmailColumn();
        String sql = hasEmail
            ? "SELECT makh, tenkhachhang, diachi, email, sdt, trangthai FROM khachhang "
            + "WHERE (tenkhachhang LIKE ? OR sdt LIKE ? OR email LIKE ? OR diachi LIKE ?) ORDER BY makh ASC"
            : "SELECT makh, tenkhachhang, diachi, sdt, trangthai FROM khachhang "
            + "WHERE (tenkhachhang LIKE ? OR sdt LIKE ? OR diachi LIKE ?) ORDER BY makh ASC";

        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {

            if (c == null) {
                throw new IllegalStateException("Chưa kết nối được DB");
            }

            String like = "%" + kw + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            if (hasEmail) {
                ps.setString(3, like);
                ps.setString(4, like);
            } else {
                ps.setString(3, like);
            }

            try (ResultSet rs = ps.executeQuery()) {
                List<Customer> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs, hasEmail));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Customer create(Customer customer) {
        boolean hasEmail = hasEmailColumn();
        String sql = hasEmail
            ? "INSERT INTO khachhang (tenkhachhang, diachi, email, sdt, trangthai) VALUES (?, ?, ?, ?, ?)"
            : "INSERT INTO khachhang (tenkhachhang, diachi, sdt, trangthai) VALUES (?, ?, ?, ?)";

        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (c == null) {
                throw new IllegalStateException("Chưa kết nối được DB");
            }

            bind(ps, customer, false, hasEmail);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    customer.setId(keys.getLong(1));
                }
            }
            return customer;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Customer update(Customer customer) {
        boolean hasEmail = hasEmailColumn();
        String sql = hasEmail
            ? "UPDATE khachhang SET tenkhachhang=?, diachi=?, email=?, sdt=?, trangthai=? WHERE makh=?"
            : "UPDATE khachhang SET tenkhachhang=?, diachi=?, sdt=?, trangthai=? WHERE makh=?";

        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {

            if (c == null) {
                throw new IllegalStateException("Chưa kết nối được DB");
            }

            bind(ps, customer, true, hasEmail);
            int updated = ps.executeUpdate();
            if (updated <= 0) {
                throw new IllegalArgumentException("Không tìm thấy khách hàng");
            }
            return customer;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "UPDATE khachhang SET trangthai=0 WHERE makh=?";

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

    private Customer map(ResultSet rs, boolean hasEmail) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getLong("makh"));
        c.setName(rs.getString("tenkhachhang"));
        c.setAddress(rs.getString("diachi"));
        if (hasEmail) {
            c.setEmail(rs.getString("email"));
        }
        c.setPhone(rs.getString("sdt"));

        int status = rs.getInt("trangthai");
        c.setStatus(rs.wasNull() ? null : status);
        return c;
    }

    private void bind(PreparedStatement ps, Customer c, boolean includeIdAtEnd, boolean hasEmail) throws SQLException {
        int i = 1;
        ps.setString(i++, c.getName());
        ps.setString(i++, c.getAddress());
        if (hasEmail) {
            ps.setString(i++, c.getEmail());
        }
        ps.setString(i++, c.getPhone());

        Integer status = c.getStatus();
        if (status == null) status = 1;
        ps.setInt(i++, status);

        if (includeIdAtEnd) {
            ps.setLong(i, c.getId());
        }
    }
}
