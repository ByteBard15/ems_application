package com.bytebard.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class DateUtils {
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    public static LocalDateTime now() {
        return LocalDateTime.now(DEFAULT_ZONE);
    }

    public static Date nowAsDate() {
        return Date.from(now().atZone(DEFAULT_ZONE).toInstant());
    }

    public static LocalDateTime parse(String date, String format) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            return LocalDate.parse(date, formatter).atStartOfDay(DEFAULT_ZONE).toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    public static String format(LocalDateTime date, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return date.format(formatter);
    }

    public static long dateInSeconds(LocalDateTime date) {
        Instant instant = date.atZone(DEFAULT_ZONE).toInstant();
        return instant.getEpochSecond();
    }

    public static boolean valid(String date, String format) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            LocalDate.parse(date, formatter);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static long durationSince(LocalDateTime ref, LocalDateTime date, ChronoUnit unit) {
        return unit.between(ref, date);
    }

    public static String nowInISOString() {
        return Instant.now().toString();
    }

    public static Date toDate(String isoString) {
        return Date.from(Instant.parse(isoString));
    }

    public static Date toDate(LocalDateTime date) {
        return Date.from(date.atZone(DEFAULT_ZONE).toInstant());
    }

    public static String toISOString(Date date) {
        return date.toInstant().toString();
    }

    public static long nowInUnix() {
        return Instant.now().getEpochSecond();
    }

    public static LocalDateTime add(LocalDateTime date, long amount, ChronoUnit unit) {
        return date.plus(amount, unit);
    }

    public static LocalDateTime subtract(LocalDateTime date, long amount, ChronoUnit unit) {
        return date.minus(amount, unit);
    }

    public static LocalDateTime yesterday() {
        return subtract(now(), 1, ChronoUnit.DAYS);
    }

    public static LocalDateTime tomorrow() {
        return add(now(), 1, ChronoUnit.DAYS);
    }

    public static boolean isToday(LocalDateTime date) {
        return date.toLocalDate().isEqual(LocalDate.now(DEFAULT_ZONE));
    }

    public static boolean isAfter(LocalDateTime ref, LocalDateTime input, ChronoUnit unit) {
        return ref.truncatedTo(unit).isAfter(input.truncatedTo(unit));
    }

    public static boolean equals(LocalDateTime date1, LocalDateTime date2, ChronoUnit unit) {
        return date1.truncatedTo(unit).isEqual(date2.truncatedTo(unit));
    }

    public static boolean isEqualOrAfter(LocalDateTime ref, LocalDateTime input, ChronoUnit unit) {
        return isAfter(ref, input, unit) || equals(ref, input, unit);
    }

    public static double convert(long duration, ChronoUnit input, ChronoUnit output) {
        Duration d = Duration.of(duration, input);
        return (double) d.toNanos() / Duration.of(1, output).toNanos();
    }

    public static String getFullYear(LocalDateTime date) {
        return String.valueOf(date.getYear());
    }

    public static String[] formatDatesToYMD(LocalDateTime start, LocalDateTime end) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return new String[]{start.format(fmt), end.format(fmt)};
    }

    public static LocalDateTime getStartOfWeek(LocalDateTime date) {
        LocalDate d = date.toLocalDate();
        DayOfWeek dow = d.getDayOfWeek();
        LocalDate monday = d.minusDays((dow == DayOfWeek.SUNDAY ? 6 : dow.getValue() - 1));
        return monday.atStartOfDay();
    }

    public static LocalDateTime normalizeToMidday(LocalDateTime date) {
        return date.withHour(12).withMinute(0).withSecond(0).withNano(0);
    }
}
