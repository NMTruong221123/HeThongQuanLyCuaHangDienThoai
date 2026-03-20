package com.phonestore.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordHasher {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {}

    // Format: pbkdf2$<iterations>$<saltB64>$<hashB64>
    public static String hash(String password) {
        try {
            int iterations = 120_000;
            byte[] salt = new byte[16];
            RANDOM.nextBytes(salt);
            byte[] hash = pbkdf2(password.toCharArray(), salt, iterations, 32);
            return "pbkdf2$" + iterations + "$" + b64(salt) + "$" + b64(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot hash password", e);
        }
    }

    public static boolean verify(String password, String encoded) {
        if (password == null || encoded == null || encoded.isBlank()) {
            return false;
        }
        try {
            String[] parts = encoded.split("\\$");
            if (parts.length != 4) {
                return false;
            }
            if (!"pbkdf2".equals(parts[0])) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(password.toCharArray(), salt, iterations, expected.length);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLenBytes) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLenBytes * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }

    private static String b64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
