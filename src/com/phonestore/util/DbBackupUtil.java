package com.phonestore.util;

import com.phonestore.config.JDBCUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

public class DbBackupUtil {
    // Simple CSV export for selected tables into target directory. Returns file created path on success.
    public static File exportTablesAsCsv(File dir, String... tables) throws Exception {
        if (dir == null) throw new IllegalArgumentException("dir is null");
        if (!dir.exists()) dir.mkdirs();
        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException("Cannot connect to DB");
            for (String t : tables) {
                if (t == null || t.isBlank()) continue;
                File out = new File(dir, t + "_backup.csv");
                try (PreparedStatement ps = c.prepareStatement("SELECT * FROM " + t)) {
                    try (ResultSet rs = ps.executeQuery(); PrintWriter pw = new PrintWriter(new FileWriter(out))) {
                        ResultSetMetaData md = rs.getMetaData();
                        int cols = md.getColumnCount();
                        // header
                        for (int i = 1; i <= cols; i++) {
                            if (i > 1) pw.print(',');
                            pw.print('"'); pw.print(md.getColumnName(i)); pw.print('"');
                        }
                        pw.println();
                        while (rs.next()) {
                            for (int i = 1; i <= cols; i++) {
                                if (i > 1) pw.print(',');
                                String v = rs.getString(i);
                                if (v == null) v = "";
                                pw.print('"'); pw.print(v.replace("\"","\"\"")); pw.print('"');
                            }
                            pw.println();
                        }
                    }
                } catch (Exception ex) {
                    // ignore single table failure but continue
                }
            }
        }
        return dir;
    }

    // Export full SQL dump (CREATE TABLE + INSERTs) for given tables into a single .sql file.
    public static File exportTablesAsSql(File dir, String dumpName, String... tables) throws Exception {
        if (dir == null) throw new IllegalArgumentException("dir is null");
        if (!dir.exists()) dir.mkdirs();
        File out = new File(dir, (dumpName == null || dumpName.isBlank() ? "dump" : dumpName) + ".sql");
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(out))) {
            try (java.sql.Connection c = JDBCUtil.getConnectionSilent()) {
                if (c == null) throw new IllegalStateException("Cannot connect to DB");
                for (String t : tables) {
                    if (t == null || t.isBlank()) continue;
                    // write CREATE TABLE using SHOW CREATE TABLE
                    try (java.sql.PreparedStatement ps = c.prepareStatement("SHOW CREATE TABLE `" + t + "`")) {
                        try (java.sql.ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                String create = rs.getString(2);
                                if (create != null) {
                                    pw.println("-- ----------------------------");
                                    pw.println("-- Table structure for `" + t + "`");
                                    pw.println("-- ----------------------------");
                                    pw.println(create + ";\n");
                                }
                            }
                        }
                    } catch (Exception ex) {
                        // ignore show create failure
                    }

                    // write INSERTs
                    try (java.sql.PreparedStatement ps = c.prepareStatement("SELECT * FROM `" + t + "`")) {
                        try (java.sql.ResultSet rs = ps.executeQuery()) {
                            java.sql.ResultSetMetaData md = rs.getMetaData();
                            int cols = md.getColumnCount();
                            pw.println("-- ----------------------------");
                            pw.println("-- Records for `" + t + "`");
                            pw.println("-- ----------------------------");
                            while (rs.next()) {
                                StringBuilder vals = new StringBuilder();
                                for (int i = 1; i <= cols; i++) {
                                    if (i > 1) vals.append(',');
                                    Object obj = rs.getObject(i);
                                    if (obj == null) {
                                        vals.append("NULL");
                                    } else {
                                        String s = rs.getString(i);
                                        if (s == null) { vals.append("NULL"); }
                                        else {
                                            s = s.replace("\\", "\\\\").replace("'", "\\'");
                                            vals.append('\'').append(s).append('\'');
                                        }
                                    }
                                }
                                pw.println("INSERT INTO `" + t + "` VALUES (" + vals.toString() + ");");
                            }
                            pw.println();
                        }
                    } catch (Exception ex) {
                        // ignore select failure
                    }
                }
            }
        }
        return out;
    }
}
