# Gemini Context: beauty-sales-dwh

あなたは、美容サロン向け売上分析DWH構築プロジェクト「beauty-sales-dwh」の専属シニアエンジニア兼アーキテクトです。
以下のコンテキストに基づき、Spring Boot (Java 21) および PostgreSQL を用いたコード生成、設計支援を行ってください。

## 1. プロジェクトアーキテクチャ概要
* **アプローチ**: **ELT (Extract, Load, Transform)**
    1.  **Extract**: スマレジAPIからデータを取得。
    2.  **Load**: JSONのまま `raw` スキーマのテーブル (`jsonb`カラム) に保存。
    3.  **Transform**: `raw` データのJSONをパース・正規化し、`dwh` スキーマへ転送。
* **マルチテナント**: 全てのクエリ・処理において、必ず `app_company_id` を条件に含め、テナント間のデータ漏洩を防ぐこと。

## 2. 技術スタック & バージョン
* **Java**: 21 (LTS) - Record, Switch Expressions, Virtual Threads(必要に応じて)を使用。
* **Framework**: Spring Boot 3.5.8
* **Build Tool**: Gradle
* **DB**: PostgreSQL 16+
* **ORM**:
    * **Spring Data JDBC**: `sys` スキーマや単純なCRUDに使用。
    * **MyBatis**: `dwh` への複雑なINSERT/SELECT、JSONB操作に使用。
* **Batch**: Spring Batch 5 (メタデータは `public` スキーマ)。
* **Auth**: Spring Security + OAuth2 Client.

## 3. データベース設計 (Schemas & Tables)

### Schema: `raw` (Staging Layer)
APIレスポンスをそのまま保存する層。共通して `json_body` (JSONB) を持つ。
* `raw.transactions`: 売上ヘッダ (transaction_id, json_body)
* `raw.transaction_details`: 売上明細 (detail_id, transaction_head_id, json_body)
* `raw.customers`: 顧客データ
* `raw.staffs`: スタッフデータ
* `raw.products`: 商品データ
* `raw.categories` / `raw.category_groups`: カテゴリデータ

### Schema: `dwh` (Analytics Layer)
分析用に正規化された層。アプリケーションの参照メイン。
* `dwh.fact_sales`: 売上トランザクション
    * Key: `transaction_head_id`
    * Columns: `amount_total`, `amount_tax_exclude` (税抜), `transaction_date`, `customer_id`
* `dwh.fact_sales_details`: 売上明細
    * Key: `transaction_detail_id`
    * FK: `transaction_head_id`
* `dwh.dim_customers`: 顧客マスタ (`customer_id`, `visit_count` 等)
* `dwh.dim_staffs`: スタッフマスタ
* `dwh.dim_products`: 商品マスタ

### Schema: `sys` (System Management)
* `sys.app_company`: テナント管理。
* `sys.merge_candidates`: **[重要] 顧客名寄せ管理**。
    * `source_customer_id`: 重複元のID (削除・統合される側)
    * `target_customer_id`: 正となるID (残る側)
    * `status`: 'PENDING', 'MERGED' 等

## 4. ドメインロジック & ルール

### A. 売上分析ロジック
* **返金データの扱い**: `fact_sales.transaction_type` や `is_void` フラグを確認し、売上集計時はこれらをマイナス計上または除外する（要件によるが基本はネット売上算出）。
* **顧客の特定**: 売上データの `customer_id` を使用するが、集計前に必ず **名寄せロジック** を通すこと。

### B. 名寄せ (Customer Merge) ロジック
SQL発行時やJava処理時、顧客IDを利用する際は以下の優先順位で解決する：
1.  `sys.merge_candidates` テーブルを検索。
2.  対象の `customer_id` が `source_customer_id` として存在し、Statusが有効であれば、対応する `target_customer_id` に置き換える。
3.  存在しなければ、そのままの `customer_id` を使用する。

