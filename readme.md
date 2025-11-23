# Beauty Sales DWH (美容業向け売上分析DWH)

![Java](https://img.shields.io/badge/Java-Spring%20Boot-green) ![Next.js](https://img.shields.io/badge/Next.js-TypeScript-black) ![Docker](https://img.shields.io/badge/Docker-Compose-blue) ![PostgreSQL](https://img.shields.io/badge/DB-PostgreSQL-336791)

美容サロン（ヘア・ネイル・エステ等）向けの**売上分析および顧客データ統合（名寄せ）プラットフォーム**です。
スマレジPOSのAPIからデータを自動収集(ETL)し、DWH構築、名寄せによる顧客ID統合を経て、経営判断に直結するダッシュボードを提供します。

## 📖 プロジェクト背景と課題 (Background)

中小規模の美容サロンでは、POSシステムで会計を行っているものの、そのデータを**「分析」**に活かせていない課題があります。

* **データの分断:** 分析のためにCSVを出力し、Excelで手作業加工する工数が負担。
* **顧客の重複:** 予約サイト経由や電話予約などで、同一人物が別IDとして登録され、正確なリピート分析やLTV算出ができない。
* **属人的な勘:** 数字に基づいたスタッフ評価やマーケティングができていない。

本プロジェクトは、これらの課題を**「データパイプラインの自動化」**と**「名寄せアルゴリズム」**によって解決する実証的なWebアプリケーションです。

## 🛠 技術スタック (Tech Stack)

「データ統合から可視化までの一貫した開発力」を示すため、以下の技術を選定しました。

| カテゴリ | 技術 | 選定理由 |
| :--- | :--- | :--- |
| **Backend** | **Java 17 / Spring Boot 3** | 堅牢なAPI構築およびSpring BatchによるETL処理の実装。 |
| **Frontend** | **Next.js (App Router)** | SEOとパフォーマンス、サーバーサイドレンダリングによる高速な描画。 |
| **Database** | **PostgreSQL** | JSON型対応と分析クエリへの親和性。Rawデータ層とDWH層を分離設計。 |
| **Infra** | **Docker / Docker Compose** | ポータビリティの確保とローカル開発環境の統一。 |
| **Auth** | **Spring Security (OAuth2)** | Google認証を用いたセキュアなログインとJWT管理。 |
| **Charts** | **Recharts** | Reactベースの柔軟なデータ可視化ライブラリ。 |

## 📐 アーキテクチャと機能 (Architecture & Features)

### 1. データ収集・統合 (ETL & DWH)
* **スマレジAPI連携:** POSデータ（売上・顧客・スタッフ）を日次バッチで自動取得。
* **Raw / DWH 層の分離:**
    * `Staging Area`: APIレスポンスをそのまま格納。
    * `Data Warehouse`: 分析用にスタースキーマライクに正規化したテーブル群。

### 2. 顧客名寄せエンジン (Identity Resolution) 🌟Key Feature
本システムの核となる機能です。表記揺れのある顧客データを半自動で統合します。
* 氏名（カナ）、電話番号の一致度から「同一人物候補」を自動検出。
* 管理画面にて、ユーザー（店長）が候補を確認し、IDを統合（Merge）する機能を提供。
* **効果:** 正確な「来店回数」「累計売上」の算出が可能に。

![Identity Resolution UI](./docs/images/identity_resolution.png)

### 3. 分析ダッシュボード
* **月次売上推移:** 昨対比を含めた棒・折れ線複合グラフ。
* **スタッフ別ランキング:** 売上貢献度を可視化。
* **ABC分析 (パレート図):** 上位顧客（ロイヤルカスタマー）の特定。

## 🗂 データベース設計 (ER Diagram)

DWHとしての分析効率と、データ整合性を両立させるスキーマ設計を行っています。

```mermaid
erDiagram
    Company ||--o{ Staff : has
    Company ||--o{ Sale : has
    Customer ||--o{ Sale : makes
    Customer ||--o{ CustomerMapping : merges
    
    Sale ||--o{ SaleDetail : contains
    Item ||--o{ SaleDetail : purchased

    %% 簡略化して表示しています
    Sale {
        string saleId PK
        date saleDate
        number totalAmount
        varchar type "入金/返金"
    }
    CustomerMapping {
        string sourceCustomerId
        string targetCustomerId
    }