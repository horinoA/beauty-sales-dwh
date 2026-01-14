package com.beauty.beauty_sales_dwh.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.beauty.beauty_sales_dwh.domain.CategoryRawData;

@Mapper
public interface RawCategoryMapper {
    void insertRawCategory(CategoryRawData data);
}