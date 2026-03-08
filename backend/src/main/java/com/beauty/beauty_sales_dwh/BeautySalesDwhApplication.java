package com.beauty.beauty_sales_dwh;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

import com.beauty.beauty_sales_dwh.config.AppVendorProperties;

@SpringBootApplication
public class BeautySalesDwhApplication {

	public static void main(String[] args) {
		SpringApplication.run(BeautySalesDwhApplication.class, args);
	}
	
	// RestTemplate は意図的にカスタマイズ（タイムアウト設定など）することが多いため、
	// デフォルトでは自動生成されないためこちらでインスタンス作成
    /*
	@Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    */

    /**
     * RestTemplate の Bean 定義。
     * テストで @AutoConfigureMockRestServiceServer を使用して MockRestServiceServer を
     * 自動構成するには、RestTemplateBuilder を経由して作成したインスタンスである必要があります。
     * (new RestTemplate() では自動構成のフックが効かないため)
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
    
	// ★これを追加：アプリ起動時に強制的にジョブを実行するランナー
    @Bean
    @Profile("!test")
    public CommandLineRunner runJob(
            JobLauncher jobLauncher, 
            @Qualifier("importSmaregiRawDataJob") Job importSmaregiRawDataJob,
            @Qualifier("importSmaregiTransactionJob") Job importSmaregiTransactionJob,
            AppVendorProperties vendorProperties
    ) {
        return args -> {
            System.out.println("==========================================");
            System.out.println("🚀 バッチジョブを強制起動します...");
            System.out.println("==========================================");

            // 共通の JobParameters (会社IDを確実に含める)
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("executedAt", System.currentTimeMillis())
                    .addLong("companyId", Long.valueOf(vendorProperties.getId()))
                    .toJobParameters();

            // 1. マスタデータの取込
            System.out.println("--- 1. マスタデータ取込開始 ---");
            jobLauncher.run(importSmaregiRawDataJob, jobParameters);
            
            // 2. 取引（売上）データの取込
            System.out.println("--- 2. 取引データ取込開始 ---");
            jobLauncher.run(importSmaregiTransactionJob, jobParameters);
            
            System.out.println("==========================================");
            System.out.println("✅ 全てのバッチジョブ起動完了");
            System.out.println("==========================================");
        };
    }
}
