package com.beauty.beauty_sales_dwh.common.exception;

import org.springframework.http.HttpStatus;

public abstract class CustomAppException extends RuntimeException{
    public CustomAppException(String message){
        super(message);
    }
    
    // 子クラスごとに異なるステータスコードを返せるようにする（デフォルトは400）
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
