package com.beauty.beauty_sales_dwh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class BeautySalesDwhApplication {

	public static void main(String[] args) {
		SpringApplication.run(BeautySalesDwhApplication.class, args);
	}
	
	//RestTemplate は意図的にカスタマイズ（タイムアウト設定など）することが多いため、
	// デフォルトでは自動生成されないためこちらでインスタンス作成
	@Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
	// ★これを追加：アプリ起動時に強制的にジョブを実行するランナー
    /*@Bean
    public CommandLineRunner runJob(JobLauncher jobLauncher, Job importSmaregiCustomerJob) {
        return args -> {
            System.out.println("==========================================");
            System.out.println("🚀 バッチジョブを強制起動します...");
            System.out.println("==========================================");

            // 毎回異なるパラメータを渡して、再実行可能な状態にする
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("executedAt", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(importSmaregiCustomerJob, jobParameters);
            
            System.out.println("==========================================");
            System.out.println("✅ バッチジョブ起動完了");
            System.out.println("==========================================");
        };
    }*/
}
