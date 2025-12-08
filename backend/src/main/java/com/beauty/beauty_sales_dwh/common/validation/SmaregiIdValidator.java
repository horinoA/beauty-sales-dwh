package com.beauty.beauty_sales_dwh.common.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SmaregiIdValidator implements ConstraintValidator<ValidSmaregiId,String>{
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context){
        if (value.isBlank() || value == null) { //null,空白はtrue（null許容）
            return true;
        }
        Pattern pattern = Pattern.compile("^[0-9]+$");
        Matcher matcher = pattern.matcher(value);
        if (!matcher.matches()){ //数字以外はfalse
            return false;
        }
        //idの範囲を超えた場合false
        try {
            if (Long.parseLong(value)<1 || Long.parseLong(value)>999999999){
                return false;
            }   
        }catch (NumberFormatException e){
            return false;
        }
        return true;
    }
}
