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

#### 変更点４
1. SQLの堅牢性: ::NUMERIC::INTEGER による安全な型変換と、DISTINCT ON
      による重複データ対策。
2. API仕様への準拠: スマレジAPI特有の sort
      パラメータ（キャメルケース）の修正。
3. データ整合性:
      親テーブル（raw.transactions）の更新日時に基づいた、明細データの確実なフィ
      ルタリング。


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


## 15 名寄せ（Identity Resolution / RecordLinkage）の実装、
  美容サロンのデータ（名前、カナ、電話番号）を扱う上で、検討すべき代表的なアルゴリズムと手法。

#### 「決定論的（Exact Match）」 なルール


  1. 前処理（正規化）アルゴリズム
  アルゴリズムを適用する前に、データを「同じ土俵」に乗せる手法です。
   * Unicode正規化 (NFKC): 全角・半角、カタカナ・ひらがなの統一。
   * ストップワード除去: 名字と名前の間のスペース、ハイフンの除去。
   * 住所正規化: （もし住所があれば）「1丁目2番3号」と「1-2-3」の変換。


  2. 決定論的名寄せ (Deterministic Matching)
  「この項目が一致したら同一人物」と断定するルールベースの手法です。
   * 完全一致 (Exact Match):
     電話番号、メールアドレスなど一意性が高い項目の比較。
   * クロス一致 (Cross Match):
     顧客Aの「電話番号」と顧客Bの「携帯番号」が一致するか、などの複数項目クロス
     チェック。


  3. 曖昧一致 / 文字列類似度 (Fuzzy Matching)
  「似ている」度合いを数値化（0.0〜1.0）するアルゴリズムです。
   * レーベンシュタイン距離 (Levenshtein Distance):
     1文字の挿入・削除・置換で何回操作が必要か。名字の誤字脱字に強い。
   * ジャロ・ウィンクラー距離 (Jaro-Winkler Distance):
     文字列の「接頭辞」の一致を重視。日本語の名字（「斉藤」と「齋藤」など）の判
     定に有効。
   * N-gram (バイグラム等):
     文字列をN文字ずつに分割して共通項を比較。名前の一部が欠けている場合に強い。


  4. 音韻一致 (Phonetic Matching)
  読み（カナ）が似ているものを探す手法です。
   * Soundex / Metaphone:
     英語圏で主流ですが、日本語では「カナの濁音・半濁音・拗音の正規化」がこれに
     相当します（例：「ハ」と「パ」、「ユ」と「ユウ」の同一視）。


  5. 確率論的名寄せ (Probabilistic Matching)
  複数の項目の「一致確率」に重みをつけて合計スコアで判定する手法です。
   * Fellegi-Sunterモデル:
     名寄せの数学的基盤。名前が一致した時の「嬉しさ（重み）」と、誕生日が一致し
     た時の「嬉しさ」を統計的に処理します。


  6. ブロッキング / インデクシング (Blocking)
  全件 vs 全件の比較は O(N^2) で重くなるため、比較対象を絞り込む技術です。
   * Sorted Neighborhood:
     カナ順に並べて、前後数件（ウィンドウ）だけを比較対象にする。
   * Hash Blocking:
     生年月日の「月日」だけをハッシュ化し、同じハッシュ値を持つグループ内だけで
     詳細比較を行う。

  ---


  今回のプロジェクトでの狙い目
  サロンデータの場合、以下の組み合わせが現実的で強力です。


   1. Normalization: 全角半角・スペースの徹底排除。
   2. Blocking: 生年月日の「月日」または「電話番号の下4桁」でバケツ分け。
   3. Fuzzy Match: Jaro-Winkler を使って、カナ氏名の類似度スコアを算出。
   4. Verification: 最終的には人間が承認するための
      sys.merge_candidates（ステータス：PENDING）への登録。


  「ジャロ・ウィンクラー距離」や「レーベンシュタイン距離」は PostgreSQL
  の拡張機能（fuzzystrmatch）でも使える

#### 統計学・AIの視点での「名寄せ」


   1. 二値分類問題 (Binary Classification):
       * 「2つのレコードは同一人物か (1) か、別人か (0)
         か」を判定するモデルとして捉えられます。
   2. 特徴量エンジニアリング (Feature Engineering):
       * 氏名、カナ、電話番号、来店日などをどう数値化（ベクトル化）して、類似度
         を計算するか。
   3. 適合率 (Precision) と再現率 (Recall) のトレードオフ:
       * 適合率重視:
         「別人なのにマージしてしまう」エラーを最小限にする（サロンではこちらが
         重要：他人の履歴が混ざるとクレームになるため）。
       * 再現率重視:
         「同一人物なのに漏れてしまう」エラーを最小限にする（分析精度には効く）
         。
   4. 教師なし学習 (Clustering):
       * 階層的クラスタリングなどを使って、類似した顧客を「重複グループ」として
         自動抽出する手法。
   5. 重み付け (Weighting):
       * 「電話番号の一致」と「名字の一致」、どちらがどれだけ「同一人物である確
         率」に寄与するか（ベイズ統計的な考え方）。

  次のステップのヒント
  最初は 「決定論的（Exact Match）」 なルールから始めて、徐々に
  「確率論的（Fuzzy Match / Score-based）」
  なロジックへ拡張していくのが定石です。

  PostgreSQLには fuzzystrmatch や pg_trgm
  という、類似度計算に特化した拡張モジュールもあります。


