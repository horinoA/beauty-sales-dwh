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

import com.beauty.beauty_sales_dwh.batch.processor.CategoryGroupRawDataProcessor;
import com.beauty.beauty_sales_dwh.batch.processor.CategoryRawDataProcessor;
import com.beauty.beauty_sales_dwh.batch.processor.CustomerRawDataProcessor;
import com.beauty.beauty_sales_dwh.batch.processor.ProductRawDataProcessor;
import com.beauty.beauty_sales_dwh.batch.processor.StaffRawDataProcessor;
import com.beauty.beauty_sales_dwh.batch.reader.SmaregiCategoryGroupItemReader;
import com.beauty.beauty_sales_dwh.batch.reader.SmaregiCategoryItemReader;
import com.beauty.beauty_sales_dwh.batch.reader.SmaregiCustomerItemReader;
import com.beauty.beauty_sales_dwh.batch.reader.SmaregiProductItemReader;
import com.beauty.beauty_sales_dwh.batch.reader.SmaregiStaffItemReader;
import com.beauty.beauty_sales_dwh.batch.tasklet.CustomerTransformTasklet;
import com.beauty.beauty_sales_dwh.batch.tasklet.ProductTransformTasklet;
import com.beauty.beauty_sales_dwh.batch.tasklet.SmaregiAuthTasklet;
import com.beauty.beauty_sales_dwh.batch.tasklet.StaffTransformTasklet;
import com.beauty.beauty_sales_dwh.domain.CategoryGroupRawData;
import com.beauty.beauty_sales_dwh.domain.CategoryRawData;
import com.beauty.beauty_sales_dwh.domain.CustomerRawData;
import com.beauty.beauty_sales_dwh.domain.ProductRawData;
import com.beauty.beauty_sales_dwh.domain.StaffRawData;

import lombok.RequiredArgsConstructor;

/**
 * スマレジデータ取込ジョブの設定クラス
 * Step 1: 認証 (Tasklet)
 * Step 2: 各種マスタデータ取込 (Chunk: Reader -> Processor -> Writer)
 * Step 3: データ整形 (Tasklet)
 */
@Configuration
@RequiredArgsConstructor
public class SmaregiBatchConfig {


    // --- インフラストラクチャ ---
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SqlSessionFactory sqlSessionFactory;

    // --- Tasklet ---
    private final SmaregiAuthTasklet smaregiAuthTasklet;
    private final CustomerTransformTasklet customerTransformTasklet;
    private final ProductTransformTasklet productTransformTasklet;
    private final StaffTransformTasklet staffTransformTasklet;

    // --- Readers ---
    private final SmaregiCustomerItemReader smaregiCustomerReader;
    private final SmaregiCategoryItemReader smaregiCategoryReader;
    private final SmaregiCategoryGroupItemReader smaregiCategoryGroupReader;
    private final SmaregiProductItemReader smaregiProductReader;
    private final SmaregiStaffItemReader smaregiStaffReader;

    // --- Processors ---
    private final CustomerRawDataProcessor customerProcessor;
    private final CategoryRawDataProcessor categoryProcessor;
    private final CategoryGroupRawDataProcessor categoryGroupProcessor;
    private final ProductRawDataProcessor productProcessor;
    private final StaffRawDataProcessor staffProcessor;

