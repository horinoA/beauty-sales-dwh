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
}
