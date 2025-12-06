package com.beauty.beauty_sales_dwh.common.validation;

import com.beauty.beauty_sales_dwh.analytics.sales.FactSales;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TransactionAmountValidator implements ConstraintValidator<ValidTransactionAmount, FactSales> {

    @Override
    public boolean isValid(FactSales value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        if ("SALES".equals(value.transactionType())) {
            return value.amountTotal() >= 0;
        } 
        
        if ("REFUND".equals(value.transactionType())) {
            return value.amountTotal() <= 0;
        }

        return true;
    }
}