package com.beauty.beauty_sales_dwh.batch.processor;

import java.util.Map;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.beauty.beauty_sales_dwh.config.AppVendorProperties;
import com.beauty.beauty_sales_dwh.domain.CustomerRawData;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomerRawDataProcessor implements ItemProcessor<Map<String, Object>, CustomerRawData> {

    private final ObjectMapper objectMapper;
    private final AppVendorProperties vendorProperties;

    @Override
    public CustomerRawData process(Map<String, Object> item) throws Exception {
        // 1. Map -> JSON文字列
        String json = objectMapper.writeValueAsString(item);
        // 2. 設定ファイルから読み込んだIDを使用
        Long companyId = Long.valueOf(vendorProperties.getId());
        
        // 3. 企業IDを付与してWriterへ渡す
        return new CustomerRawData(companyId, json);
    }
}