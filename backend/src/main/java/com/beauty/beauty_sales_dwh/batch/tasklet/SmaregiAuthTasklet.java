package com.beauty.beauty_sales_dwh.batch.tasklet;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.beauty.beauty_sales_dwh.config.SmaregiApiProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Step 1: スマレジAPI認証タスクレット
 * アクセストークンを取得し、JobExecutionContextに保存します。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SmaregiAuthTasklet implements Tasklet {

    private final SmaregiApiProperties properties; // 設定クラス(ID/PW等)
    private final RestTemplate restTemplate;       // HTTPクライアント

    // コンテキストに保存する際のキー名（Step 2のReaderでこのキーを使って取り出します）
    public static final String KEY_ACCESS_TOKEN = "SMAREGI_ACCESS_TOKEN";

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        
        log.info("Step 1: スマレジAPI認証を開始します...");

        // 1. Authorizationヘッダーの作成 (Basic認証: Base64(clientId:clientSecret))
        String authString = properties.getClientId() + ":" + properties.getClientSecret();
        String base64Auth = Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + base64Auth);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 2. リクエストボディの作成 (grant_type, scope)
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("scope", properties.getScope()); // pos.customers:read など

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        // 3. トークン発行APIのエンドポイントURL構築
        String tokenUrl = properties.getUrl() + "/app/" + properties.getContractId() + "/token";
        
        try {
            // 4. APIコール実行
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, requestEntity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 5. レスポンスから access_token を抽出
                String accessToken = (String) response.getBody().get("access_token");
                
                if (accessToken == null) {
                    throw new RuntimeException("アクセストークンがレスポンスに含まれていません。");
                }

                // 6. 【重要】JobExecutionContext（共有メモリ）にトークンを保存
                // これにより、後続のStep 2 (Reader) からこのトークンを参照可能になります。
                chunkContext.getStepContext().getStepExecution().getJobExecution()
                        .getExecutionContext().put(KEY_ACCESS_TOKEN, accessToken);
                
                log.info("認証成功。アクセストークンを取得しました。次ステップへ進みます。");
                return RepeatStatus.FINISHED; // 処理完了
                
            } else {
                throw new RuntimeException("認証失敗: ステータスコード " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("スマレジ認証中にエラーが発生しました: URL={}, Error={}", tokenUrl, e.getMessage());
            throw e; // Job全体を失敗させるために例外を再スロー
        }
    }
}