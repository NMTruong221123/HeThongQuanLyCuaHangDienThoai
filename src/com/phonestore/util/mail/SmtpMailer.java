package com.phonestore.util.mail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.Base64;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public final class SmtpMailer {

    private SmtpMailer() {}

    public static void sendText(String to, String subject, String body) {
        if (to == null || to.trim().isEmpty()) throw new IllegalArgumentException("Email nhận không hợp lệ");
        MailConfig.Smtp cfg = MailConfig.load();
        if (cfg == null) {
            throw new IllegalStateException(
                    "Chưa cấu hình mail (thiếu smtp.host).\n"
                            + "Hãy cấu hình 1 trong các vị trí sau:\n"
                            + "- mail.properties (cùng thư mục chạy)\n"
                            + "- config/mail.properties\n"
                            + "- resources/mail.properties\n"
                            + "Hoặc set System properties/env vars (smtp.host, smtp.port, smtp.username, smtp.password, ...)."
            );
        }
        if (cfg.username() == null || cfg.username().isBlank() || cfg.password() == null) {
            throw new IllegalStateException("Thiếu smtp.username hoặc smtp.password trong mail.properties");
        }

        String from = (cfg.from() == null || cfg.from().isBlank()) ? cfg.username() : cfg.from();
        String rcpt = to.trim();

        try {
            SmtpConn conn = connect(cfg);
            try {
                conn.expect(220);
                conn.send("EHLO phonestore");
                conn.readMulti();

                if (!cfg.ssl() && cfg.startTls()) {
                    conn.send("STARTTLS");
                    conn.expect(220);
                    conn.upgradeToTls(cfg.host());
                    conn.send("EHLO phonestore");
                    conn.readMulti();
                }

                authLogin(conn, cfg.username(), cfg.password());

                conn.send("MAIL FROM:<" + from + ">");
                conn.expect2xx3xx();

                conn.send("RCPT TO:<" + rcpt + ">");
                conn.expect2xx3xx();

                conn.send("DATA");
                conn.expect(354);

                String msg = buildMessage(from, cfg.fromName(), rcpt, subject, body, "text/plain; charset=UTF-8");
                conn.writeRaw(msg);
                conn.writeRaw("\r\n.\r\n");
                conn.expect2xx3xx();

                conn.send("QUIT");
            } finally {
                conn.closeQuiet();
            }
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException("Gửi mail thất bại: " + e.getMessage(), e);
        }
    }

    public static void sendHtml(String to, String subject, String htmlBody) {
        if (to == null || to.trim().isEmpty()) throw new IllegalArgumentException("Email nhận không hợp lệ");
        MailConfig.Smtp cfg = MailConfig.load();
        if (cfg == null) {
            throw new IllegalStateException(
                    "Chưa cấu hình mail (thiếu smtp.host).\n"
                            + "Hãy cấu hình 1 trong các vị trí sau:\n"
                            + "- mail.properties (cùng thư mục chạy)\n"
                            + "- config/mail.properties\n"
                            + "- resources/mail.properties\n"
                            + "Hoặc set System properties/env vars (smtp.host, smtp.port, smtp.username, smtp.password, ...)."
            );
        }
        if (cfg.username() == null || cfg.username().isBlank() || cfg.password() == null) {
            throw new IllegalStateException("Thiếu smtp.username hoặc smtp.password trong mail.properties");
        }

        String from = (cfg.from() == null || cfg.from().isBlank()) ? cfg.username() : cfg.from();
        String rcpt = to.trim();
        String body = htmlBody == null ? "" : htmlBody;

        try {
            SmtpConn conn = connect(cfg);
            try {
                conn.expect(220);
                conn.send("EHLO phonestore");
                conn.readMulti();

                if (!cfg.ssl() && cfg.startTls()) {
                    conn.send("STARTTLS");
                    conn.expect(220);
                    conn.upgradeToTls(cfg.host());
                    conn.send("EHLO phonestore");
                    conn.readMulti();
                }

                authLogin(conn, cfg.username(), cfg.password());

                conn.send("MAIL FROM:<" + from + ">");
                conn.expect2xx3xx();

                conn.send("RCPT TO:<" + rcpt + ">");
                conn.expect2xx3xx();

                conn.send("DATA");
                conn.expect(354);

                String msg = buildMessage(from, cfg.fromName(), rcpt, subject, body, "text/html; charset=UTF-8");
                conn.writeRaw(msg);
                conn.writeRaw("\r\n.\r\n");
                conn.expect2xx3xx();

                conn.send("QUIT");
            } finally {
                conn.closeQuiet();
            }
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException("Gửi mail thất bại: " + e.getMessage(), e);
        }
    }

    private static String buildMessage(String from, String fromName, String to, String subject, String body, String contentType) {
        String safeSubject = encodeHeaderUtf8(subject == null ? "" : subject);
        String safeBody = body == null ? "" : body;

        String fromHeader = buildFromHeader(from, fromName);
        String dateHeader = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now());
        String messageId = buildMessageId(from);
        String messageBody = dotStuff(normalizeNewlines(safeBody));
        String ct = (contentType == null || contentType.isBlank()) ? "text/plain; charset=UTF-8" : contentType.trim();

        return "From: " + fromHeader + "\r\n"
                + "To: <" + to + ">\r\n"
                + "Subject: " + safeSubject + "\r\n"
            + "Date: " + dateHeader + "\r\n"
            + "Message-ID: " + messageId + "\r\n"
            + "X-Mailer: PhoneStoreManagement\r\n"
            + "Content-Language: vi\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: " + ct + "\r\n"
                + "Content-Transfer-Encoding: 8bit\r\n"
                + "\r\n"
                + messageBody
                + "\r\n";
    }

    private static String normalizeNewlines(String s) {
        // Ensure CRLF for SMTP DATA
        String t = s.replace("\r\n", "\n");
        t = t.replace("\r", "\n");
        return t.replace("\n", "\r\n");
    }

    private static String dotStuff(String s) {
        // RFC 5321: lines beginning with '.' must be doubled
        if (s == null || s.isEmpty()) return "";
        String t = s;
        if (t.startsWith(".")) t = "." + t;
        return t.replace("\r\n.", "\r\n..");
    }

    private static String buildFromHeader(String from, String fromName) {
        String email = (from == null) ? "" : from.trim();
        String name = (fromName == null) ? "" : fromName.trim();
        if (name.isEmpty()) return "<" + email + ">";

        // Use RFC 2047 encoding for display name (supports Vietnamese)
        String encodedName = encodeHeaderUtf8(name);
        // Do not wrap encoded-words in quotes.
        return encodedName + " <" + email + ">";
    }

    private static String buildMessageId(String from) {
        String email = (from == null) ? "" : from.trim();
        int at = email.indexOf('@');
        String domain = (at > 0 && at < email.length() - 1) ? email.substring(at + 1) : "localhost";
        return "<" + UUID.randomUUID() + "@" + domain + ">";
    }

    private static String encodeHeaderUtf8(String value) {
        if (value == null) return "";
        String v = value.trim();
        if (v.isEmpty()) return "";
        boolean needsEncoding = false;
        for (int i = 0; i < v.length(); i++) {
            char ch = v.charAt(i);
            if (ch > 127) {
                needsEncoding = true;
                break;
            }
        }
        if (!needsEncoding) return v;
        byte[] bytes = v.getBytes(StandardCharsets.UTF_8);
        String b64 = Base64.getEncoder().encodeToString(bytes);
        return "=?UTF-8?B?" + b64 + "?=";
    }

    private static void authLogin(SmtpConn conn, String username, String password) throws Exception {
        conn.send("AUTH LOGIN");
        int code = conn.readCode();
        if (code != 334) {
            throw new IllegalStateException("SMTP không hỗ trợ AUTH LOGIN (code=" + code + ")");
        }
        conn.send(base64(username));
        conn.expect(334);
        conn.send(base64(password));
        conn.expect2xx3xx();
    }

    private static String base64(String s) {
        byte[] b = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(b);
    }

    private static SmtpConn connect(MailConfig.Smtp cfg) throws Exception {
        if (cfg.ssl()) {
            SSLSocketFactory f = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket s = (SSLSocket) f.createSocket(cfg.host(), cfg.port());
            s.startHandshake();
            return new SmtpConn(s);
        }
        Socket s = new Socket(cfg.host(), cfg.port());
        return new SmtpConn(s);
    }

    private static final class SmtpConn {
        private Socket socket;
        private BufferedReader in;
        private BufferedWriter out;

        private SmtpConn(Socket socket) throws Exception {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        }

        void send(String line) throws Exception {
            writeRaw(line + "\r\n");
        }

        void writeRaw(String s) throws Exception {
            out.write(s);
            out.flush();
        }

        void expect(int expectedCode) throws Exception {
            int code = readCode();
            if (code != expectedCode) {
                throw new IllegalStateException("SMTP lỗi (expected=" + expectedCode + ", got=" + code + ")");
            }
        }

        void expect2xx3xx() throws Exception {
            int code = readCode();
            if (code < 200 || code >= 400) {
                throw new IllegalStateException("SMTP lỗi (code=" + code + ")");
            }
        }

        int readCode() throws Exception {
            String line = in.readLine();
            if (line == null) throw new IllegalStateException("SMTP connection closed");
            if (line.length() < 3) throw new IllegalStateException("SMTP response invalid: " + line);
            int code;
            try {
                code = Integer.parseInt(line.substring(0, 3));
            } catch (Exception e) {
                throw new IllegalStateException("SMTP response invalid: " + line);
            }

            // multiline responses: 250-... until 250 <space>
            if (line.length() > 3 && line.charAt(3) == '-') {
                readMulti();
            }
            return code;
        }

        void readMulti() throws Exception {
            while (true) {
                String line = in.readLine();
                if (line == null) return;
                if (line.length() < 4) continue;
                if (line.charAt(3) == ' ') return;
            }
        }

        void upgradeToTls(String host) throws Exception {
            SSLSocketFactory f = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket ssl = (SSLSocket) f.createSocket(socket, host, socket.getPort(), true);
            ssl.startHandshake();
            this.socket = ssl;
            this.in = new BufferedReader(new InputStreamReader(ssl.getInputStream(), StandardCharsets.UTF_8));
            this.out = new BufferedWriter(new OutputStreamWriter(ssl.getOutputStream(), StandardCharsets.UTF_8));
        }

        void closeQuiet() {
            try {
                if (socket != null) socket.close();
            } catch (Exception ignored) {
            }
        }
    }
}
