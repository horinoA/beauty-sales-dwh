package com.beauty.beauty_sales_dwh.mapper;

import java.time.OffsetDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 取引データ（売上・明細）を raw スキーマから dwh スキーマへ変換・転送するためのマッパー。
 */
@Mapper
public interface FactSalesTransformMapper {

    /**
     * dwh.fact_sales テーブルから指定された会社IDの最新の取引日時を取得します。
     * 差分更新の基準日として使用します。
     * 
     * @param companyId 会社ID
     * @return 最新の取引日時（データがない場合は null）
     */
    OffsetDateTime findMaxTransactionDateTime(@Param("companyId") Long companyId);

    /**
     * raw.transactions から dwh.fact_sales へデータを UPSERT します。
     * 判定ロジック（is_void, transaction_type）および名寄せ解決を含みます。
     * 
     * @param companyId 会社ID
     * @param fromDate  この日時以降に更新された raw データを対象とする
     * @return 更新・挿入された件数
     */
    int upsertFactSales(@Param("companyId") Long companyId, 
                        @Param("fromDate") OffsetDateTime fromDate);

    /**
     * raw.transaction_details から dwh.fact_sales_details へデータを UPSERT します。
     * 商品マスタ・カテゴリマスタとの結合および種別判定を含みます。
     * 
     * @param companyId 会社ID
     * @param fromDate  この日時以降に更新された raw データを対象とする
     * @return 更新・挿入された件数
     */
    int upsertFactSalesDetails(@Param("companyId") Long companyId, 
                               @Param("fromDate") OffsetDateTime fromDate);
}
