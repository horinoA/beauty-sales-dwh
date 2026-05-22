package com.beauty.beauty_sales_dwh.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 文字列操作に関するユーティリティクラス。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StringUtil {

    /**
     * 文字列が null または空文字、あるいは空白のみであるか判定します。
     *
     * @param str 判定対象の文字列
     * @return null, 空文字, または空白のみの場合は true
     */
    public static boolean isNullOrBlank(String str) {
        return str == null || str.isBlank();
    }

    /**
     * 文字列が null でなく、かつ空白以外の商品（中身）があるか判定します。
     *
     * @param str 判定対象の文字列
     * @return 中身がある場合は true
     */
    public static boolean hasContent(String str) {
        return !isNullOrBlank(str);
    }
}
