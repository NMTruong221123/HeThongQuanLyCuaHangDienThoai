package com.phonestore.util.mail;

/**
 * CLI smoke test for SMTP configuration.
 *
 * Usage:
 *   java -cp "out-verify;lib/*;resources" com.phonestore.util.mail.MailSmokeTest to@example.com
 *
 * It loads SMTP config from the same places as the app (see {@link MailConfig}).
 */
public final class MailSmokeTest {

    private MailSmokeTest() {}

    public static void main(String[] args) {
        if (args == null || args.length < 1 || args[0] == null || args[0].trim().isEmpty()) {
            System.out.println("Usage: MailSmokeTest <toEmail>");
            System.exit(2);
            return;
        }

        String to = args[0].trim();
        String subject = "[PhoneStore] Test gửi email";
        String body = "Đây là email test cấu hình SMTP của PhoneStore.\n\nNếu bạn nhận được email này nghĩa là cấu hình SMTP đang hoạt động.";

        try {
            SmtpMailer.sendText(to, subject, body);
            System.out.println("OK: đã gửi email test tới " + to);
        } catch (Exception e) {
            System.err.println("FAILED: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
