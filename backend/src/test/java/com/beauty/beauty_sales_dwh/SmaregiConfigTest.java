package com.beauty.beauty_sales_dwh;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;


@SpringBootTest
@ActiveProfiles("test")
class SmaregiConfigTest {

    // application.properties 経由で環境変数を注入
    //test
    @Value("${smaregi.api.client-id}")
    private String clientId;

    @Value("${smaregi.api.client-secret}")
    private String clientSecret;

    @Value("${smaregi.api.contract-id}")
    private String contractId;

    @Value("${app.vendor.id}")
    private String appVendor;

    @Test
    void 環境変数が正しく読み込まれているか確認() {
        // 1. 値がnullでないかチェック（読み込めていなければここで落ちます）
        assertNotNull(clientId, "クライアントIDが読み込めていません");
        assertNotNull(clientSecret, "クライアントシークレットが読み込めていません");
        assertNotNull(contractId, "契約IDが読み込めていません");
        assertNotNull(appVendor, "アプリ使用業者IDが読み込めていません");

        // 2. コンソールに出力して目視確認
        System.out.println("============================================");
        System.out.println("【設定値確認】");
        System.out.println("Contract ID   : " + contractId);
        System.out.println("Client ID     : " + clientId);
        
        // セキュリティのため、シークレットは最初の3文字だけ表示して残りは伏せ字にする
        String maskedSecret = (clientSecret != null && clientSecret.length() > 3) 
                              ? clientSecret.substring(0, 3) + "********" 
                              : "****";
        System.out.println("Client Secret : " + maskedSecret);
        System.out.println("App Vendor : " + appVendor);
        System.out.println("============================================");
    }
}