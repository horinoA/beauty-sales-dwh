package com.beauty.beauty_sales_dwh.analytics.product;

import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import com.beauty.beauty_sales_dwh.common.validation.ValidSmaregiId;
import com.beauty.beauty_sales_dwh.common.validation.ValidSnowflakeId;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * カテゴリグループディメンション
 * DWH層: dwh.dim_category_groups
 */
@Table(name = "dim_category_groups", schema = "dwh")
public record DimCategoryGroup(
    @ValidSnowflakeId
    @NotNull
    Long appCompanyId,

    @NotNull
    @Id
    @ValidSmaregiId(min = 1,max = 999999999)    
    String catGroupId,

    @NotNull
    @Pattern(regexp = "^[^<>]*$", message = "{CategoryGroup.groupName.pattern}")
    String catGroupName,

    OffsetDateTime insertDataTime,
    OffsetDateTime updateDataTime
) {
    public DimCategoryGroup {
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