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

### テーブル、スキーマ初期作成用SQL
01_schema.sql

### postgres pg_dumpコマンド
pg_dump -U ユーザー名 --format=p --file=フルパス.sql db名

### SpringBootプロジェクトフォルダ構成
```
com.example.beautydwh
├── BeautyDwhApplication.java  // [Main] 起動クラス (ルートに置くのが鉄則)
│
├── common                     // [Shared] 全ドメイン共通の部品
│   ├── config                 // Security, JDBC, Batch設定
│   ├── security               // OAuth2, JWT, 認証Userモデル
│   └── tenant                 // マルチテナント(CompanyId)のContextHolder
│
├── ingestion                  // [Domain: ETL] データ収集・Raw層
│   ├── client                 // スマレジAPIクライアント
│   ├── batch                  // Spring BatchのJob/Step定義
│   └── raw                    // Raw層のEntity/Repository (RawTransactionなど)
│
├── analytics                  // [Domain: 分析] DWH層・可視化
│   ├── sales                  // 売上分析ドメイン
│   │   ├── FactSales.java     // Entity (Record)
│   │   ├── SalesRepository.java
│   │   ├── SalesService.java
│   │   └── SalesController.java
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