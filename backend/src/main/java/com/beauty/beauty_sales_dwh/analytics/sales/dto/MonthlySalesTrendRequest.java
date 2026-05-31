package com.beauty.beauty_sales_dwh.analytics.sales.dto;

import java.util.stream.Stream;

import com.beauty.beauty_sales_dwh.common.exception.DateRangeSettings;
import com.beauty.beauty_sales_dwh.common.exception.InvalidFormatException;
import com.beauty.beauty_sales_dwh.common.exception.RequiredParameterMissingException;
import com.beauty.beauty_sales_dwh.common.util.DateUtil;
import com.beauty.beauty_sales_dwh.common.util.StringUtil;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

public record MonthlySalesTrendRequest(
    String transaction_month_from,
    String transaction_month_to
) {

    public MonthlySalesTrendRequest{
        //全てのフィールドがnull
        if (Stream.of(transaction_month_from,transaction_month_to)
            .allMatch(field -> field == null || field.isBlank())){
                throw new RequiredParameterMissingException("error.parameter.required", new Object[]{"transaction_month_from/to"});
        }
        //transaction_month_from 入力値チェック
        if (StringUtil.hasContent(transaction_month_from)){
            if (!isValidYYYYMM(transaction_month_from)){
                throw new InvalidFormatException("error.parameter.invalidformat",new Object[]{"transaction_month_from",transaction_month_from});
            }
        }
        //transaction_month_to 入力値チェック
        if (StringUtil.hasContent(transaction_month_to)){
            if (!isValidYYYYMM(transaction_month_to)){
                throw new InvalidFormatException("error.parameter.invalidformat",new Object[]{"transaction_month_to",transaction_month_to});
            }
        }
    }

    public OffsetDateTime getFromDate() {
        OffsetDateTime fromDay = null;
        try {
            if (StringUtil.hasContent(transaction_month_from)){
                fromDay = DateUtil.startOfMonth(transaction_month_from);
                if (fromDay.toLocalDate().isAfter(LocalDate.now())){
                    throw new DateRangeSettings("error.parameter.daterangesettings",new Object[]{"transaction_month_from",transaction_month_from});
                }else if (fromDay.isAfter(DateUtil.endOfMonth(transaction_month_to))){
                    throw new DateRangeSettings("error.parameter.daterangesettings",new Object[]{"transaction_month_from",transaction_month_from});
                }
            }else{
                //空白の場合transaction_month_toより11ヶ月前のデータを返す
                final Long NUMBER_MONTH_DISPLAY = 11L;
                fromDay = DateUtil.ReduceMonthAtStartOfDay(transaction_month_to, NUMBER_MONTH_DISPLAY);
                if (fromDay.toLocalDate().isAfter(LocalDate.now())){
                    //toDateより１１ヶ月引いた1年月日が本日より大きい場合はtransaction_month_toで指定した年月が大きすぎる
                    throw new DateRangeSettings("error.parameter.daterangesettings",new Object[]{"transaction_month_to",transaction_month_to});
                }
            }
        } catch (DateTimeParseException e) {
            String errorField = (transaction_month_from != null && 
                                transaction_month_from.equals(e.getParsedString())) ? "transaction_month_from" :"transaction_month_to";
            throw new InvalidFormatException("error.parameter.invalidformat", new Object[]{errorField, e.getParsedString()});
        }
        return fromDay;
    }

    public OffsetDateTime getToDate(){
        OffsetDateTime toDay = null;
        if (StringUtil.hasContent(transaction_month_to)){

        }else {
            
        }
        return toDay;
    }

    //YYYY-MM形式か
    private boolean isValidYYYYMM(String param){
        String regex = "^\\d{4}-(0[1-9]|1[0-2])$";
        return param.matches(regex);
    }
 
}
