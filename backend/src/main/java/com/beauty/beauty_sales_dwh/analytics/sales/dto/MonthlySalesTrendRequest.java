package com.beauty.beauty_sales_dwh.analytics.sales.dto;

import java.util.stream.Stream;

import com.beauty.beauty_sales_dwh.common.exception.RequiredParameterMissingException;


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
    }
}
