package com.beauty.beauty_sales_dwh;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.beauty.beauty_sales_dwh.analytics.product.DimCategoryGroup;
import com.beauty.beauty_sales_dwh.analytics.product.DimProduct;
import com.beauty.beauty_sales_dwh.analytics.staff.DimStaff;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@SpringBootTest
@ActiveProfiles("test")
public class OtherModelValifationTest {
    
    @Autowired
    private Validator validator;

    @Test
    void dimproductTest(){
        //正常系
        DimProduct validProduct = new DimProduct(
        1L,                      // appCompanyId
        "20001",                 // productId (@ValidSmaregiId)
        "デザインカット(S/B込)",   // productName (200文字以内, <>禁止, 一般的な記号はOK)
        "100",                   // catGroupId (FK -> DimCategoryGroup)
        5500,                    // price (マスタ単価)
        "1",                     // storeId
        null,                    // insertDataTime -> デフォルトで現在日時
        null                     // updateDataTime -> デフォルトで現在日時
        );
        // Act
        Set<ConstraintViolation<DimProduct>> violations = validator.validate(validProduct);

        // Assert
        assertThat(violations).isEmpty();
        System.out.println(validProduct);

    }

    @Test
    void DimStaffTest(){
        // 正常系インスタンス (バリデーションOK)
        DimStaff validStaff = new DimStaff(
            1L,                      // appCompanyId
            "30001",                 // staffId (@ValidSmaregiId)
            "美容_師太郎",            // staffName (漢字, カナ, 英数, スペース, _, -, 々 許可)
            "トップスタイリスト",      // rank (50文字以内, <>禁止)
            "1",                     // storeId
            1,                       // employFlag (0:退職, 1:在籍)
            null,                    // insertDataTime -> デフォルトで現在日時
            null                     // updateDataTime -> デフォルトで現在日時
        );
        // Act
        Set<ConstraintViolation<DimStaff>> violations = validator.validate(validStaff);
        // Assert
        assertThat(violations).isEmpty();
        System.out.println(validStaff);
    }

    @Test
    void dimStaffStaffName(){
        // staffNameに禁止記号
        DimStaff validStaff_1 = new DimStaff(
            1L,                      // appCompanyId
            "30001",                 // staffId (@ValidSmaregiId)
            "<>_",            // staffName (禁止記号)
            "トップスタイリスト",      // rank (50文字以内, <>禁止)
            "1",                     // storeId
            1,                       // employFlag (0:退職, 1:在籍)
            null,                    // insertDataTime -> デフォルトで現在日時
            null                     // updateDataTime -> デフォルトで現在日時
        );
        // Act
        Set<ConstraintViolation<DimStaff>> violations_1 = validator.validate(validStaff_1);
        // Assert
        assertThat(violations_1).isNotEmpty();

        // メッセージがプロパティファイルから取得できているか確認
        boolean messageFound_1 = violations_1.stream()
            .anyMatch(v -> v.getMessage().equals("スタッフ名は漢字、ひらがな、カタカナ、英数字(半角/全角)、スペース、記号(ー 々)、アンダーバーのみのみ入力可です"));
            
        assertThat(messageFound_1).isTrue();

    }

    @Test
    void dimStaffRank(){
        // rankに禁止記号
        DimStaff validStaff_1 = new DimStaff(
            1L,                      // appCompanyId
            "30001",                 // staffId (@ValidSmaregiId)
            "山田_太郎",            // staffName (禁止記号)
            "トップスタイ<>リスト",      // rank (50文字以内, <>禁止)
            "1",                     // storeId
            1,                       // employFlag (0:退職, 1:在籍)
            null,                    // insertDataTime -> デフォルトで現在日時
            null                     // updateDataTime -> デフォルトで現在日時
        );
        // Act
        Set<ConstraintViolation<DimStaff>> violations_1 = validator.validate(validStaff_1);
        // Assert
        assertThat(violations_1).isNotEmpty();

        // メッセージがプロパティファイルから取得できているか確認
        boolean messageFound_1 = violations_1.stream()
            .anyMatch(v -> v.getMessage().equals("ランクで記号< >を含む文字列は登録できません"));
            
        assertThat(messageFound_1).isTrue();

    }

    @Test
    void dimStaffemployFlag(){
        // employflagエラー値
        DimStaff validStaff = new DimStaff(
            1L,                      // appCompanyId
            "30001",                 // staffId (@ValidSmaregiId)
            "美容_師太郎",            // staffName (漢字, カナ, 英数, スペース, _, -, 々 許可)
            "トップスタイリスト",      // rank (50文字以内, <>禁止)
            "1",                     // storeId
            -1,                       // employFlag (0:退職, 1:在籍)
            null,                    // insertDataTime -> デフォルトで現在日時
            null                     // updateDataTime -> デフォルトで現在日時
        );
        // Act
        Set<ConstraintViolation<DimStaff>> violations = validator.validate(validStaff);
        // Assert
        assertThat(violations).isNotEmpty();

        // メッセージがプロパティファイルから取得できているか確認
        boolean messageFound = violations.stream()
            .anyMatch(v -> v.getMessage().equals("削除フラグは0,1で指定してください"));
            
        assertThat(messageFound).isTrue();

        // employflagエラー値
        DimStaff validStaff_1 = new DimStaff(
            1L,                      // appCompanyId
            "30001",                 // staffId (@ValidSmaregiId)
            "美容_師太郎",            // staffName (漢字, カナ, 英数, スペース, _, -, 々 許可)
            "トップスタイリスト",      // rank (50文字以内, <>禁止)
            "1",                     // storeId
            2,                       // employFlag (0:退職, 1:在籍)
            null,                    // insertDataTime -> デフォルトで現在日時
            null                     // updateDataTime -> デフォルトで現在日時
        );
        // Act
        Set<ConstraintViolation<DimStaff>> violations_1 = validator.validate(validStaff_1);
        // Assert
        assertThat(violations_1).isNotEmpty();

        // メッセージがプロパティファイルから取得できているか確認
        boolean messageFound_1 = violations_1.stream()
            .anyMatch(v -> v.getMessage().equals("削除フラグは0,1で指定してください"));
            
        assertThat(messageFound_1).isTrue();

    }

    @Test
    void dimCategoryGroupTest(){
        // 正常系インスタンス (バリデーションOK)
        DimCategoryGroup validCategoryGroup = new DimCategoryGroup(
            1L,                      // appCompanyId (@ValidSnowflakeId)
            "100",                   // catGroupId (@ValidSmaregiId: 数値文字列)
            "カットメニュー",          // catGroupName (<> 禁止, 記号OK)
            null,                    // insertDataTime -> デフォルトで現在日時
            null                     // updateDataTime -> デフォルトで現在日時
        );
        // Act
        Set<ConstraintViolation<DimCategoryGroup>> violations = validator.validate(validCategoryGroup);

        // Assert
        assertThat(violations).isEmpty();
        System.out.println(validCategoryGroup);

    }
}
