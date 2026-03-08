package com.beauty.beauty_sales_dwh.batch.reader;

import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemReader;
import org.springframework.core.ParameterizedTypeReference; // ★これが必要です
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.beauty.beauty_sales_dwh.batch.tasklet.SmaregiAuthTasklet;

import lombok.extern.slf4j.Slf4j;

/**
 * スマレジAPI読み込みの共通親クラス
 * APIレスポンスが配列 [ {...}, {...} ] の形式であることを前提とします。
 */
@Slf4j
public abstract class AbstractSmaregiItemReader implements ItemReader<Map<String, Object>>, StepExecutionListener {

    private final RestTemplate restTemplate;
    protected String accessToken;
    private int page = 1;
    private Iterator<Map<String, Object>> currentIterator;

    protected AbstractSmaregiItemReader(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    //  String ではなく URI を返すように定義
    protected abstract URI getApiUrl(int page);

    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.accessToken = (String) stepExecution.getJobExecution()
                .getExecutionContext().get(SmaregiAuthTasklet.KEY_ACCESS_TOKEN);

        if (this.accessToken == null || this.accessToken.isEmpty()) {
            log.error("アクセストークンが ExecutionContext に見つかりません。キー: {}", SmaregiAuthTasklet.KEY_ACCESS_TOKEN);
            throw new RuntimeException("アクセストークンが見つかりません。");
        }
        log.info("Reader initialized with token. Length: {}", accessToken.length());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return null;
    }
    
    @Override
    public Map<String, Object> read() {
        if (currentIterator == null || !currentIterator.hasNext()) {
            fetchNextPage();
        }
        if (currentIterator != null && currentIterator.hasNext()) {
            return currentIterator.next();
        }
        return null;
    }

    // レスポンスをList<Map<String, Object>> 型で受け取る
    private void fetchNextPage() {
        // ★変更: StringではなくURI型で受け取る
        URI uri = getApiUrl(page);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Content-Type", "application/json");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        log.info("API Request URL: {}", uri);

        try {
            // URIを渡しつつ、ParameterizedTypeReference で List型として受け取る
            // これにより、自動的にJSONがList<Map>に変換されます
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> rows = response.getBody();

            // nullチェックと空チェック
            if (rows != null && !rows.isEmpty()) {
                currentIterator = rows.iterator();
                page++; // データが取れたので次ページへ
            } else {
                // データが空（[]）なら終了
                currentIterator = null;
            }

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("API Error: HTTP Status {} - Response Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("API取得失敗 (HTTPエラー): " + uri, e);
        } catch (Exception e) {
            log.error("API Error: {}", e.getMessage(), e);
            throw new RuntimeException("API取得失敗: " + uri, e);
        }
    }
}