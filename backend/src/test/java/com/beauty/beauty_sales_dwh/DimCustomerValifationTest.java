package com.beauty.beauty_sales_dwh;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.beauty.beauty_sales_dwh.analytics.customer.DimCustomer;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@SpringBootTest
public class DimCustomerValifationTest {

    @Autowired
    private Validator validator;

    @Test
    void defaultValueTest(){
        DimCustomer defaultValuedCustomer = new DimCustomer(
            1L,
            "10002",
            "佐藤_花子",
            "サトウ_ハナコ",
            null,                    // phoneNumber (null許可)
            null,                    // mobileNumber (null許可)
            "1",
            null,                    // firstVisitDate (null許可)
            null,                    // lastVisitDate (null許可)
            0,                       // visitCount
            null,                    // isDeleted -> コンストラクタで false に変換されるはず
            null,               // insertDataTime -> コンストラクタで 現在日時になるはず
            null            // updateDataTime -> コンストラクタで 現在日時 になるはず
        );
        // Act
        Set<ConstraintViolation<DimCustomer>> violations = validator.validate(defaultValuedCustomer);
        // Assert
        assertThat(violations).isEmpty();
        System.out.println(defaultValuedCustomer);
    }


}
