package com.beauty.beauty_sales_dwh.batch.tasklet;

import com.beauty.beauty_sales_dwh.mapper.IdentityResolutionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * 顧客の名寄せ候補を抽出・登録するタスクレット
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdentityResolutionTasklet implements Tasklet {

    private final IdentityResolutionMapper identityResolutionMapper;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        Long companyId = (Long) chunkContext.getStepContext().getJobParameters().get("companyId");
        
        log.info("名寄せ候補の抽出を開始します。会社ID: {}", companyId);
        
        int count = identityResolutionMapper.findAndInsertMergeCandidates(companyId);
        
        log.info("名寄せ候補の抽出が完了しました。新規登録件数: {}", count);
        
        return RepeatStatus.FINISHED;
    }
}
