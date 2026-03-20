package com.phonestore.ui.common;

import com.phonestore.ui.common.images.ImageLoader;
import com.phonestore.ui.common.session.UserSession;
import com.phonestore.ui.common.LoginDialog;
import com.phonestore.ui.common.toast.Toast;
import com.phonestore.ui.common.WindowUtils;
import com.phonestore.ui.common.session.SessionContext;
import com.phonestore.ui.dashboard.DashboardPanel;
import com.phonestore.ui.product.ProductPanel;
import com.phonestore.ui.product.AttributePanel;
import com.phonestore.ui.inventory.WarehouseZonePanel;
import com.phonestore.ui.inventory.ImportReceiptPanel;
import com.phonestore.ui.inventory.ExportReceiptPanel;
import com.phonestore.ui.product.partner.CustomerPanel;
import com.phonestore.ui.product.partner.SupplierPanel;
import com.phonestore.ui.employee.EmployeePanel;
import com.phonestore.ui.employee.AccountPanel;
import com.phonestore.ui.stats.StatisticsPanel;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainFrame extends JFrame {

    private final UserSession session;

    private final JPanel content = new JPanel(new CardLayout());
    private final Map<String, JComponent> screens = new LinkedHashMap<>();

    private final Color mainBackground = new Color(250, 250, 250);
    private final Color sidebarBackground = Color.white;
    private final Color hoverFontColor = new Color(1, 87, 155);
    private final Color hoverBackgroundColor = new Color(187, 222, 251);
    private final Color separatorColor = new Color(204, 214, 219);
    private final Color menuTextColor = new Color(33, 33, 33);
    private final Color subTextColor = new Color(120, 144, 156);

    private final java.util.List<JButton> menuButtons = new java.util.ArrayList<>();
    private JButton selectedButton;

    public MainFrame(UserSession session) {
        super("PhoneStoreManagement");
        this.session = session;
        initUi();
    }

    public void registerScreen(String key, JComponent component) {
        screens.put(key, component);
        content.add(component, key);
    }

    public void showScreen(String key) {
        CardLayout cl = (CardLayout) content.getLayout();
        cl.show(content, key);
    }

    private void initUi() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(1400, 800);
        setLocationRelativeTo(null);
        setTitle("Hệ thống quản lý kho hàng");

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildSidebar(), BorderLayout.WEST);

        content.setBackground(mainBackground);
        root.add(content, BorderLayout.CENTER);

        setContentPane(root);
    }

    // confirm on window close
    @Override
    public void addNotify() {
        super.addNotify();
        // attach once
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                int res = JOptionPane.showConfirmDialog(
                        MainFrame.this,
                        "Bạn có chắc chắn muốn thoát?",
                        "Xác nhận thoát",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );
                if (res == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        });
    }

    private JComponent buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 0));
        sidebar.setPreferredSize(new Dimension(320, 0));
        sidebar.setBackground(sidebarBackground);

        JPanel top = new JPanel(new BorderLayout());
        top.setPreferredSize(new Dimension(320, 140));
        top.setBackground(sidebarBackground);

        JLabel appTitle = new JLabel("Hệ thống quản lý kho hàng");
        appTitle.setForeground(menuTextColor);
        appTitle.setBorder(BorderFactory.createEmptyBorder(10, 16, 4, 16));
        top.add(appTitle, BorderLayout.NORTH);

        JPanel profile = new JPanel(new BorderLayout(12, 0));
        profile.setOpaque(false);
        profile.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        // Avatar (use existing svg in resources)
        JLabel avatar = new JLabel(ImageLoader.loadIcon("man_50px.svg", 56, 56));
        avatar.setPreferredSize(new Dimension(64, 64));
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        profile.add(avatar, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        String displayName = session == null ? "" : session.getUsername();
        JLabel lblUser = new JLabel(displayName);
        lblUser.setFont(lblUser.getFont().deriveFont(Font.BOLD, 22f));
        lblUser.setForeground(menuTextColor);

        String roleText = "";
        if (session != null) {
            // prefer explicit role from session permissions (role:...)
            String found = null;
            for (String p : session.getPermissions()) {
                if (p != null && p.startsWith("role:")) {
                    found = p.substring("role:".length());
                    break;
                }
            }
            if (found != null && !found.isBlank()) roleText = found;
            else roleText = "admin".equalsIgnoreCase(session.getUsername()) ? "Quản lý kho" : "Nhân viên";
        }
        JLabel lblRole = new JLabel(roleText);
        lblRole.setFont(lblRole.getFont().deriveFont(Font.PLAIN, 14f));
        lblRole.setForeground(subTextColor);

        info.add(lblUser);
        info.add(Box.createVerticalStrut(4));
        info.add(lblRole);

        profile.add(info, BorderLayout.CENTER);
        top.add(profile, BorderLayout.CENTER);

        JPanel barTopRight = new JPanel();
        barTopRight.setBackground(separatorColor);
        barTopRight.setPreferredSize(new Dimension(1, 0));
        top.add(barTopRight, BorderLayout.EAST);

        JPanel barTopBottom = new JPanel();
        barTopBottom.setBackground(separatorColor);
        barTopBottom.setPreferredSize(new Dimension(0, 1));
        top.add(barTopBottom, BorderLayout.SOUTH);

        sidebar.add(top, BorderLayout.NORTH);

        JPanel menu = new JPanel();
        menu.setBackground(sidebarBackground);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));

        JPanel menuWrap = new JPanel(new BorderLayout());
        menuWrap.setBackground(sidebarBackground);
        menuWrap.setBorder(BorderFactory.createEmptyBorder(10, 14, 0, 14));
        menuWrap.add(menu, BorderLayout.NORTH);

        JPanel barRight = new JPanel();
        barRight.setBackground(separatorColor);
        barRight.setPreferredSize(new Dimension(1, 1));
        sidebar.add(barRight, BorderLayout.EAST);

        sidebar.add(menuWrap, BorderLayout.CENTER);

        // Items (only add buttons the session is allowed to see)
        int gap = 4;
        if (isScreenAllowed("dashboard")) { menu.add(menuButton("Trang chủ", "home.svg", () -> showScreen("dashboard"))); menu.add(Box.createVerticalStrut(gap)); }
        if (isScreenAllowed("products")) { menu.add(menuButton("Sản phẩm", "product.svg", () -> showScreen("products"))); menu.add(Box.createVerticalStrut(gap)); }
        if (isScreenAllowed("attributes")) { menu.add(menuButton("Thuộc tính", "brand.svg", () -> showScreen("attributes"))); menu.add(Box.createVerticalStrut(gap)); }
        if (isScreenAllowed("warehouse_zones")) { menu.add(menuButton("Khu vực kho", "area.svg", () -> showScreen("warehouse_zones"))); menu.add(Box.createVerticalStrut(gap)); }
        if (isScreenAllowed("import_receipts")) { menu.add(menuButton("Phiếu nhập", "import.svg", () -> showScreen("import_receipts"))); menu.add(Box.createVerticalStrut(gap)); }
        if (isScreenAllowed("export_receipts")) { menu.add(menuButton("Phiếu xuất", "export.svg", () -> showScreen("export_receipts"))); menu.add(Box.createVerticalStrut(gap)); }
        if (isScreenAllowed("customers")) { menu.add(menuButton("Khách hàng", "customer.svg", () -> showScreen("customers"))); menu.add(Box.createVerticalStrut(gap)); }
        if (isScreenAllowed("suppliers")) { menu.add(menuButton("Nhà cung cấp", "supplier.svg", () -> showScreen("suppliers"))); menu.add(Box.createVerticalStrut(gap)); }
        if (isScreenAllowed("employees")) { menu.add(menuButton("Nhân viên", "staff.svg", () -> showScreen("employees"))); menu.add(Box.createVerticalStrut(gap)); }
        if (isScreenAllowed("accounts")) { menu.add(menuButton("Tài khoản", "account.svg", () -> showScreen("accounts"))); menu.add(Box.createVerticalStrut(gap)); }
        if (isScreenAllowed("statistics")) { menu.add(menuButton("Thống kê", "statistical.svg", () -> showScreen("statistics"))); menu.add(Box.createVerticalStrut(gap)); }

        menu.add(Box.createVerticalGlue());

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setPreferredSize(new Dimension(320, 70));
        bottom.setBackground(sidebarBackground);

        JPanel barBottomRight = new JPanel();
        barBottomRight.setBackground(separatorColor);
        barBottomRight.setPreferredSize(new Dimension(1, 1));
        bottom.add(barBottomRight, BorderLayout.EAST);

        JButton logout = menuButton("Đăng xuất", "log_out.svg", () -> performLogout());
        logout.setForeground(new Color(183, 28, 28));
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 14, 12, 14));
        bottom.add(logout, BorderLayout.CENTER);
        sidebar.add(bottom, BorderLayout.SOUTH);

        // Default selected: choose first enabled button
        JButton firstAllowed = null;
        for (JButton b : menuButtons) {
            if (b.isEnabled()) { firstAllowed = b; break; }
        }
        if (firstAllowed == null && !menuButtons.isEmpty()) firstAllowed = menuButtons.get(0);
        if (firstAllowed != null) setSelected(firstAllowed);

        return sidebar;
    }

    private void performLogout() {
        int res = JOptionPane.showConfirmDialog(
                MainFrame.this,
                "Bạn có chắc muốn đăng xuất?",
                "Xác nhận đăng xuất",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (res != JOptionPane.YES_OPTION) return;

        try {
            // clear session
            SessionContext.clear();

            // dispose current frame and show login dialog
            SwingUtilities.invokeLater(() -> {
                try {
                    MainFrame.this.dispose();

                    LoginDialog loginDialog = new LoginDialog(null);
                    WindowUtils.centerOnScreen(loginDialog);
                    loginDialog.setVisible(true);

                    UserSession session = SessionContext.getSession();
                    if (session == null) {
                        // user cancelled login; exit
                        return;
                    }

                    MainFrame newFrame = new MainFrame(session);
                    newFrame.registerScreen("dashboard", new DashboardPanel());
                    newFrame.registerScreen("products", new ProductPanel());
                    newFrame.registerScreen("attributes", new AttributePanel());
                    newFrame.registerScreen("warehouse_zones", new WarehouseZonePanel());
                    newFrame.registerScreen("import_receipts", new ImportReceiptPanel());
                    newFrame.registerScreen("export_receipts", new ExportReceiptPanel());
                    newFrame.registerScreen("customers", new CustomerPanel());
                    newFrame.registerScreen("suppliers", new SupplierPanel());
                    newFrame.registerScreen("employees", new EmployeePanel());
                    newFrame.registerScreen("accounts", new AccountPanel());
                    newFrame.registerScreen("statistics", new StatisticsPanel());

                    newFrame.showScreen("dashboard");
                    newFrame.setVisible(true);

                    ImageLoader.preloadDefaults();
                    Toast.info(newFrame, "Đăng nhập thành công: " + session.getUsername());
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    private JButton menuButton(String text, String icon, Runnable onClick) {
        JButton button = new JButton(text);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIcon(ImageLoader.loadIcon(icon, 26, 26));
        button.setPreferredSize(new Dimension(260, 46));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMargin(new Insets(8, 16, 8, 16));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setBackground(sidebarBackground);
        button.setForeground(menuTextColor);
        button.setFont(button.getFont().deriveFont(Font.PLAIN, 16f));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFocusable(false);

        // FlatLaf: rounded background when selected/hover
        button.putClientProperty("JComponent.arc", 14);
        button.putClientProperty("JButton.buttonType", "borderless");

        button.addActionListener(e -> {
            try {
                setSelected(button);
                onClick.run();
            } catch (Throwable t) {
                try {
                    t.printStackTrace();
                } catch (Throwable ignored) {
                    // ignore
                }

                String msg = t.getClass().getSimpleName();
                if (t.getMessage() != null && !t.getMessage().isBlank()) {
                    msg += ": " + t.getMessage();
                }
                JOptionPane.showMessageDialog(
                        MainFrame.this,
                        msg,
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (button != selectedButton) {
                    button.setBackground(hoverBackgroundColor);
                    button.setForeground(hoverFontColor);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (button != selectedButton) {
                    button.setBackground(sidebarBackground);
                    button.setForeground(menuTextColor);
                }
            }
        });

        menuButtons.add(button);
        return button;
    }

    private JButton menuButtonForScreen(String screenKey, String text, String icon) {
        // This helper is no longer used for adding buttons; keep for backward compatibility
        return menuButton(text, icon, () -> showScreen(screenKey));
    }

    private boolean isScreenAllowed(String screenKey) {
        try {
            // no special-case screens
            com.phonestore.ui.common.session.UserSession s = SessionContext.getSession();
            if (s == null) return true;
            // full-role shortcut
            for (String p : s.getPermissions()) {
                if (p != null && p.startsWith("role:")) {
                    String r = p.substring("role:".length()).toLowerCase();
                    if (r.contains("quản lý") || r.contains("quan ly")) return true;
                }
            }
            if (s.getPermissions().contains(screenKey)) return true;
            return false;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private void setSelected(JButton button) {
        selectedButton = button;
        for (JButton b : menuButtons) {
            if (b == button) {
                b.setBackground(hoverBackgroundColor);
                b.setForeground(hoverFontColor);
            } else {
                b.setBackground(sidebarBackground);
                b.setForeground(menuTextColor);
            }
        }
    }
}
