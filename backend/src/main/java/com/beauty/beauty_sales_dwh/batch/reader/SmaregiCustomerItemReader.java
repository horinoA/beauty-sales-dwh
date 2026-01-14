package com.beauty.beauty_sales_dwh.batch.reader;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.beauty.beauty_sales_dwh.config.SmaregiApiProperties;
import com.beauty.beauty_sales_dwh.mapper.RawCustomerMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@StepScope
@Slf4j
public class SmaregiCustomerItemReader extends AbstractSmaregiItemReader {

    private final SmaregiApiProperties properties;
    private final RawCustomerMapper rawCustomerMapper;

    private String updDateTimeFrom; // テーブル最新更新日

    // コンストラクタ注入
    public SmaregiCustomerItemReader(RestTemplate restTemplate,
                                     SmaregiApiProperties properties,
                                     RawCustomerMapper rawCustomerMapper) {
        super(restTemplate); // 親クラスにRestTemplateを渡す
        this.properties = properties;
        this.rawCustomerMapper = rawCustomerMapper;
    }

    // 親クラスの beforeStep も実行しつつ、自分独自の初期化（日付取得）も行う
    @Override
    public void beforeStep(StepExecution stepExecution) {
        // 1. 親クラスの処理（トークン取得）を必ず呼ぶ
        super.beforeStep(stepExecution);

        // 2. DBから最終更新日時を取得
        OffsetDateTime maxDate = rawCustomerMapper.findMaxFetchedAt();
        if (maxDate == null) {
            maxDate = OffsetDateTime.of(2000, 1, 1, 0, 0, 0,0,ZoneOffset.ofHours(9));
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
        this.updDateTimeFrom = maxDate.format(formatter);
        
        log.info("CustomerReader初期化完了: upd_date_time-from={}", this.updDateTimeFrom);
    }

    // URL生成
    @Override
    protected URI getApiUrl(int page) {
        // ベースURL
        String baseUrl = properties.getBaseUrl() + "/" +properties.getContractId() + "/pos/customers";
        try {
            // Java標準のURLEncoderを使う
            // これを行うと:
            // "2000-01-01T00:00:00+09:00" 
            //   ↓
            // "2000-01-01T00%3A00%3A00%2B09%3A00" (完全にURLセーフな形) になります
            String encodedDate = URLEncoder.encode(this.updDateTimeFrom, StandardCharsets.UTF_8);

            // エンコード済みの文字列を使ってURLを組み立てる
            String urlString = String.format("%s?limit=1000&page=%d&upd_date_time-from=%s",
                    baseUrl, page, encodedDate);
            
            // ログに出して確認（%2B になっているはずです）
            log.info("Request URL: {}", urlString);

            // そのままURIオブジェクトにする
            return URI.create(urlString);

        } catch (Exception e) {
            throw new RuntimeException("URLエンコードに失敗しました", e);
        }
    }
}