package com.phonestore.app;

import com.phonestore.constant.PathConstants;
import com.phonestore.ui.common.UiTheme;
import com.phonestore.ui.common.WindowUtils;
import com.phonestore.ui.common.images.ImageLoader;
import com.phonestore.ui.common.session.SessionContext;
import com.phonestore.ui.common.session.UserSession;
import com.phonestore.ui.common.toast.Toast;
import com.phonestore.ui.employee.AccountPanel;
import com.phonestore.ui.employee.EmployeePanel;
import com.phonestore.ui.employee.RolePermissionPanel;
import com.phonestore.ui.inventory.ExportReceiptPanel;
import com.phonestore.ui.inventory.ImportReceiptPanel;
import com.phonestore.ui.inventory.WarehouseZonePanel;
import com.phonestore.ui.product.AttributePanel;
import com.phonestore.ui.product.ProductPanel;
import com.phonestore.ui.product.partner.CustomerPanel;
import com.phonestore.ui.product.partner.SupplierPanel;
import com.phonestore.ui.dashboard.DashboardPanel;
import com.phonestore.ui.stats.StatisticsPanel;
import com.phonestore.ui.common.MainFrame;
import com.phonestore.ui.common.LoginDialog;

import javax.swing.*;

public final class AppBootstrap {

    private AppBootstrap() {}

    public static void start() {
        PathConstants.ensureDataDirectories();

        SwingUtilities.invokeLater(() -> {
            UiTheme.apply();

            LoginDialog loginDialog = new LoginDialog(null);
            WindowUtils.centerOnScreen(loginDialog);
            loginDialog.setVisible(true);

            UserSession session = SessionContext.getSession();
            if (session == null) {
                return;
            }

            MainFrame mainFrame = new MainFrame(session);
            mainFrame.registerScreen("dashboard", new DashboardPanel());
            mainFrame.registerScreen("products", new ProductPanel());
            mainFrame.registerScreen("attributes", new AttributePanel());
            mainFrame.registerScreen("warehouse_zones", new WarehouseZonePanel());
            mainFrame.registerScreen("import_receipts", new ImportReceiptPanel());
            mainFrame.registerScreen("export_receipts", new ExportReceiptPanel());
            mainFrame.registerScreen("customers", new CustomerPanel());
            mainFrame.registerScreen("suppliers", new SupplierPanel());
            mainFrame.registerScreen("employees", new EmployeePanel());
            mainFrame.registerScreen("accounts", new AccountPanel());
            mainFrame.registerScreen("employees_permissions", new RolePermissionPanel());
            mainFrame.registerScreen("statistics", new StatisticsPanel());

            mainFrame.showScreen("dashboard");
            mainFrame.setVisible(true);

            ImageLoader.preloadDefaults();
            Toast.info(mainFrame, "Đăng nhập thành công: " + session.getUsername());
        });
    }
}
