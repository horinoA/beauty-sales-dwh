package com.beauty.beauty_sales_dwh.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = {
    TransactionAmountValidator.class,//FactSalse用バリデータクラス
    FactSalseDetailsTranAmountValidator.class//FactSalseDeatils用バリデータクラス
}) // ロジッククラスを指定
@Target({ElementType.TYPE}) // クラス(Record)全体に対してチェックする設定
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTransactionAmount {
    String message() default "{factSales.transactionAmount.invalid}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}