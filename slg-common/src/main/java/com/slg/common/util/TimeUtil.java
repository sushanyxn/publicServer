package com.slg.common.util;

import com.slg.common.log.LoggerUtil;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 时间工具类
 * 提供时间格式化、解析、计算等常用方法
 * 基于 Java 8+ 时间 API 实现
 * 
 * @author yangxunan
 * @date 2025-12-23
 */
public class TimeUtil {

    public static final long SECOND = 1000;
    public static final long MINUTE = 60 * SECOND;
    public static final long HOUR = 60 * MINUTE;
    public static final long DAY = 24 * HOUR;

    /**
     * 默认日期时间格式：yyyy-MM-dd HH:mm:ss
     */
    public static final String DEFAULT_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 默认日期格式：yyyy-MM-dd
     */
    public static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 默认时间格式：HH:mm:ss
     */
    public static final String DEFAULT_TIME_PATTERN = "HH:mm:ss";

    private static final DateTimeFormatter DEFAULT_DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATETIME_PATTERN);
    private static final DateTimeFormatter DEFAULT_DATE_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATE_PATTERN);

    /**
     * 获取当前时间戳（秒）
     *
     * @return 当前时间戳（秒）
     */
    public static long currentSeconds() {
        return Instant.now().getEpochSecond();
    }

    /**
     * 获取当前时间戳（毫秒）
     *
     * @return 当前时间戳（毫秒）
     */
    public static long now() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前日期时间字符串（格式：yyyy-MM-dd HH:mm:ss）
     *
     * @return 当前日期时间字符串
     */
    public static String currentDateTime() {
        return LocalDateTime.now().format(DEFAULT_DATETIME_FORMATTER);
    }

    /**
     * 获取当前日期字符串（格式：yyyy-MM-dd）
     *
     * @return 当前日期字符串
     */
    public static String currentDate() {
        return LocalDate.now().format(DEFAULT_DATE_FORMATTER);
    }

    /**
     * 将 LocalDateTime 格式化为字符串（默认格式：yyyy-MM-dd HH:mm:ss）
     *
     * @param dateTime 日期时间对象
     * @return 格式化后的字符串
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DEFAULT_DATETIME_FORMATTER);
    }

    /**
     * 将 LocalDateTime 格式化为字符串（自定义格式）
     *
     * @param dateTime 日期时间对象
     * @param pattern 格式模式
     * @return 格式化后的字符串
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null || pattern == null || pattern.isEmpty()) {
            return null;
        }
        try {
            return dateTime.format(DateTimeFormatter.ofPattern(pattern));
        } catch (Exception e) {
            LoggerUtil.error("日期格式化失败: pattern={}", pattern, e);
            return null;
        }
    }

    /**
     * 将 LocalDate 格式化为字符串（默认格式：yyyy-MM-dd）
     *
     * @param date 日期对象
     * @return 格式化后的字符串
     */
    public static String format(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DEFAULT_DATE_FORMATTER);
    }

    /**
     * 解析日期时间字符串为 LocalDateTime（默认格式：yyyy-MM-dd HH:mm:ss）
     *
     * @param dateTimeStr 日期时间字符串
     * @return LocalDateTime 对象，解析失败返回 null
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DEFAULT_DATETIME_FORMATTER);
        } catch (Exception e) {
            LoggerUtil.error("日期时间解析失败: {}", dateTimeStr, e);
            return null;
        }
    }

    /**
     * 解析日期时间字符串为 LocalDateTime（自定义格式）
     *
     * @param dateTimeStr 日期时间字符串
     * @param pattern 格式模式
     * @return LocalDateTime 对象，解析失败返回 null
     */
    public static LocalDateTime parseDateTime(String dateTimeStr, String pattern) {
        if (dateTimeStr == null || dateTimeStr.isEmpty() || pattern == null || pattern.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern));
        } catch (Exception e) {
            LoggerUtil.error("日期时间解析失败: dateTimeStr={}, pattern={}", dateTimeStr, pattern, e);
            return null;
        }
    }

    /**
     * 解析日期字符串为 LocalDate（默认格式：yyyy-MM-dd）
     *
     * @param dateStr 日期字符串
     * @return LocalDate 对象，解析失败返回 null
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DEFAULT_DATE_FORMATTER);
        } catch (Exception e) {
            LoggerUtil.error("日期解析失败: {}", dateStr, e);
            return null;
        }
    }

    /**
     * 时间戳（秒）转 LocalDateTime
     *
     * @param seconds 时间戳（秒）
     * @return LocalDateTime 对象
     */
    public static LocalDateTime fromSeconds(long seconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZoneId.systemDefault());
    }

    /**
     * 时间戳（毫秒）转 LocalDateTime
     *
     * @param millis 时间戳（毫秒）
     * @return LocalDateTime 对象
     */
    public static LocalDateTime fromMillis(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }

    /**
     * LocalDateTime 转时间戳（秒）
     *
     * @param dateTime 日期时间对象
     * @return 时间戳（秒）
     */
    public static long toSeconds(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    /**
     * LocalDateTime 转时间戳（毫秒）
     *
     * @param dateTime 日期时间对象
     * @return 时间戳（毫秒）
     */
    public static long toMillis(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 计算两个时间之间的天数差
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 天数差
     */
    public static long daysBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 计算两个时间之间的小时差
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 小时差
     */
    public static long hoursBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(start, end);
    }

    /**
     * 计算两个时间之间的分钟差
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 分钟差
     */
    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(start, end);
    }

    /**
     * 计算两个时间之间的秒差
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 秒差
     */
    public static long secondsBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.SECONDS.between(start, end);
    }

    /**
     * 增加天数
     *
     * @param dateTime 原日期时间
     * @param days 要增加的天数（可为负数）
     * @return 新的日期时间
     */
    public static LocalDateTime plusDays(LocalDateTime dateTime, long days) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusDays(days);
    }

    /**
     * 增加小时
     *
     * @param dateTime 原日期时间
     * @param hours 要增加的小时数（可为负数）
     * @return 新的日期时间
     */
    public static LocalDateTime plusHours(LocalDateTime dateTime, long hours) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusHours(hours);
    }

    /**
     * 增加分钟
     *
     * @param dateTime 原日期时间
     * @param minutes 要增加的分钟数（可为负数）
     * @return 新的日期时间
     */
    public static LocalDateTime plusMinutes(LocalDateTime dateTime, long minutes) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusMinutes(minutes);
    }

    /**
     * 判断是否是今天
     *
     * @param dateTime 日期时间
     * @return 是否是今天
     */
    public static boolean isToday(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return dateTime.toLocalDate().equals(LocalDate.now());
    }

    /**
     * 判断时间是否过期（相对于当前时间）
     *
     * @param dateTime 日期时间
     * @return 是否过期
     */
    public static boolean isExpired(LocalDateTime dateTime) {
        if (dateTime == null) {
            return true;
        }
        return dateTime.isBefore(LocalDateTime.now());
    }
}

