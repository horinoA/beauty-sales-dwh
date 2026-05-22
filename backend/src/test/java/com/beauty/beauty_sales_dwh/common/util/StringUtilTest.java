package com.beauty.beauty_sales_dwh.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "\t", "\n"})
    @DisplayName("isNullOrBlank: null, 空文字, 空白文字の場合にtrueを返すこと")
    void isNullOrBlank_ShouldReturnTrueForBlankStrings(String input) {
        assertTrue(StringUtil.isNullOrBlank(input));
    }

    @Test
    @DisplayName("isNullOrBlank: 中身がある場合にfalseを返すこと")
    void isNullOrBlank_ShouldReturnFalseForNonBlankStrings() {
        assertFalse(StringUtil.isNullOrBlank("a"));
        assertFalse(StringUtil.isNullOrBlank(" word "));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    @DisplayName("hasContent: 空白の場合にfalseを返すこと")
    void hasContent_ShouldReturnFalseForBlankStrings(String input) {
        assertFalse(StringUtil.hasContent(input));
    }

    @Test
    @DisplayName("hasContent: 中身がある場合にtrueを返すこと")
    void hasContent_ShouldReturnTrueForNonBlankStrings() {
        assertTrue(StringUtil.hasContent("a"));
    }
}
