package com.phonestore.util.service.impl;

import com.phonestore.config.JDBCUtil;
import com.phonestore.model.Employee;
import com.phonestore.util.mail.SmtpMailer;
import com.phonestore.util.otp.OtpManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Dedicated OTP flow for deleting ADMIN accounts.
 * Uses a separate OTP key namespace and a separate email subject/body.
 */
public class AdminDeleteOtpServiceImpl {

    public Employee findEmployeeById(long employeeId) {
        if (employeeId <= 0) throw new IllegalArgumentException("Mã nhân viên không hợp lệ");
        requireDb();

        String sql = "SELECT manv, hoten, gioitinh, ngaysinh, sdt, email, trangthai FROM nhanvien WHERE manv=?";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {

            if (c == null) throw new IllegalStateException(JDBCUtil.buildConnectionError("Chưa kết nối được DB"));
            ps.setLong(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Employee e = new Employee();
                e.setId(rs.getLong("manv"));
                e.setFullName(rs.getString("hoten"));
                int g = rs.getInt("gioitinh");
                e.setGender(rs.wasNull() ? null : g);
                java.sql.Date d = rs.getDate("ngaysinh");
                e.setBirthDate(d == null ? null : d.toLocalDate());
                e.setPhone(rs.getString("sdt"));
                e.setEmail(rs.getString("email"));
                int st = rs.getInt("trangthai");
                e.setStatus(rs.wasNull() ? null : st);
                return e;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public String sendOtp(long employeeId, String emailInput) {
        Employee e = findEmployeeById(employeeId);
        if (e == null) throw new IllegalArgumentException("Không tìm thấy nhân viên");

        String input = trim(emailInput);
        if (input.isBlank()) throw new IllegalArgumentException("Vui lòng nhập mail của nhân viên");

        String dbEmail = trim(e.getEmail());
        if (dbEmail.isBlank()) {
            throw new IllegalStateException("Nhân viên chưa có mail trong hệ thống");
        }

        if (!dbEmail.equalsIgnoreCase(input)) {
            throw new IllegalArgumentException("Mail không khớp với dữ liệu nhân viên");
        }

        // Ensure this employee really has an ADMIN account
        AdminAccount ai = findAdminAccount(employeeId);
        if (ai == null) {
            throw new IllegalArgumentException("Nhân viên này không có tài khoản ADMIN");
        }

        String otp = OtpManager.issue(otpKey(employeeId));

        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        String subject = "[PhoneStore] OTP xác nhận xóa tài khoản ADMIN";
        String body = buildOtpMailBody(e, ai, dbEmail, otp, now);
        SmtpMailer.sendText(dbEmail, subject, body);

        return maskEmail(dbEmail);
    }

    public boolean verifyOtp(long employeeId, String otp) {
        if (employeeId <= 0) return false;
        String o = trim(otp);
        if (o.isBlank()) return false;
        return OtpManager.verify(otpKey(employeeId), o);
    }

    private static final class AdminAccount {
        final String username;
        final String roleName;

        private AdminAccount(String username, String roleName) {
            this.username = username;
            this.roleName = roleName;
        }
    }

    private AdminAccount findAdminAccount(long employeeId) {
        String sql = "SELECT tk.tendangnhap AS username, nq.tennhomquyen AS role_name "
                + "FROM taikhoan tk JOIN nhomquyen nq ON tk.manhomquyen = nq.manhomquyen "
                + "WHERE tk.manv = ? AND (LOWER(nq.tennhomquyen) LIKE ? OR LOWER(tk.tendangnhap) = 'admin') LIMIT 1";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {
            if (c == null) throw new IllegalStateException(JDBCUtil.buildConnectionError("Chưa kết nối được DB"));
            ps.setLong(1, employeeId);
            ps.setString(2, "%admin%");
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new AdminAccount(rs.getString("username"), rs.getString("role_name"));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String buildOtpMailBody(Employee e, AdminAccount ai, String email, String otp, LocalDateTime whenTime) {
        long employeeId = (e == null) ? 0 : e.getId();
        String fullName = (e == null) ? "" : trim(e.getFullName());
        String phone = (e == null) ? "" : trim(e.getPhone());
        String when = (whenTime == null)
                ? LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                : whenTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        String code = (otp == null) ? "" : otp.trim();
        String username = ai == null ? "" : trim(ai.username);
        String roleName = ai == null ? "" : trim(ai.roleName);

        return "PHONESTORE - XÁC THỰC XÓA TÀI KHOẢN ADMIN (OTP)\n"
                + "==========================================\n"
                + "\n"
                + "Kính gửi " + (fullName.isBlank() ? "nhân viên" : fullName) + ",\n"
                + "\n"
                + "Hệ thống PhoneStore ghi nhận yêu cầu XÓA tài khoản có quyền ADMIN.\n"
                + "Để tiếp tục, vui lòng nhập mã OTP dưới đây vào màn hình xác thực.\n"
                + "\n"
                + "Mã xác thực (OTP) của bạn:\n\n"
                + "    " + code + "\n\n"
                + "Thời hạn hiệu lực: 05 phút kể từ thời điểm gửi email này.\n"
                + "Tuyệt đối không cung cấp mã OTP cho bất kỳ ai dưới mọi hình thức.\n"
                + "\n"
                + "Thông tin truy xuất từ hệ thống:\n"
                + "- Nhân viên: " + (employeeId <= 0 ? "" : (employeeId + (fullName.isBlank() ? "" : " - " + fullName))) + "\n"
                + (phone.isBlank() ? "" : "- SĐT: " + phone + "\n")
                + (username.isBlank() ? "" : "- Tài khoản: " + username + "\n")
                + (roleName.isBlank() ? "" : "- Nhóm quyền: " + roleName + "\n")
                + "- Email nhận OTP: " + email + "\n"
                + "- Thời điểm yêu cầu: " + when + "\n"
                + "\n"
                + "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.\n"
                + "Trân trọng,\n"
                + "PhoneStore";
    }

    private void requireDb() {
        if (!JDBCUtil.canConnect()) {
            throw new IllegalStateException(JDBCUtil.buildConnectionError("Chưa kết nối được DB"));
        }
    }

    private String otpKey(long employeeId) {
        return "admindelete:" + employeeId;
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private String maskEmail(String email) {
        String e = trim(email);
        int at = e.indexOf('@');
        if (at <= 1) return e;
        String name = e.substring(0, at);
        String domain = e.substring(at);
        String masked = name.charAt(0) + "***" + name.charAt(name.length() - 1);
        return masked + domain;
    }
}
