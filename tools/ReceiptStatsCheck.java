import java.io.FileInputStream;
import java.nio.file.Path;
import java.sql.*;
import java.util.Properties;

public class ReceiptStatsCheck {
    public static void main(String[] args) throws Exception {
        Properties p = new Properties();
        Path f = Path.of("db.properties");
        if (!f.toFile().exists()) {
            System.err.println("Missing db.properties");
            System.exit(2);
        }
        try (FileInputStream in = new FileInputStream(f.toFile())) { p.load(in); }

        String url = p.getProperty("db.url");
        String user = p.getProperty("db.username");
        String pass = p.getProperty("db.password");

        System.out.println("Using db.url=" + url);
        try (Connection c = DriverManager.getConnection(url, user, pass);
             Statement s = c.createStatement()) {
            System.out.println("Connected. DB=" + c.getCatalog());

            reportTable(s, "phieunhap", "maphieunhap", "thoigian", "trangthai");
            reportTable(s, "phieuxuat", "maphieuxuat", "thoigian", "trangthai");
        }
    }

    private static void reportTable(Statement s, String table, String idCol, String timeCol, String statusCol) throws SQLException {
        System.out.println("\n== " + table + " ==");
        try (ResultSet rs = s.executeQuery(
                "SELECT COUNT(*) total, "
                        + "SUM(CASE WHEN " + timeCol + " IS NULL THEN 1 ELSE 0 END) null_time, "
                        + "SUM(CASE WHEN " + statusCol + " IS NULL THEN 1 ELSE 0 END) null_status, "
                        + "SUM(CASE WHEN " + statusCol + "=1 THEN 1 ELSE 0 END) st1, "
                        + "SUM(CASE WHEN " + statusCol + "=0 THEN 1 ELSE 0 END) st0 "
                        + "FROM " + table)) {
            if (rs.next()) {
                System.out.println("total=" + rs.getLong("total")
                        + ", null_time=" + rs.getLong("null_time")
                        + ", null_status=" + rs.getLong("null_status")
                        + ", st1=" + rs.getLong("st1")
                        + ", st0=" + rs.getLong("st0"));
            }
        }

        String q = "SELECT " + idCol + " AS id, " + timeCol + " AS t, " + statusCol + " AS st FROM " + table + " ORDER BY " + idCol + " DESC LIMIT 10";
        try (ResultSet rs = s.executeQuery(q)) {
            System.out.println("last 10:");
            while (rs.next()) {
                Object id = rs.getObject("id");
                Object t = rs.getObject("t");
                Object st = rs.getObject("st");
                System.out.println(" - id=" + id + " | thoigian=" + t + " | trangthai=" + st);
            }
        }

        // show column default for time column
        try (ResultSet rs = s.executeQuery(
                "SELECT IS_NULLABLE, COLUMN_DEFAULT, DATA_TYPE "
                        + "FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name='" + table + "' AND column_name='" + timeCol + "'")) {
            if (rs.next()) {
                System.out.println("column " + timeCol + ": type=" + rs.getString("DATA_TYPE")
                        + ", nullable=" + rs.getString("IS_NULLABLE")
                        + ", default=" + rs.getString("COLUMN_DEFAULT"));
            }
        }
    }
}