<insert id="findAndInsertMergeCandidates">
    2     WITH normalized_customers AS (
    3         SELECT
    4             app_company_id,
    5             customer_id,
    6             -- 名字と名前の間のスペースを削除して比較しやすくする
    7             replace(replace(customer_name, ' ', ''), '　', '') AS
      norm_name,
    8             replace(replace(customer_kana, ' ', ''), '　', '') AS
      norm_kana,
    9             regexp_replace(phone_number, '[^0-9]', '', 'g') AS norm_phone,
   10             regexp_replace(mobile_number, '[^0-9]', '', 'g') AS
      norm_mobile,
   11             --
      ブロッキング用：カナの先頭2文字（例：「サト」）が一致する人同士だけで比較
      する
   12             substring(replace(replace(customer_kana, ' ', ''), '　', ''),
      1, 2) AS kana_block,
   13             visit_count
   14         FROM dwh.dim_customers
   15         WHERE app_company_id = #{companyId}
   16           AND is_deleted = false
   17     ),
   18     matched_pairs AS (
   19         SELECT
   20             c1.customer_id AS id1,
   21             c2.customer_id AS id2,
   22             CASE
   23                 -- 1. 電話番号が一致（最強）
   24                 WHEN (c1.norm_phone = c2.norm_phone AND c1.norm_phone !=
      '') OR (c1.norm_mobile = c2.norm_mobile AND c1.norm_mobile != '') THEN 100
   25                 -- 2. カナが完全一致（かなり強い）
   26                 WHEN c1.norm_kana = c2.norm_kana AND c1.norm_kana != ''
      THEN 95
   27                 -- 3. 漢字氏名が完全一致（強い）
   28                 WHEN c1.norm_name = c2.norm_name AND c1.norm_name != ''
      THEN 90
   29                 ELSE 0
   30             END AS score
   31         FROM normalized_customers c1
   32         JOIN normalized_customers c2 ON c1.app_company_id =
      c2.app_company_id
   33             AND c1.customer_id &lt; c2.customer_id
   34             -- 【重要：ブロッキング】
   35             --
      電話番号が一致するか、もしくは「カナの先頭2文字」が一致するペアのみを詳細
      比較の対象にする
   36             -- これにより、計算量を数百分の一に削減できます。
   37             AND (
   38                 (c1.norm_phone = c2.norm_phone AND c1.norm_phone != '') OR
   39                 (c1.norm_mobile = c2.norm_mobile AND c1.norm_mobile != '')
      OR
   40                 (c1.kana_block = c2.kana_block AND c1.kana_block != '')
   41             )
   42         WHERE
   43             -- スコアがついたものだけを抽出
   44             ( (c1.norm_phone = c2.norm_phone AND c1.norm_phone != '') OR
      (c1.norm_mobile = c2.norm_mobile AND c1.norm_mobile != '') )
   45             OR (c1.norm_kana = c2.norm_kana AND c1.norm_kana != '')
   46             OR (c1.norm_name = c2.norm_name AND c1.norm_name != '')
   47     ),
   48     final_candidates AS (
   49         -- (以下、前回と同様に visit_count で target/source を決定)
   50         SELECT
   51             #{companyId} AS app_company_id,
   52             CASE WHEN cust1.visit_count >= cust2.visit_count THEN
      cust2.customer_id ELSE cust1.customer_id END AS source_customer_id,
   53             CASE WHEN cust1.visit_count >= cust2.visit_count THEN
      cust1.customer_id ELSE cust2.customer_id END AS target_customer_id,
   54             pairs.score
   55         FROM matched_pairs pairs
   56         JOIN dwh.dim_customers cust1 ON pairs.id1 = cust1.customer_id AND
      cust1.app_company_id = #{companyId}
   57         JOIN dwh.dim_customers cust2 ON pairs.id2 = cust2.customer_id AND
      cust2.app_company_id = #{companyId}
   58     )
   59     INSERT INTO sys.merge_candidates ( ... )
   60     SELECT ... FROM final_candidates fc
   61     WHERE NOT EXISTS ( ... );
   62 </insert>

  工夫した点と計算量への対策


   1. スペースの除去 (replace):
      「山田 太郎」と「山田太郎」が一致するように正規化しています。
   2. カナの先頭2文字ブロッキング (kana_block):

  全く名前が掠りもしない人同士（例：「佐藤」と「鈴木」）を比較対象から外していま
  す。これにより、結合処理の負荷を大幅に軽減できます。
   3. 電話番号優先:

  電話番号が一致していれば、氏名が多少違っても（例：家族が同じ番号を使っている、
  あるいは名字が変わった）候補として抽出されます。


  さらに高度な「曖昧一致」をしたい場合
  もし、「齋藤」と「斉藤」のような、完全一致しないが似ている名前も抽出したい場合
  は、PostgreSQL の levenshtein 関数を使用できます。


   1 -- 名字の編集距離が1以内なら似ていると判定する例（要 fuzzystrmatch 拡張）
   2 WHEN levenshtein(c1.norm_name, c2.norm_name) &lt;= 1 THEN 85



