package com.beauty.beauty_sales_dwh.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDetailRawData {
    private Long companyId; // 企業ID
    private Long transactionId; // raw.transactions.transaction_id (外部キー)
    private String transactionHeadId; // 親のtransactionHeadId (file_nameの代替)
    private String jsonBody;   // APIレスポンス(JSON文字列)
}
