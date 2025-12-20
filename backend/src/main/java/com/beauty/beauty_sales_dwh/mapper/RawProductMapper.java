package com.beauty.beauty_sales_dwh.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.beauty.beauty_sales_dwh.domain.ProductRawData;

@Mapper
public interface RawProductMapper {
    void insertRawProduct(ProductRawData data);
}