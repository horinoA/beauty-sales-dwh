package com.beauty.beauty_sales_dwh.batch.tasklet;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.beauty.beauty_sales_dwh.config.AppVendorProperties;
import com.beauty.beauty_sales_dwh.mapper.FactSalesTransformMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * raw スキーマの取引データを dwh スキーマ（fact_sales, fact_sales_details）へ
 * 変換・転送する Tasklet。
 * 判定ロジック（SALES/REFUND/is_void）および名寄せ解決を伴う UPSERT を実行します。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FactSalesTransformTasklet implements Tasklet {

    private final FactSalesTransformMapper mapper;
    private final AppVendorProperties vendorProperties;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("取引データの Transform 処理（raw -> dwh）を開始します...");

        // 1. 会社IDの取得 (JobParameters を優先)
        Long companyId = (Long) chunkContext.getStepContext().getJobParameters().get("companyId");
        if (companyId == null) {
            companyId = Long.valueOf(vendorProperties.getId());
        }
        log.info("CompanyID: {} を使用して処理を開始します...", companyId);

        // 2. 差分更新の基準日時を取得
        OffsetDateTime fromDate = mapper.findMaxTransactionDateTime(companyId);
        
        if (fromDate == null) {
            // 初回実行時：TransactionPeriodPartitioner の仕様に合わせ、過去3ヶ月前の月初を基準にする
            LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3).withDayOfMonth(1);
            fromDate = OffsetDateTime.of(threeMonthsAgo.atStartOfDay(), ZoneOffset.ofHours(9));
            log.info("dwh.fact_sales: 初回実行のため、過去3ヶ月分を対象とします。基準日: {}", fromDate);
        } else {
            // 継続実行時：DWH内の最新日時を基準にする
            log.info("dwh.fact_sales: 差分更新を実行します。基準日: {}", fromDate);
        }

        // 3. 売上ヘッダー (fact_sales) の UPSERT
        // 判定ロジック（SALES/REFUND, is_void）および名寄せ解決を含む SQL を実行
        int salesCount = mapper.upsertFactSales(companyId, fromDate);
        log.info("fact_sales へ {} 件反映しました。", salesCount);

        // 4. 売上明細 (fact_sales_details) の UPSERT
        // 商品・カテゴリマスタとの結合および種別判定を含む SQL を実行
        int detailCount = mapper.upsertFactSalesDetails(companyId, fromDate);
        log.info("fact_sales_details へ {} 件反映しました。", detailCount);

        log.info("取引データの Transform 処理が完了しました。");
        return RepeatStatus.FINISHED;
    }
}
