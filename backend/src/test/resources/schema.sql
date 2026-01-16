-- H2 Test Schema, adapted from the updated PostgreSQL schema dump.

-- Schema definitions
CREATE SCHEMA IF NOT EXISTS dwh;
CREATE SCHEMA IF NOT EXISTS raw;
CREATE SCHEMA IF NOT EXISTS sys;

-- DWH Tables
CREATE TABLE IF NOT EXISTS dwh.dim_category_groups (
    app_company_id bigint NOT NULL,
    cat_group_id VARCHAR(50) NOT NULL,
    cat_group_name VARCHAR(100),
    insert_data_time TIMESTAMP WITH TIME ZONE,
    update_data_time TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (app_company_id, cat_group_id)
);

CREATE TABLE IF NOT EXISTS dwh.dim_customers (
    app_company_id bigint NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    customer_name VARCHAR(100),
    customer_kana VARCHAR(100),
    phone_number VARCHAR(50),
    mobile_number VARCHAR(50),
    store_id VARCHAR(50),
    first_visit_date date,
    last_visit_date date,
    visit_count integer DEFAULT 0,
    is_deleted boolean DEFAULT false,
    insert_data_time TIMESTAMP WITH TIME ZONE,
    update_data_time TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (app_company_id, customer_id)
);

CREATE TABLE IF NOT EXISTS dwh.dim_products (
    app_company_id bigint NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    product_name VARCHAR(200),
    cat_group_id VARCHAR(50),
    price integer,
    store_id VARCHAR(50),
    insert_data_time TIMESTAMP WITH TIME ZONE,
    update_data_time TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (app_company_id, product_id)
);

CREATE TABLE IF NOT EXISTS dwh.dim_staffs (
    app_company_id bigint NOT NULL,
    staff_id VARCHAR(50) NOT NULL,
    staff_name VARCHAR(100),
    staff_rank VARCHAR(50),
    store_id VARCHAR(50),
    employ_flag integer DEFAULT 1,
    insert_data_time TIMESTAMP WITH TIME ZONE,
    update_data_time TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (app_company_id, staff_id)
);

CREATE TABLE IF NOT EXISTS dwh.fact_sales (
    app_company_id bigint NOT NULL,
    transaction_head_id VARCHAR(100) NOT NULL,
    transaction_date_time TIMESTAMP WITH TIME ZONE NOT NULL,
    transaction_date date NOT NULL,
    customer_id VARCHAR(50),
    staff_id VARCHAR(50),
    store_id VARCHAR(50),
    amount_total integer NOT NULL,
    amount_subtotal integer NOT NULL,
    amount_tax_include integer DEFAULT 0,
    amount_tax_exclude integer DEFAULT 0,
    amount_subtotal_discount_price integer DEFAULT 0,
    amount_fee integer DEFAULT 0,
    amount_shipping integer DEFAULT 0,
    discount_point integer DEFAULT 0,
    discount_coupon integer DEFAULT 0,
    transaction_type VARCHAR(20),
    is_void boolean DEFAULT false,
    PRIMARY KEY (app_company_id, transaction_head_id)
);

CREATE TABLE IF NOT EXISTS dwh.fact_sales_details (
    app_company_id bigint NOT NULL,
    transaction_head_id VARCHAR(100) NOT NULL,
    transaction_detail_id VARCHAR(100) NOT NULL,
    product_id VARCHAR(50),
    product_name VARCHAR(200),
    category_group_name VARCHAR(100),
    quantity integer NOT NULL,
    sales_price integer NOT NULL,
    tax_division integer,
    category_type VARCHAR(20),
    PRIMARY KEY (app_company_id, transaction_head_id, transaction_detail_id)
);

-- RAW Tables
CREATE TABLE IF NOT EXISTS raw.categories (
    cat_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fetched_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    json_body TEXT NOT NULL,
    app_company_id bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS raw.category_groups (
    cat_group_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fetched_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    json_body TEXT NOT NULL,
    app_company_id bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS raw.customers (
    customer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fetched_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    json_body TEXT NOT NULL,
    app_company_id bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS raw.products (
    product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fetched_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    json_body TEXT NOT NULL,
    app_company_id bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS raw.staffs (
    staff_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fetched_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    json_body TEXT NOT NULL,
    app_company_id bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS raw.transaction_details (
    detail_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_head_id VARCHAR(100) NOT NULL,
    json_body TEXT NOT NULL,
    file_name VARCHAR(255),
    row_number bigint,
    fetched_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    app_company_id bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS raw.transactions (
    transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fetched_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    json_body TEXT NOT NULL,
    app_company_id bigint NOT NULL
);

-- SYS Tables
CREATE TABLE IF NOT EXISTS sys.app_company (
    app_company_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS sys.app_user (
    app_user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    app_company_id bigint NOT NULL,
    user_name VARCHAR(100),
    email VARCHAR(255) NOT NULL UNIQUE,
    user_role VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS sys.job_logs (
    job_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    app_company_id bigint,
    job_name VARCHAR(100) NOT NULL,
    job_detail TEXT,
    status VARCHAR(20),
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE,
    message TEXT
);

CREATE TABLE IF NOT EXISTS sys.merge_candidates (
    candidate_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    app_company_id bigint NOT NULL,
    source_customer_id VARCHAR(50) NOT NULL,
    target_customer_id VARCHAR(50) NOT NULL,
    similarity_score integer,
    status VARCHAR(20) DEFAULT 'PENDING',
    UNIQUE(app_company_id, source_customer_id, target_customer_id)
);