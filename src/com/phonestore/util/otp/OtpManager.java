package com.phonestore.util.otp;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class OtpManager {

    private static final SecureRandom rnd = new SecureRandom();
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    private static final class Entry {
        private final String otp;
        private final Instant expiresAt;

        private Entry(String otp, Instant expiresAt) {
            this.otp = otp;
            this.expiresAt = expiresAt;
        }
    }

    private static final Map<String, Entry> store = new ConcurrentHashMap<>();

    private OtpManager() {}

    public static String issue(String key) {
        return issue(key, DEFAULT_TTL);
    }

    public static String issue(String key, Duration ttl) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Key không hợp lệ");
        Duration t = (ttl == null || ttl.isNegative() || ttl.isZero()) ? DEFAULT_TTL : ttl;
        String otp = String.format("%06d", rnd.nextInt(1_000_000));
        store.put(key.trim(), new Entry(otp, Instant.now().plus(t)));
        return otp;
    }

    public static boolean verify(String key, String otp) {
        if (key == null || key.isBlank()) return false;
        if (otp == null || otp.isBlank()) return false;
        Entry e = store.get(key.trim());
        if (e == null) return false;
        if (Instant.now().isAfter(e.expiresAt)) {
            store.remove(key.trim());
            return false;
        }
        boolean ok = e.otp.equals(otp.trim());
        if (ok) {
            store.remove(key.trim());
        }
        return ok;
    }

    public static void clear(String key) {
        if (key == null || key.isBlank()) return;
        store.remove(key.trim());
    }
}
