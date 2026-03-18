import java.io.FileInputStream;
import java.sql.*;
import java.util.*;

public class DbIntrospect {
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("db.properties")) {
            props.load(in);
        }
        String url = props.getProperty("db.url");
        String user = props.getProperty("db.username");
        String pass = props.getProperty("db.password");

        System.out.println("URL=" + url);
        try (Connection c = DriverManager.getConnection(url, user, pass)) {
            String db = c.getCatalog();
            System.out.println("Connected. catalog=" + db);

            listTables(c, db);
            describeIfExists(c, db, "sanpham");
            describeIfExists(c, db, "ctsanpham");
            describeIfExists(c, db, "phienbansanpham");

            // Find candidate config/detail tables
            findTablesLike(c, db, List.of("%cauhinh%", "%ctsanpham%", "%chitiet%", "%variant%", "%phienban%"));
            findTablesLike(c, db, List.of("%rom%", "%ram%", "%mausac%", "%hedieuhanh%"));
        }
    }

    private static void listTables(Connection c, String db) throws SQLException {
        System.out.println("\n== TABLES ==");
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT table_name FROM information_schema.tables WHERE table_schema=? ORDER BY table_name")) {
            ps.setString(1, db);
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    System.out.println(rs.getString(1));
                    count++;
                }
                System.out.println("(count=" + count + ")");
            }
        }
    }

    private static void findTablesLike(Connection c, String db, List<String> likes) throws SQLException {
        System.out.println("\n== TABLES MATCHING " + likes + " ==");
        StringBuilder sql = new StringBuilder(
                "SELECT table_name FROM information_schema.tables WHERE table_schema=? AND (");
        for (int i = 0; i < likes.size(); i++) {
            if (i > 0) sql.append(" OR ");
            sql.append("table_name LIKE ?");
        }
        sql.append(") ORDER BY table_name");

        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            ps.setString(1, db);
            for (int i = 0; i < likes.size(); i++) {
                ps.setString(2 + i, likes.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    System.out.println(rs.getString(1));
                    count++;
                }
                System.out.println("(count=" + count + ")");
            }
        }
    }

    private static void describeIfExists(Connection c, String db, String table) throws SQLException {
        if (!tableExists(c, db, table)) {
            System.out.println("\n== DESCRIBE " + table + " ==\n<not found>");
            return;
        }

        System.out.println("\n== DESCRIBE " + table + " ==");
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT column_name, data_type, is_nullable, column_default, column_key, extra " +
                        "FROM information_schema.columns " +
                        "WHERE table_schema=? AND table_name=? ORDER BY ordinal_position")) {
            ps.setString(1, db);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("- %-24s %-12s nullable=%-3s key=%-3s extra=%s default=%s%n",
                            rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(5), rs.getString(6), rs.getString(4));
                }
            }
        }
    }

    private static boolean tableExists(Connection c, String db, String table) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM information_schema.tables WHERE table_schema=? AND table_name=? LIMIT 1")) {
            ps.setString(1, db);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
