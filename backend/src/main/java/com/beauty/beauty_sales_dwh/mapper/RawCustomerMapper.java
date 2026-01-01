package com.beauty.beauty_sales_dwh.mapper;

import java.time.OffsetDateTime;

import org.apache.ibatis.annotations.Mapper;

import com.beauty.beauty_sales_dwh.domain.CustomerRawData;

@Mapper
public interface RawCustomerMapper {
    void insertRawCustomer(CustomerRawData data);
    OffsetDateTime findMaxFetchedAt();
}