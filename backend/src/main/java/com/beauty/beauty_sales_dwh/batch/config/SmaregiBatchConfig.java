package com.beauty.beauty_sales_dwh.batch.config;

import java.util.Map;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.builder.MyBatisBatchItemWriterBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.beauty.beauty_sales_dwh.batch.processor.CustomerRawDataProcessor;
import com.beauty.beauty_sales_dwh.batch.reader.SmaregiCustomerItemReader;
import com.beauty.beauty_sales_dwh.batch.tasklet.CustomerTransformTasklet;
import com.beauty.beauty_sales_dwh.batch.tasklet.SmaregiAuthTasklet;
import com.beauty.beauty_sales_dwh.domain.CustomerRawData;

import lombok.RequiredArgsConstructor;

/**
 * スマレジデータ取込ジョブの設定クラス
 * Step 1: 認証 (Tasklet)
 * Step 2: 顧客データ取込 (Chunk: Reader -> Processor -> Writer)
 * Step 3: データ整形 (Tasklet)
 */
@Configuration
@RequiredArgsConstructor
public class SmaregiBatchConfig {

    // --- インフラストラクチャ ---
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SqlSessionFactory sqlSessionFactory; // MyBatis用

    // --- コンポーネント (これまでに作ったもの) ---
    private final SmaregiAuthTasklet smaregiAuthTasklet;          // Step 1
    private final SmaregiCustomerItemReader smaregiCustomerReader; // Step 2 (Read)
    private final CustomerRawDataProcessor customerProcessor;      // Step 2 (Process)
    private final CustomerTransformTasklet customerTransformTasklet;// Step 3

    // =================================================================================
    // 1. Job 定義 (メインフロー)
    // =================================================================================
    @Bean
    public Job importSmaregiCustomerJob() {
        return new JobBuilder("importSmaregiCustomerJob", jobRepository)
                .incrementer(new RunIdIncrementer()) // 実行ごとにIDをインクリメント(何度でも実行可能にする)
                .start(step1Auth())      // 認証
                .next(step2Fetch())      // 取込
                .next(step3Transform())  // 整形
                .build();
    }

    // =================================================================================
    // 2. Step 定義
    // =================================================================================

    /**
     * Step 1: 認証タスク
     * APIトークンを取得し、ExecutionContextに保存します。
     */
    @Bean
    public Step step1Auth() {
        return new StepBuilder("step1Auth", jobRepository)
                .tasklet(smaregiAuthTasklet, transactionManager)
                .build();
    }

    /**
     * Step 2: データ取込 (Chunkモデル)
     * APIから読み込み -> JSON変換 -> DB保存 (RAWテーブル)
     */
    @Bean
    public Step step2Fetch() {
        return new StepBuilder("step2Fetch", jobRepository)
                .<Map<String, Object>, CustomerRawData>chunk(100, transactionManager) // 100件ごとにコミット
                .reader(smaregiCustomerReader)
                .processor(customerProcessor)
                .writer(customerWriter()) // 下で定義したMyBatisWriterを使用
                .build();
    }

    /**
     * Step 3: データ整形タスク
     * RAWテーブルから正規テーブルへデータを移します。
     */
    @Bean
    public Step step3Transform() {
        return new StepBuilder("step3Transform", jobRepository)
                .tasklet(customerTransformTasklet, transactionManager)
                .build();
    }

    // =================================================================================
    // 3. Helper 定義 (Writerなど)
    // =================================================================================

    /**
     * Step 2用 Writer
     * MyBatisを使ってRAWテーブルへ一括INSERTします。
     * RawCustomerMapper.xml の insertRawCustomer を呼び出します。
     */
    @Bean
    public ItemWriter<CustomerRawData> customerWriter() {
        return new MyBatisBatchItemWriterBuilder<CustomerRawData>()
                .sqlSessionFactory(sqlSessionFactory)
                // XMLの namespace + id を指定します (間違えないよう注意！)
                .statementId("com.beauty.beauty_sales_dwh.mapper.RawCustomerMapper.insertRawCustomer")
                .build();
    }
}