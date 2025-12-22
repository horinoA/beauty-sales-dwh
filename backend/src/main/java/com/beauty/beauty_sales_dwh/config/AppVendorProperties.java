package com.beauty.beauty_sales_dwh.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "app.vendor") // "app.vendor" から始まる設定を読み込む
@Data
public class AppVendorProperties {

    /**
     * app.vendor.id に対応
     * 環境変数の APP_VENDOR_ID がここに注入されます
     */
    private String id;
}