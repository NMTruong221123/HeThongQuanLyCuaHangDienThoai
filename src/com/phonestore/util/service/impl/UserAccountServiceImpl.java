package com.phonestore.util.service.impl;

import com.phonestore.config.JDBCUtil;
import com.phonestore.dao.UserAccountDao;
import com.phonestore.dao.jdbc.UserAccountJdbcDao;
import com.phonestore.model.UserAccount;
import com.phonestore.util.service.UserAccountService;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class UserAccountServiceImpl implements UserAccountService {
    private final UserAccountDao jdbcDao = new UserAccountJdbcDao();

    private static final int MAX_ADMIN_ACCOUNTS = 2;

    private void requireDb() {
        if (!JDBCUtil.canConnect()) {
            throw new IllegalStateException("Chưa kết nối được DB");
        }
    }

    @Override
    public List<UserAccount> findAll() {
        requireDb();
        return jdbcDao.findAll();
    }

    @Override
    public List<UserAccount> search(String keyword) {
        requireDb();
        return jdbcDao.search(keyword);
    }

    @Override
    public UserAccount create(UserAccount account) {
        validate(account);
        requireDb();
        enforceAdminLimit(account, false);
        account.setPassword(hashPasswordIfNeeded(account.getPassword()));
        if (account.getStatus() == null) account.setStatus(1);
        return jdbcDao.create(account);
    }

    @Override
    public UserAccount update(UserAccount account) {
        validate(account);
        requireDb();
        enforceAdminLimit(account, true);
        account.setPassword(hashPasswordIfNeeded(account.getPassword()));
        if (account.getStatus() == null) account.setStatus(1);
        return jdbcDao.update(account);
    }

    @Override
    public void delete(long employeeId) {
        requireDb();
        jdbcDao.delete(employeeId);
    }

    @Override
    public void updateStatus(long employeeId, int status) {
        requireDb();
        jdbcDao.updateStatus(employeeId, status);
    }

    private void validate(UserAccount a) {
        if (a == null) throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        if (a.getEmployeeId() <= 0) throw new IllegalArgumentException("Mã NV không hợp lệ");
        if (isBlank(a.getPassword())) throw new IllegalArgumentException("Mật khẩu là bắt buộc");
        if (isBlank(a.getUsername())) throw new IllegalArgumentException("Tên đăng nhập là bắt buộc");
        if (a.getRoleId() == null || a.getRoleId() <= 0) throw new IllegalArgumentException("Nhóm quyền (ID) là bắt buộc");
    }

    private void enforceAdminLimit(UserAccount a, boolean isUpdate) {
        Integer roleId = a == null ? null : a.getRoleId();
        if (!isAdminRole(roleId)) return;

        long excludeEmployeeId = isUpdate ? (a == null ? -1L : a.getEmployeeId()) : -1L;
        int adminCount = countAdminAccounts(excludeEmployeeId);
        if (adminCount >= MAX_ADMIN_ACCOUNTS) {
            throw new IllegalStateException("Nhóm quyền ADMIN chỉ tối đa " + MAX_ADMIN_ACCOUNTS
                    + " tài khoản. Hiện đã có " + adminCount + " tài khoản ADMIN.");
        }
    }

    private boolean isAdminRole(Integer roleId) {
        if (roleId == null || roleId <= 0) return false;
        String sql = "SELECT tennhomquyen FROM nhomquyen WHERE manhomquyen = ? LIMIT 1";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {
            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");
            ps.setInt(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                String name = rs.getString(1);
                return name != null && name.toLowerCase().contains("admin");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private int countAdminAccounts(long excludeEmployeeId) {
        boolean exclude = excludeEmployeeId > 0;
        String sql = "SELECT COUNT(*) "
                + "FROM taikhoan tk "
                + "JOIN nhomquyen nq ON tk.manhomquyen = nq.manhomquyen "
                + "WHERE LOWER(nq.tennhomquyen) LIKE ? "
                + (exclude ? "AND tk.manv <> ?" : "");
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {
            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");
            ps.setString(1, "%admin%");
            if (exclude) ps.setLong(2, excludeEmployeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return 0;
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String hashPasswordIfNeeded(String password) {
        if (password == null) return null;
        if (password.startsWith("pbkdf2$")) return password;
        if (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$")) return password;
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }
}
