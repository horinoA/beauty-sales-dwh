package com.beauty.beauty_sales_dwh.common.exception;

import org.springframework.http.HttpStatus;

public class DateRangeSettings extends CustomAppException{

    public DateRangeSettings(String message) {
        super(message);
    }
    
    public DateRangeSettings(String message, Object[] args){
        super(message,args);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }

}
