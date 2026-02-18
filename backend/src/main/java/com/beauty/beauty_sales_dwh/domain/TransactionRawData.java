package com.beauty.beauty_sales_dwh.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionRawData {
    private Long transactionId; //挿入ずみのTransavtion_id
    private String jsonBody;   // APIレスポンス(JSON文字列)
    private Long companyId; // 企業ID
}
