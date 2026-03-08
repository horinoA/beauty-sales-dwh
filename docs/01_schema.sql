CREATE TABLE dwh.dim_category_groups (
    app_company_id bigint NOT NULL,
    cat_group_id character varying(50) NOT NULL,
    cat_group_name character varying(100),
    insert_data_time timestamp with time zone,
    update_data_time timestamp with time zone
);



CREATE TABLE dwh.dim_customers (
    app_company_id bigint NOT NULL,
    customer_id character varying(50) NOT NULL,
    customer_name character varying(100),
    customer_kana character varying(100),
    phone_number character varying(50),
    mobile_number character varying(50),
    store_id character varying(50),
    first_visit_date date,
    last_visit_date date,
    visit_count integer DEFAULT 0,
    is_deleted boolean DEFAULT false,
    insert_data_time timestamp with time zone,
    update_data_time timestamp with time zone
);



CREATE TABLE dwh.dim_products (
    app_company_id bigint NOT NULL,
    product_id character varying(50) NOT NULL,
    product_name character varying(200),
    cat_group_id character varying(50),
    price integer,
    store_id character varying(50),
    insert_data_time timestamp with time zone,
    update_data_time timestamp with time zone
);



CREATE TABLE dwh.dim_staffs (
    app_company_id bigint NOT NULL,
    staff_id character varying(50) NOT NULL,
    staff_name character varying(100),
    rank character varying(50),
    store_id character varying(50),
    employ_flag integer DEFAULT 1,
    insert_data_time timestamp with time zone,
    update_data_time timestamp with time zone
);



CREATE TABLE dwh.fact_sales (
    app_company_id bigint NOT NULL,
    transaction_head_id character varying(100) NOT NULL,
    transaction_date_time timestamp with time zone NOT NULL,
    transaction_date date NOT NULL,
    customer_id character varying(50),
    staff_id character varying(50),
    store_id character varying(50),
    amount_total integer NOT NULL,
    amount_subtotal integer NOT NULL,
    amount_tax_include integer DEFAULT 0,
    amount_tax_exclude integer DEFAULT 0,
    amount_subtotal_discount_price integer DEFAULT 0,
    amount_fee integer DEFAULT 0,
    amount_shipping integer DEFAULT 0,
    discount_point integer DEFAULT 0,
    discount_coupon integer DEFAULT 0,
    transaction_type character varying(20),
    is_void boolean DEFAULT false
);



CREATE TABLE dwh.fact_sales_details (
    app_company_id bigint NOT NULL,
    transaction_head_id character varying(100) NOT NULL,
    transaction_detail_id character varying(100) NOT NULL,
    product_id character varying(50),
    product_name character varying(200),
    category_group_name character varying(100),
    quantity integer NOT NULL,
    sales_price integer NOT NULL,
    tax_division integer,
    category_type character varying(20)
);

CREATE TABLE raw.categories (
    cat_id bigint NOT NULL,
    fetched_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    json_body jsonb NOT NULL,
    app_company_id bigint NOT NULL
);

CREATE TABLE raw.category_groups (
    cat_group_id bigint NOT NULL,
    fetched_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    json_body jsonb NOT NULL,
    app_company_id bigint NOT NULL
);

CREATE TABLE raw.customers (
    customer_id bigint NOT NULL,
    fetched_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    json_body jsonb NOT NULL,
    app_company_id bigint NOT NULL
);

CREATE TABLE raw.products (
    product_id bigint NOT NULL,
    fetched_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    json_body jsonb NOT NULL,
    app_company_id bigint NOT NULL
);

CREATE TABLE raw.staffs (
    staff_id bigint NOT NULL,
    fetched_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    json_body jsonb NOT NULL,
    app_company_id bigint NOT NULL
);

CREATE TABLE raw.transaction_details (
    detail_id bigint NOT NULL,
    transaction_head_id character varying(100) NOT NULL,
    json_body jsonb NOT NULL,
    file_name character varying(255),
    row_number bigint,
    fetched_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    app_company_id bigint NOT NULL
);

CREATE TABLE raw.transactions (
    transaction_id bigint NOT NULL,
    fetched_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    json_body jsonb NOT NULL,
    app_company_id bigint NOT NULL,
    details_extracted boolean
);

CREATE TABLE sys.app_company (
    app_company_id bigint NOT NULL,
    company_name character varying(100) NOT NULL
);


CREATE TABLE sys.app_user (
    app_user_id bigint NOT NULL,
    app_company_id bigint NOT NULL,
    user_name character varying(100),
    email character varying(255) NOT NULL,
    role character varying(50) NOT NULL
);

CREATE TABLE sys.job_logs (
    job_id bigint NOT NULL,
    app_company_id bigint,
    job_name character varying(100) NOT NULL,
    job_detail jsonb,
    status character varying(20),
    start_time timestamp with time zone,
    end_time timestamp with time zone,
    message text
);


CREATE TABLE sys.merge_candidates (
    candidate_id bigint NOT NULL,
    app_company_id bigint NOT NULL,
    source_customer_id character varying(50) NOT NULL,
    target_customer_id character varying(50) NOT NULL,
    similarity_score integer,
    status character varying(20) DEFAULT 'PENDING'::character varying
);
