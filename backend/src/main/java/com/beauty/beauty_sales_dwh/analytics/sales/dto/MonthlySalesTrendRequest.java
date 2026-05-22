package com.beauty.beauty_sales_dwh.analytics.sales.dto;

import java.util.stream.Stream;

import com.beauty.beauty_sales_dwh.common.exception.InvalidFormatException;
import com.beauty.beauty_sales_dwh.common.exception.RequiredParameterMissingException;
import com.beauty.beauty_sales_dwh.common.util.DateUtil;
import com.beauty.beauty_sales_dwh.common.util.StringUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

public record MonthlySalesTrendRequest(
    
    String transaction_month_from,
    String transaction_month_to

) {
    public MonthlySalesTrendRequest{
        OffsetDateTime fromDate = null;
        OffsetDateTime toDate = null;

        //全てのフィールドがnull
        if (Stream.of(transaction_month_from,transaction_month_to)
            .allMatch(field -> field == null || field.isBlank())){
                throw new RequiredParameterMissingException("error.parameter.required", new Object[]{"transaction_month_from/to"});
        }

        try {
            //transaction_month_from 入力値チェック
            if (StringUtil.hasContent(transaction_month_from)){
                if (!isValidYYYYMM(transaction_month_from)){
                    throw new InvalidFormatException("error.parameter.invalidformat",new Object[]{"transaction_month_from",transaction_month_from});
                }
                fromDate = DateUtil.startOfMonth(transaction_month_from);
            }
            //transaction_month_to 入力値チェック
            if (StringUtil.hasContent(transaction_month_to)){
                if (!isValidYYYYMM(transaction_month_to)){
                    throw new InvalidFormatException("error.parameter.invalidformat",new Object[]{"transaction_month_to",transaction_month_to});
                }
                toDate = DateUtil.endOfMonth(transaction_month_to);
            }
            // 期間の前後関係チェック
            if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
                //fromDate > toDate
                throw new InvalidFormatException("error.parameter.invalidrange", new Object[]{transaction_month_from, transaction_month_to});
            } else if (fromDate == null && toDate != null){
                //fromDateがnullの場合はtoDate12ヶ月前を指定
                fromDate = OffsetDateTime.of((toDate.minusMonths(12)).toLocalDate(),LocalTime.MIN,ZoneOffset.ofHours(9));
            } else if (fromDate != null && toDate == null){
                //toDateがnullの場合はfromDate12ヶ月後を指定
                toDate = OffsetDateTime.of((fromDate.plusMonths(12)).toLocalDate(),LocalTime.MAX,ZoneOffset.ofHours(9));
                if (YearMonth.from(toDate).isAfter(YearMonth.now())){
                    //計算結果が現在月より大きい場合は現在月とする
                    toDate = OffsetDateTime.of(LocalDate.now(),LocalTime.MAX,ZoneOffset.ofHours(9));
                }
            }
        } catch (DateTimeParseException e) {
            // 指定したstringが日付型ではない（2026-13)のように
            // パースに失敗した場合は一律で invalidformat を投げる
                String errorField = (transaction_month_from != null && 
                                    transaction_month_from.equals(e.getParsedString())) ? "transaction_month_from" :"transaction_month_to";
                throw new InvalidFormatException("error.parameter.invalidformat", new Object[]{errorField, e.getParsedString()});
        }
    }

    //YYYY-MM形式か
    private boolean isValidYYYYMM(String param){
        String regex = "^\\d{4}-(0[1-9]|1[0-2])$";
        return param.matches(regex);
    }
 
}
