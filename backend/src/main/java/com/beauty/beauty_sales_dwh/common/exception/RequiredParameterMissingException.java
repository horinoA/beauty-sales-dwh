package com.beauty.beauty_sales_dwh.common.exception;

import org.springframework.http.HttpStatus;

public class RequiredParameterMissingException extends CustomAppException{
    public RequiredParameterMissingException(String message){
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
