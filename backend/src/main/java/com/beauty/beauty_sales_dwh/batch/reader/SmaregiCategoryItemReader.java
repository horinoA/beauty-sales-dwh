package com.beauty.beauty_sales_dwh.batch.reader;

import java.net.URI;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.beauty.beauty_sales_dwh.config.SmaregiApiProperties;

import lombok.extern.slf4j.Slf4j;

@Component
@StepScope
@Slf4j
public class SmaregiCategoryItemReader extends AbstractSmaregiItemReader {

    private final SmaregiApiProperties properties;

    public SmaregiCategoryItemReader(RestTemplate restTemplate,
                                            SmaregiApiProperties properties) {
        super(restTemplate);
        this.properties = properties;
    }

    @Override
    protected URI getApiUrl(int page) {
        String baseUrl = properties.getBaseUrl() + "/" + properties.getContractId() + "/pos/categories";
        try {
        String urlString = String.format("%s?limit=1000&page=%d", baseUrl, page);
        log.info("Request URL for Categories: {}", urlString);
        return URI.create(urlString);
        } catch (Exception e) {
            throw new RuntimeException("URLエンコードに失敗しました", e);
        }

    }
}