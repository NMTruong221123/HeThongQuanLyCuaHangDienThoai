package com.phonestore.ui.common;

import com.phonestore.config.JDBCUtil;
import com.phonestore.controller.PasswordResetController;
import com.phonestore.model.Employee;
import com.phonestore.ui.common.toast.Toast;
import com.phonestore.util.service.impl.AdminDeleteOtpServiceImpl;
import com.phonestore.util.service.impl.UserAccountServiceImpl;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Delete an ADMIN account from DB (taikhoan) from the login screen.
 * UX mirrors the ForgotPasswordDialog: Step 1 input employee id, Step 2 confirm.
 */
public class DeleteAdminAccountDialog extends JDialog {

    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);

    private final JTextField txtEmployeeCode = new JTextField();

    private final JLabel lblInfoId = new JLabel();
    private final JLabel lblInfoName = new JLabel();
    private final JLabel lblInfoGender = new JLabel();
    private final JLabel lblInfoBirth = new JLabel();
    private final JLabel lblInfoPhone = new JLabel();
    private final JLabel lblInfoStatus = new JLabel();

    private final JLabel lblAccUsername = new JLabel();
    private final JLabel lblAccRole = new JLabel();
    private final JLabel lblAccActive = new JLabel();

    private final JTextField txtEmail = new JTextField();
    private final JTextField txtOtp = new JTextField();
    private boolean otpVerified;

    private final PasswordResetController employeeController = new PasswordResetController();
    private final AdminDeleteOtpServiceImpl otpService = new AdminDeleteOtpServiceImpl();
    private final UserAccountServiceImpl accountService = new UserAccountServiceImpl();

    private long employeeId;
    private Employee loaded;

    public DeleteAdminAccountDialog(Window owner, String initialUsername) {
        super(owner, "Xóa tài khoản ADMIN", ModalityType.APPLICATION_MODAL);
        initUi();

        if (initialUsername != null && !initialUsername.isBlank()) {
            SwingUtilities.invokeLater(() -> {
                try {
                    Long id = findAdminEmployeeIdByAccount(initialUsername);
                    if (id != null) {
                        txtEmployeeCode.setText(String.valueOf(id));
                        onLoadAdminEmployee();
                    }
                } catch (Exception ignored) {
                }
            });
        }
    }

    private void initUi() {
        setSize(760, 520);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());

        JPanel wrap = new JPanel(new BorderLayout(12, 12));
        wrap.setBorder(BorderFactory.createEmptyBorder(24, 16, 16, 16));

        cardPanel.add(stepEmployeeCode(), "step1");
        cardPanel.add(stepConfirmDelete(), "step2");

        wrap.add(cardPanel, BorderLayout.CENTER);
        add(wrap, BorderLayout.CENTER);

        cards.show(cardPanel, "step1");
    }

    private JComponent stepEmployeeCode() {
        JPanel p = new JPanel(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        JLabel lbl = new JLabel("Mã nhân viên (Admin)");
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 14f));
        form.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        txtEmployeeCode.setPreferredSize(new Dimension(300, 40));
        txtEmployeeCode.putClientProperty("JTextField.placeholderText", "Nhập mã NV (vd: NVPS-11 hoặc 11)...");
        form.add(txtEmployeeCode, gbc);

        p.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        JButton btnCancel = new JButton("Hủy Bỏ");
        JButton btnNext = new JButton("Tiếp tục");

        btnNext.setBackground(new Color(23, 142, 201));
        btnNext.setForeground(Color.WHITE);
        btnNext.setPreferredSize(new Dimension(140, 40));
        btnNext.setFocusPainted(false);

        btnCancel.setBackground(new Color(224, 224, 224));
        btnCancel.setForeground(new Color(60, 60, 60));
        btnCancel.setPreferredSize(new Dimension(120, 40));
        btnCancel.setFocusPainted(false);

        btnCancel.addActionListener(e -> dispose());
        btnNext.addActionListener(e -> onLoadAdminEmployee());
        actions.add(btnCancel);
        actions.add(btnNext);
        p.add(actions, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(btnNext);

        return p;
    }

    private JComponent stepConfirmDelete() {
        JPanel p = new JPanel(new BorderLayout(12, 12));

        JPanel info = new JPanel(new GridBagLayout());
        info.setBorder(BorderFactory.createTitledBorder("Thông tin nhân viên"));
        info.setPreferredSize(new Dimension(0, 140));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        row = infoRow(info, gbc, row, "Mã NV", lblInfoId, "Ngày sinh", lblInfoBirth);
        row = infoRow(info, gbc, row, "Họ tên", lblInfoName, "SĐT", lblInfoPhone);
        row = infoRow(info, gbc, row, "Giới tính", lblInfoGender, "Trạng thái", lblInfoStatus);

        JPanel acc = new JPanel(new GridBagLayout());
        acc.setBorder(BorderFactory.createTitledBorder("Thông tin tài khoản"));

        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.insets = new Insets(6, 8, 6, 8);
        gbc2.fill = GridBagConstraints.HORIZONTAL;

        int r2 = 0;
        r2 = infoRowSingle(acc, gbc2, r2, "Tên đăng nhập", lblAccUsername);
        r2 = infoRowSingle(acc, gbc2, r2, "Nhóm quyền", lblAccRole);
        r2 = infoRowSingle(acc, gbc2, r2, "Kích hoạt", lblAccActive);

        JPanel center = new JPanel(new BorderLayout(12, 12));
        center.add(info, BorderLayout.NORTH);
        center.add(acc, BorderLayout.CENTER);

        JPanel verify = new JPanel(new GridBagLayout());
        verify.setBorder(BorderFactory.createTitledBorder("Xác thực OTP"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;
        y = fieldRow(verify, g, y, "Email", txtEmail);

        JButton btnSendOtp = new JButton("Gửi OTP");
        btnSendOtp.setBackground(new Color(23, 142, 201));
        btnSendOtp.setForeground(Color.WHITE);
        btnSendOtp.setFocusPainted(false);
        btnSendOtp.setPreferredSize(new Dimension(120, 36));

        g.gridx = 1;
        g.gridy = y;
        g.weightx = 1;
        JPanel sendWrap = new JPanel(new BorderLayout(8, 0));
        sendWrap.setOpaque(false);
        sendWrap.add(btnSendOtp, BorderLayout.WEST);
        verify.add(sendWrap, g);
        y++;

        y = fieldRow(verify, g, y, "Mã OTP", txtOtp);
        txtOtp.setEnabled(false);

        JButton btnVerify = new JButton("Xác nhận OTP");
        btnVerify.setBackground(new Color(40, 167, 69));
        btnVerify.setForeground(Color.WHITE);
        btnVerify.setFocusPainted(false);
        btnVerify.setPreferredSize(new Dimension(140, 36));
        btnVerify.setEnabled(false);

        g.gridx = 1;
        g.gridy = y;
        g.weightx = 1;
        JPanel verifyWrap = new JPanel(new BorderLayout(8, 0));
        verifyWrap.setOpaque(false);
        verifyWrap.add(btnVerify, BorderLayout.WEST);
        verify.add(verifyWrap, g);
        y++;

        JLabel warn = new JLabel("Lưu ý: nút Xóa chỉ mở sau khi OTP đúng. Xóa sẽ XÓA vĩnh viễn tài khoản ADMIN khỏi CSDL.");
        warn.setForeground(new Color(183, 28, 28));

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);
        south.add(verify, BorderLayout.CENTER);
        south.add(warn, BorderLayout.SOUTH);

        center.add(south, BorderLayout.SOUTH);

        p.add(center, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        JButton btnBack = new JButton("Quay lại");
        JButton btnDelete = new JButton("Xóa tài khoản Admin");

        btnBack.setPreferredSize(new Dimension(120, 40));
        btnBack.setFocusPainted(false);
        btnBack.addActionListener(e -> cards.show(cardPanel, "step1"));

        btnDelete.setBackground(new Color(229, 57, 53));
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setPreferredSize(new Dimension(180, 40));
        btnDelete.setFocusPainted(false);
        // Locked until OTP verified
        btnDelete.setEnabled(false);
        btnDelete.setBackground(new Color(189, 189, 189));
        btnDelete.setForeground(new Color(80, 80, 80));
        btnDelete.addActionListener(e -> onDelete());

        actions.add(btnBack);
        actions.add(btnDelete);

        p.add(actions, BorderLayout.SOUTH);

        btnSendOtp.addActionListener(e -> onSendOtp(btnSendOtp, btnVerify, btnDelete));
        btnVerify.addActionListener(e -> onVerifyOtp(btnDelete));

        return p;
    }

    private int fieldRow(JPanel panel, GridBagConstraints gbc, int y, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        if (field instanceof JTextField tf) tf.setPreferredSize(new Dimension(260, 34));
        panel.add(field, gbc);
        return y + 1;
    }

    private int infoRow(JPanel panel, GridBagConstraints gbc, int row,
                        String labelL, JComponent valueL,
                        String labelR, JComponent valueR) {
        gbc.gridy = row;

        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel(labelL), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.4;
        valueL.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        panel.add(valueL, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(new JLabel(labelR), gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.6;
        valueR.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        panel.add(valueR, gbc);

        return row + 1;
    }

    private int infoRowSingle(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent value) {
        gbc.gridy = row;

        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        value.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        panel.add(value, gbc);

        return row + 1;
    }

    private void onLoadAdminEmployee() {
        Long id = parseEmployeeCode(trim(txtEmployeeCode.getText()));
        if (id == null) {
            Toast.warn(this, "Mã nhân viên không hợp lệ (vd: NVPS-11 hoặc 11)");
            return;
        }

        try {
            if (!JDBCUtil.canConnect()) {
                Toast.error(this, "Chưa kết nối được DB");
                return;
            }

            AdminAccountInfo ai = findAdminAccountInfoByEmployeeId(id);
            if (ai == null) {
                Toast.warn(this, "Nhân viên này không có tài khoản ADMIN hoặc không tồn tại");
                return;
            }

            Employee e = employeeController.findEmployeeById(id);
            if (e == null) {
                Toast.warn(this, "Không tìm thấy nhân viên");
                return;
            }

            this.employeeId = id;
            this.loaded = e;

            // Reset OTP state for a new target
            this.otpVerified = false;
            txtOtp.setText("");
            txtOtp.setEnabled(false);

            bindEmployeeInfo(e);
            lblAccUsername.setText(n(ai.username));
            lblAccRole.setText(n(ai.roleName));
            lblAccActive.setText(ai.accountStatus == 1 ? "Hoạt động" : "Khóa");

            // Do not prefill email; user must enter it manually for verification
            txtEmail.setText("");

            cards.show(cardPanel, "step2");
        } catch (Throwable ex) {
            Toast.error(this, ex.getMessage() == null ? ex.toString() : ex.getMessage());
        }
    }

    private void onSendOtp(JButton btnSendOtp, JButton btnVerify, JButton btnDelete) {
        if (employeeId <= 0 || loaded == null) {
            Toast.warn(this, "Vui lòng nhập mã nhân viên");
            return;
        }

        btnSendOtp.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return otpService.sendOtp(employeeId, txtEmail.getText());
            }

            @Override
            protected void done() {
                try {
                    String masked = get();
                    txtOtp.setEnabled(true);
                    btnVerify.setEnabled(true);
                    otpVerified = false;
                    lockDelete(btnDelete);
                    Toast.info(DeleteAdminAccountDialog.this, "Đã gửi OTP tới " + masked);
                } catch (Exception ex) {
                    String msg = ex.getMessage();
                    Toast.error(DeleteAdminAccountDialog.this, msg == null ? ex.toString() : msg);
                } finally {
                    btnSendOtp.setEnabled(true);
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };

        worker.execute();
    }

    private void onVerifyOtp(JButton btnDelete) {
        if (!txtOtp.isEnabled()) {
            Toast.warn(this, "Vui lòng gửi OTP trước");
            return;
        }
        String otp = trim(txtOtp.getText());
        if (otp.isBlank()) {
            Toast.warn(this, "Vui lòng nhập OTP");
            return;
        }

        boolean ok = otpService.verifyOtp(employeeId, otp);
        if (!ok) {
            otpVerified = false;
            lockDelete(btnDelete);
            Toast.warn(this, "OTP không đúng hoặc đã hết hạn");
            return;
        }

        otpVerified = true;
        unlockDelete(btnDelete);
        Toast.info(this, "OTP hợp lệ. Bạn có thể xóa tài khoản.");
    }

    private void lockDelete(JButton btnDelete) {
        if (btnDelete == null) return;
        btnDelete.setEnabled(false);
        btnDelete.setBackground(new Color(189, 189, 189));
        btnDelete.setForeground(new Color(80, 80, 80));
    }

    private void unlockDelete(JButton btnDelete) {
        if (btnDelete == null) return;
        btnDelete.setEnabled(true);
        btnDelete.setBackground(new Color(229, 57, 53));
        btnDelete.setForeground(Color.WHITE);
    }

    private void onDelete() {
        if (employeeId <= 0 || loaded == null) {
            Toast.warn(this, "Vui lòng nhập mã nhân viên");
            return;
        }

        if (!otpVerified) {
            Toast.warn(this, "Vui lòng xác thực OTP trước khi xóa");
            return;
        }

        try {
            // Re-check admin status at the time of deletion
            AdminAccountInfo ai = findAdminAccountInfoByEmployeeId(employeeId);
            if (ai == null) {
                Toast.warn(this, "Không tìm thấy tài khoản ADMIN để xóa");
                return;
            }

            int c = JOptionPane.showConfirmDialog(this,
                    "Xóa vĩnh viễn tài khoản ADMIN của nhân viên #" + employeeId + " (" + n(ai.username) + ")?\n"
                            + "Thao tác này không thể hoàn tác.",
                    "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
            if (c != JOptionPane.YES_OPTION) return;

            accountService.delete(employeeId);
            Toast.info(this, "Đã xóa tài khoản ADMIN");
            dispose();
        } catch (Throwable ex) {
            String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            Toast.error(this, msg);
        }
    }

    private static final class AdminAccountInfo {
        final String username;
        final String roleName;
        final int accountStatus;

        AdminAccountInfo(String username, String roleName, int accountStatus) {
            this.username = username;
            this.roleName = roleName;
            this.accountStatus = accountStatus;
        }
    }

    private AdminAccountInfo findAdminAccountInfoByEmployeeId(long employeeId) {
        String sql = "SELECT tk.tendangnhap AS username, tk.trangthai AS status, nq.tennhomquyen AS role_name "
                + "FROM taikhoan tk JOIN nhomquyen nq ON tk.manhomquyen = nq.manhomquyen "
                + "WHERE tk.manv = ? AND (LOWER(nq.tennhomquyen) LIKE ? OR LOWER(tk.tendangnhap) = 'admin') "
                + "LIMIT 1";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {
            if (c == null) return null;
            ps.setLong(1, employeeId);
            ps.setString(2, "%admin%");
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String username = rs.getString("username");
                String roleName = rs.getString("role_name");
                int st = rs.getInt("status");
                if (rs.wasNull()) st = 1;
                return new AdminAccountInfo(username, roleName, st);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Long findAdminEmployeeIdByAccount(String usernameOrEmail) {
        if (usernameOrEmail == null) return null;
        String kw = usernameOrEmail.trim();
        if (kw.isBlank()) return null;

        // We match by username (and optionally by employee email via nhanvien) but only for ADMIN accounts.
        String sql = "SELECT tk.manv "
                + "FROM taikhoan tk "
                + "JOIN nhomquyen nq ON tk.manhomquyen = nq.manhomquyen "
                + "LEFT JOIN nhanvien nv ON tk.manv = nv.manv "
                + "WHERE (tk.tendangnhap = ? OR nv.email = ?) AND (LOWER(nq.tennhomquyen) LIKE ? OR LOWER(tk.tendangnhap) = 'admin') "
                + "ORDER BY tk.manv DESC LIMIT 1";

        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {
            if (c == null) return null;
            ps.setString(1, kw);
            ps.setString(2, kw);
            ps.setString(3, "%admin%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("manv");
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void bindEmployeeInfo(Employee e) {
        lblInfoId.setText(String.valueOf(e.getId()));
        lblInfoName.setText(n(e.getFullName()));
        lblInfoGender.setText(e.getGender() == null ? "" : (e.getGender() == 1 ? "Nam" : "Nữ"));
        lblInfoBirth.setText(e.getBirthDate() == null ? "" : e.getBirthDate().toString());
        lblInfoPhone.setText(n(e.getPhone()));
        lblInfoStatus.setText((e.getStatus() != null && e.getStatus() == 1) ? "Đang làm" : "Nghỉ");
    }

    /**
     * Parse employee code accepting numeric or prefixed forms like NVPS-11, NVPS11.
     */
    private Long parseEmployeeCode(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isBlank()) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException ignored) {
        }

        java.util.regex.Matcher m = java.util.regex.Pattern.compile(".*?(\\d+)").matcher(s);
        if (m.matches()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }

        return null;
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private String n(String s) {
        return s == null ? "" : s;
    }
}
