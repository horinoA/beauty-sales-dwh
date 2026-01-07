package com.beauty.beauty_sales_dwh.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.beauty.beauty_sales_dwh.mapper.CustomerTransformMapper;

@SpringBatchTest // 1. Batchテスト用ユーティリティを有効化
@SpringBootTest  // 2. アプリ全体のコンテキストを起動
@ActiveProfiles("test")
public class SmaregiBatchIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils; // Jobを実行するツール

    @Autowired
    private RestTemplate restTemplate; // 本番コードで使われているRestTemplate

    @Autowired
    private JdbcTemplate jdbcTemplate; // DB掃除用

    @Autowired
    private CustomerTransformMapper transformMapper; // 結果確認用

    private MockRestServiceServer mockServer;

    @BeforeEach
    public void setUp() {
        // MockServerのセットアップ
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();

        // テーブルを綺麗にする (テスト間の干渉を防ぐ)
        jdbcTemplate.execute("TRUNCATE TABLE raw.customers");
        jdbcTemplate.execute("TRUNCATE TABLE dwh.dim_customers");
    }

    @Test
    @DisplayName("統合テスト: 認証からデータ整形までJob全体が正常終了すること（2件データ）")
    public void testFullJobExecution() throws Exception {
        // ==========================================
        // 1. APIモックの定義
        // ==========================================

        // --- Step 1: 認証 ---
        mockServer.expect(requestTo(containsString("/token")))
                .andExpect(method(POST))
                .andRespond(withSuccess("{\"access_token\":\"mock_test_token\"}", MediaType.APPLICATION_JSON));

        // --- Step 2: データ取得 (Page 1) ---
        // ★ご提示いただいた2件のJSONデータを使用
        // (JavaのText Block機能で貼り付けます)
        String customersJson = """
            [
              {
                "customerId": "6",
                "customerCode": "10007",
                "firstName": "健",
                "lastName": "田中",
                "phoneNumber": "06-7777-9999",
                "sex": "1",
                "mailReceiveFlag": "1",
                "status": "0",
                "insDateTime": "2026-01-02T22:57:03+09:00",
                "updDateTime": "2026-01-05T10:15:03+09:00"
              },
              {
                "customerId": "16",
                "customerCode": "10020",
                "firstName": "絵里",
                "lastName": "横山",
                "phoneNumber": "03-4444-5555",
                "sex": "2",
                "mailReceiveFlag": "1",
                "status": "0",
                "insDateTime": "2026-01-05T10:15:03+09:00",
                "updDateTime": "2026-01-05T10:15:03+09:00"
              }
            ]
        """;

        // page=1 でこの2件を返す
        mockServer.expect(requestTo(containsString("/pos/customers")))
                .andExpect(method(GET))
                .andExpect(requestTo(containsString("page=1")))
                .andRespond(withSuccess(customersJson, MediaType.APPLICATION_JSON));

        // --- Step 2: データ取得 (Page 2) -> 空配列 ---
        mockServer.expect(requestTo(containsString("/pos/customers")))
                .andExpect(method(GET))
                .andExpect(requestTo(containsString("page=2")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));


        // ==========================================
        // 2. Jobの実行
        // ==========================================
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();


        // ==========================================
        // 3. 結果の検証
        // ==========================================

        // A. Job成功確認
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        // B. Dimテーブル(dim.customers)に「2件」入っているか確認
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM dwh.dim_customers", Integer.class); 
        assertThat(count).isEqualTo(2);

        // C. 1人目(田中 健, ID=6) のデータ検証
        // Mapで取得して中身をチェック
        var user1 = jdbcTemplate.queryForMap(
            "SELECT customer_name ,phone_number FROM dwh.dim_customers WHERE customer_id = '6'");
        
        assertThat(user1.get("customer_name")).isEqualTo("田中_健");
        assertThat(user1.get("phone_number")).isEqualTo("06-7777-9999");; 

        // D. 2人目(横山 絵里, ID=16) のデータ検証
        var user2 = jdbcTemplate.queryForMap(
            "SELECT customer_name ,phone_number FROM dwh.dim_customers WHERE customer_id = '16'");
        
        assertThat(user2.get("customer_name")).isEqualTo("横山_絵里");

        // E. Mock検証
        mockServer.verify();
    }
}