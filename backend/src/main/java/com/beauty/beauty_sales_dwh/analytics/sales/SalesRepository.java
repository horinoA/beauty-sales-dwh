package com.beauty.beauty_sales_dwh.analytics.sales;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * 売上データアクセスリポジトリ
 * 複合主キーに対するUpsertや、分析用の集計クエリを担当
 */
public interface SalesRepository extends Repository<FactSales, String> {

    /**
     * 月次売上推移 (分析UI用)
     */
    @Query("""
        SELECT 
            transaction_date, 
            SUM(amount_total) as total_sales
        FROM dwh.fact_sales
        WHERE 
            app_company_id = :companyId
            AND transaction_date BETWEEN :startDate AND :endDate
            AND is_void = FALSE
        GROUP BY transaction_date
        ORDER BY transaction_date ASC
    """)
    List<DailySalesDto> findDailySales(
        @Param("companyId") Long companyId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * ETL用 UPSERT (重複時は更新)
     * store_id を追加反映済み
     */
    @Modifying
    @Query("""
        INSERT INTO dwh.fact_sales (
            app_company_id, transaction_head_id, transaction_date_time, transaction_date,
            customer_id, staff_id, store_id,
            amount_total, amount_subtotal, amount_tax_include, amount_tax_exclude,
            amount_subtotal_discount_price, amount_fee, amount_shipping, discount_point, discount_coupon,
            transaction_type, is_void
        ) VALUES (
            :#{#s.appCompanyId}, :#{#s.transactionHeadId}, :#{#s.transactionDateTime}, :#{#s.transactionDate},
            :#{#s.customerId}, :#{#s.staffId}, :#{#s.storeId},
            :#{#s.amountTotal}, :#{#s.amountSubtotal}, :#{#s.amountTaxInclude}, :#{#s.amountTaxExclude},
            :#{#s.amountSubtotalDiscountPrice}, :#{#s.amountFee}, :#{#s.amountShipping}, :#{#s.discountPoint}, :#{#s.discountCoupon},
            :#{#s.transactionType}, :#{#s.isVoid}
        )
        ON CONFLICT (app_company_id, transaction_head_id)
        DO UPDATE SET
            amount_total = EXCLUDED.amount_total,
            transaction_date_time = EXCLUDED.transaction_date_time,
            store_id = EXCLUDED.store_id,
            is_void = EXCLUDED.is_void
    """)
    void upsert(@Param("s") FactSales factSales);

    // 射影用DTO
    record DailySalesDto(LocalDate transactionDate, Long totalSales) {}
}