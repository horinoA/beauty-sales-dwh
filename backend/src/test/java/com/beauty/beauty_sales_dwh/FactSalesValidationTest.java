package com.beauty.beauty_sales_dwh;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.beauty.beauty_sales_dwh.analytics.sales.FactSales;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@SpringBootTest
@ActiveProfiles("test")
public class FactSalesValidationTest {
    
    @Autowired
    private Validator validator;

    @Test
    void validateMessageFromProperties() {

        FactSales invalidSales = new FactSales(
            null, // CompanyID null -> エラー
            "1",
            OffsetDateTime.now(), LocalDate.now(), "1", "1", "1", 
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
    
    @Test
    void validateSnowFlake(){
        
       FactSales invalidSales = new FactSales(
            0L,// CompanyID 0 -> Snowflakeエラー
            "1",
            OffsetDateTime.now(), LocalDate.now(), "1", "1", "1", 
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
            .anyMatch(v -> v.getMessage().equals("不正なSnowflake ID形式です"));
            
        assertThat(messageFound).isTrue();

    }

    @Test
    void smaregiIdOKtest(){
        //スマレジIDが範囲内ならOK
        FactSales invalidSales = new FactSales(
            4069L,
            "2", 
            OffsetDateTime.now(), LocalDate.now(), "1", "1", "1", 
            1000, 1000, 1100, 1000, 0, 0, 0, 0, 0, 
            "SALES", 
            false
        );

        // Act
        Set<ConstraintViolation<FactSales>> violations = validator.validate(invalidSales);

        // Assert
        assertThat(violations).isEmpty();

    }

    @Test
    void smaregiIdrenge(){
        FactSales invalidSales = new FactSales(
            4069L,
            "0", //スマレジIDが1以下ならエラー
            OffsetDateTime.now(), LocalDate.now(), "1", "1", "1", 
            1000, 1000, 1100, 1000, 0, 0, 0, 0, 0, 
            "SALES", 
            false
        );

        // Act
        Set<ConstraintViolation<FactSales>> violations = validator.validate(invalidSales);

        // Assert
        assertThat(violations).isNotEmpty();

        violations.forEach(constraintViolation -> {
            String msg =
                "message = " + constraintViolation.getMessage() + "\n" +
                "messageTemplate = " + constraintViolation.getMessageTemplate() + "\n" +
                "rootBean = " + constraintViolation.getRootBean() + "\n" +
                "rootBeanClass = " + constraintViolation.getRootBeanClass() + "\n" +
                "invalidValue = " + constraintViolation.getInvalidValue() + "\n" +
                "propertyPath = " + constraintViolation.getPropertyPath() + "\n" +
                "leafBean = " + constraintViolation.getLeafBean() + "\n" +
                "descriptor = " + constraintViolation.getConstraintDescriptor() + "\n"
            ;
            
            System.out.println(msg);
        });

        // メッセージがプロパティファイルから取得できているか確認
        boolean messageFound = violations.stream()
            .anyMatch(v -> v.getMessage().equals("IDは1〜999999999の間で指定してください"));
            
        assertThat(messageFound).isTrue();


        FactSales invalidSales_1 = new FactSales(
            4069L,
            "9999999999", //スマレジIDが999999999以上ならエラー
            OffsetDateTime.now(), LocalDate.now(), "1", "1", "1", 
            1000, 1000, 1100, 1000, 0, 0, 0, 0, 0, 
            "SALES", 
            false
        );

        // Act
        Set<ConstraintViolation<FactSales>> violations_1 = validator.validate(invalidSales_1);

        // Assert
        assertThat(violations_1).isNotEmpty();

        // メッセージがプロパティファイルから取得できているか確認
        boolean messageFound_1 = violations_1.stream()
            .anyMatch(v -> v.getMessage().equals("IDは1〜999999999の間で指定してください"));
            
        assertThat(messageFound_1).isTrue();

    }

    @Test
    void sumareziNullOk(){
        FactSales invalidSales = new FactSales(
            4069L,
            "1",
            OffsetDateTime.now(), LocalDate.now(), 
            "", null, "1", //@ValidSmaregiIdは空白やnullは許可
            1000, 1000, 1100, 1000, 0, 0, 0, 0, 0, 
            "SALES", 
            false
        );

        // Act
        Set<ConstraintViolation<FactSales>> violations = validator.validate(invalidSales);

        // Assert
        assertThat(violations).isEmpty();

    }


    @Test
    void validateTranType(){
       FactSales invalidSales = new FactSales(
            4069L,
            "2", 
            OffsetDateTime.now(), LocalDate.now(), "1", "1", "1", 
            1000, 1000, 1100, 1000, 0, 0, 0, 0, 0, 
            "",//tranTypeが指定値以外 
            false
        );

        // Act
        Set<ConstraintViolation<FactSales>> violations = validator.validate(invalidSales);

        // Assert
        assertThat(violations).isNotEmpty();

        // メッセージがプロパティファイルから取得できているか確認
        boolean messageFound = violations.stream()
            .anyMatch(v -> v.getMessage().equals("取引区分はSALESまたはREFUNDである必要があります"));
            
        assertThat(messageFound).isTrue();

        FactSales invalidSales_1 = new FactSales(
            4069L,
            "2", 
            OffsetDateTime.now(), LocalDate.now(), "1", "1", "1", 
            1000, 1000, 1100, 1000, 0, 0, 0, 0, 0, 
            null,//tranTypeがnull 
            false
        );

        // Act
        Set<ConstraintViolation<FactSales>> violations_1 = validator.validate(invalidSales_1);

        // Assert
        assertThat(violations_1).isNotEmpty();

        // メッセージがプロパティファイルから取得できているか確認
        boolean messageFound_1 = violations_1.stream()
            .anyMatch(v -> v.getMessage().equals("取引区分はは必須です"));
            
        assertThat(messageFound_1).isTrue();
    }

    @Test
    void validTransactionAmount(){

        FactSales invalidSales = new FactSales(
            4069L,
            "2", 
            OffsetDateTime.now(), LocalDate.now(), "1", "1", "1", 
            -1000, //totalがー（返金処理）だがtranTypeが入金
            1000, 
            1100, 
            1000, 
            0, 0, 0, 0, 0, 
            "SALES", 
            false
        );
        

        // Act
        Set<ConstraintViolation<FactSales>> violations = validator.validate(invalidSales);

        // Assert
        assertThat(violations).isNotEmpty();

        // メッセージがプロパティファイルから取得できているか確認
        boolean messageFound = violations.stream()
            .anyMatch(v -> v.getMessage().equals("取引区分と金額の符号が矛盾しています（SALESは正、REFUNDは負である必要があります）"));
            
        assertThat(messageFound).isTrue();

        FactSales invalidSales_1 = new FactSales(
            4069L,
            "2", 
            OffsetDateTime.now(), LocalDate.now(), "1", "1", "1", 
            1000, //totalが+（入金処理）だがtranTypeが返金
            1000, 
            1100, 
            1000, 
            0, 0, 0, 0, 0, 
            "REFUND", 
            false
        );

        // Act
        Set<ConstraintViolation<FactSales>> violations_1 = validator.validate(invalidSales_1);

        // Assert
        assertThat(violations_1).isNotEmpty();

        // メッセージがプロパティファイルから取得できているか確認
        boolean messageFound_1 = violations_1.stream()
            .anyMatch(v -> v.getMessage().equals("取引区分と金額の符号が矛盾しています（SALESは正、REFUNDは負である必要があります）"));
            
        assertThat(messageFound_1).isTrue();

    }


}
