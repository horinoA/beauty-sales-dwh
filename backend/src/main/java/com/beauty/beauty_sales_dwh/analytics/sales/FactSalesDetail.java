package com.beauty.beauty_sales_dwh.analytics.sales;

import org.springframework.data.relational.core.mapping.Table;

import com.beauty.beauty_sales_dwh.common.validation.ValidSmaregiId;
import com.beauty.beauty_sales_dwh.common.validation.ValidSnowflakeId;
import com.beauty.beauty_sales_dwh.common.validation.ValidTransactionAmount;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * 売上明細ファクト
 * DWH層: dwh.fact_sales_details
 * 複合主キー: app_company_id + transaction_head_id + transaction_detail_id
 */
@Table(name = "fact_sales_details", schema = "dwh")
@ValidTransactionAmount // カスタムバリデーション: 取引区分CategyTypeと販売単価の整合性チェック
@Slf4j
public record FactSalesDetail(
    @ValidSnowflakeId
    @NotNull(message = "{factSales.companyId.notNull}")
    Long appCompanyId,

    @ValidSmaregiId(min = 1,max = 999999999)
    @NotNull(message = "{factSales.transactionHeadId.notNull}")
    String transactionHeadId,

    @ValidSmaregiId(min = 1,max = 999)
    @NotNull(message = "{factSalesDetail.transactionDetailId.notNull}")
    String transactionDetailId,

    @ValidSmaregiId(min = 1,max = 999999999)
    String productId,
    String productName,      // Snapshot
    String categoryGroupName, // Snapshot

    @NotNull
    @Min(value = 1, message="{factSalesDetail.quantity.minsize}")
    @Max(value = 999999, message = "{factSalesDetail.quantity.maxsize}")
    Integer quantity,

    @NotNull
    @Min(value = -999999999, message = "{sumaregi.amount.minsize}")
    @Max(value = 999999999, message = "{sumaregi.amount.maxsize}")
    Integer salesPrice,

    @Min(value=0, message = "{factSalesDetail.taxDivision.size}")
    @Max(value=2, message = "{factSalesDetail.taxDivision.size}")
    Integer taxDivision,     // 0:込, 1:抜, 2:非

    @NotNull(message = "{factSales.transactionType.notNull}")
    @Pattern(regexp = "^(SALES|REFUND)$", message = "{factSales.transactionType.pattern}")
    String categoryType      // SALES/REFUND区分
) {
}