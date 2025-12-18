# 作業メモ

## インフラ
### docker-compose.yml
フォルダ直下
### dokerfile
./backendでSpring Boot用 Dockerfile設定
### pgAdminへのアクセス:
URL: http://localhost:5050
Login: .env で設定したEmailとPassword
サーバー追加設定 (重要):
Host name/address: db (localhostではありません)
Port: 5432
Maintenance database: beauty_dwh_db
Username/Password: .env の設定値
zq
### 自動作成の仕組み
PostgreSQLの公式イメージは、「初回起動時（データが空っぽの時）」に限り、設定された環境変数を読み取って初期セットアップを行うようにプログラムされています。

データの流れは以下のようになっています。

 .env ファイルに書いた POSTGRES_DB=beauty_dwh_db などの値を、docker-compose.yml が読み込んでコンテナに渡し、
PostgreSQLコンテナ が起動した瞬間に、「お、変数が設定されているな」と判断して、裏側で以下のようなコマンドを自動実行します。

```SQL
-- コンテナが裏で勝手にやってくれていること
CREATE USER beauty_user WITH PASSWORD 'beauty_pass';
CREATE DATABASE beauty_dwh_db OWNER beauty_user;
```

### ⚠️ 【重要】注意点が1つだけあります
この自動作成機能が働くのは、「一番最初の起動時だけ（ボリュームが空の時だけ）」 です。
もし、「パスワードやDB名を間違えたから .env を書き直して、もう一度 docker-compose up しよう」としても、すでに一度作成されたデータベース（ボリューム内のデータ）が優先されるため、設定変更は反映されません。
もし設定をやり直したい場合は、一度ボリューム（データの実体）ごと削除する必要があります。

## スマレジAPI仕様のポイント
```
1.金額・税周り（1円の壁対策）の確認に対しては
小計subtotalは「取引明細の値引き後小計の合計。」との記載なので税抜金額はこれで対応できそうです。
合計totalは「合計：小計 - 小計値引き - ポイント値引き - クーポン値引き + 端数値引額 + 外税額 + 手数料 + 送料 - 免税額」となっています。ので税込額ですが「手数料 + 送料」は税込対象ではないと思うので税込対象額を出したければ「手数料 、 送料」の列を取得しないといけないと思います。
2. IDのユニーク範囲（マルチテナント対策）
実際にスマレジ管理画面で入力して確認したところログインIDごとの「取引」「店舗」「会員」などのエンティティでSERIALで自動裁判されていて特にリレーションはないです。なのでSQL発行時WHERE説app_company_idで縛ればユニーク範囲はUI上では問題なさそうです。（逆にapp_company_idの縛りがなければかぶる）
3. キャンセル・削除データの扱い
cancelDivision：取消区分：取引の取消を識別する区分。 (0:通常、1：取消）
transactionHeadDivision取引区分：取引を識別する区分。 （1:通常、2:入金、3:出金、・・・）
という項目があり、cancelDivision=0&transactionHeadDivision=1
(取消区分が通常で取引区分が通常）なら返金処理は合計がマイナスで出力された形で出るのでこの条件で行けそうです。
```
### DB接続はSpring Data JDBC
delete,insert,updateはORマッパーはセキュリティ上必要ですが、今回は集計処理のselect文は複雑になりますので直接SQLが書ける方が良い
おっしゃる通り、以下の理由から今回のプロジェクトにはJPAよりもJDBC（Spring Data JDBC）が圧倒的に向いています。

「N+1問題」からの解放: 分析画面で大量のデータをリスト表示する際、JPAのLazy Loadingによる意図しないクエリ発行はパフォーマンスの致命傷になります。Spring Data JDBCならその心配がありません。

SQLの完全制御: 分析クエリ（Window関数、CTE、複雑なJOIN、GROUP BY）は、JPQLやCriteria APIで書くと地獄を見ます。生SQL (@Query) で書く方が保守性が高く、実行計画も予測しやすいです。

