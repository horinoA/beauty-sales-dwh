package com.beauty.beauty_sales_dwh;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.beauty.beauty_sales_dwh.analytics.sales.FactSales;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@SpringBootTest
public class FactSalesValidationTest {
    
    @Autowired
    private Validator validator;

    @Test
    void validateMessageFromProperties() {

        FactSales invalidSales = new FactSales(
            null, // CompanyID null -> エラー
            "TX123",
            null, null, null, null, null, 
            1000, 1000, 1100, 1000, 0, 0, 0, 0, 0, 
            "SALES", 
            false
        );

        // Act
        Set<ConstraintViolation<FactSales>> violations = validator.validate(invalidSales);

        // Assert
        assertThat(violations).isNotEmpty();
        
        // メッセージがプロパティファイルから取得できているか確認
        boolean messageFound = violations.stream()
            .anyMatch(v -> v.getMessage().equals("CompanyIDは必須です"));
            
        assertThat(messageFound).isTrue();
    }
    
}
