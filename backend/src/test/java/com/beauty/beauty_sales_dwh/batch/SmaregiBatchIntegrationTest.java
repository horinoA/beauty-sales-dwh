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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

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

import com.beauty.beauty_sales_dwh.config.SmaregiApiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
public class SmaregiBatchIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

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
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        jdbcTemplate.execute("TRUNCATE TABLE raw.customers, raw.categories, raw.category_groups RESTART IDENTITY");
        jdbcTemplate.execute("TRUNCATE TABLE dwh.dim_customers RESTART IDENTITY");
        
    }

    @Test
    @DisplayName("統合テスト: 認証からデータ整形までJob全体が正常終了すること（顧客データ2件）")
    public void testFullJobExecution() throws Exception {
        // --- API Mocks ---
        mockAuth();
        mockCustomersApi(2); // 2件の顧客データを返すモック
        mockCategoriesApi(0); // 0件のカテゴリデータを返すモック
        mockCategoryGroupsApi(0); // 0件のカテゴリグループデータを返すモック

        // --- Job Execution ---
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // --- Assertions ---
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        Integer customerCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dwh.dim_customers", Integer.class);
        assertThat(customerCount).isEqualTo(2);

        var user1 = jdbcTemplate.queryForMap("SELECT customer_name, phone_number FROM dwh.dim_customers WHERE customer_id = '6'");
        assertThat(user1.get("customer_name")).isEqualTo("田中_健");
        assertThat(user1.get("phone_number")).isEqualTo("06-7777-9999");

        mockServer.verify();
    }

    @Test
    @DisplayName("ステップテスト: カテゴリデータ取込が正常終了すること")
    public void testFetchCategoriesStep() throws Exception {
        // --- API Mocks ---
        mockAuth();
        mockCustomersApi(0); // 顧客APIは空
        mockCategoriesApi(2); // カテゴリAPIは2件返す
        mockCategoryGroupsApi(0); // カテゴリグループAPIは空

        // --- Job Execution ---
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // --- Assertions ---
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        // 各テーブルの件数確認
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM raw.customers", Integer.class)).isEqualTo(0);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM raw.categories", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM raw.category_groups", Integer.class)).isEqualTo(0);

        // 登録されたJSONの内容を確認
        String jsonBody = jdbcTemplate.queryForObject("SELECT json_body FROM raw.categories WHERE cat_id = 1", String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonBody);
        assertThat(jsonNode.get("categoryId").asText()).isEqualTo("101");
        assertThat(jsonNode.get("categoryName").asText()).isEqualTo("Cuts");
        
        mockServer.verify();
    }

    @Test
    @DisplayName("ステップテスト: カテゴリグループデータ取込が正常終了すること")
    public void testFetchCategoryGroupsStep() throws Exception {
        // --- API Mocks ---
        mockAuth();
        mockCustomersApi(0);
        mockCategoriesApi(0);
        mockCategoryGroupsApi(2); // カテゴリグループAPIは2件返す

        // --- Job Execution ---
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // --- Assertions ---
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        
        // 各テーブルの件数確認
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM raw.customers", Integer.class)).isEqualTo(0);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM raw.categories", Integer.class)).isEqualTo(0);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM raw.category_groups", Integer.class)).isEqualTo(2);

        // 登録されたJSONの内容を確認
        String jsonBody = jdbcTemplate.queryForObject("SELECT json_body FROM raw.category_groups WHERE cat_group_id = 1", String.class);
        JsonNode jsonNode = objectMapper.readTree(jsonBody);
        assertThat(jsonNode.get("categoryGroupId").asText()).isEqualTo("1");
        assertThat(jsonNode.get("categoryGroupName").asText()).isEqualTo("Technical");

        mockServer.verify();
    }


    // ==========================================
    // Helper methods for mocking APIs
    // ==========================================

    private void mockAuth() {
        mockServer.expect(requestTo(containsString("/token")))
            .andExpect(method(POST))
            .andRespond(withSuccess("{\"access_token\":\"mock_test_token\"}", MediaType.APPLICATION_JSON));
    }

    private void mockCustomersApi(int count) {
        String json = "[]";
        if (count == 2) {
            json = """
                [
                  {"customerId": "6", "customerCode": "10007", "firstName": "健", "lastName": "田中", "phoneNumber": "06-7777-9999", "sex": "1", "mailReceiveFlag": "1", "status": "0", "insDateTime": "2026-01-02T22:57:03+09:00", "updDateTime": "2026-01-05T10:15:03+09:00"},
                  {"customerId": "16", "customerCode": "10020", "firstName": "絵里", "lastName": "横山", "phoneNumber": "03-4444-5555", "sex": "2", "mailReceiveFlag": "1", "status": "0", "insDateTime": "2026-01-05T10:15:03+09:00", "updDateTime": "2026-01-05T10:15:03+09:00"}
                ]
            """;
        }
        String baseUrl = smaregiApiProperties.getBaseUrl() + "/" + smaregiApiProperties.getContractId() + "/pos/customers";

        // Since the test truncates the DB, the reader will use the default date.
        OffsetDateTime defaultDate = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(9));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
        String encodedDate = URLEncoder.encode(defaultDate.format(formatter), StandardCharsets.UTF_8);

        // Page 1 expectation
        String urlPage1 = String.format("%s?limit=1000&page=1&upd_date_time-from=%s", baseUrl, encodedDate);
        mockServer.expect(requestTo(urlPage1))
            .andExpect(method(GET))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
        
        // ONLY expect page 2 if data is returned for page 1
        if (count > 0) {
            // Page 2 expectation (returns empty)
            String urlPage2 = String.format("%s?limit=1000&page=2&upd_date_time-from=%s", baseUrl, encodedDate);
            mockServer.expect(requestTo(urlPage2))
                .andExpect(method(GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        }
    }

    private void mockCategoriesApi(int count) {
        String json = "[]";
        if (count > 0) {
            json = """
                [
                  {"categoryId": "101", "categoryName": "Cuts"},
                  {"categoryId": "102", "categoryName": "Colors"}
                ]
            """;
        }
        String baseUrl = smaregiApiProperties.getBaseUrl() + "/" + smaregiApiProperties.getContractId() + "/pos/categories";
        
        // Page 1
        mockServer.expect(requestTo(String.format("%s?limit=1000&page=1", baseUrl)))
            .andExpect(method(GET))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
        
        // ONLY expect page 2 if data is returned for page 1
        if (count > 0) {
            // Page 2
            mockServer.expect(requestTo(String.format("%s?limit=1000&page=2", baseUrl)))
                .andExpect(method(GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        }
    }

    private void mockCategoryGroupsApi(int count) {
        String json = "[]";
        if (count > 0) {
            json = """
                [
                  {"categoryGroupId": "1", "categoryGroupName": "Technical"},
                  {"categoryGroupId": "2", "categoryGroupName": "Retail"}
                ]
            """;
        }
        String baseUrl= smaregiApiProperties.getBaseUrl() + "/" + smaregiApiProperties.getContractId() + "/pos/category_groups";

        // Page 1
        mockServer.expect(requestTo(String.format("%s?limit=1000&page=1", baseUrl)))
            .andExpect(method(GET))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        // ONLY expect page 2 if data is returned for page 1
        if (count > 0) {
            // Page 2
            mockServer.expect(requestTo(String.format("%s?limit=1000&page=2", baseUrl)))
                .andExpect(method(GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        }
    }
}