バッチ処理との相性: Hibernate特有の「セッションキャッシュ（1次キャッシュ）」がないため、大量データをInsert/Updateしてもメモリがあふれにくく、ETL処理が高速かつシンプルになります。


## DB
#### 1. FACT_SALES_DETAILS の主キー戦略（致命的になり得る）
前回の議論で「親（FACT_SALES）は複合PK」にしましたが、子（DETAILS）のPKがまだ transaction_id 単体のままに見えます（または意図が曖昧です）。
現状のリスク: 将来、データ量が億単位になり「パーティショニング（テーブル分割）」をしたくなった際、PostgreSQLの仕様上、「パーティションキー（今回なら app_company_id や transaction_date）は、主キーの一部でなければならない」 という制約があります。 主キーが transaction_id 単体だと、将来的に app_company_id 単位でテーブルを切り分けることができなくなります。
修正案: ここも 「複合主キー」 に倒しきってください。


FACT_SALES_DETAILS {
    BIGINT app_company_id PK "複合PK 1/3"
    VARCHAR transaction_head_id PK "複合PK 2/3 (親ID)" 
    VARCHAR transaction_detail_id PK "複合PK 3/3 (明細ID)"
    %% ...
}
※ transaction_head_id もPKに含めておくと、親テーブルと同じパーティション構成にできるため、Joinが劇的に速くなります（Co-located Join）。

#### 2. TIMESTAMP 型のタイムゾーン問題
現在 TIMESTAMP と定義されていますが、PostgreSQLの TIMESTAMP は 「タイムゾーン情報を持たない（TIMESTAMP WITHOUT TIME ZONE）」 がデフォルトです。
リスク: サーバー（Dockerコンテナ）のタイムゾーン設定(JST/UTC)が変わったり、将来海外展開（またはサーバー移転）した際に、「9:00」が「JSTの9:00」なのか「UTCの9:00」なのか判別不能になり、集計が9時間ズレる 事故が起きます。
修正案: DWHの定義としては、TIMESTAMPTZ (TIMESTAMP WITH TIME ZONE) を使うのが鉄則です。 （※ DATE 型はそのままでOKです）

#### 3. 名寄せ候補 (MERGE_CANDIDATES) の重複爆撃
バッチ処理（名寄せジョブ）は何回も実行されます。今の定義だと、同じペア（Aさん, Bさん）が、実行されるたびに何度も INSERT されませんか？
リスク: UIを開いたら、同じ「AさんとBさん」の候補が100行並んでいる……という「うんこUI」になります。
修正案: 複合ユニーク制約をER図（設計）に明記しましょう。


### テーブル、スキーマ初期作成用SQL
01_schema.sql

### postgres pg_dumpコマンド
pg_dump -U ユーザー名 --format=p --file=フルパス.sql db名


## BackEnd
### SpringBootプロジェクトフォルダ構成
```
com.example.beautydwh
├── BeautyDwhApplication.java  // [Main] 起動クラス (ルートに置くのが鉄則)
│
├── common                     // [Shared] 全ドメイン共通の部品
│   ├── config                 // Security, JDBC, Batch設定
│   ├── security               // OAuth2, JWT, 認証Userモデル
│   └── tenant                 // マルチテナント(CompanyId)のContextHolder
│   └── util                  //  システムに必要な処理を格納（SnowflakeGenerator.java)
│   └── validation             // カスタムバリデーション格納

│
├── ingestion                  // [Domain: ETL] データ収集・Raw層
│   ├── client                 // スマレジAPIクライアント
│   ├── batch                  // Spring BatchのJob/Step定義
│   └── raw                    // Raw層のEntity/Repository (RawTransactionなど)
│
├── analytics                  // [Domain: 分析] DWH層・可視化
│   ├── sales                  // 売上分析ドメイン
│   │   ├── FactSales.java     // Entity (Record)
│   │   ├── FactSalesDetails.java
│   │   └── SalesRepository.java
│   ├── customer               // 顧客分析ドメイン (DimCustomer)
│   ├── staff                  // スタッフ分析ドメイン (DimStaff)
│   └── product                // 商品分析ドメイン (DimProduct)
│
├── identity                   // [Domain: 名寄せ] 顧客統合ロジック
│   ├── matching               // 類似度判定アルゴリズム
│   ├── candidate              // 名寄せ候補 (MergeCandidates)
│   └── MergeController.java   // 名寄せ画面用API
│
└── system                     // [Domain: 管理] システム管理
    ├── joblog                 // ジョブ実行ログ
    ├── user                   // アプリユーザー管理
    └── company                // テナント(Company)管理
```

