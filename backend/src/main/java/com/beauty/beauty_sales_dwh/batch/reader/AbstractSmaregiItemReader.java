package com.beauty.beauty_sales_dwh.batch.reader;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
//import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.core.ParameterizedTypeReference;
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
public abstract class AbstractSmaregiItemReader implements ItemReader<Map<String, Object>> , StepExecutionListener{

    private final RestTemplate restTemplate;
    protected String accessToken;
    private int page = 1;
    private Iterator<Map<String, Object>> currentIterator;

    protected AbstractSmaregiItemReader(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    protected abstract String getApiUrl(int page);

    //@BeforeStep
    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.accessToken = (String) stepExecution.getJobExecution()
                .getExecutionContext().get(SmaregiAuthTasklet.KEY_ACCESS_TOKEN);

        if (this.accessToken == null) {
            throw new RuntimeException("アクセストークンが見つかりません。");
        }
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
        String url = getApiUrl(page);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        log.debug("API Fetch: {}", url);

        try {
            // ParameterizedTypeReference を使用して List<Map<String, Object>> 型で受け取る
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> rows = response.getBody();

            // nullチェックと空チェックのみを行う 
            if (rows != null && !rows.isEmpty()) {
                currentIterator = rows.iterator();
                page++; // データが取れたので次ページへ
            } else {
                // データが空（[]）なら終了
                currentIterator = null;
            }

        } catch (Exception e) {
            log.error("API Error: {}", e.getMessage());
            throw new RuntimeException("API取得失敗: " + url, e);
        }
    }
}