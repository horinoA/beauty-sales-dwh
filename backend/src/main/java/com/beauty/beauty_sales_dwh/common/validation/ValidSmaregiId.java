package com.beauty.beauty_sales_dwh.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = SmaregiIdValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSmaregiId {

    //最小値
    long min() default 1;  //スマレジIDは１開始
    //最大値
    long max() default 999999999;

    String message() default "{sumaregi.id.size}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
