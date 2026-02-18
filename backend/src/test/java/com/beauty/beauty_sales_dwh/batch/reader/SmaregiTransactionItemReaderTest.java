package com.beauty.beauty_sales_dwh.batch.reader;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.StepScopeTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;

import com.beauty.beauty_sales_dwh.batch.tasklet.SmaregiAuthTasklet;
import com.beauty.beauty_sales_dwh.config.SmaregiApiProperties;

@SpringBootTest
@SpringBatchTest
@AutoConfigureMockRestServiceServer
@ActiveProfiles("test")
class SmaregiTransactionItemReaderTest {

    @Autowired
    private SmaregiTransactionItemReader reader;

    @Autowired
    private MockRestServiceServer mockServer;

    @MockBean
    private SmaregiApiProperties properties;

    private StepExecution stepExecution;

    @BeforeEach
    void setUp() {
        // プロパティのモック設定
        org.mockito.Mockito.when(properties.getBaseUrl()).thenReturn("https://api.smaregi.jp");
        org.mockito.Mockito.when(properties.getContractId()).thenReturn("sb_test");

        // JobParameters の設定
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("from", "2024-01-01")
                .addString("to", "2024-01-31")
                .toJobParameters();

        // Spring Batchのユーティリティを使って StepExecution を作成
        stepExecution = MetaDataInstanceFactory.createStepExecution(jobParameters);
        stepExecution.getJobExecution().getExecutionContext().put(SmaregiAuthTasklet.KEY_ACCESS_TOKEN, "test-token");
    }

    @Test
    void testRead_Success() throws Exception {
        // StepScope の中で Reader を実行する
        StepScopeTestUtils.doInStepScope(stepExecution, () -> {
            // beforeStep を手動で呼んで初期化
            reader.beforeStep(stepExecution);

            // 期待されるURLの検証
            mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("transaction_datetime-from=2024-01-01T00%3A00%3A00%2B09%3A00")))
                    .andExpect(requestTo(org.hamcrest.Matchers.containsString("transaction_datetime-to=2024-01-31T23%3A59%3A59%2B09%3A00")))
                    .andExpect(header("Authorization", "Bearer test-token"))
                    .andRespond(withSuccess("[{\"transactionId\": \"123\"}]", MediaType.APPLICATION_JSON));

            // 1回目の read
            Map<String, Object> item = reader.read();
            assertNotNull(item);
            assertEquals("123", (String) item.get("transactionId"));

            // 2回目の read (ページングの確認)
            mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("page=2")))
                    .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
            
            item = reader.read();
            assertNull(item);

            mockServer.verify();
            return null;
        });
    }
}
