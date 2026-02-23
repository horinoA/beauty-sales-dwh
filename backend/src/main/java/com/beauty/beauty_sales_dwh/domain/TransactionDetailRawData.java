package com.beauty.beauty_sales_dwh.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionDetailRawData {
    private Long companyId;         // app_company_id
    private String transactionHeadId; // 親の transaction_id (文字列として保持)
    private String jsonBody;        // 明細単体のJSON
    private String fileName;        // トレーサビリティ用 (APIリクエスト識別子など)
    private Long rowNumber;         // 明細配列のインデックス
}
