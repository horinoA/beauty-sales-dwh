package com.beauty.beauty_sales_dwh.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.beauty.beauty_sales_dwh.domain.CategoryGroupRawData;

@Mapper
public interface RawCategoryGroupMapper {
    void insertRawCategoryGroup(CategoryGroupRawData data);
}