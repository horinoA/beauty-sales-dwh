package com.beauty.beauty_sales_dwh.batch.reader;

import java.net.URI;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.beauty.beauty_sales_dwh.config.SmaregiApiProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * スマレジ取引データ取得用 Reader
 * CommandLineRunner 等から渡された JobParameters ("from", "to") を元に
 * 特定期間の取引データをページングしながら取得します。
 */
@Component
@StepScope // Step実行のたびに JobParameters を注入するために必須
@Slf4j
public class SmaregiTransactionItemReader extends AbstractSmaregiItemReader {

    private final SmaregiApiProperties properties;
    private final String from; // yyyy-MM-dd
    private final String to;   // yyyy-MM-dd

    public SmaregiTransactionItemReader(
            RestTemplate restTemplate,
            SmaregiApiProperties properties,
            @Value("#{jobParameters['from']}") String from,
            @Value("#{jobParameters['to']}") String to
    ) {
        super(restTemplate);
        this.properties = properties;
        this.from = from;
        this.to = to;
    }

    /**
     * スマレジ取引APIのURLを生成します。
     * UriComponentsBuilder により、タイムゾーンの '+' や日時の ':' は
     * 自動的にURLエンコード（%2B, %3A）されます。
     */
    @Override
    protected URI getApiUrl(int page) {
        // 例: https://api.smaregi.jp/{contract_id}/pos/transactions/
        String baseUrl = String.format("%s/%s/pos/transactions/", 
                properties.getBaseUrl(), 
                properties.getContractId());

        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("transaction_datetime-from", from + "T00:00:00+09:00")
                .queryParam("transaction_datetime-to", to + "T23:59:59+09:00")
                .queryParam("page", page)
                .queryParam("limit", 100)
                .queryParam("sort", "transaction_datetime:asc")
                .build()
                .toUri();

        log.info("=== Generated URI for Smaregi API ===");
        log.info("URI: {}", uri.toString());
        log.info("=====================================");
        
        return uri;
    }
}
