package com.beauty.beauty_sales_dwh.analytics.customer;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import com.beauty.beauty_sales_dwh.common.validation.ValidSmaregiId;
import com.beauty.beauty_sales_dwh.common.validation.ValidSnowflakeId;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 顧客ディメンション
 * DWH層: dwh.dim_customers
 */
@Table(name = "dim_customers", schema = "dwh")
public record DimCustomer(
    @ValidSnowflakeId
    @NotNull
    Long appCompanyId,

    @NotNull
    @Id
    @ValidSmaregiId(min = 1,max = 999999999)
    String customerId,

    // 漢字、ひらがな、カタカナ、英数字(半角/全角)、スペース、記号(ー 々)、アンダーバーを許可
    @Size(max = 100, message = "{DimCustomer.customerName.size}")
    @Pattern(regexp = "^[a-zA-Zａ-ｚＡ-Ｚ0-9０-９一-龠ぁ-んァ-ヶー々\\s_]+$", message = "{DimCustomer.customerName.pattern}")
    String customerName,

    // 全角カナと_アンダーバーのみ (null許可)
    @Size(max = 100, message = "{DimCustomer.customerName.size}")
    @Pattern(regexp = "^[ァ-ヶー_]+$", message = "{DimCustomer.customerKana.pattern}")
    String customerKana,

    // 半角数字と-ハイフンのみ (null許可)
    @Pattern(regexp = "^[0-9-]+$", message = "{DimCustomer.phoneNumber.pattern}")
    String phoneNumber,

    // 半角数字と-ハイフンのみ (null許可)
    @Pattern(regexp = "^[0-9-]+$", message = "{DimCustomer.mobileNumber.pattern}")
    String mobileNumber,

    @ValidSmaregiId(min = 1,max = 99999999)
    String storeId,

    @PastOrPresent
    LocalDate firstVisitDate,

    @PastOrPresent
    LocalDate lastVisitDate,

    @Min(value=0, message="{DimCustomer.visitCount.minsize}")
    Integer visitCount,

    Boolean isDeleted,

    OffsetDateTime insertDataTime,
    OffsetDateTime updateDataTime
) {
    // コンパクトコンストラクタ（デフォルト値設定）
    public DimCustomer {
        //訪問回数: nullなら　0
        if (visitCount == null){
            visitCount = 0;
        }

        // 論理削除フラグ: nullなら false
        if (isDeleted == null) {
            isDeleted = false;
        }

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