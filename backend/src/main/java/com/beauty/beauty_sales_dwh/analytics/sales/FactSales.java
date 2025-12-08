package com.beauty.beauty_sales_dwh.analytics.sales;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import com.beauty.beauty_sales_dwh.common.validation.ValidSmaregiId;
import com.beauty.beauty_sales_dwh.common.validation.ValidSnowflakeId;
import com.beauty.beauty_sales_dwh.common.validation.ValidTransactionAmount;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
    @Pattern(regexp = "^[1-9][0-9]*$", message = "正の整数を入力してください")
    String transactionHeadId,

    @NotNull
    OffsetDateTime transactionDateTime, // TIMESTAMPTZ -> OffsetDateTime

    @NotNull
    LocalDate transactionDate,

    @ValidSmaregiId
    String customerId,
    @ValidSmaregiId
    String staffId,
    @ValidSmaregiId
    String storeId, // 最新スキーマ対応

    // --- 金額・税（1円の壁対策 / Null許容しない） ---
    @NotNull
    @Size(min = -999999999, max = 999999999, message = "{sumaregi.amount.size}")
    Integer amountTotal,
    @NotNull
    @Size(min = -999999999, max = 999999999, message = "{sumaregi.amount.size}")
    Integer amountSubtotal,
    @NotNull
    @Size(min = -999999999, max = 999999999, message = "{sumaregi.amount.size}")
    Integer amountTaxInclude,
    @NotNull
    @Size(min = -999999999, max = 999999999, message = "{sumaregi.amount.size}")
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