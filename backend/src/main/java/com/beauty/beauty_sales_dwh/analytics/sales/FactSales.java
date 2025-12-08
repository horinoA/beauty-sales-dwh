package com.beauty.beauty_sales_dwh.analytics.sales;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import com.beauty.beauty_sales_dwh.common.validation.ValidSnowflakeId;
import com.beauty.beauty_sales_dwh.common.validation.ValidTransactionAmount;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 売上ファクト (ヘッダー)
 * DWH層: dwh.fact_sales
 * 複合主キー: app_company_id + transaction_head_id
 */
@Table(name = "fact_sales", schema = "dwh")
@ValidTransactionAmount // カスタムバリデーション: 取引区分と金額の整合性チェック
public record FactSales(
    @ValidSnowflakeId
    @NotNull(message = "{factSales.companyId.notNull}")
    Long appCompanyId,

    @NotNull(message = "{factSales.transactionHeadId.notNull}")
    @Id // Spring Data JDBC上の識別子（実際はRepositoryでSQL制御）
    String transactionHeadId,

    @NotNull
    OffsetDateTime transactionDateTime, // TIMESTAMPTZ -> OffsetDateTime

    @NotNull
    LocalDate transactionDate,

    String customerId,
    String staffId,
    String storeId, // 最新スキーマ対応

    // --- 金額・税（1円の壁対策 / Null許容しない） ---
    @NotNull
    Integer amountTotal,
    @NotNull
    Integer amountSubtotal,
    @NotNull
    Integer amountTaxInclude,
    @NotNull
    Integer amountTaxExclude,

    Integer amountSubtotalDiscountPrice,
    Integer amountFee,
    Integer amountShipping,
    Integer discountPoint,
    Integer discountCoupon,

    @NotNull(message = "{factSales.transactionType.notNull}")
    @Pattern(regexp = "^(SALES|REFUND)$", message = "{factSales.transactionType.pattern}")
    String transactionType, // SALES, REFUND

    @NotNull
    Boolean isVoid
    
) {
    // ドメインロジック: 返金データかどうか
    public boolean isRefund() {
        return "REFUND".equals(transactionType);
    }
}