package com.beauty.beauty_sales_dwh.mapper;

import java.time.OffsetDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StaffTransformMapper {
    
    /**
     * dwh.dim_stafsfテーブルの最終更新日時を取得します。
     * データがない場合は NULL が返ります。
     */
    OffsetDateTime findMaxUpdateDataTimeFromDimStaffs(@Param("companyId") Long companyId);
    /**
     * RAWデータをdwh.dim_staffsテーブルへUPSERTします。
     * @param companyId プロパティから取得した会社ID
     * @param fromDate  この日時より新しいデータを対象とする
     * @return 更新・挿入された件数
     */
    int upsertStaffsFromRaw(@Param("companyId") Long companyId,
                              @Param("fromDate") OffsetDateTime fromDate);

}
