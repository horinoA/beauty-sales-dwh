package com.beauty.beauty_sales_dwh.mapper;

import java.time.OffsetDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductTransformMapper { 

    /**
     * dwh.dim_productsテーブルの最終更新日時を取得します。
     * データがない場合は NULL が返ります。
     */
    OffsetDateTime findMaxUpdateDataTimeFromDimProducts(@Param("companyId") Long companyId);

    OffsetDateTime findMaxUpdateDataTimeFromDimCategoryGroups(@Param("companyId") Long companyId);
    
    /**
     * RAWデータをdwh.dim_category_groupsテーブルへUPSERTします。
     * @param companyId プロパティから取得した会社ID
     * @param fromDate  この日時より新しいデータを対象とする
     * @return 更新・挿入された件数
     */
    int upsertCategoryGroupsFromRaw(@Param("companyId") Long companyId,
                                    @Param("fromDate") OffsetDateTime fromDate);

    /**
     * RAWデータをdwh.dim_productsテーブルへUPSERTします。
     * @param companyId プロパティから取得した会社ID
     * @param fromDate  この日時より新しいデータを対象とする
     * @return 更新・挿入された件数
     */
    int upsertProductsFromRaw(@Param("companyId") Long companyId,
                              @Param("fromDate") OffsetDateTime fromDate);
}