package com.phonestore.util;

import java.util.Locale;

public final class TextSearch {

    private TextSearch() {}

    // Returns true if every token in query is contained (case-insensitive) in the combined fields.
    public static boolean matches(String query, String... fields) {
        if (query == null) return true;
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isBlank()) return true;
        StringBuilder sb = new StringBuilder();
        if (fields != null) {
            for (String f : fields) {
                if (f == null) continue;
                sb.append(f).append(' ');
            }
        }
        String hay = sb.toString().toLowerCase(Locale.ROOT);
        // split on whitespace
        String[] toks = q.split("\\s+");
        for (String t : toks) {
            if (t.isBlank()) continue;
            if (!hay.contains(t)) return false;
        }
        return true;
    }
}
