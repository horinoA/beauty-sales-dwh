package com.beauty.beauty_sales_dwh.batch.config;

import java.util.Map;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.builder.MyBatisBatchItemWriterBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.beauty.beauty_sales_dwh.batch.partitioner.TransactionPeriodPartitioner;
import com.beauty.beauty_sales_dwh.batch.processor.CategoryGroupRawDataProcessor;
import com.beauty.beauty_sales_dwh.batch.processor.CategoryRawDataProcessor;
import com.beauty.beauty_sales_dwh.batch.processor.CustomerRawDataProcessor;
import com.beauty.beauty_sales_dwh.batch.processor.ProductRawDataProcessor;
import com.beauty.beauty_sales_dwh.batch.processor.StaffRawDataProcessor;
import com.beauty.beauty_sales_dwh.batch.processor.TransactionRawDataProcessor;
import com.beauty.beauty_sales_dwh.batch.reader.SmaregiCategoryGroupItemReader;
import com.beauty.beauty_sales_dwh.batch.reader.SmaregiCategoryItemReader;
import com.beauty.beauty_sales_dwh.batch.reader.SmaregiCustomerItemReader;
import com.beauty.beauty_sales_dwh.batch.reader.SmaregiProductItemReader;
import com.beauty.beauty_sales_dwh.batch.reader.SmaregiStaffItemReader;
import com.beauty.beauty_sales_dwh.batch.reader.SmaregiTransactionItemReader;
import com.beauty.beauty_sales_dwh.batch.tasklet.CustomerTransformTasklet;
import com.beauty.beauty_sales_dwh.batch.tasklet.CustomerVisitUpdateTasklet;
import com.beauty.beauty_sales_dwh.batch.tasklet.FactSalesTransformTasklet;
import com.beauty.beauty_sales_dwh.batch.tasklet.IdentityResolutionTasklet;
import com.beauty.beauty_sales_dwh.batch.tasklet.ProductTransformTasklet;
import com.beauty.beauty_sales_dwh.batch.tasklet.SmaregiAuthTasklet;
import com.beauty.beauty_sales_dwh.batch.tasklet.StaffTransformTasklet;
import com.beauty.beauty_sales_dwh.batch.tasklet.TransactionDetailsExtractTasklet;
import com.beauty.beauty_sales_dwh.domain.CategoryGroupRawData;
import com.beauty.beauty_sales_dwh.domain.CategoryRawData;
import com.beauty.beauty_sales_dwh.domain.CustomerRawData;
import com.beauty.beauty_sales_dwh.domain.ProductRawData;
import com.beauty.beauty_sales_dwh.domain.StaffRawData;
import com.beauty.beauty_sales_dwh.domain.TransactionRawData;
import com.beauty.beauty_sales_dwh.mapper.RawTransactionMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * スマレジデータ取込ジョブの設定クラス
 * Step 1: 認証 (Tasklet)
 * Step 2: 各種マスタデータ取込 (Chunk: Reader -> Processor -> Writer)
 * Step 3: データ整形 (Tasklet)
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class SmaregiBatchConfig {


    // --- インフラストラクチャ ---
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SqlSessionFactory sqlSessionFactory;
    private final RawTransactionMapper rawTransactionMapper;

    // --- Tasklet ---
    private final SmaregiAuthTasklet smaregiAuthTasklet;
    private final CustomerTransformTasklet customerTransformTasklet;
    private final ProductTransformTasklet productTransformTasklet;
    private final StaffTransformTasklet staffTransformTasklet;
    private final TransactionDetailsExtractTasklet transactionDetailsExtractTasklet;
    private final FactSalesTransformTasklet factSalesTransformTasklet;
    private final IdentityResolutionTasklet identityResolutionTasklet;
    private final CustomerVisitUpdateTasklet customerVisitUpdateTasklet;

    // --- Readers (引数注入に切り替えるため削除) ---

    // --- Processors ---
    private final CustomerRawDataProcessor customerProcessor;
    private final CategoryRawDataProcessor categoryProcessor;
    private final CategoryGroupRawDataProcessor categoryGroupProcessor;
    private final ProductRawDataProcessor productProcessor;
    private final StaffRawDataProcessor staffProcessor;
    private final TransactionRawDataProcessor transactionProcessor;

    // =================================================================================
    // 1. Job 定義 (メインフロー)
    // =================================================================================
    @Bean
    public Job importSmaregiRawDataJob(
            Step stepFetchCustomers,
            Step stepFetchCategories,
            Step stepFetchCategoryGroups,
            Step stepFetchProducts,
            Step stepFetchStaffs) {
        return new JobBuilder("importSmaregiRawDataJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step1Auth())
                .next(stepFetchCustomers)
                .next(stepFetchCategories)
                .next(stepFetchCategoryGroups)
                .next(stepFetchProducts)
                .next(stepFetchStaffs)
                .next(stepTransformCustomers())
                .next(stepTransformProducts())
                .next(stepTransformStaffs())
                .next(stepUpdateCustomerVisitStats())
                .next(stepIdentifyResolution())
                .build();
    }

    /**
     * スマレジ取引データ取込ジョブ (Step A: Head取得 -> Step B: 明細展開)
     * パーティショニングを使用して過去3ヶ月分または差分を月単位で取得します。
     */
    @Bean
    public Job importSmaregiTransactionJob(
            Step stepMasterTransaction,
            Step stepExtractTransactionDetails,
            Step stepTransformFactSales) {
        return new JobBuilder("importSmaregiTransactionJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step1Auth())
                .next(stepMasterTransaction)
                .next(stepExtractTransactionDetails)
                .next(stepTransformFactSales)
                .next(stepUpdateCustomerVisitStats())
                .build();
    }

    // =================================================================================
    // 2. Step 定義
    // =================================================================================
    
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
     * Step 2.0: 取引データ（ヘッダ）取得 - マスタステップ
     * パーティショナーを呼び出し、月ごとのWorker Stepを実行します。
     */
    @Bean
    public Step stepMasterTransaction(Step stepFetchTransactionsHead) {
        return new StepBuilder("stepMasterTransaction", jobRepository)
                .partitioner("stepFetchTransactionsHead", transactionPeriodPartitioner(null))
                .step(stepFetchTransactionsHead)
                .gridSize(1) // 順次実行
                .taskExecutor(new org.springframework.core.task.SyncTaskExecutor()) // 同期実行を強制
                .build();
    }

    /**
     * Step 2.0: 取引データ（ヘッダ）取得 - ワーカーステップ
     */
    @Bean
    public Step stepFetchTransactionsHead(SmaregiTransactionItemReader smaregiTransactionReader) {
        return new StepBuilder("stepFetchTransactionsHead", jobRepository)
                .<Map<String, Object>, TransactionRawData>chunk(100, transactionManager)
                .reader(smaregiTransactionReader)
                .processor(transactionProcessor)
                .writer(transactionWriter())
                .build();
    }

    /**
     * Step 2.1: 取引明細展開タスク (Step B)
     */
    @Bean
    public Step stepExtractTransactionDetails() {
        return new StepBuilder("stepExtractTransactionDetails", jobRepository)
                .tasklet(transactionDetailsExtractTasklet, transactionManager)
                .build();
    }

    /**
     * Step 2.2: 取引データ整形タスク (Step C: raw -> dwh)
     */
    @Bean
    public Step stepTransformFactSales() {
        return new StepBuilder("stepTransformFactSales", jobRepository)
                .tasklet(factSalesTransformTasklet, transactionManager)
                .build();
    }

    /**
     * Step 2.3: 顧客来店統計更新タスク
     */
    @Bean
    public Step stepUpdateCustomerVisitStats() {
        return new StepBuilder("stepUpdateCustomerVisitStats", jobRepository)
                .tasklet(customerVisitUpdateTasklet, transactionManager)
                .build();
    }

    /**
     * Step 2.1: 顧客データ取込
     */
    @Bean
    public Step stepFetchCustomers(SmaregiCustomerItemReader smaregiCustomerReader) {
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
    public Step stepFetchCategories(SmaregiCategoryItemReader smaregiCategoryReader) {
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
    public Step stepFetchCategoryGroups(SmaregiCategoryGroupItemReader smaregiCategoryGroupReader) {
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
    public Step stepFetchProducts(SmaregiProductItemReader smaregiProductReader) {
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
    public Step stepFetchStaffs(SmaregiStaffItemReader smaregiStaffReader) {
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
    public Step stepTransformStaffs() {
        return new StepBuilder("stepTransformStaffs", jobRepository)
            .tasklet(staffTransformTasklet, transactionManager)
            .build();
    }

    /**
     * Step 4: 名寄せ候補抽出タスク
     */
    @Bean
    public Step stepIdentifyResolution() {
        return new StepBuilder("stepIdentifyResolution", jobRepository)
                .tasklet(identityResolutionTasklet, transactionManager)
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
    public ItemWriter<TransactionRawData> transactionWriter() {
        return new MyBatisBatchItemWriterBuilder<TransactionRawData>()
                .sqlSessionFactory(sqlSessionFactory)
                .statementId("com.beauty.beauty_sales_dwh.mapper.RawTransactionMapper.insertRawTransaction")
                .build();
    }

    /**
     * 取引データの取得期間を分割するパーティショナー
     */
    @Bean
    @StepScope
    public TransactionPeriodPartitioner transactionPeriodPartitioner(
            @Value("#{jobParameters['companyId']}") Long companyId) {
        log.info("TransactionPeriodPartitionerを生成します。companyId: {}", companyId);
        return new TransactionPeriodPartitioner(rawTransactionMapper, companyId);
    }
}