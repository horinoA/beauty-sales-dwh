package com.beauty.beauty_sales_dwh;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.beauty.beauty_sales_dwh.analytics.sales.FactSalesDetail;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@SpringBootTest
public class FactSalesDetailsValidationTest {

    @Autowired
    private Validator validator;

        /*FactSalesDetail validFactSalesDetail = new FactSalesDetail(
                1L, // appCompanyId (@ValidSnowflakeId 想定: 正の整数)
                "1",    // transactionHeadId (@ValidSmaregiId min=1)
                "1",    // transactionDetailId (@ValidSmaregiId max=999 なので "1" はOK)
                "1001",     // productId (@ValidSmaregiId)
                "カット",   // productName (任意)
                "技術売上", // categoryGroupName (任意)
                1,  // quantity (@Min(1) なので 1以上)
                5000,   // salesPrice (金額)
                1,  // taxDivision (0:込, 1:抜, 2:非 のいずれか)
                "SALES" // categoryType ("SALES" または "REFUND")
        );*/

    @Test
    void transactionDetailIdTest(){
        FactSalesDetail validFactSalesDetail = new FactSalesDetail(
                1L, // appCompanyId (@ValidSnowflakeId 想定: 正の整数)
                "1",    // transactionHeadId (@ValidSmaregiId min=1)
                null,    // エラー値
                "1001",     // productId (@ValidSmaregiId)
                "カット",   // productName (任意)
                "技術売上", // categoryGroupName (任意)
                1,  // quantity (@Min(1) なので 1以上)
                5000,   // salesPrice (金額)
                1,  // taxDivision (0:込, 1:抜, 2:非 のいずれか)
                "SALES" // categoryType ("SALES" または "REFUND")
        );

        // Act
        Set<ConstraintViolation<FactSalesDetail>> violations = validator.validate(validFactSalesDetail);

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
            .anyMatch(v -> v.getMessage().equals("取引明細IDは必須です"));
            
        assertThat(messageFound).isTrue();
       
    }

    @Test
    void quantitySizetest(){
        FactSalesDetail validFactSalesDetail = new FactSalesDetail(
                1L, // appCompanyId (@ValidSnowflakeId 想定: 正の整数)
                "1",    // transactionHeadId (@ValidSmaregiId min=1)
                "1",    // transactionDetailId (@ValidSmaregiId max=999 なので "1" はOK)
                "1001",     // productId (@ValidSmaregiId)
                "カット",   // productName (任意)
                "技術売上", // categoryGroupName (任意)
                0,  // quantity (@Min(1) なので 1以下となりエラー)
                5000,   // salesPrice (金額)
                1,  // taxDivision (0:込, 1:抜, 2:非 のいずれか)
                "SALES" // categoryType ("SALES" または "REFUND")
        );

        // Act
        Set<ConstraintViolation<FactSalesDetail>> violations = validator.validate(validFactSalesDetail);

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
            .anyMatch(v -> v.getMessage().equals("数量は1以上で指定してください"));
            
        assertThat(messageFound).isTrue();

        FactSalesDetail validFactSalesDetail_1 = new FactSalesDetail(
                1L, // appCompanyId (@ValidSnowflakeId 想定: 正の整数)
                "1",    // transactionHeadId (@ValidSmaregiId min=1)
                "1",    // transactionDetailId (@ValidSmaregiId max=999 なので "1" はOK)
                "1001",     // productId (@ValidSmaregiId)
                "カット",   // productName (任意)
                "技術売上", // categoryGroupName (任意)
                1999999,  // quantity (@Max(999999) なので 999999以上となりエラー)
                5000,   // salesPrice (金額)
                1,  // taxDivision (0:込, 1:抜, 2:非 のいずれか)
                "SALES" // categoryType ("SALES" または "REFUND")
        );

        // Act
        Set<ConstraintViolation<FactSalesDetail>> violations_1 = validator.validate(validFactSalesDetail_1);

        // Assert
        assertThat(violations_1).isNotEmpty();

                // メッセージがプロパティファイルから取得できているか確認
        boolean messageFound_1 = violations_1.stream()
            .anyMatch(v -> v.getMessage().equals("数量は999999以下で指定してください"));
            
        assertThat(messageFound_1).isTrue();

    }

    @Test
    void taxDivisionTest(){
        FactSalesDetail validFactSalesDetail = new FactSalesDetail(
                1L, // appCompanyId (@ValidSnowflakeId 想定: 正の整数)
                "1",    // transactionHeadId (@ValidSmaregiId min=1)
                "1",    // transactionDetailId (@ValidSmaregiId max=999 なので "1" はOK)
                "1001",     // productId (@ValidSmaregiId)
                "カット",   // productName (任意)
                "技術売上", // categoryGroupName (任意)
                1,  // quantity (@Min(1) なので 1以上)
                5000,   // salesPrice (金額)
                3,  // taxDivision ０〜２以外の数値はエラー
                "SALES" // categoryType ("SALES" または "REFUND")
        );

        // Act
        Set<ConstraintViolation<FactSalesDetail>> violations = validator.validate(validFactSalesDetail);

        // Assert
        assertThat(violations).isNotEmpty();

                // メッセージがプロパティファイルから取得できているか確認
        boolean messageFound = violations.stream()
            .anyMatch(v -> v.getMessage().equals("税区分は0,1,2です"));
            
        assertThat(messageFound).isTrue();

    }


}
