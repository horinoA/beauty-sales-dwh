package com.beauty.beauty_sales_dwh.common.validation;

import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SmaregiIdValidator implements ConstraintValidator<ValidSmaregiId,String>{

    private long min;
    private long max;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^[0-9]+$");

    @Override
    public void initialize(ValidSmaregiId validSmaregiId) {
        this.min = validSmaregiId.min();
        this.max = validSmaregiId.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context){

        if ( value == null || value.isBlank()) { //null,空白はtrue（null許容）
            return true;
        }

        if (!NUMBER_PATTERN.matcher(value).matches()){ //数字以外はfalse
            return false;
        }

        //idの範囲を超えた場合false
        try {
            long longValue = Long.parseLong(value);
            if (longValue<this.min || longValue>this.max){
                return false;
            }   
        }catch (NumberFormatException e){
            return false;
        }

        return true;
    }
}
