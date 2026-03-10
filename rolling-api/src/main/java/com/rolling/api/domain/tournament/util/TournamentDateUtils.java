package com.rolling.api.domain.tournament.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TournamentDateUtils {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Pattern KOREAN_DATE_PATTERN =
            Pattern.compile("^(\\d{4})\\s*년\\s*(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일$");

    public static LocalDate parse(String raw) {
        if (raw == null) {
            return null;
        }

        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(value, ISO_DATE);
        } catch (DateTimeParseException ignored) {
            // continue
        }

        Matcher matcher = KOREAN_DATE_PATTERN.matcher(value);
        if (!matcher.matches()) {
            return null;
        }

        int year = Integer.parseInt(matcher.group(1));
        int month = Integer.parseInt(matcher.group(2));
        int day = Integer.parseInt(matcher.group(3));
        try {
            return LocalDate.of(year, month, day);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static String toIsoString(LocalDate date) {
        return date == null ? null : date.format(ISO_DATE);
    }

    public static String normalizeToIso(String raw) {
        LocalDate parsed = parse(raw);
        return toIsoString(parsed);
    }
}
