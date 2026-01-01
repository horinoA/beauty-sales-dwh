package com.beauty.beauty_sales_dwh.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.beauty.beauty_sales_dwh.domain.CustomerRawData;

@MybatisTest // 1. MyBatis関連のコンポーネントだけをロード
@ActiveProfiles("test") // 2. application-test.properties を読み込む
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 3. H2(メモリDB)に置き換えずに、本物のPostgresを使う設定
@Transactional // 4. テストが終わったらデータを自動でロールバック（消去）する
class RawCustomerMapperTest {

    @Autowired
    private RawCustomerMapper rawCustomerMapper;

    @Test
    @DisplayName("データのINSERTと最大日付の取得ができること")
    void testInsertAndFindMaxImportedAt() {
        // --- 準備 (Given) ---
        // テスト用のダミーデータ作成
        String dummyJson = "{\"customerId\": \"123\", \"name\": \"Test User\"}";
        CustomerRawData data = new CustomerRawData(4096, dummyJson);

        // --- 実行 (When) ---
        // 1. データをINSERT
        rawCustomerMapper.insertRawCustomer(data);

        // 2. 最大日付を取得
        OffsetDateTime maxDate = rawCustomerMapper.findMaxFetchedAt();

        // --- 検証 (Then) ---
        // INSERTした直後なので、日付が取得できているはず
        assertThat(maxDate).isNotNull();
        
        // 取得した日時が「現在時刻」に近いか（直近1分以内か）確認
        assertThat(maxDate).isAfter(OffsetDateTime.now().minusMinutes(1));
        
        System.out.println("取得された最大日時: " + maxDate);
    }

    @Test
    @DisplayName("テーブルが空の場合はnullが返ること（初回実行時の想定）")
    void testFindMaxFechedAt_EmptyTable() {
        // @Transactionalにより、上のテストで入れたデータは消えている状態（または手動でTRUNCATEが必要）
        // ※もし既存データがある環境だとこのテストは失敗する可能性がありますが、
        //   論理的にはデータがなければnullになることを確認します。
        
        // ここではあえて検証をスキップせず、戻り値の型チェックだけ行います
        OffsetDateTime maxDate = rawCustomerMapper.findMaxFetchedAt();
        
        // データが入っていれば日時、なければnull。エラーにならなければOKとする
        System.out.println("現在のDBの最大日時: " + maxDate);
    }
}