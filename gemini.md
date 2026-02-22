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
   7. `SmaregiTransactionItemReader.java`の作成:
      スマレジAPIから取引データを取得するリーダーです。
＊取引は過去３年まで取得とする（対前年度比が見たいのでここまでが最大かなと思います）
＊transaction_date_time-from,transaction_date_time-toの日付を1ヶ月ごとに作成し繰り返す（最初は36回リクエストを投げる）
   8. `TransactionRawDataProcessor.java`の作成:
      ステップA用のプロセッサー（生マップをTransactionRawDataに変換）です。
   9. `SmaregiBatchConfig.java`の更新:
       * ステップA用のstepFetchTransactionsHead()を追加します。
       * ステップB用のstepExtractTransactionDetails()を追加します。
       * 新しいコンポーネント（リーダー、プロセッサー、マッパー、タスクレット）を注入します。
   10. `TransactionDetailsExtractTasklet.java`の作成
       ステップB用のタスクレット（明細を抽出し、raw.transaction_detailsに挿入）です。
