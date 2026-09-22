package com.M198.Majorproject.common.util;

public final class StringUtils {

    private StringUtils() {
    }

    public static boolean hasText(String str) {
        return str != null && !str.isBlank();
    }

    public static String trimOrNull(String str) {
        if (str == null) return null;
        String trimmed = str.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String normalizeHandle(String rawHandle) {
        if (rawHandle == null) return null;
        String handle = rawHandle.trim();
        if (handle.startsWith("@")) {
            handle = handle.substring(1).trim();
        }
        return handle.toLowerCase();
    }
}
