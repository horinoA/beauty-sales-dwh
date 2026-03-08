package com.beauty.beauty_sales_dwh.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.beauty.beauty_sales_dwh.config.SmaregiApiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
public class SmaregiTransactionIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("importSmaregiTransactionJob")
    private Job importSmaregiTransactionJob;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SmaregiApiProperties smaregiApiProperties;

    private MockRestServiceServer mockServer;

    @BeforeEach
    public void setUp() {
        jobLauncherTestUtils.setJob(importSmaregiTransactionJob);
        // 順序を無視するように設定
        mockServer = MockRestServiceServer.bindTo(restTemplate)
                .ignoreExpectOrder(true)
                .build();
        // 関連テーブルのクリーニング
        jdbcTemplate.execute("TRUNCATE TABLE raw.transactions, raw.transaction_details RESTART IDENTITY");
        jdbcTemplate.execute("TRUNCATE TABLE dwh.fact_sales, dwh.fact_sales_details RESTART IDENTITY");
    }

    @Test
    @DisplayName("統合テスト: 取引データの取得から明細展開、DWH変換までが正常に動作すること")
    public void testImportTransactionJob() throws Exception {
        // --- API Mocks ---
        mockAuth();
        
        // パーティショナーが生成する期間を計算
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusMonths(3).withDayOfMonth(1);
        
        LocalDate current = startDate;
        while (current.isBefore(today) || current.isEqual(today)) {
            LocalDate endOfMonth = current.withDayOfMonth(current.lengthOfMonth());
            LocalDate currentEnd = endOfMonth.isAfter(today) ? today : endOfMonth;
            
            // 2025-12月分だけデータを返し、それ以外は空を返す
            if (current.getMonthValue() == 12 && current.getYear() == 2025) {
                mockTransactionsApi(current, currentEnd, 2); // 通常 + 取消 の2件
            } else {
                mockTransactionsApi(current, currentEnd, 0);
            }
            
            current = current.plusMonths(1).withDayOfMonth(1);
        }

        // --- Job Parameters ---
        JobParameters params = new JobParametersBuilder()
                .addLong("companyId", 1L)
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        // --- Job Execution ---
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);

        // --- Assertions ---
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        // 1. raw.transactions の検証
        System.out.println("DEBUG: Current Vendor ID: " + smaregiApiProperties.getContractId()); // Just a guess, let's use vendorProperties if I could but I don't have it here
        List<Map<String, Object>> transactions = jdbcTemplate.queryForList("SELECT * FROM raw.transactions ORDER BY (json_body->>'transactionHeadId')");
        System.out.println("DEBUG: raw.transactions size: " + transactions.size());
        for (Map<String, Object> row : transactions) {
            System.out.println("DEBUG: raw.transactions row: id=" + row.get("transaction_id") + 
                               ", company_id=" + row.get("app_company_id") + 
                               ", updDateTime=" + objectMapper.readTree(row.get("json_body").toString()).get("updDateTime"));
        }
        assertThat(transactions).hasSize(2);
        
        // 通常データ (1001) の展開済みフラグ確認
        assertThat(transactions.get(0).get("details_extracted")).isEqualTo(true);

        // 2. dwh.fact_sales の検証
        List<Map<String, Object>> factSales = jdbcTemplate.queryForList("SELECT * FROM dwh.fact_sales ORDER BY transaction_head_id");
        assertThat(factSales).hasSize(2);
        
        // 通常データ (1001)
        Map<String, Object> sale1001 = factSales.get(0);
        System.out.println("DEBUG: transaction_head_id: " + sale1001.get("transaction_head_id"));
        System.out.println("DEBUG: amount_total value: " + sale1001.get("amount_total"));
        System.out.println("DEBUG: amount_total type: " + (sale1001.get("amount_total") != null ? sale1001.get("amount_total").getClass().getName() : "null"));
        
        assertThat(sale1001.get("transaction_head_id")).isEqualTo("1001");
        assertThat(((Number) sale1001.get("amount_total")).intValue()).isEqualTo(11550);
        assertThat(sale1001.get("transaction_type")).isEqualTo("SALES");
        assertThat(sale1001.get("is_void")).isEqualTo(false);

        // 取消データ (1002)
        Map<String, Object> sale1002 = factSales.get(1);
        assertThat(sale1002.get("transaction_head_id")).isEqualTo("1002");
        assertThat(sale1002.get("is_void")).isEqualTo(true); // cancelDivision=1 により true

        // 3. dwh.fact_sales_details の検証
        List<Map<String, Object>> factDetails1001 = jdbcTemplate.queryForList(
            "SELECT * FROM dwh.fact_sales_details WHERE transaction_head_id = '1001' ORDER BY transaction_detail_id");
        assertThat(factDetails1001).hasSize(4);

        // 返品明細 (transactionDetailDivision=2) の category_type 検証
        Map<String, Object> refundDetail = factDetails1001.get(3); // グリースワックス
        assertThat(refundDetail.get("product_name")).isEqualTo("グリースワックス(ハード)");
        assertThat(refundDetail.get("category_type")).isEqualTo("REFUND");

        mockServer.verify();
    }

    private void mockAuth() {
        mockServer.expect(requestTo(containsString("/token")))
            .andExpect(method(POST))
            .andRespond(withSuccess("{\"access_token\":\"mock_test_token\"}", MediaType.APPLICATION_JSON));
    }

    /**
     * Reader側と同じロジックでURLを構築し、モックを定義します
     */
    private void mockTransactionsApi(LocalDate from, LocalDate to, int count) throws Exception {
        ZoneOffset jst = ZoneOffset.ofHours(9);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
        
        String fromFormatted = from.atTime(LocalTime.MIN).atOffset(jst).format(formatter);
        String toFormatted = to.atTime(LocalTime.MAX).atOffset(jst).format(formatter);
        
        String encodedFrom = URLEncoder.encode(fromFormatted, StandardCharsets.UTF_8);
        String encodedTo = URLEncoder.encode(toFormatted, StandardCharsets.UTF_8);

        String baseUrl = String.format("%s/%s/pos/transactions/", 
                smaregiApiProperties.getBaseUrl(), 
                smaregiApiProperties.getContractId());

        String jsonResponse = (count > 0) ? "[" + getSampleTransactionJson() + "]" : "[]";

        // Page 1
        String urlPage1 = String.format("%s?transaction_date_time-from=%s&transaction_date_time-to=%s&page=1&limit=100&with_details=all&sort=transaction_datetime:asc",
                baseUrl, encodedFrom, encodedTo);
        
        mockServer.expect(requestTo(urlPage1))
            .andExpect(method(GET))
            .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        // Page 2 (終了確認用)
        if (count > 0) {
            String urlPage2 = String.format("%s?transaction_date_time-from=%s&transaction_date_time-to=%s&page=2&limit=100&with_details=all&sort=transaction_datetime:asc",
                    baseUrl, encodedFrom, encodedTo);
            
            mockServer.expect(requestTo(urlPage2))
                .andExpect(method(GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        }
    }

    private String getSampleTransactionJson() {
        return """
        {
          "transactionHeadId": "1001",
          "transactionHeadDivision": "1",
          "cancelDivision": "0",
          "subtotal": "11550",
          "total": "11550",
          "taxInclude": "11550",
          "taxExclude": "0",
          "subtotalDiscountPrice": "0",
          "commission": "0",
          "carriage": "0",
          "pointDiscount": "0",
          "customerId": "1",
          "terminalTranDateTime": "2025-12-02T14:30:00+09:00",
          "updDateTime": "2025-12-02T14:30:00+09:00",
          "staffId": "2",
          "storeId": "1",
          "details": [
            {
              "transactionHeadId": "1001",
              "transactionDetailId": "1",
              "productId": "8000001",
              "productName": "デザインカット",
              "salesPrice": "5500",
              "taxDivision": "1",
              "transactionDetailDivision": "1",
              "quantity": "1",
              "updDateTime": "2025-12-02T14:30:00+09:00"
            },
            {
              "transactionHeadId": "1001",
              "transactionDetailId": "2",
              "productId": "8000005",
              "productName": "リタッチカラー",
              "salesPrice": "5500",
              "taxDivision": "1",
              "transactionDetailDivision": "1",
              "quantity": "1",
              "updDateTime": "2025-12-02T14:30:00+09:00"
            },
            {
              "transactionHeadId": "1001",
              "transactionDetailId": "3",
              "productId": "8000012",
              "productName": "指名料",
              "salesPrice": "550",
              "taxDivision": "1",
              "transactionDetailDivision": "1",
              "quantity": "1",
              "updDateTime": "2025-12-02T14:30:00+09:00"
            },
            {
              "transactionHeadId": "1001",
              "transactionDetailId": "4",
              "productId": "8000019",
              "productName": "グリースワックス(ハード)",
              "salesPrice": "-2420",
              "taxDivision": "1",
              "transactionDetailDivision": "2",
              "quantity": "1",
              "updDateTime": "2025-12-02T14:30:00+09:00"
            }
          ]
        },
        {
          "transactionHeadId": "1002",
          "transactionHeadDivision": "1",
          "cancelDivision": "1",
          "subtotal": "5500",
          "total": "5500",
          "taxInclude": "5500",
          "taxExclude": "0",
          "customerId": "1",
          "terminalTranDateTime": "2025-12-02T15:00:00+09:00",
          "updDateTime": "2025-12-02T15:00:00+09:00",
          "staffId": "2",
          "storeId": "1",
          "details": [
            {
              "transactionHeadId": "1002",
              "transactionDetailId": "1",
              "productId": "8000001",
              "productName": "デザインカット",
              "salesPrice": "5500",
              "taxDivision": "1",
              "transactionDetailDivision": "1",
              "quantity": "1",
              "updDateTime": "2025-12-02T15:00:00+09:00"
            }
          ]
        }
        """;
    }
}
