package com.phonestore.dao.jdbc;

import com.phonestore.config.JDBCUtil;
import com.phonestore.dao.EmployeeDao;
import com.phonestore.model.Employee;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmployeeJdbcDao implements EmployeeDao {

    @Override
    public List<Employee> findAll() {
        String base = "SELECT manv, hoten, gioitinh, ngaysinh, sdt, email, trangthai";
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");
            boolean hasCode = columnExists(c, "nhanvien", "manv_code");
            String sql = base + (hasCode ? ", manv_code" : "") + " FROM nhanvien ORDER BY manv ASC";
            try (PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                List<Employee> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs, hasCode));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Employee> search(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isBlank()) return findAll();
        String base = "SELECT manv, hoten, gioitinh, ngaysinh, sdt, email, trangthai";
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");
            boolean hasCode = columnExists(c, "nhanvien", "manv_code");
            String sql = base + (hasCode ? ", manv_code" : "") + " FROM nhanvien WHERE (hoten LIKE ? OR sdt LIKE ? OR email LIKE ?) ORDER BY manv ASC";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String like = "%" + kw + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Employee> list = new ArrayList<>();
                    while (rs.next()) list.add(map(rs, hasCode));
                    return list;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Employee getById(long id) {
        String base = "SELECT manv, hoten, gioitinh, ngaysinh, sdt, email, trangthai";
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");
            boolean hasCode = columnExists(c, "nhanvien", "manv_code");
            String sql = base + (hasCode ? ", manv_code" : "") + " FROM nhanvien WHERE manv = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return map(rs, hasCode);
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Employee create(Employee employee) {
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");
            boolean hasCode = columnExists(c, "nhanvien", "manv_code");
            String sql;
            if (hasCode) {
                // generate next code with prefix NVPS-
                int next = fetchNextCodeNumber(c, "NVPS-");
                String code = String.format("NVPS-%02d", next);
                employee.setCode(code);
                sql = "INSERT INTO nhanvien (hoten, gioitinh, ngaysinh, sdt, email, trangthai, manv_code) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    bindWithCode(ps, employee, false);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) employee.setId(keys.getLong(1));
                    }
                }
            } else {
                sql = "INSERT INTO nhanvien (hoten, gioitinh, ngaysinh, sdt, email, trangthai) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    bind(ps, employee, false);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) employee.setId(keys.getLong(1));
                    }
                }
                // fallback transient code when DB doesn't store it
                employee.setCode(String.format("NVPS-%02d", employee.getId() <= 0 ? 1 : employee.getId()));
            }
            return employee;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Employee update(Employee employee) {
        String sql = "UPDATE nhanvien SET hoten=?, gioitinh=?, ngaysinh=?, sdt=?, email=?, trangthai=? WHERE manv=?";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {

            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");

            // intentionally do NOT change manv_code on update to preserve employee code
            bind(ps, employee, true);
            int updated = ps.executeUpdate();
            if (updated <= 0) throw new IllegalArgumentException("Không tìm thấy nhân viên");
            return employee;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(long id) {
        // Permanently remove the employee row
        String sql = "DELETE FROM nhanvien WHERE manv = ?";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {

            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");

            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Employee map(ResultSet rs, boolean hasCode) throws SQLException {
        Employee e = new Employee();
        e.setId(rs.getLong("manv"));
        e.setFullName(rs.getString("hoten"));
        int g = rs.getInt("gioitinh");
        e.setGender(rs.wasNull() ? null : g);
        Date d = rs.getDate("ngaysinh");
        e.setBirthDate(d == null ? null : d.toLocalDate());
        e.setPhone(rs.getString("sdt"));
        e.setEmail(rs.getString("email"));
        int st = rs.getInt("trangthai");
        e.setStatus(rs.wasNull() ? null : st);
        if (hasCode) {
            e.setCode(rs.getString("manv_code"));
        } else {
            // transient fallback code using ID
            e.setCode(String.format("NVPS-%02d", e.getId() <= 0 ? 1 : e.getId()));
        }
        return e;
    }

    private void bind(PreparedStatement ps, Employee e, boolean includeIdAtEnd) throws SQLException {
        ps.setString(1, e.getFullName());
        ps.setInt(2, e.getGender() == null ? 0 : e.getGender());
        LocalDate bd = e.getBirthDate();
        ps.setDate(3, bd == null ? null : Date.valueOf(bd));
        ps.setString(4, e.getPhone());
        ps.setString(5, e.getEmail());
        ps.setInt(6, e.getStatus() == null ? 1 : e.getStatus());
        if (includeIdAtEnd) {
            ps.setLong(7, e.getId());
        }
    }

    private void bindWithCode(PreparedStatement ps, Employee e, boolean includeIdAtEnd) throws SQLException {
        ps.setString(1, e.getFullName());
        ps.setInt(2, e.getGender() == null ? 0 : e.getGender());
        LocalDate bd = e.getBirthDate();
        ps.setDate(3, bd == null ? null : Date.valueOf(bd));
        ps.setString(4, e.getPhone());
        ps.setString(5, e.getEmail());
        ps.setInt(6, e.getStatus() == null ? 1 : e.getStatus());
        ps.setString(7, e.getCode());
        if (includeIdAtEnd) {
            ps.setLong(8, e.getId());
        }
    }

    private boolean columnExists(Connection c, String tableName, String columnName) throws SQLException {
        DatabaseMetaData md = c.getMetaData();
        try (ResultSet rs = md.getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }

    private int fetchNextCodeNumber(Connection c, String prefix) throws SQLException {
        // Attempt to read maximum numeric suffix for codes like PREFIX-<num>
        String q = "SELECT MAX(CAST(SUBSTRING_INDEX(manv_code, '-', -1) AS UNSIGNED)) AS mx FROM nhanvien WHERE manv_code LIKE ?";
        try (PreparedStatement ps = c.prepareStatement(q)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int mx = rs.getInt("mx");
                    if (rs.wasNull()) mx = 0;
                    return mx + 1;
                }
            }
        }
        return 1;
    }
}
