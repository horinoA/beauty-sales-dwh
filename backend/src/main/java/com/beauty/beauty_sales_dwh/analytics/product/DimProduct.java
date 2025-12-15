package com.beauty.beauty_sales_dwh.analytics.product;

import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import com.beauty.beauty_sales_dwh.common.validation.ValidSmaregiId;
import com.beauty.beauty_sales_dwh.common.validation.ValidSnowflakeId;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 商品ディメンション
 * DWH層: dwh.dim_products
 */
@Table(name = "dim_products", schema = "dwh")
public record DimProduct(
    @ValidSnowflakeId
    @NotNull
    Long appCompanyId,

    @NotNull
    @ValidSmaregiId(min = 1,max = 999999999)
    @Id
    String productId,

    @NotNull
    @Size(max = 200, message = "{FactSalesDetail.productName.size}") 
    @Pattern(regexp = "^[^<>]*$", message = "{FactSalesDetail.productName.pattern}")
    String productName,

    @ValidSmaregiId(min = 1,max = 999999999)    
    String catGroupId, // カテゴリグループID (FK)

    //マスタ単価
    @NotNull
    @Min(value = -999999999, message = "{sumaregi.amount.minsize}")
    @Max(value = 999999999, message = "{sumaregi.amount.maxsize}")
    Integer price,

    @ValidSmaregiId(min = 1,max = 999999999)
    String storeId,

    OffsetDateTime insertDataTime,
    OffsetDateTime updateDataTime
) {
    public DimProduct{
        // 登録日時: nullなら現在日時
        if (insertDataTime == null) {
            insertDataTime = OffsetDateTime.now();
        }

        // 更新日時: nullなら現在日時
        if (updateDataTime == null) {
            updateDataTime = OffsetDateTime.now();
        }
    }
}