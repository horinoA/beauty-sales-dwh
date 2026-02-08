package com.beauty.beauty_sales_dwh.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.beauty.beauty_sales_dwh.domain.TransactionDetailRawData;

@Mapper
public interface RawTransactionDetailMapper {
    void insertRawTransactionDetail(TransactionDetailRawData data);
}
