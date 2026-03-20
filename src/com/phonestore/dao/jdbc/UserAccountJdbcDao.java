package com.phonestore.dao.jdbc;

import com.phonestore.config.JDBCUtil;
import com.phonestore.dao.UserAccountDao;
import com.phonestore.model.UserAccount;

import java.sql.*;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

public class UserAccountJdbcDao implements UserAccountDao {

    private static boolean hasEmailColumn() {
        return JDBCUtil.hasColumn("taikhoan", "email");
    }

    private static String baseSelectSql() {
        // email is optional across DB dumps
        String emailSelect = hasEmailColumn() ? ", tk.email AS tk_email " : ", NULL AS tk_email ";
        return "SELECT tk.manv, nv.hoten AS nv_ten, tk.tendangnhap, tk.matkhau, tk.manhomquyen, tk.trangthai, tk.otp"
                + emailSelect
                + "FROM taikhoan tk "
                + "LEFT JOIN nhanvien nv ON tk.manv = nv.manv ";
    }

    @Override
    public List<UserAccount> findAll() {
        String sql = baseSelectSql() + "ORDER BY tk.manv ASC";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql);
             ResultSet rs = ps == null ? null : ps.executeQuery()) {

            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");

            List<UserAccount> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<UserAccount> search(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isBlank()) return findAll();

        String where = "WHERE tk.tendangnhap LIKE ? OR CAST(tk.manv AS CHAR) LIKE ? OR nv.hoten LIKE ? ";
        if (hasEmailColumn()) {
            where = "WHERE tk.tendangnhap LIKE ? OR CAST(tk.manv AS CHAR) LIKE ? OR nv.hoten LIKE ? OR tk.email LIKE ? ";
        }

        String sql = baseSelectSql() + where + "ORDER BY tk.manv ASC";

        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {

            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");

            String like = "%" + kw + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            if (hasEmailColumn()) {
                ps.setString(4, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<UserAccount> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UserAccount create(UserAccount account) {
        String sql;
        if (hasEmailColumn()) {
            sql = "INSERT INTO taikhoan (manv, tendangnhap, email, matkhau, manhomquyen, trangthai, otp) VALUES (?, ?, ?, ?, ?, ?, ?)";
        } else {
            sql = "INSERT INTO taikhoan (manv, tendangnhap, matkhau, manhomquyen, trangthai, otp) VALUES (?, ?, ?, ?, ?, ?)";
        }
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {

            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");
            bind(ps, account, false);
            ps.executeUpdate();
            return account;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UserAccount update(UserAccount account) {
        String sql;
        if (hasEmailColumn()) {
            sql = "UPDATE taikhoan SET tendangnhap=?, email=?, matkhau=?, manhomquyen=?, trangthai=?, otp=? WHERE manv=?";
        } else {
            sql = "UPDATE taikhoan SET tendangnhap=?, matkhau=?, manhomquyen=?, trangthai=?, otp=? WHERE manv=?";
        }
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {

            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");
            bind(ps, account, true);
            int updated = ps.executeUpdate();
            if (updated <= 0) throw new IllegalArgumentException("Không tìm thấy tài khoản");
            return account;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(long employeeId) {
        // Permanently remove the account row so the employee no longer has an account
        String sql = "DELETE FROM taikhoan WHERE manv = ?";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {

            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");
            ps.setLong(1, employeeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            // If delete is blocked by foreign key constraints, give a friendly error
            if (e instanceof SQLIntegrityConstraintViolationException
                    || (e.getMessage() != null && e.getMessage().toLowerCase().contains("foreign key"))) {
                throw new IllegalStateException("Không thể xóa tài khoản vì có dữ liệu tham chiếu (ví dụ: phiếu nhập/phiếu xuất). Hãy khóa tài khoản thay vì xóa.", e);
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateStatus(long employeeId, int status) {
        String sql = "UPDATE taikhoan SET trangthai = ? WHERE manv = ?";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {
            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");
            ps.setInt(1, status);
            ps.setLong(2, employeeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private UserAccount map(ResultSet rs) throws SQLException {
        UserAccount a = new UserAccount();
        a.setEmployeeId(rs.getLong("manv"));
        a.setEmployeeName(rs.getString("nv_ten"));
        a.setUsername(rs.getString("tendangnhap"));
        a.setEmail(rs.getString("tk_email"));
        a.setPassword(rs.getString("matkhau"));
        int role = rs.getInt("manhomquyen");
        a.setRoleId(rs.wasNull() ? null : role);
        int st = rs.getInt("trangthai");
        a.setStatus(rs.wasNull() ? null : st);
        a.setOtp(rs.getString("otp"));
        return a;
    }

    private void bind(PreparedStatement ps, UserAccount a, boolean includeIdAtEnd) throws SQLException {
        boolean hasEmail = hasEmailColumn();
        if (!includeIdAtEnd) {
            ps.setLong(1, a.getEmployeeId());
            ps.setString(2, a.getUsername());
            int i = 3;
            if (hasEmail) {
                ps.setString(i++, a.getEmail());
            }
            ps.setString(i++, a.getPassword());
            ps.setInt(i++, a.getRoleId() == null ? 1 : a.getRoleId());
            ps.setInt(i++, a.getStatus() == null ? 1 : a.getStatus());
            ps.setString(i, a.getOtp());
        } else {
            ps.setString(1, a.getUsername());
            int i = 2;
            if (hasEmail) {
                ps.setString(i++, a.getEmail());
            }
            ps.setString(i++, a.getPassword());
            ps.setInt(i++, a.getRoleId() == null ? 1 : a.getRoleId());
            ps.setInt(i++, a.getStatus() == null ? 1 : a.getStatus());
            ps.setString(i++, a.getOtp());
            ps.setLong(i, a.getEmployeeId());
        }
    }
}
