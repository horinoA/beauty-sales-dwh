package com.beauty.beauty_sales_dwh.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = CustomerDateValidator.class)
@Target({ElementType.TYPE}) // クラス全体にかける
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCustomerDates {
    String message() default "{dimCustomer.data.comparison}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}