### ドメイン（機能・関心事）によるパッケージ構成（Package by Feature）
この構成のメリット（なぜこれが良いのか）
#### 凝集度が高い (High Cohesion):
「売上集計のロジックを直したい」と思った時、analytics.sales パッケージを見るだけで、コントローラーからSQL(Repository)、エンティティまで全てがそこにあります。あちこちのパッケージを行き来する必要がありません。

#### 可視性の制御 (Package Scope):
例えば、SalesRepository は SalesService からしか使われたくない場合、public をつけずに パッケージプライベート にすることで、他のドメイン（identityなど）から勝手にアクセスされるのを防げます。これはアーキテクチャの崩壊を防ぐ強力な武器です。

#### マイクロサービスへの分割が容易:
将来、「名寄せ機能だけ別のマイクロサービスに切り出したい」となった場合、identity パッケージをフォルダごと引っこ抜けば良いので、リファクタリングが容易です。


### 依存関係
```
カテゴリ,選択する依存関係,理由
Web,Spring Web,ダッシュボードAPI用
SQL,Spring Data JDBC,JPAを選ばないように注意！
SQL,PostgreSQL Driver,DB接続
I/O,Spring Batch,ETL・CSV処理
Security,OAuth2 Client,Google認証
DevTools,Lombok,コード量削減
DevTools,Spring Boot DevTools,再起動の手間削減
```
### スマレジデベロッパーAPIは取引明細（FACT_SALES_DETAILS）のRAWデータは別に圧縮されたCSVデータでDLすることが推奨
推奨策：Row-oriented JSONB Storage
「CSVファイル」として保存するのではなく、Spring Batchのストリーム処理を使って**「CSVの1行を、1つのJSONレコード」**としてRawテーブルに保存します。
これなら、「APIの生データ（Raw）を保存する」という哲学を守りつつ、DBのパフォーマンスも維持できます。

変更後のアーキテクチャ
* Download: ZIPファイルを一時ディレクトリ（/tmpなど）にDL。
* Stream: Javaで解凍しながらストリーム（1行ずつ）読み込み。
* Convert: CSVの1行（カンマ区切り）を、シンプルなJSONオブジェクトに変換。
* Insert: raw.smaregi_transaction_details に1行ずつ（実際はバッチインサートでまとめて）保存。

推奨するテーブル設計 (Raw層)
詳細データ用のRawテーブルを別途用意します。

``` SQL

CREATE TABLE raw.smaregi_transaction_details (
    -- CSVの中にユニークなIDがあればそれをPKに。なければ複合キーかSERIAL
    detail_id SERIAL PRIMARY KEY, 
    transaction_id VARCHAR(100) NOT NULL, -- 親の取引ID（CSVに含まれているはず）
    
    -- ★ここがポイント
    -- CSVの1行分をJSON化したものを入れる
    -- 例: {"itemId": "100", "price": 5000, "quantity": 1}
    json_body JSONB NOT NULL, 
    
    file_name VARCHAR(255), -- どのCSVファイル由来か（トレーサビリティ用）
    row_number INTEGER,     -- CSV内の行番号（エラー調査用）
    fetched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 親IDで検索できるようにインデックスを貼る
CREATE INDEX idx_raw_details_tran_id ON raw.smaregi_transaction_details(transaction_id);
```
