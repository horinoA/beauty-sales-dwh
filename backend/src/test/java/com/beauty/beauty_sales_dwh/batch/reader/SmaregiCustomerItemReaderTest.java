package com.beauty.beauty_sales_dwh.batch.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.beauty.beauty_sales_dwh.batch.tasklet.SmaregiAuthTasklet;
import com.beauty.beauty_sales_dwh.config.AppVendorProperties;
import com.beauty.beauty_sales_dwh.config.SmaregiApiProperties;
import com.beauty.beauty_sales_dwh.mapper.RawCustomerMapper;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class) // ★1. JUnit5とMockitoを連携
public class SmaregiCustomerItemReaderTest {

    private SmaregiCustomerItemReader reader;
    private MockRestServiceServer mockServer; // APIサーバーのふりをする人
    @Mock
    private RawCustomerMapper rawCustomerMapper; // DBのふりをする人

    @BeforeEach
    void setUp() {
        // 1. RestTemplate と MockServer の準備
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();

        // 2. 設定クラス (Properties) の準備
        SmaregiApiProperties properties = new SmaregiApiProperties();
        properties.setBaseUrl("https://api.smaregi.dev");
        properties.setContractId("test_contract");

        AppVendorProperties vendorProperties = new AppVendorProperties();
        vendorProperties.setId("4096"); // テスト用の会社ID (String想定)

        // 3. Mapper (DB) のモック化
        rawCustomerMapper = mock(RawCustomerMapper.class);

        // 4. テスト対象の Reader をインスタンス化
        reader = new SmaregiCustomerItemReader(restTemplate, properties, rawCustomerMapper,vendorProperties);
    }

    @Test
    @DisplayName("正常系: APIから2件のデータを取得し、1件ずつReadできること")
    void testRead_Success() throws Exception {
        // --- 準備 (Given) ---

        // A. JobExecutionContext にトークンをセット
        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution();
        ExecutionContext executionContext = stepExecution.getJobExecution().getExecutionContext();
        executionContext.put(SmaregiAuthTasklet.KEY_ACCESS_TOKEN, "dummy_access_token");

        // B. DBから取得する「最終更新日時」を定義
        // 例: 2023-01-01 10:00:00 (+09:00)
        OffsetDateTime lastImportedAt = OffsetDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneOffset.ofHours(9));
        when(rawCustomerMapper.findMaxFetchedAt(4096L)).thenReturn(lastImportedAt);

        // 日付を文字列化 パターン: yyyy-MM-dd'T'HH:mm:ssXXX (XXXはタイムゾーンオフセット)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
        String dateStr = lastImportedAt.format(formatter);
        // それをURLエンコードする (2023-01-01T10%3A00%3A00%2B09%3A00)
        String encodedDateParam = "upd_date_time-from=" + URLEncoder.encode(dateStr, StandardCharsets.UTF_8);

        // C. APIのモック定義 (ここが重要！)
        // 想定されるリクエストURL: .../pos/customers?limit=1000&page=1&upd_date_time-from=2023-01-01T10:00:00
        // レスポンス: 顧客2人分のJSON配列
        String mockJsonResponse = "[{\"customerId\": \"101\", \"name\": \"User A\"}, {\"customerId\": \"102\", \"name\": \"User B\"}]";

        // --- 1回目のAPIコール (Page 1) ---
        mockServer.expect(requestTo(containsString("/pos/customers"))) // URLの一部がマッチすればOK
                .andExpect(requestTo(containsString("page=1")))       // ページ番号確認
                .andExpect(requestTo(containsString(encodedDateParam))) 
                //.andExpect(requestTo(containsString("upd_date_time-from=2023-01-01T10:00:00"))) // 日付パラメータ確認(URLエンコード注意)
                .andRespond(withSuccess(mockJsonResponse, MediaType.APPLICATION_JSON));
        
        // --- 2回目のAPIコール (Page 2)  ---
        // データがなくなったことを示すため、空の配列 "[]" を返します
        // 「データが空なら終了」なので、MockServerに2ページ目の「空配列」を定義してあげるのが丁寧
        mockServer.expect(requestTo(containsString("page=2")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));


        // --- 実行 (When) & 検証 (Then) ---

        // 1. Step開始前の初期化 (beforeStep) を手動で呼び出す
        reader.beforeStep(stepExecution);

        // 2. read() 1回目 -> User A が取れるはず
        Map<String, Object> item1 = reader.read();
        assertThat(item1).isNotNull();
        assertThat(item1.get("customerId")).isEqualTo("101");
        assertThat(item1.get("name")).isEqualTo("User A");

        // 3. read() 2回目 -> User B が取れるはず (APIはもう叩かず、メモリ内から返す)
        Map<String, Object> item2 = reader.read();
        assertThat(item2).isNotNull();
        assertThat(item2.get("customerId")).isEqualTo("102");

        // 4. read() 3回目 -> もうデータがないので null (または次ページ取得トライ)
        // ※今回は「1ページ目でデータ終了」の挙動を確認するため、次ページのリクエストをExpectしていない状態で
        //   APIを叩こうとするとMockServerがエラーを吐くか、nullになるかを確認

        Map<String, Object> item3 = reader.read();
        assertThat(item3).isNull(); // 終了

        // 5. 全てのAPIリクエストが想定通り行われたか検証
        mockServer.verify();
    }
}