package com.beauty.beauty_sales_dwh.analytics.staff;

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
 * スタッフディメンション
 * DWH層: dwh.dim_staffs
 */
@Table(name = "dim_staffs", schema = "dwh")
public record DimStaff(
    @ValidSnowflakeId
    @NotNull
    Long appCompanyId,

    @NotNull
    @Id
    @ValidSmaregiId(min = 1,max = 999999999)
    String staffId,

    @NotNull
    // 漢字、ひらがな、カタカナ、英数字(半角/全角)、スペース、記号(ー 々)、アンダーバーを許可
    @Size(max = 100, message = "{DimCustomer.customerName.size}")
    @Pattern(regexp = "^[a-zA-Zａ-ｚＡ-Ｚ0-9０-９一-龠ぁ-んァ-ヶー々\\s_]+$", message = "{DimStaff.staffName.pattern}")
    String staffName,

    // XSS対策: < > を禁止。
    @Size(max = 50, message = "{FactSalesDetail.categoryGroupName.size}") 
    @Pattern(regexp = "^[^<>]*$", message = "{DimStaff.rank.pattern}")
    String rank,

    @ValidSmaregiId(min = 1,max = 999999999)
    String storeId,

    @Min(value = 0, message="{DimStaff.employFlag.size}")
    @Max(value = 1, message="{DimStaff.employFlag.size}") // 0:退職, 1:在籍
    Integer employFlag,

    OffsetDateTime insertDataTime,  // TIMESTAMPTZ -> OffsetDateTime
    OffsetDateTime updateDataTime   // TIMESTAMPTZ -> OffsetDateTime
) {
    public DimStaff{
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