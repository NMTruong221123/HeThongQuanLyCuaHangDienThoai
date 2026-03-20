package com.phonestore.util.service.impl;

import com.phonestore.config.JDBCUtil;
import com.phonestore.ui.common.session.UserSession;
import com.phonestore.util.PasswordHasher;
import com.phonestore.util.service.AuthService;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

public class AuthServiceImpl implements AuthService {

    @Override
    public UserSession login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Sai tài khoản hoặc mật khẩu");
        }

        String u = username.trim();

        // Support both DB schemas:
        // - Newer schema: taikhoan.tendangnhap
        // - Older dumps: taikhoan.email (used as login id)
        // Order by manv DESC so newer rows win (helps when dumps contain duplicated tendangnhap).
        String sqlUsername = "SELECT manv, tendangnhap AS username, matkhau, trangthai FROM taikhoan WHERE tendangnhap=? ORDER BY manv DESC";
        String sqlEmail = "SELECT manv, email AS username, matkhau, trangthai FROM taikhoan WHERE email=? ORDER BY manv DESC";

        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement psUsername = c == null ? null : c.prepareStatement(sqlUsername);
             PreparedStatement psEmail = c == null ? null : c.prepareStatement(sqlEmail)) {

            if (c == null) {
                throw new IllegalStateException(JDBCUtil.buildConnectionError("Chưa kết nối được DB"));
            }

            UserSession session = tryLogin(psUsername, u, password);
            if (session != null) return session;

            // Fallback for older dumps using email as username
            session = tryLogin(psEmail, u, password);
            if (session != null) return session;

            // If there are no accounts at all, give a more helpful message.
            int totalAccounts = countAccounts(c);
            if (totalAccounts == 0) {
                throw new IllegalStateException(
                        "Bảng tài khoản (taikhoan) đang rỗng hoặc bạn đang kết nối nhầm database.\n"
                                + "Hãy import đúng database và chạy script tạo admin: scripts/create_admin_account.sql\n"
                                + "Sau đó đăng nhập: admin / admin");
            }

            // If accounts exist but none can be verified, and at least one uses BCrypt, hint how to reset.
            if (hasBcryptAccount(c, u)) {
                throw new IllegalStateException(
                        "Tài khoản tồn tại nhưng mật khẩu đang được lưu dạng BCrypt ($2a$...).\n"
                        + "Vui lòng nhập đúng mật khẩu được cấp cho tài khoản này.");
            }

            throw new IllegalArgumentException("Sai tài khoản hoặc mật khẩu");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private int countAccounts(Connection c) {
        if (c == null) return 0;
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) AS cnt FROM taikhoan")) {
            if (!rs.next()) return 0;
            return rs.getInt("cnt");
        } catch (Exception ignored) {
            return 0;
        }
    }

    private UserSession tryLogin(PreparedStatement ps, String username, String password) throws SQLException {
        if (ps == null) return null;
        try {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                boolean anyRow = false;
                boolean anyLocked = false;
                while (rs.next()) {
                    anyRow = true;

                    long employeeId = rs.getLong("manv");
                    String dbUser = rs.getString("username");
                    String dbPass = rs.getString("matkhau");

                    Integer status = null;
                    int st = rs.getInt("trangthai");
                    status = rs.wasNull() ? null : st;

                    if (status != null && status == 0) {
                        anyLocked = true;
                        continue;
                    }

                    if (verifyPassword(password, dbPass)) {
                        // determine role name (if available) and build permissions
                        String roleName = null;
                        try (PreparedStatement psRole = ps.getConnection().prepareStatement(
                                "SELECT n.tennhomquyen AS role_name FROM taikhoan t LEFT JOIN nhomquyen n ON t.manhomquyen = n.manhomquyen WHERE t.manv = ? LIMIT 1")) {
                            psRole.setLong(1, employeeId);
                            try (ResultSet rsr = psRole.executeQuery()) {
                                if (rsr.next()) {
                                    roleName = rsr.getString("role_name");
                                }
                            }
                        } catch (Exception ignored) {
                            // ignore problems reading role; fall back to defaults
                        }

                        java.util.Set<String> perms = new java.util.HashSet<>();
                        // always allow dashboard
                        perms.add("dashboard");
                        if (roleName != null) perms.add("role:" + roleName);

                        // If explicit admin account (username admin or role contains 'admin'), grant full manager-like UI + admin role
                        boolean isAdminUser = "admin".equalsIgnoreCase(dbUser) || (roleName != null && roleName.toLowerCase().contains("admin"));
                        if (isAdminUser) {
                            perms.clear();
                            perms.add("dashboard");
                            // grant full access similar to manager
                            perms.add("products");
                            perms.add("attributes");
                            perms.add("warehouse_zones");
                            perms.add("import_receipts");
                            perms.add("export_receipts");
                            perms.add("customers");
                            perms.add("suppliers");
                            perms.add("employees");
                            perms.add("accounts");
                            perms.add("statistics");
                            perms.add("role:admin");
                            return new UserSession(employeeId, dbUser, java.util.Collections.unmodifiableSet(perms));
                        }

                        String rn = roleName == null ? "" : roleName.trim().toLowerCase();
                        if (rn.contains("quản lý") || rn.contains("quan ly") || rn.contains("quản lý kho") || rn.contains("quan ly kho")) {
                            perms.add("products");
                            perms.add("attributes");
                            perms.add("warehouse_zones");
                            perms.add("employees");
                            perms.add("accounts");
                            perms.add("statistics");
                        } else if (rn.contains("nhập") || rn.contains("nhap") || rn.contains("nhân viên nhập") || rn.contains("nhan vien nhap")) {
                            // import staff should also manage suppliers
                            perms.add("products");
                            perms.add("attributes");
                            perms.add("import_receipts");
                            perms.add("suppliers");
                        } else if (rn.contains("xuất") || rn.contains("xuat") || rn.contains("nhân viên xuất") || rn.contains("nhan vien xuat") || rn.contains("phiếu xuất") || rn.contains("phieu xuat")) {
                            perms.add("products");
                            perms.add("attributes");
                            perms.add("export_receipts");
                        } else {
                            // default for unknown role: basic access to dashboard and products
                            perms.add("products");
                            perms.add("attributes");
                        }

                        return new UserSession(employeeId, dbUser, java.util.Collections.unmodifiableSet(perms));
                    }
                }

                if (!anyRow) return null;
                if (anyLocked) {
                    // If the only matching records are locked, report it clearly.
                    throw new IllegalArgumentException("Tài khoản đã bị khóa");
                }

                // Records exist but none matched password.
                throw new IllegalArgumentException("Sai tài khoản hoặc mật khẩu");
            }
        } catch (SQLException ex) {
            // If the column doesn't exist in the current schema, treat as not applicable and let caller fallback.
            String m = ex.getMessage();
            if (m != null) {
                String ml = m.toLowerCase();
                if (ml.contains("unknown column") || ml.contains("doesn't exist") || ml.contains("does not exist")) {
                    return null;
                }
            }
            throw ex;
        }
    }

    private boolean verifyPassword(String rawPassword, String stored) {
        if (rawPassword == null) return false;
        if (stored == null || stored.isBlank()) return false;

        if (stored.startsWith("pbkdf2$")) {
            return PasswordHasher.verify(rawPassword, stored);
        }

        if (isBcryptHash(stored)) {
            try {
                return BCrypt.checkpw(rawPassword, stored);
            } catch (Exception ignored) {
                return false;
            }
        }

        // Fallback for DBs storing plaintext
        return stored.equals(rawPassword);
    }

    private boolean isBcryptHash(String s) {
        return s != null && (s.startsWith("$2a$") || s.startsWith("$2b$") || s.startsWith("$2y$"));
    }

    private boolean hasBcryptAccount(Connection c, String username) {
        if (c == null || username == null || username.isBlank()) return false;
        String sql = "SELECT 1 FROM taikhoan WHERE tendangnhap=? AND matkhau LIKE '$2%' LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception ignored) {
            return false;
        }
    }
}
