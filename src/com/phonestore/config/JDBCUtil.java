package com.phonestore.config;

import javax.swing.*;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public final class JDBCUtil {

    private JDBCUtil() {}

    private static volatile boolean loggedDriverMissing;
    private static volatile boolean loggedConnectionFailure;

        private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/quanlikhohang"
            + "?useUnicode=true&characterEncoding=UTF-8"
            + "&useSSL=false&allowPublicKeyRetrieval=true"
            + "&serverTimezone=Asia/Ho_Chi_Minh";
        private static final String DEFAULT_USER = "root";
        private static final String DEFAULT_PASSWORD = "";

        private static volatile DbConfig cachedConfig;
        private static volatile ConnectionDiagnostic lastDiagnostic;

        private record DbConfig(String url, String username, String password) {}

        public record ConnectionDiagnostic(boolean ok, String message) {}

    private static final Map<String, Boolean> columnExistsCache = new ConcurrentHashMap<>();

    public static Connection getConnection() {
        try {
            Connection c = getConnectionSilent();
            if (c == null) {
                throw new IllegalStateException(buildConnectionError("Không thể kết nối đến cơ sở dữ liệu"));
            }
            return c;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Không thể kết nối đến cơ sở dữ liệu!\n" + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    public static Connection getConnectionSilent() {
        try {
            if (!isDriverAvailable()) {
                lastDiagnostic = new ConnectionDiagnostic(false, "Thiếu MySQL driver (mysql-connector-j).\nKiểm tra thư mục lib/ có file mysql-connector*.jar và classpath đã include lib/*.");
                return null;
            }
            DbConfig cfg = getDbConfig();
            Connection c = DriverManager.getConnection(cfg.url(), cfg.username(), cfg.password());
            lastDiagnostic = new ConnectionDiagnostic(true, "OK");
            return c;
        } catch (Exception e) {
            DbConfig cfg = getDbConfig();
            lastDiagnostic = new ConnectionDiagnostic(false, buildConnectionFailureMessage(cfg, e));
            if (!loggedConnectionFailure) {
                loggedConnectionFailure = true;
                System.err.println("[JDBC] Không kết nối được MySQL: " + e);
                System.err.println("[JDBC] " + lastDiagnostic.message());
            }
            return null;
        }
    }

    /**
     * Returns a connection diagnostic message that is safe to show to users (no password printed).
     */
    public static String buildConnectionError(String prefix) {
        ConnectionDiagnostic d = lastDiagnostic;
        if (d == null || d.ok()) {
            d = diagnose();
        }
        if (d.ok()) return prefix;
        if (prefix == null || prefix.isBlank()) return d.message();
        return prefix + "\n" + d.message();
    }

    /**
     * Attempts to connect once and returns a readable diagnostic message.
     */
    public static ConnectionDiagnostic diagnose() {
        if (lastDiagnostic != null && lastDiagnostic.ok()) {
            return lastDiagnostic;
        }

        if (!isDriverAvailable()) {
            ConnectionDiagnostic d = new ConnectionDiagnostic(false,
                    "Thiếu MySQL driver (mysql-connector-j).\n"
                            + "Kiểm tra thư mục lib/ có mysql-connector*.jar và classpath đã include lib/*.");
            lastDiagnostic = d;
            return d;
        }

        DbConfig cfg = getDbConfig();
        try (Connection c = DriverManager.getConnection(cfg.url(), cfg.username(), cfg.password())) {
            ConnectionDiagnostic d = new ConnectionDiagnostic(true, "OK");
            lastDiagnostic = d;
            return d;
        } catch (Exception e) {
            ConnectionDiagnostic d = new ConnectionDiagnostic(false, buildConnectionFailureMessage(cfg, e));
            lastDiagnostic = d;
            return d;
        }
    }

    /**
     * Quick connectivity check.
     * Must not leak connections.
     */
    public static boolean canConnect() {
        return diagnose().ok();
    }

    public static boolean isDriverAvailable() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return true;
        } catch (Exception e) {
            if (!loggedDriverMissing) {
                loggedDriverMissing = true;
                System.err.println("[JDBC] Thiếu MySQL driver (mysql-connector-j). Không thể chạy chế độ MySQL.");
            }
            return false;
        }
    }

    private static DbConfig getDbConfig() {
        DbConfig cfg = cachedConfig;
        if (cfg != null) return cfg;

        synchronized (JDBCUtil.class) {
            if (cachedConfig != null) return cachedConfig;

            // 1) System properties
            String url = trimToNull(System.getProperty("db.url"));
            String username = trimToNull(System.getProperty("db.username"));
            String password = System.getProperty("db.password");

            // 2) Environment variables
            if (url == null) url = trimToNull(System.getenv("PHONESTORE_DB_URL"));
            if (username == null) username = trimToNull(System.getenv("PHONESTORE_DB_USERNAME"));
            if (password == null) password = System.getenv("PHONESTORE_DB_PASSWORD");

            // 3) Working directory files
            Properties fileProps = new Properties();
            loadPropertiesFromFile(Path.of("db.properties"), fileProps);
            loadPropertiesFromFile(Path.of("config").resolve("db.properties"), fileProps);

            if (url == null) url = trimToNull(fileProps.getProperty("db.url"));
            if (username == null) username = trimToNull(fileProps.getProperty("db.username"));
            if (password == null) password = fileProps.getProperty("db.password");

            // 4) Classpath resource
            if (url == null || username == null || password == null) {
                Properties cpProps = new Properties();
                loadPropertiesFromResource("db.properties", cpProps);
                if (url == null) url = trimToNull(cpProps.getProperty("db.url"));
                if (username == null) username = trimToNull(cpProps.getProperty("db.username"));
                if (password == null) password = cpProps.getProperty("db.password");
            }

            if (url == null) url = DEFAULT_URL;
            if (username == null) username = DEFAULT_USER;
            if (password == null) password = DEFAULT_PASSWORD;

            cachedConfig = new DbConfig(url, username, password);
            return cachedConfig;
        }
    }

    private static void loadPropertiesFromFile(Path file, Properties into) {
        try {
            if (file == null) return;
            if (!Files.exists(file)) return;
            try (InputStream in = Files.newInputStream(file)) {
                into.load(in);
            }
        } catch (Exception ignored) {
        }
    }

    private static void loadPropertiesFromResource(String resourceName, Properties into) {
        try (InputStream in = JDBCUtil.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) return;
            into.load(in);
        } catch (Exception ignored) {
        }
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String buildConnectionFailureMessage(DbConfig cfg, Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Không kết nối được MySQL.\n");

        String msg = e == null ? null : e.getMessage();
        if (msg != null && !msg.isBlank()) {
            sb.append("Chi tiết: ").append(msg).append("\n");
        }

        if (cfg != null) {
            sb.append("URL: ").append(cfg.url()).append("\n");
            sb.append("User: ").append(cfg.username()).append("\n");
        }

        boolean accessDenied = false;
        boolean unknownDb = false;
        boolean comms = false;

        if (e instanceof SQLException se) {
            String state = se.getSQLState();
            if (state != null && state.startsWith("28")) {
                accessDenied = true;
            }
        }
        String m = msg == null ? "" : msg.toLowerCase(Locale.ROOT);
        if (m.contains("access denied")) accessDenied = true;
        if (m.contains("unknown database") || m.contains("unknown database")) unknownDb = true;
        if (m.contains("communications link failure") || m.contains("connect")) comms = true;

        sb.append("Gợi ý kiểm tra:\n");
        if (comms) sb.append("- MySQL service đang chạy? Host/port đúng? (mặc định 3306)\n");
        if (unknownDb) sb.append("- Database trong URL có tồn tại không?\n");
        if (accessDenied) sb.append("- Sai username/password MySQL.\n");
        sb.append("- File cấu hình: db.properties (cùng thư mục chạy) hoặc config/db.properties\n");

        return sb.toString().trim();
    }

    /**
     * Checks if a table has a column (best-effort, cached). Useful for detecting schema mismatches.
     */
    public static boolean hasColumn(String tableName, String columnName) {
        if (tableName == null || tableName.isBlank() || columnName == null || columnName.isBlank()) {
            return false;
        }

        String key = (tableName.trim().toLowerCase(Locale.ROOT) + "." + columnName.trim().toLowerCase(Locale.ROOT));
        Boolean cached = columnExistsCache.get(key);
        if (cached != null) return cached;

        if (!isDriverAvailable()) {
            columnExistsCache.put(key, false);
            return false;
        }

        try (Connection c = getConnectionSilent()) {
            if (c == null) {
                columnExistsCache.put(key, false);
                return false;
            }

            // Using DatabaseMetaData is more portable than querying INFORMATION_SCHEMA directly.
            try (ResultSet rs = c.getMetaData().getColumns(c.getCatalog(), null, tableName, columnName)) {
                boolean ok = rs.next();
                columnExistsCache.put(key, ok);
                return ok;
            }
        } catch (Exception e) {
            columnExistsCache.put(key, false);
            return false;
        }
    }

    public static void closeConnection(Connection c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
