package com.beauty.beauty_sales_dwh.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.beauty.beauty_sales_dwh.config.AppVendorProperties;
import com.beauty.beauty_sales_dwh.domain.CustomerRawData;
import com.fasterxml.jackson.databind.ObjectMapper;

class CustomerRawDataProcessorTest {

    // テスト対象
    private CustomerRawDataProcessor processor;

    @BeforeEach
    void setUp() {
        // 1. 依存するコンポーネントを準備
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 設定クラスはただのJavaクラス(POJO)として扱い、テスト用の値をセットします
        AppVendorProperties properties = new AppVendorProperties();
        properties.setId("100"); // テストでは会社IDを「100」とします

        // 2. テスト対象クラスをインスタンス化 (コンストラクタ注入)
        processor = new CustomerRawDataProcessor(objectMapper, properties);
    }

    @Test
    @DisplayName("Mapデータが正しくJSON文字列に変換され、会社IDが付与されること")
    void testProcess() throws Exception {
        // --- 準備 (Given) ---
        // APIから返ってくる想定のMapデータを作成
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("customerId", "C001");
        inputMap.put("name", "テスト花子");
        inputMap.put("point", 500);

        // --- 実行 (When) ---
        CustomerRawData result = processor.process(inputMap);

        // --- 検証 (Then) ---
        // 1. 結果がnullでないこと
        assertThat(result).isNotNull();

        // 2. 会社IDが、設定クラスに入れた「100」になっていること
        assertThat(result.getCompanyId()).isEqualTo(100);

        // 3. JSONBodyが正しく生成されていること
        // JSON文字列の中に、セットしたキーと値が含まれているか確認
        assertThat(result.getJsonBody())
                .contains("\"customerId\":\"C001\"")
                .contains("\"name\":\"テスト花子\"")
                .contains("500");

        // コンソールに出力して目視確認
        System.out.println("変換後のCompanyID: " + result.getCompanyId());
        System.out.println("変換後のJSON: " + result.getJsonBody());
    }
}