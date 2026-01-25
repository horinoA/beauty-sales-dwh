package com.beauty.beauty_sales_dwh.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductRawData {
    private Long companyId; // 会社ID
    private String jsonBody;   // APIレスポンス(JSON文字列)
}