package com.beauty.beauty_sales_dwh.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 顧客の来店統計情報を更新するためのMyBatisマッパー
 */
@Mapper
public interface CustomerVisitUpdateMapper {

    /**
     * fact_salesテーブルから集計し、dim_customersの来店回数と来店日を更新します。
     * 
     * @param companyId アプリケーション会社ID
     * @return 更新された顧客数
     */
    int updateCustomerVisitStats(@Param("companyId") Long companyId);
}
