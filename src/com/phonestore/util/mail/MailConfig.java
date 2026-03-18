package com.phonestore.util.mail;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class MailConfig {

    private MailConfig() {}

    public record Smtp(String host, int port, String username, String password, String from, String fromName,
                       boolean ssl, boolean startTls) {}

    public static Smtp load() {
        Properties p = new Properties();

        // 1) System properties
        String host = trimToNull(System.getProperty("smtp.host"));
        String port = trimToNull(System.getProperty("smtp.port"));
        String username = trimToNull(System.getProperty("smtp.username"));
        String password = normalizePassword(System.getProperty("smtp.password"));
        String from = trimToNull(System.getProperty("smtp.from"));
        String fromName = trimToNull(System.getProperty("smtp.fromName"));
        String ssl = trimToNull(System.getProperty("smtp.ssl"));
        String startTls = trimToNull(System.getProperty("smtp.starttls"));

        // 2) Environment variables
        if (host == null) host = trimToNull(System.getenv("PHONESTORE_SMTP_HOST"));
        if (port == null) port = trimToNull(System.getenv("PHONESTORE_SMTP_PORT"));
        if (username == null) username = trimToNull(System.getenv("PHONESTORE_SMTP_USERNAME"));
        if (password == null) password = normalizePassword(System.getenv("PHONESTORE_SMTP_PASSWORD"));
        if (from == null) from = trimToNull(System.getenv("PHONESTORE_SMTP_FROM"));
        if (fromName == null) fromName = trimToNull(System.getenv("PHONESTORE_SMTP_FROM_NAME"));
        if (ssl == null) ssl = trimToNull(System.getenv("PHONESTORE_SMTP_SSL"));
        if (startTls == null) startTls = trimToNull(System.getenv("PHONESTORE_SMTP_STARTTLS"));

        // 3) Files
        Properties fp = new Properties();
        loadFromFile(Path.of("mail.properties"), fp);
        loadFromFile(Path.of("config").resolve("mail.properties"), fp);
        loadFromFile(Path.of("resources").resolve("mail.properties"), fp);

        if (host == null) host = trimToNull(fp.getProperty("smtp.host"));
        if (port == null) port = trimToNull(fp.getProperty("smtp.port"));
        if (username == null) username = trimToNull(fp.getProperty("smtp.username"));
        if (password == null) password = normalizePassword(fp.getProperty("smtp.password"));
        if (from == null) from = trimToNull(fp.getProperty("smtp.from"));
        if (fromName == null) fromName = trimToNull(fp.getProperty("smtp.fromName"));
        if (ssl == null) ssl = trimToNull(fp.getProperty("smtp.ssl"));
        if (startTls == null) startTls = trimToNull(fp.getProperty("smtp.starttls"));

        // 4) Classpath resource
        Properties cp = new Properties();
        loadFromResource("mail.properties", cp);
        if (host == null) host = trimToNull(cp.getProperty("smtp.host"));
        if (port == null) port = trimToNull(cp.getProperty("smtp.port"));
        if (username == null) username = trimToNull(cp.getProperty("smtp.username"));
        if (password == null) password = normalizePassword(cp.getProperty("smtp.password"));
        if (from == null) from = trimToNull(cp.getProperty("smtp.from"));
        if (fromName == null) fromName = trimToNull(cp.getProperty("smtp.fromName"));
        if (ssl == null) ssl = trimToNull(cp.getProperty("smtp.ssl"));
        if (startTls == null) startTls = trimToNull(cp.getProperty("smtp.starttls"));

        if (host == null) return null;

        int pPort = 587;
        try {
            if (port != null) pPort = Integer.parseInt(port);
        } catch (Exception ignored) {
            // default
        }

        boolean useSsl = parseBool(ssl, pPort == 465);
        boolean useStartTls = parseBool(startTls, pPort == 587);

        if (from == null) from = username;

        return new Smtp(host, pPort, username, password, from, fromName, useSsl, useStartTls);
    }

    private static String normalizePassword(String password) {
        if (password == null) return null;
        String t = password.trim();
        if (t.isBlank()) return null;

        // Allow pasting Gmail App Password in grouped form: "xxxx xxxx xxxx xxxx".
        boolean hasWhitespace = false;
        for (int i = 0; i < t.length(); i++) {
            if (Character.isWhitespace(t.charAt(i))) {
                hasWhitespace = true;
                break;
            }
        }
        if (!hasWhitespace) return t;

        StringBuilder sb = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (!Character.isWhitespace(ch)) sb.append(ch);
        }
        String compact = sb.toString();

        // Only auto-compact if it matches the typical Gmail App Password length.
        if (compact.length() == 16) return compact;
        return t;
    }

    private static void loadFromFile(Path path, Properties into) {
        try {
            if (path == null) return;
            if (!Files.exists(path)) return;
            try (InputStream in = Files.newInputStream(path)) {
                into.load(in);
            }
        } catch (Exception ignored) {
        }
    }

    private static void loadFromResource(String name, Properties into) {
        try (InputStream in = MailConfig.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) return;
            into.load(in);
        } catch (Exception ignored) {
        }
    }

    private static boolean parseBool(String s, boolean def) {
        if (s == null) return def;
        String t = s.trim().toLowerCase();
        if (t.isBlank()) return def;
        return t.equals("true") || t.equals("1") || t.equals("yes") || t.equals("y");
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isBlank() ? null : t;
    }
}
