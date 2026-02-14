package com.beauty.beauty_sales_dwh.batch.tasklet;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.beauty.beauty_sales_dwh.config.AppVendorProperties;
import com.beauty.beauty_sales_dwh.mapper.ProductTransformMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductTransformTasklet implements Tasklet {
    
    private final ProductTransformMapper mapper;
    private final AppVendorProperties vendorProperties; // プロパティクラス
    
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Step 3: Product関連のデータ整形処理を開始します...");

        // 1. プロパティから会社IDを取得
        Long companyId = Long.valueOf(vendorProperties.getId());
        
        // 2. dwh.dim_category_groups の最終更新日時を取得
        OffsetDateTime maxUpdateDataTimeFromDimCategoryGroups = mapper.findMaxUpdateDataTimeFromDimCategoryGroups(companyId);
        if (maxUpdateDataTimeFromDimCategoryGroups == null) {
            maxUpdateDataTimeFromDimCategoryGroups = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(9));
            log.info("dwh.dim_category_groups: 初回実行、またはデータが存在しないため、全件処理対象とします。基準日: {}", maxUpdateDataTimeFromDimCategoryGroups);
        } else {
            log.info("dwh.dim_category_groups: 差分更新を実行します。基準日: {}", maxUpdateDataTimeFromDimCategoryGroups);
        }

        // 3. MyBatis経由でdwh.dim_category_groupsへUPSERT実行
        int categoryGroupCount = mapper.upsertCategoryGroupsFromRaw(companyId, maxUpdateDataTimeFromDimCategoryGroups);
        log.info("{} 件のデータを dwh.dim_category_groups に反映しました。", categoryGroupCount);

        // 4. dwh.dim_products の最終更新日時を取得
        OffsetDateTime maxUpdateDataTimeFromDimProducts = mapper.findMaxUpdateDataTimeFromDimProducts(companyId);
        if (maxUpdateDataTimeFromDimProducts == null) {
            maxUpdateDataTimeFromDimProducts = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(9));
            log.info("dwh.dim_products: 初回実行、またはデータが存在しないため、全件処理対象とします。基準日: {}", maxUpdateDataTimeFromDimProducts);
        } else {
            log.info("dwh.dim_products: 差分更新を実行します。基準日: {}", maxUpdateDataTimeFromDimProducts);
        }

        // 5. MyBatis経由でdwh.dim_productsへUPSERT実行
        int productCount = mapper.upsertProductsFromRaw(companyId, maxUpdateDataTimeFromDimProducts);
        log.info("{} 件のデータを dwh.dim_products に反映しました。", productCount);
        
        log.info("Step 3: Product関連のデータ整形処理が完了しました。");

        return RepeatStatus.FINISHED;
    }
    
}
