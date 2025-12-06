package com.beauty.beauty_sales_dwh.analytics.sales;

import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 売上明細ファクト
 * DWH層: dwh.fact_sales_details
 * 複合主キー: app_company_id + transaction_head_id + transaction_detail_id
 */
@Table(name = "fact_sales_details", schema = "dwh")
public record FactSalesDetail(
    @NotNull
    Long appCompanyId,

    @NotNull
    String transactionHeadId,

    @NotNull
    String transactionDetailId,

    String productId,
    String productName,      // Snapshot
    String categoryGroupName, // Snapshot

    @NotNull
    Integer quantity,

    @NotNull
    Integer salesPrice,

    @Min(0)
    Integer taxDivision,     // 0:込, 1:抜, 2:非

    String categoryType      // SALES/REFUND区分
) {
}