-- =================================================================
-- Project: Beauty Sales DWH
-- Description: DDL for PostgreSQL (Raw Layer, DWH Layer, System Layer)
-- Version: Final Implementation Ready
-- =================================================================

-- 1. Schema Definition
CREATE SCHEMA IF NOT EXISTS raw;
CREATE SCHEMA IF NOT EXISTS dwh;
CREATE SCHEMA IF NOT EXISTS sys;

-- =================================================================
-- [RAW LAYER] Staging Area (API Responses)
-- All tables use BIGSERIAL for internal ID management & TIMESTAMPTZ
-- =================================================================

-- 1.1 Transactions (API: /transactions)
CREATE TABLE IF NOT EXISTS raw.transactions (
    transaction_id BIGSERIAL PRIMARY KEY,
    fetched_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    json_body JSONB NOT NULL
);

-- 1.2 Transaction Details (API: /transactions/details -> CSV)
CREATE TABLE IF NOT EXISTS raw.transaction_details (
    detail_id BIGSERIAL PRIMARY KEY,
    transaction_head_id VARCHAR(100) NOT NULL,
    json_body JSONB NOT NULL, -- Converted from CSV row
    file_name VARCHAR(255),
    row_number BIGINT,
    fetched_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_raw_details_head_id ON raw.transaction_details(transaction_head_id);

-- 1.3 Customers (API: /customers)
CREATE TABLE IF NOT EXISTS raw.customers (
    customer_id BIGSERIAL PRIMARY KEY,
    fetched_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    json_body JSONB NOT NULL
);

-- 1.4 Products (API: /products)
CREATE TABLE IF NOT EXISTS raw.products (
    product_id BIGSERIAL PRIMARY KEY,
    fetched_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    json_body JSONB NOT NULL
);

-- 1.5 Staffs (API: /staffs)
CREATE TABLE IF NOT EXISTS raw.staffs (
    staff_id BIGSERIAL PRIMARY KEY,
    fetched_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    json_body JSONB NOT NULL
);

-- 1.6 Categories (API: /categories) - NEW
CREATE TABLE IF NOT EXISTS raw.categories (
    cat_id BIGSERIAL PRIMARY KEY,
    fetched_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    json_body JSONB NOT NULL
);

-- 1.7 Category Groups (API: /category_groups) - NEW
CREATE TABLE IF NOT EXISTS raw.category_groups (
    cat_group_id BIGSERIAL PRIMARY KEY,
    fetched_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    json_body JSONB NOT NULL
);


-- =================================================================
-- [DWH LAYER] Analytics Core (Star Schema / Multi-Tenant)
-- Key Strategy: Composite Primary Keys (app_company_id + smaregi_id)
-- =================================================================

-- 2.1 Dimension: Customers
CREATE TABLE IF NOT EXISTS dwh.dim_customers (
    app_company_id BIGINT NOT NULL,
    customer_id VARCHAR(50) NOT NULL, -- Smaregi ID
    
    customer_name VARCHAR(100),
    customer_kana VARCHAR(100),
    phone_number VARCHAR(50),
    mobile_number VARCHAR(50), -- Mobile specific
    
    -- Calculated Fields (Rich Dimension)
    first_visit_date DATE,
    last_visit_date DATE,
    visit_count INTEGER DEFAULT 0,
    
    is_deleted BOOLEAN DEFAULT FALSE,
    
    PRIMARY KEY (app_company_id, customer_id)
);

-- 2.2 Dimension: Staffs
CREATE TABLE IF NOT EXISTS dwh.dim_staffs (
    app_company_id BIGINT NOT NULL,
    staff_id VARCHAR(50) NOT NULL, -- Smaregi ID
    
    staff_name VARCHAR(100),
    rank VARCHAR(50),
    employ_flag INTEGER DEFAULT 1, -- 0:Retire, 1:Active
    
    PRIMARY KEY (app_company_id, staff_id)
);

-- 2.3 Dimension: Category Groups (Reference for Products)
CREATE TABLE IF NOT EXISTS dwh.dim_category_groups (
    app_company_id BIGINT NOT NULL,
    cat_group_id VARCHAR(50) NOT NULL, -- Smaregi ID
    cat_group_name VARCHAR(100), -- Tech/Retail
    
    PRIMARY KEY (app_company_id, cat_group_id)
);

-- 2.4 Dimension: Products
CREATE TABLE IF NOT EXISTS dwh.dim_products (
    app_company_id BIGINT NOT NULL,
    product_id VARCHAR(50) NOT NULL, -- Smaregi ID
    
    product_name VARCHAR(200),
    cat_group_id VARCHAR(50), -- FK Reference to Category Groups
    price INTEGER,
    
    PRIMARY KEY (app_company_id, product_id)
);

-- 2.5 Fact: Sales (Header)
CREATE TABLE IF NOT EXISTS dwh.fact_sales (
    app_company_id BIGINT NOT NULL,
    transaction_head_id VARCHAR(100) NOT NULL, -- Smaregi ID
    
    transaction_date_time TIMESTAMPTZ NOT NULL, -- Timezone Aware
    transaction_date DATE NOT NULL, -- Partition Key Candidate
    
    customer_id VARCHAR(50),
    staff_id VARCHAR(50),
    
    -- Amounts (Currency) - 1 Yen Wall Strategy
    amount_total INTEGER NOT NULL,      -- Inc. Tax (API: total)
    amount_subtotal INTEGER NOT NULL,   -- Exc. Tax (API: subtotal)
    amount_tax_include INTEGER DEFAULT 0,
    amount_tax_exclude INTEGER DEFAULT 0,
    
    amount_subtotal_discount_price INTEGER DEFAULT 0,
    amount_fee INTEGER DEFAULT 0,
    amount_shipping INTEGER DEFAULT 0,
    discount_point INTEGER DEFAULT 0,
    discount_coupon INTEGER DEFAULT 0,
    
    transaction_type VARCHAR(20), -- SALES, REFUND
    is_void BOOLEAN DEFAULT FALSE,
    
    PRIMARY KEY (app_company_id, transaction_head_id)
);

-- Indexes for Fact Sales (Performance)
CREATE INDEX idx_sales_date ON dwh.fact_sales(transaction_date);
CREATE INDEX idx_sales_customer ON dwh.fact_sales(app_company_id, customer_id);
CREATE INDEX idx_sales_staff ON dwh.fact_sales(app_company_id, staff_id);


-- 2.6 Fact: Sales Details
CREATE TABLE IF NOT EXISTS dwh.fact_sales_details (
    app_company_id BIGINT NOT NULL,
    transaction_head_id VARCHAR(100) NOT NULL,   -- Composite Key Part 2
    transaction_detail_id VARCHAR(100) NOT NULL, -- Composite Key Part 3 (Unique per line)
    
    product_id VARCHAR(50),
    
    -- Snapshots (History Preservation)
    product_name VARCHAR(200),
    category_group_name VARCHAR(100), -- Snapshot: Tech/Retail (Renamed from category_name)
    
    quantity INTEGER NOT NULL,
    sales_price INTEGER NOT NULL, -- Actual sold price
    tax_division INTEGER, -- 0:Inc, 1:Exc, 2:Non
    
    category_type VARCHAR(20), -- ANALYSIS TYPE
    
    -- Composite Primary Key (CRITICAL for Multi-tenant)
    PRIMARY KEY (app_company_id, transaction_head_id, transaction_detail_id),
    
    -- Foreign Key Constraint (Composite)
    FOREIGN KEY (app_company_id, transaction_head_id)
    REFERENCES dwh.fact_sales (app_company_id, transaction_head_id)
    ON DELETE CASCADE
);

-- =================================================================
-- [SYS LAYER] System Management
-- =================================================================

-- 3.1 App Company (Tenants)
CREATE TABLE IF NOT EXISTS sys.app_company (
    app_company_id BIGSERIAL PRIMARY KEY, -- Snowflake ID simulated
    company_name VARCHAR(100) NOT NULL
);

-- 3.2 App Users
CREATE TABLE IF NOT EXISTS sys.app_user (
    app_user_id BIGSERIAL PRIMARY KEY, -- Snowflake ID simulated
    app_company_id BIGINT NOT NULL REFERENCES sys.app_company(app_company_id),
    user_name VARCHAR(100),
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL
);

-- 3.3 Job Logs
CREATE TABLE IF NOT EXISTS sys.job_logs (
    job_id BIGSERIAL PRIMARY KEY,
    app_company_id BIGINT,
    job_name VARCHAR(100) NOT NULL,
    job_detail JSONB,
    status VARCHAR(20),
    start_time TIMESTAMPTZ,
    end_time TIMESTAMPTZ,
    message TEXT
);

-- 3.4 Merge Candidates
CREATE TABLE IF NOT EXISTS sys.merge_candidates (
    candidate_id BIGSERIAL PRIMARY KEY,
    app_company_id BIGINT NOT NULL,
    source_customer_id VARCHAR(50) NOT NULL,
    target_customer_id VARCHAR(50) NOT NULL,
    similarity_score INTEGER,
    status VARCHAR(20) DEFAULT 'PENDING',
    
    -- Prevent duplicate candidates for same pair per company
    UNIQUE(app_company_id, source_customer_id, target_customer_id)
);