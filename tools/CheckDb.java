import java.io.FileInputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.Properties;

public class CheckDb {
    public static void main(String[] args) throws Exception {
        Properties p = new Properties();
        Path f = Path.of("db.properties");
        if (f.toFile().exists()) {
            try (FileInputStream in = new FileInputStream(f.toFile())) { p.load(in); }
        }
        String url = p.getProperty("db.url").trim();
        String user = p.getProperty("db.username");
        String pass = p.getProperty("db.password");
        System.out.println("Using db.url=" + url);
        System.out.println("Using db.username=" + user);
        try (Connection c = DriverManager.getConnection(url, user, pass)) {
            System.out.println("Connected. Catalog=" + c.getCatalog());
            try (Statement s = c.createStatement()) {
                try (ResultSet rs = s.executeQuery("SELECT DATABASE()")) {
                    if (rs.next()) System.out.println("SELECT DATABASE() => " + rs.getString(1));
                }
                try (ResultSet rs2 = s.executeQuery("SHOW TABLES")) {
                    System.out.println("Tables:");
                    int cnt = 0;
                    while (rs2.next() && cnt < 50) {
                        System.out.println(" - " + rs2.getString(1));
                        cnt++;
                    }
                }

                reportCustomers(s);
            }
        } catch (Exception e) {
            System.err.println("ERROR: " + e);
            e.printStackTrace(System.err);
            System.exit(2);
        }
    }

    private static void reportCustomers(Statement s) throws SQLException {
        System.out.println("\nCustomers (khachhang):");
        try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM khachhang")) {
            if (rs.next()) System.out.println(" - total: " + rs.getLong(1));
        }
        try (ResultSet rs = s.executeQuery("SELECT trangthai, COUNT(*) cnt FROM khachhang GROUP BY trangthai ORDER BY trangthai")) {
            while (rs.next()) {
                Object status = rs.getObject("trangthai");
                long cnt = rs.getLong("cnt");
                System.out.println(" - trangthai=" + status + ": " + cnt);
            }
        }
        try (ResultSet rs = s.executeQuery(
            "SELECT makh, tenkhachhang, sdt, trangthai FROM khachhang ORDER BY makh DESC LIMIT 10")) {
            System.out.println(" - last 10:");
            while (rs.next()) {
                System.out.println(
                    "   #" + rs.getLong("makh")
                        + " | " + rs.getString("tenkhachhang")
                        + " | " + rs.getString("sdt")
                        + " | trangthai=" + rs.getObject("trangthai")
                );
            }
        }
    }
}
