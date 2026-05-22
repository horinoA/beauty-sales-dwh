package com.beauty.beauty_sales_dwh.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * 日付・時刻操作に関する共通ユーティリティクラス。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateUtil {

    private static final ZoneOffset JST_OFFSET = ZoneOffset.ofHours(9);
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * "yyyy-MM" 形式の文字列を、その月の開始時点 (1日 00:00:00) の OffsetDateTime に変換します。
     *
     * @param yearMonthStr "yyyy-MM" 形式の文字列
     * @return 指定された月の開始日時の OffsetDateTime (JST)
     */
    public static OffsetDateTime startOfMonth(String yearMonthStr) {
        if (yearMonthStr == null) return null;
        YearMonth ym = YearMonth.parse(yearMonthStr, YEAR_MONTH_FORMATTER);
        return ym.atDay(1).atStartOfDay().atOffset(JST_OFFSET);
    }

    /**
     * "yyyy-MM" 形式の文字列を、その月の終了時点 (末日 23:59:59.999...) の OffsetDateTime に変換します。
     *
     * @param yearMonthStr "yyyy-MM" 形式の文字列
     * @return 指定された月の終了日時の OffsetDateTime (JST)
     */
    public static OffsetDateTime endOfMonth(String yearMonthStr) {
        if (yearMonthStr == null) return null;
        YearMonth ym = YearMonth.parse(yearMonthStr, YEAR_MONTH_FORMATTER);
        LocalDate lastDay = ym.atEndOfMonth();
        return OffsetDateTime.of(lastDay, LocalTime.MAX, JST_OFFSET);
    }
    
    /**
     * LocalDate を JST の開始時点の OffsetDateTime に変換します。
     */
    public static OffsetDateTime atStartOfDay(LocalDate date) {
        if (date == null) return null;
        return date.atStartOfDay().atOffset(JST_OFFSET);
    }

    /**
     * LocalDate を JST の終了時点の OffsetDateTime に変換します。
     */
    public static OffsetDateTime atEndOfDay(LocalDate date) {
        if (date == null) return null;
        return OffsetDateTime.of(date, LocalTime.MAX, JST_OFFSET);
    }
}
