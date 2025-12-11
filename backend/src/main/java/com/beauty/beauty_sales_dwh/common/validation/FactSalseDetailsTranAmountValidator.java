package com.beauty.beauty_sales_dwh.common.validation;

import com.beauty.beauty_sales_dwh.analytics.sales.FactSalesDetail;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FactSalseDetailsTranAmountValidator implements ConstraintValidator<ValidTransactionAmount, FactSalesDetail> {

    @Override
    public boolean isValid(FactSalesDetail value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        if ("SALES".equals(value.categoryType())) {
            return value.salesPrice() >= 0;
        } 
        
        if ("REFUND".equals(value.categoryType())) {
            return value.salesPrice() <= 0;
        }

        return true;
    }
}