package com.phonestore.ui.common;

import com.phonestore.controller.AuthController;
import com.phonestore.ui.common.images.ImageLoader;
import com.phonestore.ui.common.ForgotPasswordDialog;
import com.phonestore.ui.common.session.SessionContext;
import com.phonestore.ui.common.session.UserSession;
import com.phonestore.ui.common.toast.Toast;

import javax.swing.*;
import java.awt.*;

public class LoginDialog extends JDialog {

    private final JTextField txtUsername = new JTextField();
    private final JPasswordField txtPassword = new JPasswordField();

    private final AuthController authController = new AuthController();

    public LoginDialog(Frame owner) {
        super(owner, "Đăng nhập", true);
        initUi();
    }

    private void initUi() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1000, 500);
        setResizable(false);
        setLayout(new BorderLayout(0, 0));

        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createEmptyBorder(3, 10, 5, 5));
        left.setPreferredSize(new Dimension(500, 0));
        JLabel lblImage = new JLabel(ImageLoader.loadDefaultImageFit("login-image.svg", 480, 440));
        lblImage.setHorizontalAlignment(SwingConstants.CENTER);
        left.add(lblImage, BorderLayout.CENTER);
        add(left, BorderLayout.WEST);

        JPanel right = new JPanel();
        right.setBackground(Color.white);
        right.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        right.setPreferredSize(new Dimension(500, 0));
        right.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 10));

        JLabel lblTitle = new JLabel("ĐĂNG NHẬP VÀO HỆ THỐNG");
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 20f));
        right.add(lblTitle);

        JPanel panelDn = new JPanel(new GridLayout(2, 1, 0, 10));
        panelDn.setBackground(Color.white);
        panelDn.setPreferredSize(new Dimension(400, 200));

        txtUsername.setPreferredSize(new Dimension(400, 45));
        txtPassword.setPreferredSize(new Dimension(400, 45));
        txtUsername.setBorder(BorderFactory.createTitledBorder("Tên đăng nhập"));
        txtPassword.setBorder(BorderFactory.createTitledBorder("Mật khẩu"));
        panelDn.add(txtUsername);
        panelDn.add(txtPassword);
        right.add(panelDn);

        JButton btnForgot = new JButton("Quên mật khẩu?");
        btnForgot.setBorderPainted(false);
        btnForgot.setContentAreaFilled(false);
        btnForgot.setFocusPainted(false);
        btnForgot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnForgot.setForeground(new Color(33, 33, 33));
        btnForgot.setFont(btnForgot.getFont().deriveFont(Font.PLAIN, 14f));
        btnForgot.setPreferredSize(new Dimension(185, 30));
        btnForgot.addActionListener(e -> {
            ForgotPasswordDialog dlg = new ForgotPasswordDialog(this, txtUsername.getText());
            dlg.setVisible(true);
        });

        JButton btnDeleteAdmin = new JButton("Xóa ADMIN");
        btnDeleteAdmin.setBorderPainted(false);
        btnDeleteAdmin.setContentAreaFilled(false);
        btnDeleteAdmin.setFocusPainted(false);
        btnDeleteAdmin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnDeleteAdmin.setForeground(new Color(33, 33, 33));
        btnDeleteAdmin.setFont(btnDeleteAdmin.getFont().deriveFont(Font.PLAIN, 14f));
        btnDeleteAdmin.setPreferredSize(new Dimension(185, 30));
        btnDeleteAdmin.addActionListener(e -> {
            DeleteAdminAccountDialog dlg = new DeleteAdminAccountDialog(this, txtUsername.getText());
            dlg.setVisible(true);
        });

        JPanel links = new JPanel(new GridLayout(1, 2, 8, 0));
        links.setBackground(Color.white);
        links.setPreferredSize(new Dimension(400, 30));
        links.setMaximumSize(new Dimension(400, 30));
        links.add(btnForgot);
        links.add(btnDeleteAdmin);
        right.add(links);

        JButton btnLogin = new JButton("ĐĂNG NHẬP");
        btnLogin.setForeground(Color.white);
        btnLogin.setBackground(Color.black);
        btnLogin.setPreferredSize(new Dimension(380, 45));
        btnLogin.addActionListener(e -> doLogin());
        getRootPane().setDefaultButton(btnLogin);

        JButton btnCancel = new JButton("Thoát");
        btnCancel.setPreferredSize(new Dimension(380, 40));
        btnCancel.addActionListener(e -> {
            SessionContext.clear();
            dispose();
        });

        right.add(btnLogin);
        right.add(btnCancel);
        add(right, BorderLayout.CENTER);
    }

    private void doLogin() {
        String username = txtUsername.getText() == null ? "" : txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        try {
            UserSession session = authController.login(username, password);
            SessionContext.setSession(session);
            dispose();
        } catch (Exception ex) {
            String msg = ex.getMessage() == null ? "Đăng nhập thất bại" : ex.getMessage();
            // If account locked, show manager contact info to request unlock
            try {
                if (msg.toLowerCase().contains("khóa") || msg.toLowerCase().contains("khoá") || msg.toLowerCase().contains("bị khóa")) {
                    String contact = fetchManagerContact();
                    String full = "Tài khoản đã bị khóa. Vui lòng liên hệ Admin (quản trị) để mở khóa.";
                    if (contact != null && !contact.isBlank()) full += "\n\nThông tin Admin:\n" + contact;
                    javax.swing.JOptionPane.showMessageDialog(this, full, "Tài khoản bị khóa", javax.swing.JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (Throwable ignored) {
            }

            Toast.error(this, msg);
        }
    }

    private String fetchManagerContact() {
        try (java.sql.Connection c = com.phonestore.config.JDBCUtil.getConnectionSilent()) {
            if (c == null) return null;
            String sql = "SELECT nv.hoten AS name, nv.email AS email, nv.sdt AS phone "
                    + "FROM taikhoan t JOIN nhanvien nv ON t.manv = nv.manv JOIN nhomquyen n ON t.manhomquyen = n.manhomquyen "
                    + "WHERE LOWER(n.tennhomquyen) LIKE ? AND t.trangthai = 1";
            try (java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, "%admin%");
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    StringBuilder sb = new StringBuilder();
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        String name = rs.getString("name");
                        String email = rs.getString("email");
                        String phone = rs.getString("phone");
                        sb.append("Quản lý ").append(count).append(":\n");
                        if (name != null && !name.isBlank()) sb.append("  Họ tên: ").append(name).append("\n");
                        if (email != null && !email.isBlank()) sb.append("  Email: ").append(email).append("\n");
                        if (phone != null && !phone.isBlank()) sb.append("  SĐT: ").append(phone).append("\n");
                        sb.append("\n");
                    }
                    if (count > 0) return sb.toString().trim();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
