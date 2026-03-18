package com.phonestore.util.service.impl;

import com.phonestore.config.JDBCUtil;
import com.phonestore.model.Employee;
import com.phonestore.util.mail.SmtpMailer;
import com.phonestore.util.otp.OtpManager;
import com.phonestore.util.service.PasswordResetService;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PasswordResetServiceImpl implements PasswordResetService {

    @Override
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

    @Override
    public String sendOtp(long employeeId, String email) {
        Employee e = findEmployeeById(employeeId);
        if (e == null) throw new IllegalArgumentException("Không tìm thấy nhân viên");

        String input = trim(email);
        if (input.isBlank()) throw new IllegalArgumentException("Vui lòng nhập mail của nhân viên");

        String dbEmail = trim(e.getEmail());
        if (dbEmail.isBlank()) {
            throw new IllegalStateException("Nhân viên chưa có mail trong hệ thống");
        }

        if (!dbEmail.equalsIgnoreCase(input)) {
            throw new IllegalArgumentException("Mail không khớp với dữ liệu nhân viên");
        }

        String key = otpKey(employeeId);
        String otp = OtpManager.issue(key);

        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        String subject = "[PhoneStore] Thông báo yêu cầu đổi mật khẩu (OTP)";
        String body = buildOtpMailBody(e, dbEmail, otp, now);
        SmtpMailer.sendText(dbEmail, subject, body);

        return maskEmail(dbEmail);
    }

    private String buildOtpMailBody(Employee e, String email, String otp, LocalDateTime whenTime) {
        long employeeId = (e == null) ? 0 : e.getId();
        String fullName = (e == null) ? "" : trim(e.getFullName());
        String phone = (e == null) ? "" : trim(e.getPhone());
        String when = (whenTime == null)
                ? LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                : whenTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        String code = (otp == null) ? "" : otp.trim();
        String codePretty = code;

        return "PHONESTORE - THÔNG BÁO BẢO MẬT\n"
            + "================================\n"
            + "\n"
            + "Kính gửi " + (fullName.isBlank() ? "nhân viên" : fullName) + ",\n"
            + "\n"
            + "Hệ thống PhoneStore ghi nhận một yêu cầu đổi/đặt lại mật khẩu cho tài khoản nhân viên.\n"
            + "Yêu cầu đã được hệ thống tự động phê duyệt để tiếp tục bước xác thực.\n"
            + "\n"
            + "Mã xác thực (OTP) của bạn:\n"
            + "\n"
            + "    " + codePretty + "\n"
            + "\n"
            + "Thời hạn hiệu lực: 05 phút kể từ thời điểm gửi email này.\n"
            + "Vì lý do bảo mật, tuyệt đối không cung cấp mã OTP cho bất kỳ ai dưới mọi hình thức.\n"
            + "\n"
            + "Thông tin truy xuất từ hệ thống:\n"
            + "- Nhân viên: " + (employeeId <= 0 ? "" : (employeeId + (fullName.isBlank() ? "" : " - " + fullName))) + "\n"
            + (phone.isBlank() ? "" : "- SĐT: " + phone + "\n")
            + "- Email nhận OTP: " + email + "\n"
            + "- Thời điểm yêu cầu: " + when + "\n"
            + "\n"
            + "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.\n"
            + "Nếu nghi ngờ có truy cập trái phép, vui lòng liên hệ quản trị cửa hàng để được hỗ trợ kịp thời.\n"
            + "\n"
            + "Trân trọng,\n"
            + "PhoneStore";
    }

    @Override
    public void resetPassword(long employeeId, String otp, String newPassword) {
        if (employeeId <= 0) throw new IllegalArgumentException("Mã nhân viên không hợp lệ");
        String o = trim(otp);
        if (o.isBlank()) throw new IllegalArgumentException("Vui lòng nhập OTP");
        String pw = trim(newPassword);
        if (pw.isBlank()) throw new IllegalArgumentException("Vui lòng nhập mật khẩu mới");

        boolean ok = OtpManager.verify(otpKey(employeeId), o);
        if (!ok) {
            throw new IllegalArgumentException("OTP không đúng hoặc đã hết hạn");
        }

        requireDb();
        String hashed = BCrypt.hashpw(pw, BCrypt.gensalt(12));

        String sql = "UPDATE taikhoan SET matkhau=? WHERE manv=?";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {

            if (c == null) throw new IllegalStateException(JDBCUtil.buildConnectionError("Chưa kết nối được DB"));
            ps.setString(1, hashed);
            ps.setLong(2, employeeId);
            int updated = ps.executeUpdate();
            if (updated <= 0) {
                throw new IllegalArgumentException("Không tìm thấy tài khoản nhân viên để cập nhật mật khẩu");
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void requireDb() {
        if (!JDBCUtil.canConnect()) {
            throw new IllegalStateException(JDBCUtil.buildConnectionError("Chưa kết nối được DB"));
        }
    }

    private String otpKey(long employeeId) {
        return "pwreset:" + employeeId;
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
