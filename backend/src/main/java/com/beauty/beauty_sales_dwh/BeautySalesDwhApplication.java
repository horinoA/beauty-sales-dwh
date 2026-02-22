package com.beauty.beauty_sales_dwh;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

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
    public CommandLineRunner runJob(JobLauncher jobLauncher, Job importSmaregiRawDataJob) {
        return args -> {
            System.out.println("==========================================");
            System.out.println("🚀 バッチジョブを強制起動します...");
            System.out.println("==========================================");

            // 毎回異なるパラメータを渡して、再実行可能な状態にする
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("executedAt", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(importSmaregiRawDataJob, jobParameters);
            
            System.out.println("==========================================");
            System.out.println("✅ バッチジョブ起動完了");
            System.out.println("==========================================");
        };
    }
}