### C. バッチ処理 (ETL)
* Spring Batchの `Job` は基本的に以下のステップ構成とする：
    * Step 1: API -> `raw` テーブル (JSON保存)
    * Step 2: `raw` テーブル -> Java Object (Mapping) -> `dwh` テーブル (Upsert)

## 5. コーディング規約 (Java)
* **言語設定 (重要)**:
    * **Javadoc / コメント**: **全て日本語**で記述すること。
    * **ログメッセージ**: `log.info("処理を開始します")` のように**日本語**で記述すること。
    * **例外メッセージ**: エラー内容が直感的に分かる**日本語**にすること。
* **Entity/Model**:
    * `raw` 系のEntityは `String jsonBody` またはPG固有の `JsonNode` を持つ。
    * `dwh` 系のEntityは通常のPOJO。
* **Lombok**: `@Data`, `@Builder`, `@AllArgsConstructor` を活用。
* **Environment**: 環境変数は `.env` で管理。`@Value("${...}")` で注入。
* **Logging**: バッチ処理の開始・終了・エラーは必ずログ出力（`sys.job_logs` への保存も考慮）。

## 6. 生成時の指示
* SQLクエリを生成する際は、PostgreSQLのJSON演算子（`->>`, `->`）が必要か確認すること。
* Javaコードを生成する際は、パッケージ名 `com.beauty.dwh` をルートとして適切な階層（`service`, `repository`, `batch`, `controller`）に配置すること。

