package com.beauty.beauty_sales_dwh.batch.tasklet;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.beauty.beauty_sales_dwh.config.AppVendorProperties;
import com.beauty.beauty_sales_dwh.mapper.CustomerTransformMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerTransformTasklet implements Tasklet {

    private final CustomerTransformMapper mapper;
    private final AppVendorProperties vendorProperties; // プロパティクラス

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Step 3: データ整形処理を開始します...");

        // 1. プロパティから会社IDを取得
        // (AppVendorPropertiesはStringで定義していた想定なので変換)
        Long companyId = Long.valueOf(vendorProperties.getId());

        // 2. DBから最終更新日時を取得 (SQLの結果を利用)
        OffsetDateTime maxUpdatedAt = mapper.findMaxUpdateDataTimeFromDimCustomers(companyId);
        
        // 初回実行時などでNULLの場合は、十分古い日付を設定
        if (maxUpdatedAt == null) {
            maxUpdatedAt = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(9));
            log.info("初回実行、またはデータが存在しないため、全件処理対象とします。基準日: {}", maxUpdatedAt);
        } else {
            log.info("差分更新を実行します。基準日: {}", maxUpdatedAt);
        }

        // 3. MyBatis経由でUPSERT実行
        int count = mapper.upsertCustomersFromRaw(companyId, maxUpdatedAt);
        
        log.info("Step 3: 完了。{} 件のデータを DIM_CUSTOMERS に反映しました。", count);

        return RepeatStatus.FINISHED;
    }
}
