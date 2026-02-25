package com.beauty.beauty_sales_dwh.batch.tasklet;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.beauty.beauty_sales_dwh.domain.TransactionDetailRawData;
import com.beauty.beauty_sales_dwh.domain.TransactionRawData;
import com.beauty.beauty_sales_dwh.mapper.RawTransactionDetailMapper;
import com.beauty.beauty_sales_dwh.mapper.RawTransactionMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 取引データから明細データを抽出し、raw.transaction_details に保存するタスクレット。
 * Step A で取得した親レコードの json_body 内にある details 配列を展開します。
 */
@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class TransactionDetailsExtractTasklet implements Tasklet {

    private final RawTransactionMapper rawTransactionMapper;
    private final RawTransactionDetailMapper rawTransactionDetailMapper;
    private final ObjectMapper objectMapper;

    @Value("#{jobParameters['companyId']}")
    private Long companyId;

    private static final int FETCH_LIMIT = 100;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("取引明細の抽出処理を開始します。companyId: {}", companyId);

        // 未処理の取引データを取得 (details_extracted = FALSE のもの)
        List<TransactionRawData> transactions = rawTransactionMapper.findUnprocessedTransactions(companyId, FETCH_LIMIT);

        if (transactions.isEmpty()) {
            log.info("処理対象の取引データがありません。");
            return RepeatStatus.FINISHED;
        }

        List<Long> processedIds = new ArrayList<>();

        for (TransactionRawData transaction : transactions) {
            if (transaction == null) {
                log.warn("取引データが null です。スキップします。");
                continue;
            }
            try {
                processTransaction(transaction);
                processedIds.add(transaction.getTransactionId());
            } catch (Exception e) {
                log.error("取引データの展開中にエラーが発生しました。 transactionId: {}", transaction.getTransactionId(), e);
                // 1件のエラーで全体を止めず、ログ出力して継続
            }
        }

        // 展開済みフラグを一括更新
        if (!processedIds.isEmpty()) {
            rawTransactionMapper.markTransactionsAsProcessed(processedIds);
            log.info("{} 件の取引データの明細展開が完了しました。", processedIds.size());
        }

        // まだデータがあるかもしれないので、継続を指示
        return RepeatStatus.CONTINUABLE;
    }

    /**
     * 単一の取引データから明細を抽出して保存する
     */
    private void processTransaction(TransactionRawData transaction) throws Exception {
        JsonNode rootNode = objectMapper.readTree(transaction.getJsonBody());
        JsonNode detailsNode = rootNode.get("details");

        if (detailsNode == null || !detailsNode.isArray()) {
            log.warn("取引データに details 配列が含まれていません。 transactionId: {}", transaction.getTransactionId());
            return;
        }

        long rowNumber = 1;
        for (JsonNode detail : detailsNode) {
            TransactionDetailRawData detailData = TransactionDetailRawData.builder()
                    .companyId(transaction.getCompanyId())
                    .transactionHeadId(transaction.getTransactionId().toString())
                    .jsonBody(detail.toString())
                    .fileName(rootNode.get("transactionHeadId").toString().replaceAll("[^0-9]", "")) // トレーサビリティ用
                    .rowNumber(rowNumber++)
                    .build();

            rawTransactionDetailMapper.insertRawTransactionDetail(detailData);
        }
    }
}
