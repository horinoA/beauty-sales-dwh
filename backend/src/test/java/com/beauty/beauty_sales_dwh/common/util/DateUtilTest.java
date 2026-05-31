package com.beauty.beauty_sales_dwh.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilTest {

    private static final ZoneOffset JST = ZoneOffset.ofHours(9);

    @Test
    @DisplayName("startOfMonth: yyyy-MM 形式の文字列から月の開始日時を取得できること")
    void testStartOfMonth() {
        OffsetDateTime result = DateUtil.startOfMonth("2024-02");
        assertNotNull(result);
        assertEquals(2024, result.getYear());
        assertEquals(2, result.getMonthValue());
        assertEquals(1, result.getDayOfMonth());
        assertEquals(LocalTime.MIN, result.toLocalTime());
        assertEquals(JST, result.getOffset());
    }

    @Test
    @DisplayName("endOfMonth: yyyy-MM 形式の文字列から月の終了日時を取得できること")
    void testEndOfMonth() {
        // うるう年の2月でテスト
        OffsetDateTime result = DateUtil.endOfMonth("2024-02");
        assertNotNull(result);
        assertEquals(2024, result.getYear());
        assertEquals(2, result.getMonthValue());
        assertEquals(29, result.getDayOfMonth());
        assertEquals(LocalTime.MAX, result.toLocalTime());
        assertEquals(JST, result.getOffset());
    }

    @Test
    @DisplayName("atStartOfDay: LocalDate からその日の開始日時を取得できること")
    void testAtStartOfDay() {
        LocalDate date = LocalDate.of(2024, 5, 18);
        OffsetDateTime result = DateUtil.atStartOfDay(date);
        assertNotNull(result);
        assertEquals(date, result.toLocalDate());
        assertEquals(LocalTime.MIN, result.toLocalTime());
        assertEquals(JST, result.getOffset());
    }

    @Test
    @DisplayName("atEndOfDay: LocalDate からその日の終了日時を取得できること")
    void testAtEndOfDay() {
        LocalDate date = LocalDate.of(2024, 5, 18);
        OffsetDateTime result = DateUtil.atEndOfDay(date);
        assertNotNull(result);
        assertEquals(date, result.toLocalDate());
        assertEquals(LocalTime.MAX, result.toLocalTime());
        assertEquals(JST, result.getOffset());
    }

    @Test
    @DisplayName("yyyy-mm形式のnヶ月前の開始日データが取得できること")
    void ReduceMonthAtStartOfDayTest(){
        OffsetDateTime result = DateUtil.ReduceMonthAtStartOfDay("2025-02", 11L);
        assertNotNull(result);
        assertEquals(2024, result.getYear());
        assertEquals(3, result.getMonthValue());
        assertEquals(1, result.getDayOfMonth());
        assertEquals(LocalTime.MIN, result.toLocalTime());
        assertEquals(JST, result.getOffset());
    }

    @Test
    @DisplayName("yyyy-mm形式のnヶ月後の終了日データが取得できること")
    void AddMonthAtEndOfDayTest(){
        // うるう年の2月でテスト
        OffsetDateTime result = DateUtil.AddMonthAtEndOfDay("2024-03", 11L);
        assertNotNull(result);
        assertEquals(2025, result.getYear());
        assertEquals(2, result.getMonthValue());
        assertEquals(28, result.getDayOfMonth());
        assertEquals(LocalTime.MAX, result.toLocalTime());
        assertEquals(JST, result.getOffset());
    }
}
