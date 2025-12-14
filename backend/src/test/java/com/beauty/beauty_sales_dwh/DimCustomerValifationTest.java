package com.beauty.beauty_sales_dwh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
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

    // ----------------------------------------------------------------
    // null許可 テスト
    // ----------------------------------------------------------------

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
    // ----------------------------------------------------------------
    // 1. @ValidCustomerDates (来店日の整合性) テスト
    // ----------------------------------------------------------------

    @Test
    @DisplayName("来店日整合性: 初回 <= 最終 ならOK")
    void validCustomerDates_Normal() {
        // 初回が最終より過去
        DimCustomer customer = createDimCustomer(
            LocalDate.of(2023, 1, 1), 
            LocalDate.of(2023, 1, 31)
        );
        Set<ConstraintViolation<DimCustomer>> violations = validator.validate(customer);
        assertTrue(violations.isEmpty(), "正常な日付範囲ならエラーにならない");
    }

    @Test
    @DisplayName("来店日整合性: 初回 == 最終 ならOK")
    void validCustomerDates_SameDate() {
        // 同日
        DimCustomer customer = createDimCustomer(
            LocalDate.of(2023, 1, 1), 
            LocalDate.of(2023, 1, 1)
        );
        Set<ConstraintViolation<DimCustomer>> violations = validator.validate(customer);
        assertTrue(violations.isEmpty(), "同日でもエラーにならない");
    }

    @Test
    @DisplayName("来店日整合性: 片方がnullならチェックをスキップしてOK")
    void validCustomerDates_Null() {
        // 初回のみあり、最終なし
        DimCustomer customer = createDimCustomer(
            LocalDate.of(2023, 1, 1), 
            null
        );
        Set<ConstraintViolation<DimCustomer>> violations = validator.validate(customer);
        assertTrue(violations.isEmpty(), "nullが含まれる場合は相関チェックはスキップされるべき");
    }

    @Test
    @DisplayName("来店日整合性エラー: 初回 > 最終 の場合はエラー")
    void validCustomerDates_Error() {
        // 初回(2月1日) > 最終(1月1日) → 矛盾
        DimCustomer customer = createDimCustomer(
            LocalDate.of(2023, 2, 1), 
            LocalDate.of(2023, 1, 1)
        );

        Set<ConstraintViolation<DimCustomer>> violations = validator.validate(customer);
        
        // クラスレベルのアノテーションなので、プロパティパスが空、またはクラス名になることが多い
        assertFalse(violations.isEmpty(), "日付逆転でエラーになるべき");
        
        // エラーメッセージの確認（必要に応じてメッセージ内容を調整してください）
        boolean hasError = violations.stream()
            .anyMatch(v -> v.getMessage().contains("最終来店日は初回来店日以降である必要があります"));
        assertTrue(hasError);
    }

    // ----------------------------------------------------------------
    // 2. customerName (氏名) テスト
    // ----------------------------------------------------------------

    @Test
    @DisplayName("氏名: 許可された文字種が全て通ること")
    void customerName_Normal_AllTypes() {
        // 漢字, ひらがな, カタカナ, 英字(半角/全角), 数字(半角/全角), スペース, 記号(ー 々), アンダーバー
        String validName = "山田_太郎 ヤマダ Ｔａｒｏ 123 １２３ 佐々木 メアリー・ジェーン"; 
        // ※正規表現に「・」は含まれていませんが、「ー(長音)」は含まれています。
        //  今回の正規表現 ^[a-zA-Zａ-ｚＡ-Ｚ0-9０-９一-龠ぁ-んァ-ヶー々\\s_]+$ に合わせるため修正
        String validNameStrict = "山田_太郎 ヤマダ Ｔａｒｏ 123 １２３ 佐々木 メアリー"; 

        DimCustomer customer = createDimCustomerWithName(validNameStrict);
        Set<ConstraintViolation<DimCustomer>> violations = validator.validate(customer);
        
        if (!violations.isEmpty()) {
            violations.forEach(v -> System.out.println("Name Error: " + v.getMessage()));
        }
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("氏名エラー: 禁止文字(<, >, @, !)が含まれる")
    void customerName_Error_Pattern() {
        String invalidName = "山田<太郎>"; // タグ記号混入

        DimCustomer customer = createDimCustomerWithName(invalidName);
        Set<ConstraintViolation<DimCustomer>> violations = validator.validate(customer);

        assertFalse(violations.isEmpty());
        System.out.println(violations);
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("customerName") && 
            v.getMessage().contains("顧客名は漢字、ひらがな、カタカナ、英数字(半角/全角)、スペース、記号(ー 々)、アンダーバーのみのみ入力可です")
        ));
    }

    @Test
    @DisplayName("氏名エラー: 文字数オーバー (101文字)")
    void customerName_Error_Size() {
        String longName = "あ".repeat(101);

        DimCustomer customer = createDimCustomerWithName(longName);
        Set<ConstraintViolation<DimCustomer>> violations = validator.validate(customer);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("customerName") && 
            v.getMessage().contains("入力できる文字数は100です")
        ));
    }

    // ----------------------------------------------------------------
    // 3. phoneNumber / mobileNumber テスト
    // ----------------------------------------------------------------

    @Test
    @DisplayName("電話番号: 数字とハイフンのみならOK")
    void phoneNumbers_Normal() {
        DimCustomer customer = createDimCustomerWithPhonesCheck(
            "03-1234-5678", 
            "090-1234-5678"
        );
        Set<ConstraintViolation<DimCustomer>> violations = validator.validate(customer);
        assertTrue(violations.isEmpty());
    }
    
    @Test
    @DisplayName("電話番号: ハイフンなし数字のみもOK")
    void phoneNumbers_Normal_NoHyphen() {
        DimCustomer customer = createDimCustomerWithPhonesCheck(
            "0312345678", 
            "09012345678"
        );
        Set<ConstraintViolation<DimCustomer>> violations = validator.validate(customer);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("電話番号エラー: 英字や記号が含まれる")
    void phoneNumbers_Error_Pattern() {
        DimCustomer customer = createDimCustomerWithPhones(
            "03-abcd-5678" // エラー
        );
        Set<ConstraintViolation<DimCustomer>> violations = validator.validate(customer);

        // Assert
        assertThat(violations).isNotEmpty();

                // メッセージがプロパティファイルから取得できているか確認
        boolean messageFound_1 = violations.stream()
            .anyMatch(v -> v.getMessage().equals("電話番号は半角数字と-ハイフンのみ入力可です"));
            
        assertThat(messageFound_1).isTrue();
    }

    @Test
    @DisplayName("携帯番号エラー: 英字や記号が含まれる")
    void mobileNumbers_Error_Pattern() {
        DimCustomer customer = createDimCustomerWithmobile(
            "090(1234)5678"  // エラー
        );
        Set<ConstraintViolation<DimCustomer>> violations = validator.validate(customer);

        // Assert
        assertThat(violations).isNotEmpty();

                // メッセージがプロパティファイルから取得できているか確認
        boolean messageFound_1 = violations.stream()
            .anyMatch(v -> v.getMessage().equals("携帯番号は半角数字と-ハイフンのみ入力可です"));
            
        assertThat(messageFound_1).isTrue();
    }

    @Test
    @DisplayName("電話番号エラー: 文字数オーバー (51文字)")
    void phone_Error_Size() {
        String longPhoneNumber = "0".repeat(51);

        DimCustomer customer = new DimCustomer(
            1L,                 // appCompanyId
            "10001",            // customerId
            "テスト_太郎",       // customerName
            "テスト_タロウ",     // customerKana
            longPhoneNumber,     // phoneNumber
            "090-0000-0000",    // mobileNumber
            "1",                // storeId
            LocalDate.now().minusDays(1), // firstVisitDate
            LocalDate.now(),    // lastVisitDate
            null,               // visitCount -> default 0
            null,               // isDeleted -> default false
            null,               // insert -> now
            null                // update -> now
        );
        Set<ConstraintViolation<DimCustomer>> violations = validator.validate(customer);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("phoneNumber") && 
            v.getMessage().contains("入力できる文字数は50です")
        ));
    }


    // ----------------------------------------------------------------
    // ヘルパーメソッド (テストデータ生成用)
    // ----------------------------------------------------------------

    /**
     * 基本的にバリデーションを通過する正常なオブジェクトを生成
     */
    private DimCustomer createValidBase() {
        return new DimCustomer(
            1L,                 // appCompanyId
            "10001",            // customerId
            "テスト_太郎",       // customerName
            "テスト_タロウ",     // customerKana
            "03-0000-0000",     // phoneNumber
            "090-0000-0000",    // mobileNumber
            "1",                // storeId
            LocalDate.now().minusDays(1), // firstVisitDate
            LocalDate.now(),    // lastVisitDate
            null,               // visitCount -> default 0
            null,               // isDeleted -> default false
            null,               // insert -> now
            null                // update -> now
        );
    }

    // 日付テスト用
    private DimCustomer createDimCustomer(LocalDate first, LocalDate last) {
        return new DimCustomer(
            1L, "10001", "テスト", "テスト", "03-0000", "090-0000", "1",
            first, last, // ★ここを指定
            null, null, null, null
        );
    }

    // 氏名テスト用
    private DimCustomer createDimCustomerWithName(String name) {
        return new DimCustomer(
            1L, "10001", 
            name, // ★ここを指定
            "テスト", "03-0000", "090-0000", "1",
            LocalDate.now(), LocalDate.now(),
            null, null, null, null
        );
    }


// 電話番号テスト用
    private DimCustomer createDimCustomerWithPhonesCheck(String phone,String mobile) {
        return new DimCustomer(
            1L, "10001", "テスト", "テスト", 
            phone, // ★ここを指定
            mobile, 
            "1",
            LocalDate.now(), LocalDate.now(),
            null, null, null, null
        );
    }




    // 電話番号テスト用
    private DimCustomer createDimCustomerWithPhones(String phone) {
        return new DimCustomer(
            1L, "10001", "テスト", "テスト", 
            phone, // ★ここを指定
            "090-0000-0000", 
            "1",
            LocalDate.now(), LocalDate.now(),
            null, null, null, null
        );
    }
    
    // 携帯番号テスト用
    private DimCustomer createDimCustomerWithmobile(String mobile) {
        return new DimCustomer(
            1L, "10001", "テスト", "テスト", 
            "07-0000-0000", 
            mobile, // ★ここを指定
            "1",
            LocalDate.now(), LocalDate.now(),
            null, null, null, null
        );
    }


}
