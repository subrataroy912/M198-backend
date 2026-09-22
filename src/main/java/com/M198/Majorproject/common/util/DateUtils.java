package com.M198.Majorproject.common.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    public static final ZoneId UTC_ZONE = ZoneId.of("UTC");

    private DateUtils() {
    }

    public static String formatIso(Instant instant) {
        return instant != null ? ISO_FORMATTER.format(instant) : null;
    }

    public static Instant parseIso(String isoString) {
        return (isoString != null && !isoString.isBlank()) ? Instant.parse(isoString.trim()) : null;
    }
}
