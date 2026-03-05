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
    }

    @Test
    @DisplayName("統合テスト: 取引データの取得から明細展開までが正常に動作すること")
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
                mockTransactionsApi(current, currentEnd, 1);
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
        List<Map<String, Object>> transactions = jdbcTemplate.queryForList("SELECT * FROM raw.transactions");
        assertThat(transactions).isNotEmpty();
        
        Map<String, Object> head = transactions.get(0);
        assertThat(head.get("details_extracted")).isEqualTo(true); // 展開済みフラグ
        
        Long transactionId = ((Number) head.get("transaction_id")).longValue();

        // 2. raw.transaction_details の検証、file_name列にraw.transactionのIDが入る
        List<Map<String, Object>> details = jdbcTemplate.queryForList(
            "SELECT * FROM raw.transaction_details WHERE file_name = ?", transactionId.toString());
        assertThat(details).hasSizeGreaterThanOrEqualTo(1);

        // 具体的な中身の検証
        boolean foundCut = false;
        for (Map<String, Object> detail : details) {
            Object jsonBodyObj = detail.get("json_body");
            String jsonBody = (jsonBodyObj != null) ? jsonBodyObj.toString() : null;
            JsonNode node = objectMapper.readTree(jsonBody);
            if ("デザインカット".equals(node.get("productName").asText())) {
                foundCut = true;
                assertThat(((Number) detail.get("row_number")).longValue()).isEqualTo(1L);
                assertThat(node.get("salesPrice").asText()).isEqualTo("5500");
            }
        }
        assertThat(foundCut).isTrue();

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

        // Page 2 (データがある場合のみ終了確認用に空配列を返す)
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
              "quantity": "1"
            },
            {
              "transactionHeadId": "1001",
              "transactionDetailId": "2",
              "productId": "8000005",
              "productName": "リタッチカラー",
              "salesPrice": "5500",
              "taxDivision": "1",
              "transactionDetailDivision": "1",
              "quantity": "1"
            },
            {
              "transactionHeadId": "1001",
              "transactionDetailId": "3",
              "productId": "8000012",
              "productName": "指名料",
              "salesPrice": "550",
              "taxDivision": "1",
              "transactionDetailDivision": "1",
              "quantity": "1"
            },
            {
              "transactionHeadId": "1001",
              "transactionDetailId": "4",
              "productId": "8000019",
              "productName": "グリースワックス(ハード)",
              "salesPrice": "2420",
              "taxDivision": "1",
              "transactionDetailDivision": "2",
              "quantity": "1"
            }
          ]
        }
        """;
    }
}
