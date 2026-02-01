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

import com.beauty.beauty_sales_dwh.config.AppVendorProperties;
import com.beauty.beauty_sales_dwh.config.SmaregiApiProperties;
import com.beauty.beauty_sales_dwh.mapper.RawStaffMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@StepScope
@Slf4j
public class SmaregiStaffItemReader extends AbstractSmaregiItemReader {

    private final SmaregiApiProperties properties;
    private final AppVendorProperties vendorProperties;
    private final RawStaffMapper rawStaffMapper;

    private String updDateTimeFrom; // テーブル最新更新日

    // コンストラクタ注入
    public SmaregiStaffItemReader(RestTemplate restTemplate,
                                     SmaregiApiProperties properties,
                                     RawStaffMapper rawStaffMapper,
                                     AppVendorProperties appVendorProperties) {
        super(restTemplate); // 親クラスにRestTemplateを渡す
        this.properties = properties;
        this.rawStaffMapper = rawStaffMapper;
        this.vendorProperties = appVendorProperties;
    }

    // 親クラスの beforeStep も実行しつつ、自分独自の初期化（日付取得）も行う
    @Override
    public void beforeStep(StepExecution stepExecution) {
        // 1. 親クラスの処理（トークン取得）を必ず呼ぶ
        super.beforeStep(stepExecution);

        // 2. プロパティから会社IDを取得
        Long companyId = Long.valueOf(vendorProperties.getId());

        // 3. DBから最終更新日時を取得
        OffsetDateTime maxDate = rawStaffMapper.findMaxFetchedAt(companyId);
        if (maxDate == null) {
            maxDate = OffsetDateTime.of(2000, 1, 1, 0, 0, 0,0,ZoneOffset.ofHours(9));
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
        this.updDateTimeFrom = maxDate.format(formatter);
        
        log.info("StaffReader初期化完了: upd_date_time-from={}", this.updDateTimeFrom);
    }

    // URL生成
    @Override
    protected URI getApiUrl(int page) {
        // ベースURL
        String baseUrl = properties.getBaseUrl() + "/" +properties.getContractId() + "/pos/staffs";
        try {
            String encodedDate = URLEncoder.encode(this.updDateTimeFrom, StandardCharsets.UTF_8);

            String urlString = String.format("%s?limit=1000&page=%d&upd_date_time-from=%s",
                    baseUrl, page, encodedDate);
            
            log.info("Request URL: {}", urlString);

            return URI.create(urlString);

        } catch (Exception e) {
            throw new RuntimeException("URLエンコードに失敗しました", e);
        }
    }
}