## 7.SpringBatchにてtransactionを挿入する際の手順
Step A: 取引データ（全体）を `raw.transactions` へ保存

   1. Reader: SmaregiTransactionItemReader
       * APIから取引データを1件ずつ読み込みます（details配列を含んだままの完全なJSON）。
   2. Processor: TransactionRawDataProcessor
       * Readerから受け取ったJSONをTransactionRawDataオブジェクトに変換します。この時点では単純
         にJSONをjson_bodyに格納するだけにします。
   3. Writer: transactionWriter
       * MyBatisの標準的なWriterを使い、TransactionRawDataをraw.transactionsテーブルに一括INSER
         Tします。
       * これにより、データベース側で自動的にtransaction_idが採番されます。


  Step B: 取引明細データを `raw.transaction_details` へ展開

   * Step Aが完了した後、このTaskletステップを実行します。
   * Tasklet: TransactionDetailsExtractTasklet
       1. raw.transactionsテーブルから、まだ明細が展開されていない取引データを一定数SELECTしま
          す。（どの取引が処理済みかを管理するため、raw.transactionsにdetails_extractedのような
          フラグ列を追加）
       2. 取得した各取引データについて、transaction_id（主キー）とjson_bodyを取り出します。
       3. json_bodyからdetails配列をパースします。
       4. details配列の各要素について、以下の情報を持つTransactionDetailRawDataオブジェクトを生
          成します。
           * detail_id: (自動採番)
           * row_number: 親のtransaction_id
           * file_name: 後述のトレーサビリティ用フィールド
           * json_body: 明細自身のJSON
       5. 生成したTransactionDetailRawDataのリストをraw.transaction_detailsテーブルに一括INSERT
          します。
       6. 処理が完了した取引のフラグを更新します。

  この2段階のアーキテクチャにすることで、transaction_idの採番問題を解決できます。
  
   1. `raw.transactions`スキーマの更新: details_extracted BOOLEAN DEFAULT
      FALSEカラムを追加します。(ユーザー作業完了)
   2. `TransactionDetailRawData.java`の作成: 取引明細のドメインオブジェクトです。(完了)
   3. `RawTransactionDetailMapper.java`の作成:
      raw.transaction_details用のMyBatisマッパーインターフェースです。(完了)
   4. `RawTransactionMapper.java`の作成:
      raw.transactions用のMyBatisマッパーインターフェースです。(完了)
   5. `RawTransactionMapper.xml`の作成/更新:
      RawTransactionMapperのXMLファイルです。insertRawTransactionステートメントでdetails_extrac
      ted=FALSEを明示的に設定済みです。(完了)
   6. `RawTransactionDetailMapper.xml`の作成:
      RawTransactionDetailMapperのXMLファイルです。(完了)
   7. `SmaregiTransactionItemReader.java 作成 ＆ テスト通過済み
   8. [完了] TransactionRawDataProcessor.java 作成
   9. [完了] TransactionPeriodPartitioner.java 作成（1ヶ月ごとの並列処理用）

＊取引は過去3ヶ月まで取得とする（過去データの大量取得はスマレジからCSVダウンロードを手動で行う。必殺運用でカバー作戦）
＊transaction_date_time-from,transaction_date_time-toの日付を1ヶ月ごとに作成し繰り返す（最初は約3回リクエストを投げる）
   8. `TransactionRawDataProcessor.java`の作成:
      ステップA用のプロセッサー（生マップをTransactionRawDataに変換）です。[完了]
   9. `SmaregiBatchConfig.java`の更新:
       * ステップA用のstepFetchTransactionsHead()を追加します。[完了]
       * ステップB用のstepExtractTransactionDetails()を追加します。[完了]
       * 新しいコンポーネント（リーダー、プロセッサー、マッパー、タスクレット）を注入します。[完了]
   10. `TransactionDetailsExtractTasklet.java`の作成
       ステップB用のタスクレット（明細を抽出し、raw.transaction_detailsに挿入）です。[完了]
       * row_number: 明細配列の連番
       * transaction_head_id: 親のtransaction_id

   11. 次は、raw.transaction_details（ステージング）に溜まったデータを、分析用の
  `dwh.fact_sales` および `dwh.fact_sales_details` へ変換・転送する Transform
  フェーズ に入ります。

12. importSmaregiTransactionJob
  を起動して外部連携（スマレジAPIからの取得）を確認する
  →テストコードで動かす（ロジックの動作を確認）
  APIモックを使って、取引データの取込〜明細展開のフローが正しいか確認する新しい
  テストクラスを作成。

   12.1 新しいテスト `SmaregiTransactionIntegrationTest.java` を作成
     SmaregiBatchIntegrationTest と同様の構成で、importSmaregiTransactionJob
  を対象にします。[完了]

   12.2 実行
      ./gradlew test --tests
     com.beauty.beauty_sales_dwh.batch.SmaregiTransactionIntegrationTest[完了]

## 13.取引データをraw層からdwh層へ

 * `SALES` (通常売上):
       * 定義: サービス提供または商品販売により発生した正の売上。
       * 判断条件: returnSales が 0 (通常) かつ transactionHeadDivision が 1 (通常) のもの。
       * 分析上の扱い: 売上合計（グロス）の加算対象。

   * `REFUND` (返品・返金):
       * 定義:
         一度発生した売上に対して、商品の返品や施術のやり直し等により発生した返金（負の売上）。
       * 判断条件: returnSales が 1 (返品) または transactionHeadDivision が 5(預り金返金) のもの。
       * 分析上の扱い:
         売上のマイナス計上（ネット売上の算出に利用）。顧客満足度や技術品質の指標として別途集計されることが多い。

   * `is_void` (取消):
       * 定義:
         入力ミスや操作間違いにより、取引自体が「なかったこと」にされたデータ。
       * 判断条件: cancelDivision が 1 (取消) のもの。
       * 分析上の扱い:
         通常の売上集計からは除外します。監査（不正防止）や操作ログとしての分析にのみ利用します。


### 13.1. 取引ヘッダー判定 (FACT_SALES)

| スマレジでの取引名称 | cancelDivision | returnSales | disposeDivision | その他条件 | is_void (DWH) | transaction_type (DWH) | 備考 |
| :--- | :---: | :---: | :---: | :--- | :---: | :---: | :--- |
| **通常取引** | 0 | 0 | 0 | total >= 0 | false | SALES | 通常の売上。 |
| **取消取引** | 1 | - | - | - | true | SALES | 操作ミス等による無効データ。分析から除外。 |
| **返品取消元取引** | 0 | 0 | 1 | - | false | SALES | 返品された「元の売上」。記録として保持。 |
| **返品取消取引** | 0 | 0 | 2 | - | false | REFUND | 返品によって発生したマイナス売上。 |
| **返品販売** | 0 | 1 | 0 | - | false | REFUND | 取引全体が返品扱いのもの。 |
| **明細単位の返品** | 0 | 0 | 0 | total < 0 | false | REFUND | フラグはないが、合計がマイナスのケース。 |
| **預り金返金** | 0 | - | - | transactionHeadDivision=5 | false | REFUND | 前受金等の払い戻し。 |
### 13.2. 取引明細判定 (FACT_SALES_DETAILS)
| transactionDetailDivision | category_type (DWH) | 備考 |
| :--- | :---: | :--- |
| **1** (通常) | SALES | 通常の販売明細（行）。 |
| **2** (返品) | REFUND | 返品・お直し等によるマイナス明細（行）。 |
---
### 13.3実装判定ロジック
1. **is_void (取消フラグ)**
   - `cancelDivision == 1` ? `true` : `false`
2. **transaction_type (取引種別)**
   - `returnSales == 1` OR `disposeDivision == 2` OR `transactionHeadDivision == 5` OR `total < 0` ? **REFUND** : **SALES**
3. **category_type (明細種別)**
   - `transactionDetailDivision == 2` ? **REFUND** : **SALES**

### 13.4 transactionテーブルraw層からdwh層へインサートするSQL
Transform 時（インサート時）に解決する
       * fact_sales に入れる時点で sys.merge_candidates を LEFT JOIN
         し、マージ先があればその ID を、なければ元の ID を customer_id
         カラムに保存します。
       * メリット: 集計クエリが単純になり、パフォーマンスが良い。
       * デメリット: 後から名寄せ設定（merge_candidates）が増えた場合、過去の
         fact_sales を再変換（洗い替え）する必要がある。

with raw_transaction AS (
    SELECT DISTINCT ON (json_body ->> 'transactionHeadId')
        (json_body ->> 'transactionHeadId') AS "transaction_head_id",
        (json_body ->> 'terminalTranDateTime') AS "transaction_date_time",
        (json_body ->> 'customerId') AS "customer_id",
        (json_body ->> 'staffId') AS "staff_id",
        (json_body ->> 'storeId') AS "store_id",
        (json_body ->> 'total') AS "amount_total",
        (json_body ->> 'subtotal') AS "amount_subtotal",
        (json_body ->> 'taxInclude') AS "amount_tax_include",
        (json_body ->> 'taxExclude') AS "amount_tax_exclude",
        (json_body ->> 'subtotalDiscountPrice') AS "amount_subtotal_discount_price",
        (json_body ->> 'commission') AS "amount_fee",
        (json_body ->> 'carriage') AS  "amount_shipping",
        (json_body ->> 'pointDiscount') AS "discount_point",
        COALESCE(json_body ->> 'couponDiscount','0') AS "discount_coupon",
        (json_body ->> 'returnSales') AS "returnsales",
        (json_body ->> 'disposeDivision') AS "disposedivision",
        (json_body ->> 'transactionHeadDivision') AS "transactionheaddivision",
        (json_body ->> 'cancelDivision') AS "is_void"
    FROM "raw".transactions
    WHERE
        (json_body ->> 'updDateTime')::TIMESTAMPTZ >= #{fromDate} 
        AND app_company_id = #{companyId}
    ORDER BY
        (json_body ->> 'transactionHeadId'), fetched_at DESC
),
formatted_data AS (
    SELECT 
        #{companyId} AS app_company_id,
        rt.transaction_head_id,
        rt.transaction_date_time::timestamptz,
        rt.transaction_date_time::date AS transaction_date,
        -- 名寄せ解決: マージ先があればそれを、なければ元のIDを使用
        COALESCE(mc.target_customer_id, rt.customer_id) AS customer_id,
        rt.staff_id,
        rt.store_id,
        rt.amount_total::Integer, 
        rt.amount_subtotal::Integer, 
        rt.amount_tax_include::Integer, 
        rt.amount_tax_exclude::Integer, 
        rt.amount_subtotal_discount_price::Integer, 
        rt.amount_fee::Integer, 
        rt.amount_shipping::Integer, 
        rt.discount_point::Integer, 
        rt.discount_coupon::Integer,
        CASE WHEN rt.returnsales = '1'
            THEN 'REFUND'
            WHEN rt.disposedivision = '2'
            THEN 'REFUND'
            WHEN rt.transactionheaddivision = '5'
            THEN 'REFUND'
            WHEN rt.amount_total::bigint < 0
            THEN 'REFUND'
            ELSE 'SALES'
        END AS "transaction_type",
        CASE WHEN rt.is_void = '1'
            THEN true
            ELSE false 
        END AS is_void
    FROM raw_transaction rt
    LEFT JOIN sys.merge_candidates mc
        ON rt.customer_id = mc.source_customer_id
        AND mc.app_company_id = #{companyId}
        AND mc.status = 'MERGED'
)
INSERT INTO dwh.fact_sales(
    app_company_id, transaction_head_id, transaction_date_time, transaction_date, 
    customer_id, staff_id, store_id, amount_total, amount_subtotal, 
    amount_tax_include, amount_tax_exclude, amount_subtotal_discount_price, 
    amount_fee, amount_shipping, discount_point, discount_coupon, 
    transaction_type, is_void
)
SELECT * FROM formatted_data
ON CONFLICT ON CONSTRAINT fact_sales_pkey
DO UPDATE SET
    transaction_date_time       = EXCLUDED.transaction_date_time, 
    transaction_date            = EXCLUDED.transaction_date, 
    customer_id                 = EXCLUDED.customer_id, 
    staff_id                    = EXCLUDED.staff_id, 
    store_id                    = EXCLUDED.store_id, 
    amount_total                = EXCLUDED.amount_total, 
    amount_subtotal             = EXCLUDED.amount_subtotal, 
    amount_tax_include          = EXCLUDED.amount_tax_include, 
    amount_tax_exclude          = EXCLUDED.amount_tax_exclude, 
    amount_subtotal_discount_price = EXCLUDED.amount_subtotal_discount_price, 
    amount_fee                  = EXCLUDED.amount_fee, 
    amount_shipping             = EXCLUDED.amount_shipping, 
    discount_point              = EXCLUDED.discount_point, 
    discount_coupon             = EXCLUDED.discount_coupon, 
    transaction_type            = EXCLUDED.transaction_type,
    is_void                     = EXCLUDED.is_void;

### 13.5 transactionDtailesテーブルraw層からdwh層へインサートするSQL
with raw_transaction_details AS (
    SELECT DISTINCT ON (json_body ->> 'transactionHeadId', json_body ->> 'transactionDetailId')
        (json_body ->> 'transactionHeadId') AS "transaction_head_id",
        (json_body ->> 'transactionDetailId') AS "transaction_detail_id",
        (json_body ->> 'productId') AS "product_id",
        (json_body ->> 'productName') AS "product_name",
        (json_body ->> 'quantity') AS "quantity",
        (json_body ->> 'salesPrice') AS "sales_price",
        (json_body ->> 'taxDivision') AS "tax_division",
        (json_body ->> 'transactionDetailDivision') AS "transaction_detail_division"
    FROM "raw".transaction_details
    WHERE
        (json_body ->> 'updDateTime')::TIMESTAMPTZ >= #{fromDate} 
        AND app_company_id = #{companyId}
    ORDER BY 
        (json_body ->> 'transactionHeadId'), 
        (json_body ->> 'transactionDetailId'), 
        fetched_at DESC
),
formatted_data AS (
    SELECT
        #{companyId} AS app_company_id,
        tr.transaction_head_id,
        tr.transaction_detail_id,
        tr.product_id,
        tr.product_name,
        COALESCE(gr.cat_group_name, '未分類') AS category_group_name,
        tr.quantity::Integer,
        tr.sales_price::Integer,
        tr.tax_division::Integer,
        CASE WHEN tr.transaction_detail_division = '2'
                THEN 'REFUND'
            ELSE 'SALES'
        END AS category_type
    FROM
        raw_transaction_details AS tr
    LEFT JOIN
        dwh.dim_products AS pr
        ON tr.product_id = pr.product_id
        AND pr.app_company_id = #{companyId}
    LEFT JOIN 
        dwh.dim_category_groups AS gr
        ON pr.cat_group_id = gr.cat_group_id
        AND gr.app_company_id = #{companyId}
)
INSERT INTO dwh.fact_sales_details(
    app_company_id, transaction_head_id, transaction_detail_id, 
    product_id, product_name, category_group_name, quantity, 
    sales_price, tax_division, category_type
)
SELECT * FROM formatted_data
ON CONFLICT ON CONSTRAINT fact_sales_details_pkey
DO UPDATE SET
    product_id          = EXCLUDED.product_id, 
    product_name        = EXCLUDED.product_name, 
    category_group_name = EXCLUDED.category_group_name, 
    quantity            = EXCLUDED.quantity, 
    sales_price         = EXCLUDED.sales_price, 
    tax_division        = EXCLUDED.tax_division, 
    category_type       = EXCLUDED.category_type;

次のステップ、これらのSQLを実行するためのFactSalesTransformTasklet（または一連の Mapper）の実装 [完了]

#### 変更点１、app_company_idをAppVendorPropertiesからJobParametersからの取得に変更
* マルチテナント（複数会社）への対応
  将来的にこのシステムを「A社」「B社」など複数の会社で運用する場合、application.
  properties（AppVendorProperties）に書かれた ID は固定値になってしまいます。
   * JobParameters を使う場合: ジョブ起動時に companyId=2 と渡せば、その実行だけ
     B社の処理として動かせます。
   * プロパティだけの場合:
     B社の処理を動かすために、アプリを再起動したり環境変数を書き換えたりする必要
     があり、運用が非常に困難になります。

#### 変更点２、BeautySalesDwhApplication.java内CommandLineRunner runJob変更
   * ジョブの追加: importSmaregiTransactionJob を runJob
     メソッドの引数に追加し、マスタデータの取り込み後に実行するようにしました。
   * 会社IDの明示: JobParameters に companyId
     を追加しました。これにより、設定ファイル（.env 等）の APP_VENDOR_ID
     が正しくジョブ全体に伝搬されます。
   * 依存関係の整理: ジョブが複数になったため、@Qualifier
     を使ってそれぞれのジョブを明示的に指定するようにしました。

#### 変更点３、SQLのエスケープ処理 (&lt;): XML内で <
     を使う際のエスケープの必要性（MyBatis特有の注意点）


## 14.後から名寄せ設定（merge_candidates）が増えた場合、過去の fact_salesを再変換（洗い替え）する
具体的なワークフローとしては、以下のようなイメージになります。

   1. 候補発見: バッチまたは画面から「名寄せ候補」をユーザーに提示。
   2. ユーザー承認: ユーザーが「ID: A と ID: B
      は同一人物なのでマージする」と確定。
   3. マージ実行 (DB更新):
       * sys.merge_candidates のステータスを MERGED に更新。
       * [ここがポイント] その直後に、dwh.fact_sales
         に対して以下の更新クエリを一発走らせます。

            UPDATE dwh.fact_sales
            SET customer_id = #{target_customer_id}
            WHERE customer_id = #{source_customer_id}
              AND app_company_id = #{companyId};
   
   4. 完了: これで、過去の売上データもすべて正い ID に紐付きます。

  なぜこのタイミングが良いか
   * 即時性:
     ユーザーが承認した瞬間に、分析画面（月次推移など）の数字が正しく統合されま
     す。
   * 効率性: 全データを Transform し直すのではなく、対象の customer_id だけを
     UPDATE すれば良いため、処理が非常に軽量です。
   * 一貫性:
     dwh.dim_customers（顧客マスタ）のマージ処理と同じタイミングで行うことで、マ
     スタと売上の整合性が常に保たれます。

