package com.beauty.beauty_sales_dwh.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidFormatException extends CustomAppException{

    public InvalidFormatException(String message) {
        super(message);
    }

    public InvalidFormatException(String message, Object[] args){
        super(message,args);
    }
    
    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }

}
