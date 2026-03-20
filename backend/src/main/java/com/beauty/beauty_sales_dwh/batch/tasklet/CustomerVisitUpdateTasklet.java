package com.beauty.beauty_sales_dwh.batch.tasklet;

import com.beauty.beauty_sales_dwh.mapper.CustomerVisitUpdateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * 顧客の来店統計情報（来店回数、最終来店日等）を更新するタスクレット
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerVisitUpdateTasklet implements Tasklet {

    private final CustomerVisitUpdateMapper customerVisitUpdateMapper;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        Long companyId = (Long) chunkContext.getStepContext().getJobParameters().get("companyId");
        
        log.info("顧客来店統計の更新を開始します。会社ID: {}", companyId);
        
        int count = customerVisitUpdateMapper.updateCustomerVisitStats(companyId);
        
        log.info("顧客来店統計の更新が完了しました。更新された顧客数: {}", count);
        
        return RepeatStatus.FINISHED;
    }
}
