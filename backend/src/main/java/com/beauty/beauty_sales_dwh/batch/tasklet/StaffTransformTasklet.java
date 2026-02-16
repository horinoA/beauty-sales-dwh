package com.beauty.beauty_sales_dwh.batch.tasklet;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.beauty.beauty_sales_dwh.config.AppVendorProperties;
import com.beauty.beauty_sales_dwh.mapper.StaffTransformMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class StaffTransformTasklet implements Tasklet{
    
    private final StaffTransformMapper mapper;
    private final AppVendorProperties vendorProperties;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Step 3: データ整形処理を開始します...");

        // 1. プロパティから会社IDを取得
        // (AppVendorPropertiesはStringで定義していた想定なので変換)
        Long companyId = Long.valueOf(vendorProperties.getId());
 
        // 2. DBから最終更新日時を取得 (SQLの結果を利用)
        OffsetDateTime maxUpdateTimeFromDimStaffs = mapper.findMaxUpdateDataTimeFromDimStaffs(companyId);

        if (maxUpdateTimeFromDimStaffs == null){
            maxUpdateTimeFromDimStaffs = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(9));
            log.info("dwh.dim_staffs: 初回実行、またはデータが存在しないため、全件処理対象とします。基準日: {}", maxUpdateTimeFromDimStaffs);
        } else {
            log.info("dwh.dim_staffs: 差分更新を実行します。基準日: {}", maxUpdateTimeFromDimStaffs);
        }

        // 3. MyBatis経由でdwh.dim_staffsへUPSERT実行
        int staffCount = mapper.upsertStaffsFromRaw(companyId, maxUpdateTimeFromDimStaffs);
        log.info("tep 3: 完了。{} 件のデータを dwh.dim_staffs に反映しました。", staffCount);

        return RepeatStatus.FINISHED;
    }
    
}
