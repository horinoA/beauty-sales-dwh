package com.beauty.beauty_sales_dwh.batch.partitioner;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import com.beauty.beauty_sales_dwh.mapper.RawTransactionMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 取引データの取得期間を月単位で分割するパーティショナー
 * 1. 初回実行時：過去3ヶ月分のパーティションを生成
 * 2. 継続実行時：最終取得日時から現在までの期間を分割して生成
 */
@Slf4j
@RequiredArgsConstructor
public class TransactionPeriodPartitioner implements Partitioner {

    private final RawTransactionMapper rawTransactionMapper;
    private final Long companyId;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        log.info("取引データの期間分割（パーティショニング）を開始します。CompanyID: {}", companyId);

        // 1. 最後にデータを取り込んだ日時を取得（RawTransactionMapperを利用）
        OffsetDateTime maxFetchedAt = rawTransactionMapper.findMaxFetchedAt(companyId);

        LocalDate startDate;
        if (maxFetchedAt == null) {
            // データがない場合は3ヶ月の月初から開始（合計約3パーティション）
            startDate = LocalDate.now().minusMonths(3).withDayOfMonth(1);
            log.info("初回取得として判定されました。開始日: {}", startDate);
        } else {
            // 既にある場合は最終取得日から開始
            startDate = maxFetchedAt.toLocalDate();
            log.info("差分取得として判定されました。開始日: {}", startDate);
        }

        LocalDate endDate = LocalDate.now();
        Map<String, ExecutionContext> result = new HashMap<>();
        
        LocalDate currentStart = startDate;
        int index = 0;

        // 月単位でループしてパーティションを生成
        while (currentStart.isBefore(endDate) || currentStart.isEqual(endDate)) {
            // その月の末日を計算
            LocalDate currentEnd = currentStart.withDayOfMonth(currentStart.lengthOfMonth());
            
            // 現在日付を超えないように制御
            if (currentEnd.isAfter(endDate)) {
                currentEnd = endDate;
            }

            // 各パーティション専用の実行コンテキストを作成
            ExecutionContext context = new ExecutionContext();
            context.putString("from", currentStart.toString()); // yyyy-MM-dd
            context.putString("to", currentEnd.toString());     // yyyy-MM-dd
            
            result.put("partition" + index, context);
            log.info("パーティション生成 [{}]: {} ～ {}", "partition" + index, currentStart, currentEnd);

            // 次の月の1日へ進める
            currentStart = currentStart.plusMonths(1).withDayOfMonth(1);
            index++;
        }

        log.info("合計 {} 個のパーティションを生成しました", result.size());
        return result;
    }
}
