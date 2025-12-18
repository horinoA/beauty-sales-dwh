package com.beauty.beauty_sales_dwh.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration // Springに「これは設定クラスだよ」と教える
@ConfigurationProperties(prefix = "smaregi.api") // "smaregi.api" から始まる設定を読み込む
@Data // Lombok: Getter/Setterを自動生成
public class SmaregiApiProperties {

    // smaregi.api.url に対応
    private String url;

    // smaregi.api.contract-id に対応 (ハイフンはキャメルケースに自動変換されます)
    private String contractId;

    // smaregi.api.client-id に対応
    private String clientId;

    // smaregi.api.client-secret に対応
    private String clientSecret;

    // smaregi.api.scope に対応
    private String scope;
}