package com.beauty.beauty_sales_dwh.batch.reader;

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
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        this.updDateTimeFrom = maxDate.format(formatter);
        
        log.info("CustomerReader初期化完了: upd_date_time-from={}", this.updDateTimeFrom);
    }

    // URL生成
    @Override
    protected String getApiUrl(int page) {
        // ベースURL
        String baseUrl = properties.getUrl() + "/" +properties.getContractId() + "/pos/customers";
        
        // パラメータ付きURLを生成して返す
        return String.format("/?limit=1000&page=%d&upd_date_time-from=%s",
                baseUrl, page, this.updDateTimeFrom);
    }
}