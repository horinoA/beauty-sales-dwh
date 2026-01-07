package com.beauty.beauty_sales_dwh.mapper;

import java.time.OffsetDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CustomerTransformMapper {

    /**
     * rawテーブルの最終更新日時を取得します。
     * データがない場合は NULL が返ります。
     */
    OffsetDateTime findMaxFetchedAt();

    /**
     * RAWデータをDimテーブルへUPSERTします。
     * * @param companyId プロパティから取得した会社ID
     * @param fromDate  この日時より新しいデータを対象とする
     * @return 更新・挿入された件数
     */
    int upsertCustomersFromRaw(@Param("companyId") Integer companyId, 
                               @Param("fromDate") OffsetDateTime fromDate);
    
}