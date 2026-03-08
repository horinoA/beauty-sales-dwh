package com.beauty.beauty_sales_dwh.batch.reader;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.beauty.beauty_sales_dwh.config.SmaregiApiProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * スマレジ取引データ取得用 Reader
 * JobParameters ("from", "to") を元に特定期間の取引データをページングしながら取得します。
 */
@Component
@StepScope
@Slf4j
public class SmaregiTransactionItemReader extends AbstractSmaregiItemReader {

    private final SmaregiApiProperties properties;
    
    // 文字列として保持 (SmaregiCustomerItemReader と同様)
    private String fromFormatted;
    private String toFormatted;

    public SmaregiTransactionItemReader(
            RestTemplate restTemplate,
            SmaregiApiProperties properties
    ) {
        super(restTemplate);
        this.properties = properties;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        // 1. 親クラスの処理（トークン取得）
        super.beforeStep(stepExecution);

        // 2. JobParameters から期間を取得 (yyyy-MM-dd)
        // String fromStr = stepExecution.getJobParameters().getString("from");
        // String toStr = stepExecution.getJobParameters().getString("to");
        
        // パーティショニング対応：ExecutionContextから優先的に取得し、なければJobParametersから取得する
        String fromStr = stepExecution.getExecutionContext().getString("from", 
                stepExecution.getJobParameters().getString("from"));
        String toStr = stepExecution.getExecutionContext().getString("to", 
                stepExecution.getJobParameters().getString("to"));

        if (fromStr == null || toStr == null) {
            throw new IllegalArgumentException("JobParameters 'from' and 'to' are required.");
        }

        // 日本標準時 (+09:00) 
        ZoneOffset jst = ZoneOffset.ofHours(9);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
        
        // from: 00:00:00
        this.fromFormatted = LocalDate.parse(fromStr)
                .atTime(LocalTime.MIN)
                .atOffset(jst)
                .format(formatter);
        
        // to: 23:59:59
        this.toFormatted = LocalDate.parse(toStr)
                .atTime(LocalTime.MAX)
                .atOffset(jst)
                .format(formatter);

        log.info("TransactionReader初期化完了: from={}, to={}", fromFormatted, toFormatted);
    }

    @Override
    protected URI getApiUrl(int page) {
        String baseUrl = String.format("%s/%s/pos/transactions/", 
                properties.getBaseUrl(), 
                properties.getContractId());

        try {
            // Java標準のURLEncoderを使ってエンコード
            String encodedFrom = URLEncoder.encode(this.fromFormatted, StandardCharsets.UTF_8);
            String encodedTo = URLEncoder.encode(this.toFormatted, StandardCharsets.UTF_8);

            // クエリパラメータを組み立てる
            String urlString = String.format("%s?transaction_date_time-from=%s&transaction_date_time-to=%s&page=%d&limit=100&with_details=all&sort=transactionDateTime:asc",
                    baseUrl, encodedFrom, encodedTo, page);
            
            log.info("Request URL: {}", urlString);

            return URI.create(urlString);

        } catch (Exception e) {
            throw new RuntimeException("URLの生成に失敗しました", e);
        }
    }
}
