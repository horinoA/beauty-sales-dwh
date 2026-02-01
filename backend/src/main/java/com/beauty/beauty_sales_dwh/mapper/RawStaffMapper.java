package com.beauty.beauty_sales_dwh.mapper;

import java.time.OffsetDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.beauty.beauty_sales_dwh.domain.StaffRawData;

@Mapper
public interface RawStaffMapper {
    void insertRawStaff(StaffRawData data);
    OffsetDateTime findMaxFetchedAt(@Param("companyId") Long companyId);
}
