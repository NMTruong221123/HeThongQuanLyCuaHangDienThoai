package com.phonestore.ui.common;

import com.phonestore.controller.PasswordResetController;
import com.phonestore.model.Employee;
import com.phonestore.config.JDBCUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.phonestore.ui.common.toast.Toast;

import javax.swing.*;
import java.awt.*;

public class ForgotPasswordDialog extends JDialog {

    private final PasswordResetController controller = new PasswordResetController();
    private final String initialUsername;

    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);

    private final JTextField txtEmployeeCode = new JTextField();

    private final JLabel lblInfoId = new JLabel();
    private final JLabel lblInfoName = new JLabel();
    private final JLabel lblInfoGender = new JLabel();
    private final JLabel lblInfoBirth = new JLabel();
    private final JLabel lblInfoPhone = new JLabel();
    private final JLabel lblInfoStatus = new JLabel();

    private final JTextField txtEmail = new JTextField();
    private final JTextField txtOtp = new JTextField();
    private final JPasswordField txtNewPassword = new JPasswordField();
    private final JPasswordField txtNewPassword2 = new JPasswordField();

    private long employeeId;
    private Employee loaded;

    public ForgotPasswordDialog(Window owner) {
        this(owner, null);
    }

    public ForgotPasswordDialog(Window owner, String initialUsername) {
        super(owner, "Quên mật khẩu", ModalityType.APPLICATION_MODAL);
        this.initialUsername = initialUsername;
        initUi();
        if (initialUsername != null && !initialUsername.isBlank()) {
            SwingUtilities.invokeLater(() -> {
                try {
                    Long id = findEmployeeIdByAccount(initialUsername);
                    if (id != null) {
                        txtEmployeeCode.setText(String.valueOf(id));
                        onLoadEmployee();
                    }
                } catch (Exception ignored) {
                }
            });
        }
    }

    private void initUi() {
        setSize(760, 560);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());

        // No header - dialog uses the window title bar instead

        JPanel wrap = new JPanel(new BorderLayout(12, 12));
        wrap.setBorder(BorderFactory.createEmptyBorder(24, 16, 16, 16));

        // content area
        cardPanel.add(stepEmployeeCode(), "step1");
        cardPanel.add(stepVerifyAndReset(), "step2");

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
        JLabel lbl = new JLabel("Mã nhân viên");
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 14f));
        form.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        txtEmployeeCode.setPreferredSize(new Dimension(300, 40));
        txtEmployeeCode.putClientProperty("JTextField.placeholderText", "Nhập mã nhân viên...");
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
        btnNext.addActionListener(e -> onLoadEmployee());
        actions.add(btnCancel);
        actions.add(btnNext);
        p.add(actions, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(btnNext);

        return p;
    }

    private JComponent stepVerifyAndReset() {
        JPanel p = new JPanel(new BorderLayout(12, 12));

        JPanel info = new JPanel(new GridBagLayout());
        info.setBorder(BorderFactory.createTitledBorder("Thông tin nhân viên"));
        info.setPreferredSize(new Dimension(0, 120));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Layout: 3 rows x 2 columns (left: 3 fields, right: 3 fields)
        // Columns: 0=labelL,1=valueL,2=labelR,3=valueR
        int row = 0;

        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel l00 = new JLabel("Mã NV");
        l00.setFont(l00.getFont().deriveFont(Font.PLAIN, 12f));
        info.add(l00, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.4;
        lblInfoId.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        info.add(lblInfoId, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JLabel l01 = new JLabel("Ngày sinh");
        l01.setFont(l01.getFont().deriveFont(Font.PLAIN, 12f));
        info.add(l01, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.6;
        info.add(lblInfoBirth, gbc);

        row++;

        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel l10 = new JLabel("Họ tên");
        l10.setFont(l10.getFont().deriveFont(Font.PLAIN, 12f));
        info.add(l10, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.4;
        info.add(lblInfoName, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JLabel l11 = new JLabel("SĐT");
        l11.setFont(l11.getFont().deriveFont(Font.PLAIN, 12f));
        info.add(l11, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.6;
        info.add(lblInfoPhone, gbc);

        row++;

        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel l20 = new JLabel("Giới tính");
        l20.setFont(l20.getFont().deriveFont(Font.PLAIN, 12f));
        info.add(l20, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.4;
        info.add(lblInfoGender, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JLabel l21 = new JLabel("Trạng thái");
        l21.setFont(l21.getFont().deriveFont(Font.PLAIN, 12f));
        info.add(l21, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.6;
        info.add(lblInfoStatus, gbc);

        JPanel verify = new JPanel(new GridBagLayout());
        verify.setBorder(BorderFactory.createTitledBorder("Xác minh email & OTP"));
        GridBagConstraints v = new GridBagConstraints();
        v.insets = new Insets(8, 8, 8, 8);
        v.fill = GridBagConstraints.HORIZONTAL;

        int vy = 0;
        // Email row (label + full-width field)
        vy = fieldRow(verify, v, vy, "Email nhân viên", txtEmail);
        txtEmail.putClientProperty("JTextField.placeholderText", "Nhập email để nhận OTP");
        txtEmail.setPreferredSize(new Dimension(520, 34));

        // Put the Send OTP button on its own centered row so the email field stays full-width
        JButton btnSendOtp = new JButton("Gửi OTP");
        btnSendOtp.setBackground(new Color(23, 142, 201));
        btnSendOtp.setForeground(Color.WHITE);
        btnSendOtp.setPreferredSize(new Dimension(140, 36));
        btnSendOtp.setFocusPainted(false);

        v.gridx = 0;
        v.gridy = vy;
        v.gridwidth = 2;
        v.weightx = 1;
        v.anchor = GridBagConstraints.CENTER;
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnWrap.setOpaque(false);
        btnWrap.add(btnSendOtp);
        verify.add(btnWrap, v);
        vy++;
        v.gridwidth = 1;
        v.anchor = GridBagConstraints.WEST;

        // OTP and password rows (fields full-width)
        vy = fieldRow(verify, v, vy, "OTP", txtOtp);
        txtOtp.setPreferredSize(new Dimension(520, 34));
        vy = fieldRow(verify, v, vy, "Mật khẩu mới", txtNewPassword);
        txtNewPassword.setPreferredSize(new Dimension(520, 34));
        vy = fieldRow(verify, v, vy, "Nhập lại mật khẩu", txtNewPassword2);
        txtNewPassword2.setPreferredSize(new Dimension(520, 34));

        txtOtp.setEnabled(false);
        txtNewPassword.setEnabled(false);
        txtNewPassword2.setEnabled(false);

        btnSendOtp.addActionListener(e -> onSendOtp(btnSendOtp));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        JButton btnBack = new JButton("Quay lại");
        JButton btnReset = new JButton("Đổi mật khẩu");
        JButton btnClose = new JButton("Hủy bỏ");

        btnReset.setBackground(new Color(23, 142, 201));
        btnReset.setForeground(Color.WHITE);
        btnReset.setPreferredSize(new Dimension(160, 40));
        btnReset.setFocusPainted(false);

        btnBack.setBackground(new Color(224, 224, 224));
        btnBack.setForeground(new Color(60, 60, 60));
        btnBack.setPreferredSize(new Dimension(120, 40));
        btnBack.setFocusPainted(false);

        btnClose.setBackground(new Color(224, 224, 224));
        btnClose.setForeground(new Color(60, 60, 60));
        btnClose.setPreferredSize(new Dimension(120, 40));
        btnClose.setFocusPainted(false);

        btnBack.addActionListener(e -> {
            clearStep2();
            cards.show(cardPanel, "step1");
        });
        btnClose.addActionListener(e -> dispose());
        btnReset.addActionListener(e -> onResetPassword());

        actions.add(btnBack);
        actions.add(btnClose);
        actions.add(btnReset);

        JPanel center = new JPanel(new GridBagLayout());
        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0;
        cc.gridy = 0;
        cc.weightx = 1.0;
        cc.weighty = 0.35;
        cc.fill = GridBagConstraints.BOTH;
        cc.insets = new Insets(0, 0, 8, 0);
        center.add(info, cc);

        cc.gridy = 1;
        cc.weighty = 0.65;
        cc.insets = new Insets(0, 0, 0, 0);
        center.add(verify, cc);

        p.add(center, BorderLayout.CENTER);
        p.add(actions, BorderLayout.SOUTH);

        return p;
    }

    private int infoRow(JPanel panel, GridBagConstraints gbc, int y, String label, JLabel value) {
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        value.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        panel.add(value, gbc);
        return y + 1;
    }

    private int fieldRow(JPanel panel, GridBagConstraints gbc, int y, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        if (field instanceof JTextField tf) tf.setPreferredSize(new Dimension(240, 34));
        if (field instanceof JPasswordField pf) pf.setPreferredSize(new Dimension(240, 34));
        panel.add(field, gbc);
        return y + 1;
    }

    private void onLoadEmployee() {
        Long id = parseEmployeeCode(trim(txtEmployeeCode.getText()));
        if (id == null) {
            Toast.warn(this, "Mã nhân viên không hợp lệ (vd: NVPS-11 hoặc 11)");
            return;
        }

        try {
            // Check if account for this employee is locked
            if (isAccountLocked(id)) {
                String contact = fetchManagerContact();
                String msg = "Tài khoản nhân viên đang bị khóa. Vui lòng liên hệ Admin (quản trị) để mở khóa.";
                if (contact != null && !contact.isBlank()) msg += "\n\nThông tin quản lý:\n" + contact;
                javax.swing.JOptionPane.showMessageDialog(this, msg, "Tài khoản bị khóa", javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }

            Employee e = controller.findEmployeeById(id);
            if (e == null) {
                Toast.warn(this, "Không tìm thấy nhân viên");
                return;
            }

            // Forgot-password flow only applies to employees who already have an account.
            if (!hasAccount(id)) {
                String contact = fetchManagerContact();
                StringBuilder msg = new StringBuilder("Nhân viên chưa có tài khoản.\n"
                        + "Hãy liên hệ tài khoản mang quyền ADMIN để được hỗ trợ."
                );
                if (contact != null && !contact.isBlank()) {
                    msg.append("\n\nThông tin liên hệ ADMIN:\n").append(contact);
                }
                JOptionPane.showMessageDialog(
                        this,
                        msg.toString(),
                        "Nhân viên chưa có tài khoản",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            this.employeeId = id;
            this.loaded = e;
            bindEmployeeInfo(e);

            cards.show(cardPanel, "step2");
        } catch (Throwable ex) {
            Toast.error(this, ex.getMessage() == null ? ex.toString() : ex.getMessage());
        }
    }

    /**
     * Parse employee code accepting numeric or prefixed forms like NVPS-11, NVPS11.
     * Returns null if parsing fails.
     */
    private Long parseEmployeeCode(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isBlank()) return null;
        // If it's purely numeric, parse directly
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException ignored) {
        }

        // Try to extract trailing digits (e.g., NVPS-11 or NVPS11)
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(".*?(\\d+)").matcher(s);
        if (m.matches()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }

        return null;
    }

    private Long findEmployeeIdByAccount(String username) {
        if (username == null) return null;
        String kw = username.trim();
        if (kw.isBlank()) return null;

        String sql = "SELECT manv FROM taikhoan WHERE tendangnhap = ? OR email = ? ORDER BY manv DESC LIMIT 1";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {
            if (c == null) return null;
            ps.setString(1, kw);
            ps.setString(2, kw);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("manv");
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean isAccountLocked(long employeeId) {
        String sql = "SELECT trangthai FROM taikhoan WHERE manv = ? LIMIT 1";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {
            if (c == null) return false;
            ps.setLong(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int st = rs.getInt("trangthai");
                    return !rs.wasNull() && st == 0;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean hasAccount(long employeeId) {
        String sql = "SELECT 1 FROM taikhoan WHERE manv = ? LIMIT 1";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {
            if (c == null) return false;
            ps.setLong(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private String fetchManagerContact() {
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) return null;
                String sql = "SELECT nv.hoten AS name, nv.email AS email, nv.sdt AS phone "
                    + "FROM taikhoan t JOIN nhanvien nv ON t.manv = nv.manv JOIN nhomquyen n ON t.manhomquyen = n.manhomquyen "
                    + "WHERE LOWER(n.tennhomquyen) LIKE ? AND t.trangthai = 1";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, "%admin%");
                try (ResultSet rs = ps.executeQuery()) {
                    StringBuilder sb = new StringBuilder();
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        String name = rs.getString("name");
                        String email = rs.getString("email");
                        String phone = rs.getString("phone");
                        sb.append("ADMIN ").append(count).append(":\n");
                        if (name != null && !name.isBlank()) sb.append("  Họ tên: ").append(name).append("\n");
                        if (email != null && !email.isBlank()) sb.append("  Email: ").append(email).append("\n");
                        if (phone != null && !phone.isBlank()) sb.append("  SĐT: ").append(phone).append("\n");
                        sb.append("\n");
                    }
                    if (count > 0) return sb.toString().trim();
                }
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

        // intentionally NOT showing employee email here (the user must input it for verification)
    }

    private void onSendOtp(JButton btnSendOtp) {
        if (employeeId <= 0 || loaded == null) {
            Toast.warn(this, "Vui lòng nhập mã nhân viên");
            return;
        }

        btnSendOtp.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return controller.sendOtp(employeeId, txtEmail.getText());
            }

            @Override
            protected void done() {
                try {
                    String masked = get();
                    txtOtp.setEnabled(true);
                    txtNewPassword.setEnabled(true);
                    txtNewPassword2.setEnabled(true);
                    Toast.info(ForgotPasswordDialog.this, "Đã gửi OTP tới " + masked);
                } catch (Exception ex) {
                    String msg = ex.getMessage();
                    Toast.error(ForgotPasswordDialog.this, msg == null ? ex.toString() : msg);
                } finally {
                    btnSendOtp.setEnabled(true);
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };

        worker.execute();
    }

    private void onResetPassword() {
        if (!txtOtp.isEnabled()) {
            Toast.warn(this, "Vui lòng gửi OTP trước");
            return;
        }

        String otp = trim(txtOtp.getText());
        String p1 = new String(txtNewPassword.getPassword());
        String p2 = new String(txtNewPassword2.getPassword());

        if (otp.isBlank()) {
            Toast.warn(this, "Vui lòng nhập OTP");
            return;
        }
        if (trim(p1).isBlank()) {
            Toast.warn(this, "Vui lòng nhập mật khẩu mới");
            return;
        }
        if (!p1.equals(p2)) {
            Toast.warn(this, "Mật khẩu nhập lại không khớp");
            return;
        }

        try {
            controller.resetPassword(employeeId, otp, p1);
            Toast.info(this, "Đổi mật khẩu thành công");
            dispose();
        } catch (Throwable ex) {
            Toast.error(this, ex.getMessage() == null ? ex.toString() : ex.getMessage());
        }
    }

    private void clearStep2() {
        employeeId = 0;
        loaded = null;
        txtEmail.setText("");
        txtOtp.setText("");
        txtNewPassword.setText("");
        txtNewPassword2.setText("");
        txtOtp.setEnabled(false);
        txtNewPassword.setEnabled(false);
        txtNewPassword2.setEnabled(false);
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private String n(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
