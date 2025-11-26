```mermaid
erDiagram
    %% ==========================================
    %% Schema: RAW (Staging Area)
    %% APIレスポンスをそのまま保存。DWHへの依存なし
    %% ==========================================
    RAW_TRANSACTION {
        BIGSERIAL transaction_id PK "取引API取得処理ID"
        TIMESTAMP fetched_at "取得日時"
        JSONB json_body "APIレスポンス全文"
    }

    RAW_CUSTOMER {
        BIGSERIAL customer_id PK "会員API取得処理ID"
        TIMESTAMP fetched_at "取得日時"
        JSONB json_body "APIレスポンス全文"
    }

    %% ==========================================
    %% Schema: DWH (Analytics Core)
    %% スタースキーマ構成
    %% ==========================================
    DIM_CUSTOMER {
        BIGINT app_company_id FK
        VARCHAR customer_id PK "統合ID"
        VARCHAR customer_name
        VARCHAR customer_kana
        VARCHAR phone_number
        DATE first_visit_date
        DATE last_visit_date
        INTEGER visit_count        
        BOOLEAN is_deleted "名寄せで消滅した場合True"
    }

    DIM_STAFF {
        BIGINT app_company_id FK
        VARCHAR staff_id PK
        VARCHAR staff_name
        VARCHAR rank "経営側評価"
        boolean employ_flag "在籍フラグ"
    }

    DIM_ITEM {
        BIGINT app_company_id FK
        VARCHAR item_id PK
        VARCHAR item_name
        VARCHAR category_name "技術/店販"
        INTEGER unit_price
    }

    FACT_SALES {
        BIGINT app_company_id FK
        VARCHAR transactionHeadId PK "スマレジが保持するID"
        TIMESTAMP transaction_date_time
        DATE transaction_date "パーティション/検索用"
        VARCHAR customer_id FK
        VARCHAR staff_id FK
        INTEGER total_amount "返金はマイナス値"
        VARCHAR transaction_type "SALES/REFUND"
    }

    FACT_SALES_DETAILS {
        BIGINT app_company_id FK
        SERIAL detail_id PK
        VARCHAR transaction_id FK
        VARCHAR item_id FK
        VARCHAR item_name "スナップショット"
        INTEGER quantity
        INTEGER price_sales "実売価格,返金はマイナス値"
        INTEGER taxRate　"10%,8%などパーセンテージ"
        VARCHAR category_type "SALES/REFUND"
    }

    %% ==========================================
    %% Schema: SYS (System Management)
    %% ==========================================
    APP_COMPANY{
        BIGINT app_company_id PK "snowflakeID"
        VARCHAR company_name "NotNULL"
    }
    
    APP_USER{
        BIGINT app_user_id PK "snowflakeID"
        BIGINT app_company_id FK
        VARCHAR user_name
        VARCHAR email "NotNULL unique inx"
        VARCHAR role "NotNULL"
    }

    JOB_LOGS {
        BIGSERIAL job_id PK
        BIGINT app_company_id FK
        VARCHAR job_name
        VARCHAR status
        TIMESTAMP start_time
        TIMESTAMP end_time
        TEXT message
    }

    MERGE_CANDIDATES {
        SERIAL candidate_id PK
        BIGINT app_company_id FK
        VARCHAR source_customer_id FK "消えるID"
        VARCHAR target_customer_id FK "残るID"
        INTEGER similarity_score "類似性スコア"
        VARCHAR status "PENDING/MERGED/IGNORED"
    }



    %% ==========================================
    %% Relationships
    %% ==========================================
    
    %% Fact - Dimensions
    DIM_CUSTOMER ||--o{ FACT_SALES : "purchases"
    DIM_STAFF ||--o{ FACT_SALES : "handles"
    
    %% Fact Header - Detail
    FACT_SALES ||--|{ FACT_SALES_DETAILS : "contains"
    
    %% Detail - Item
    DIM_ITEM ||--o{ FACT_SALES_DETAILS : "defined_as"

    %% System - DWH Logic
    DIM_CUSTOMER ||--o{ MERGE_CANDIDATES : "is_source_of"
    DIM_CUSTOMER ||--o{ MERGE_CANDIDATES : "is_target_of"
```