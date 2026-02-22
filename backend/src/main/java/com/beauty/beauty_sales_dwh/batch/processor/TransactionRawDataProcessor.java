package com.beauty.beauty_sales_dwh.batch.processor;

import java.util.Map;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.beauty.beauty_sales_dwh.domain.TransactionRawData;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * スマレジ売上（取引）データを RawTransactionData に変換するプロセッサ
 */
@Component
@Slf4j
public class TransactionRawDataProcessor implements ItemProcessor<Map<String, Object>, TransactionRawData> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Long companyId;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        // JobParameters から companyId を取得
        this.companyId = stepExecution.getJobParameters().getLong("companyId");
    }

    @Override
    public TransactionRawData process(Map<String, Object> item) throws Exception {
        // 全体をJSON文字列として保持
        String jsonBody = objectMapper.writeValueAsString(item);

        TransactionRawData rawData = new TransactionRawData();
        rawData.setCompanyId(this.companyId);
        rawData.setJsonBody(jsonBody);

        return rawData;
    }
}