    // =================================================================================
    // 1. Job 定義 (メインフロー)
    // =================================================================================
    @Bean
    public Job importSmaregiRawDataJob() {
        return new JobBuilder("importSmaregiRawDataJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step1Auth())
                //.next(stepFetchTransactionsHead()) // Add this line
                .next(stepFetchCustomers())
                .next(stepFetchCategories())
                .next(stepFetchCategoryGroups())
                .next(stepFetchProducts())
                .next(stepFetchStaffs())
                .next(stepTransformCustomers())
                .next(stepTransformProducts())
                .next(stepTranceformStaffs())
                .build();
    }

    // =================================================================================
    // 2. Step 定義
    // =================================================================================
    /*
    /**
     * Step 1: 認証タスク
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
      * MyBatisを使ってRAWテーブルへ一括INSERTします。
      * Writerは下で定義した3. Writer Bean 定義を使用 
    */
    /**
     * Step 2.1: 顧客データ取込
     */
    @Bean
    public Step stepFetchCustomers() {
        return new StepBuilder("stepFetchCustomers", jobRepository)
                .<Map<String, Object>, CustomerRawData>chunk(100, transactionManager)
                // 100件ごとにコミット                                                                    
                .reader(smaregiCustomerReader)
                .processor(customerProcessor)
                .writer(customerWriter())
                .build();
    }
    
    /**
     * Step 2.2: カテゴリデータ取込
     */
    @Bean
    public Step stepFetchCategories() {
        return new StepBuilder("stepFetchCategories", jobRepository)
                .<Map<String, Object>, CategoryRawData>chunk(100, transactionManager)
                // 100件ごとにコミット                                                                    
                .reader(smaregiCategoryReader)
                .processor(categoryProcessor)
                .writer(categoryWriter())
                .build();
    }

    /**
     * Step 2.3: カテゴリグループデータ取込
     */
    @Bean
    public Step stepFetchCategoryGroups() {
        return new StepBuilder("stepFetchCategoryGroups", jobRepository)
                .<Map<String, Object>, CategoryGroupRawData>chunk(100, transactionManager)
                // 100件ごとにコミット                                                                    
                .reader(smaregiCategoryGroupReader)
                .processor(categoryGroupProcessor)
                .writer(categoryGroupWriter())
                .build();
    }

    /**
     * Step 2.4: 商品データ取込
     */
    @Bean
    public Step stepFetchProducts() {
        return new StepBuilder("stepFetchProducts", jobRepository)
                .<Map<String, Object>, ProductRawData>chunk(100, transactionManager)
                // 100件ごとにコミット
                .reader(smaregiProductReader)
                .processor(productProcessor)
                .writer(productWriter())
                .build();
    }

    /**
     * Step 2.5: スタッフデータ取込
     */
    @Bean
    public Step stepFetchStaffs() {
        return new StepBuilder("stepFetchStaffs", jobRepository)
                .<Map<String, Object>, StaffRawData>chunk(100, transactionManager)
                // 100件ごとにコミット
                .reader(smaregiStaffReader)
                .processor(staffProcessor)
                .writer(staffWriter())
                .build();
    }

    /**
     * Step 3: 顧客データ整形タスク
     */
    @Bean
    public Step stepTransformCustomers() {
        return new StepBuilder("stepTransformCustomers", jobRepository)
                .tasklet(customerTransformTasklet, transactionManager)
                .build();
    }

    /**
     * Step 3: 商品関連データ整形タスク
     */
    @Bean
    public Step stepTransformProducts() {
        return new StepBuilder("stepTransformProducts", jobRepository)
                .tasklet(productTransformTasklet, transactionManager)
                .build();
    }

    /**
     * Step 3: スタッフデータ整形タスク
     */
    @Bean
    public Step stepTranceformStaffs() {
        return new StepBuilder("stepTransformTasklet", jobRepository)
            .tasklet(staffTransformTasklet, transactionManager)
            .build();
    }

    // =================================================================================
    // 3. Writer Bean 定義
    // =================================================================================

    @Bean
    public ItemWriter<CustomerRawData> customerWriter() {
        return new MyBatisBatchItemWriterBuilder<CustomerRawData>()
                .sqlSessionFactory(sqlSessionFactory)
                .statementId("com.beauty.beauty_sales_dwh.mapper.RawCustomerMapper.insertRawCustomer")
                .build();
    }
    
    @Bean
    public ItemWriter<CategoryRawData> categoryWriter() {
        return new MyBatisBatchItemWriterBuilder<CategoryRawData>()
                .sqlSessionFactory(sqlSessionFactory)
                .statementId("com.beauty.beauty_sales_dwh.mapper.RawCategoryMapper.insertRawCategory")
                .build();
    }

    @Bean
    public ItemWriter<CategoryGroupRawData> categoryGroupWriter() {
        return new MyBatisBatchItemWriterBuilder<CategoryGroupRawData>()
                .sqlSessionFactory(sqlSessionFactory)
                .statementId("com.beauty.beauty_sales_dwh.mapper.RawCategoryGroupMapper.insertRawCategoryGroup")
                .build();
    }

    @Bean
    public ItemWriter<ProductRawData> productWriter() {
        return new MyBatisBatchItemWriterBuilder<ProductRawData>()
                .sqlSessionFactory(sqlSessionFactory)
                .statementId("com.beauty.beauty_sales_dwh.mapper.RawProductMapper.insertRawProduct")
                .build();
    }

    @Bean
    public ItemWriter<StaffRawData> staffWriter() {
        return new MyBatisBatchItemWriterBuilder<StaffRawData>()
                .sqlSessionFactory(sqlSessionFactory)
                .statementId("com.beauty.beauty_sales_dwh.mapper.RawStaffMapper.insertRawStaff")
                .build();
    }

    @Bean
    public ItemWriter<com.beauty.beauty_sales_dwh.domain.TransactionRawData> transactionWriter() {
        return new MyBatisBatchItemWriterBuilder<com.beauty.beauty_sales_dwh.domain.TransactionRawData>()
                .sqlSessionFactory(sqlSessionFactory)
                .statementId("com.beauty.beauty_sales_dwh.mapper.RawTransactionMapper.insertRawTransaction")
                .build();
    }
}