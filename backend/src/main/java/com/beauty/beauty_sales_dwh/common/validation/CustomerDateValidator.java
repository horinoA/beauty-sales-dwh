package com.beauty.beauty_sales_dwh.common.validation;

import com.beauty.beauty_sales_dwh.analytics.customer.DimCustomer;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CustomerDateValidator implements ConstraintValidator<ValidCustomerDates, DimCustomer> {

    @Override
    public boolean isValid(DimCustomer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        
        // 片方でもnullの場合は比較できないのでスルー（@NotNull等はフィールド側で制御）
        if (value.firstVisitDate() == null || value.lastVisitDate() == null) {
            return true;
        }

        // firstVisitDate が lastVisitDate より「後」になっていたらNG
        // 正: first <= last
        return !value.firstVisitDate().isAfter(value.lastVisitDate());
    }
}