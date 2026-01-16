--
-- PostgreSQL database dump
--

\restrict JikfNbiFu8PD596j6UgoC5bOmh5ARSNlLUaAdhun4JyTUtdFoUqzy9ZIla4B1sG

-- Dumped from database version 16.11
-- Dumped by pg_dump version 16.11

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: dwh; Type: SCHEMA; Schema: -; Owner: beauty_user
--

CREATE SCHEMA dwh;


ALTER SCHEMA dwh OWNER TO beauty_user;

--
-- Name: raw; Type: SCHEMA; Schema: -; Owner: beauty_user
--

CREATE SCHEMA raw;


ALTER SCHEMA raw OWNER TO beauty_user;

--
-- Name: sys; Type: SCHEMA; Schema: -; Owner: beauty_user
--

CREATE SCHEMA sys;


ALTER SCHEMA sys OWNER TO beauty_user;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: dim_category_groups; Type: TABLE; Schema: dwh; Owner: beauty_user
--

CREATE TABLE dwh.dim_category_groups (
    app_company_id bigint NOT NULL,
    cat_group_id character varying(50) NOT NULL,
    cat_group_name character varying(100),
    insert_data_time timestamp with time zone,
    update_data_time timestamp with time zone
);


ALTER TABLE dwh.dim_category_groups OWNER TO beauty_user;

--
-- Name: dim_customers; Type: TABLE; Schema: dwh; Owner: beauty_user
--

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


ALTER TABLE dwh.dim_customers OWNER TO beauty_user;

--
-- Name: dim_products; Type: TABLE; Schema: dwh; Owner: beauty_user
--

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


ALTER TABLE dwh.dim_products OWNER TO beauty_user;

--
-- Name: dim_staffs; Type: TABLE; Schema: dwh; Owner: beauty_user
--

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


ALTER TABLE dwh.dim_staffs OWNER TO beauty_user;

--
-- Name: fact_sales; Type: TABLE; Schema: dwh; Owner: beauty_user
--

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


ALTER TABLE dwh.fact_sales OWNER TO beauty_user;

--
-- Name: fact_sales_details; Type: TABLE; Schema: dwh; Owner: beauty_user
--

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


ALTER TABLE dwh.fact_sales_details OWNER TO beauty_user;

--
-- Name: categories; Type: TABLE; Schema: raw; Owner: beauty_user
--

CREATE TABLE raw.categories (
    cat_id bigint NOT NULL,
    fetched_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    json_body jsonb NOT NULL,
    app_company_id bigint NOT NULL
);


ALTER TABLE raw.categories OWNER TO beauty_user;

--
-- Name: categories_cat_id_seq; Type: SEQUENCE; Schema: raw; Owner: beauty_user
--

CREATE SEQUENCE raw.categories_cat_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE raw.categories_cat_id_seq OWNER TO beauty_user;

--
-- Name: categories_cat_id_seq; Type: SEQUENCE OWNED BY; Schema: raw; Owner: beauty_user
--

ALTER SEQUENCE raw.categories_cat_id_seq OWNED BY raw.categories.cat_id;


--
-- Name: category_groups; Type: TABLE; Schema: raw; Owner: beauty_user
--

CREATE TABLE raw.category_groups (
    cat_group_id bigint NOT NULL,
    fetched_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    json_body jsonb NOT NULL,
    app_company_id bigint NOT NULL
);


ALTER TABLE raw.category_groups OWNER TO beauty_user;

--
-- Name: category_groups_cat_group_id_seq; Type: SEQUENCE; Schema: raw; Owner: beauty_user
--

CREATE SEQUENCE raw.category_groups_cat_group_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE raw.category_groups_cat_group_id_seq OWNER TO beauty_user;

--
-- Name: category_groups_cat_group_id_seq; Type: SEQUENCE OWNED BY; Schema: raw; Owner: beauty_user
--

ALTER SEQUENCE raw.category_groups_cat_group_id_seq OWNED BY raw.category_groups.cat_group_id;


--
-- Name: customers; Type: TABLE; Schema: raw; Owner: beauty_user
--

CREATE TABLE raw.customers (
    customer_id bigint NOT NULL,
    fetched_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    json_body jsonb NOT NULL,
    app_company_id bigint NOT NULL
);


ALTER TABLE raw.customers OWNER TO beauty_user;

--
-- Name: customers_customer_id_seq; Type: SEQUENCE; Schema: raw; Owner: beauty_user
--

CREATE SEQUENCE raw.customers_customer_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE raw.customers_customer_id_seq OWNER TO beauty_user;

--
-- Name: customers_customer_id_seq; Type: SEQUENCE OWNED BY; Schema: raw; Owner: beauty_user
--

ALTER SEQUENCE raw.customers_customer_id_seq OWNED BY raw.customers.customer_id;


--
-- Name: products; Type: TABLE; Schema: raw; Owner: beauty_user
--

CREATE TABLE raw.products (
    product_id bigint NOT NULL,
    fetched_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    json_body jsonb NOT NULL,
    app_company_id bigint NOT NULL
);


ALTER TABLE raw.products OWNER TO beauty_user;

--
-- Name: products_product_id_seq; Type: SEQUENCE; Schema: raw; Owner: beauty_user
--

CREATE SEQUENCE raw.products_product_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE raw.products_product_id_seq OWNER TO beauty_user;

--
-- Name: products_product_id_seq; Type: SEQUENCE OWNED BY; Schema: raw; Owner: beauty_user
--

ALTER SEQUENCE raw.products_product_id_seq OWNED BY raw.products.product_id;


--
-- Name: staffs; Type: TABLE; Schema: raw; Owner: beauty_user
--

CREATE TABLE raw.staffs (
    staff_id bigint NOT NULL,
    fetched_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    json_body jsonb NOT NULL,
    app_company_id bigint NOT NULL
);


ALTER TABLE raw.staffs OWNER TO beauty_user;

--
-- Name: staffs_staff_id_seq; Type: SEQUENCE; Schema: raw; Owner: beauty_user
--

CREATE SEQUENCE raw.staffs_staff_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE raw.staffs_staff_id_seq OWNER TO beauty_user;

--
-- Name: staffs_staff_id_seq; Type: SEQUENCE OWNED BY; Schema: raw; Owner: beauty_user
--

ALTER SEQUENCE raw.staffs_staff_id_seq OWNED BY raw.staffs.staff_id;


--
-- Name: transaction_details; Type: TABLE; Schema: raw; Owner: beauty_user
--

CREATE TABLE raw.transaction_details (
    detail_id bigint NOT NULL,
    transaction_head_id character varying(100) NOT NULL,
    json_body jsonb NOT NULL,
    file_name character varying(255),
    row_number bigint,
    fetched_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    app_company_id bigint NOT NULL
);


ALTER TABLE raw.transaction_details OWNER TO beauty_user;

--
-- Name: transaction_details_detail_id_seq; Type: SEQUENCE; Schema: raw; Owner: beauty_user
--

CREATE SEQUENCE raw.transaction_details_detail_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE raw.transaction_details_detail_id_seq OWNER TO beauty_user;

--
-- Name: transaction_details_detail_id_seq; Type: SEQUENCE OWNED BY; Schema: raw; Owner: beauty_user
--

ALTER SEQUENCE raw.transaction_details_detail_id_seq OWNED BY raw.transaction_details.detail_id;


--
-- Name: transactions; Type: TABLE; Schema: raw; Owner: beauty_user
--

CREATE TABLE raw.transactions (
    transaction_id bigint NOT NULL,
    fetched_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    json_body jsonb NOT NULL,
    app_company_id bigint NOT NULL
);


ALTER TABLE raw.transactions OWNER TO beauty_user;

--
-- Name: transactions_transaction_id_seq; Type: SEQUENCE; Schema: raw; Owner: beauty_user
--

CREATE SEQUENCE raw.transactions_transaction_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE raw.transactions_transaction_id_seq OWNER TO beauty_user;

--
-- Name: transactions_transaction_id_seq; Type: SEQUENCE OWNED BY; Schema: raw; Owner: beauty_user
--

ALTER SEQUENCE raw.transactions_transaction_id_seq OWNED BY raw.transactions.transaction_id;


--
-- Name: app_company; Type: TABLE; Schema: sys; Owner: beauty_user
--

CREATE TABLE sys.app_company (
    app_company_id bigint NOT NULL,
    company_name character varying(100) NOT NULL
);


ALTER TABLE sys.app_company OWNER TO beauty_user;

--
-- Name: app_company_app_company_id_seq; Type: SEQUENCE; Schema: sys; Owner: beauty_user
--

CREATE SEQUENCE sys.app_company_app_company_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE sys.app_company_app_company_id_seq OWNER TO beauty_user;

--
-- Name: app_company_app_company_id_seq; Type: SEQUENCE OWNED BY; Schema: sys; Owner: beauty_user
--

ALTER SEQUENCE sys.app_company_app_company_id_seq OWNED BY sys.app_company.app_company_id;


--
-- Name: app_user; Type: TABLE; Schema: sys; Owner: beauty_user
--

CREATE TABLE sys.app_user (
    app_user_id bigint NOT NULL,
    app_company_id bigint NOT NULL,
    user_name character varying(100),
    email character varying(255) NOT NULL,
    role character varying(50) NOT NULL
);


ALTER TABLE sys.app_user OWNER TO beauty_user;

--
-- Name: app_user_app_user_id_seq; Type: SEQUENCE; Schema: sys; Owner: beauty_user
--

CREATE SEQUENCE sys.app_user_app_user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE sys.app_user_app_user_id_seq OWNER TO beauty_user;

--
-- Name: app_user_app_user_id_seq; Type: SEQUENCE OWNED BY; Schema: sys; Owner: beauty_user
--

ALTER SEQUENCE sys.app_user_app_user_id_seq OWNED BY sys.app_user.app_user_id;


--
-- Name: job_logs; Type: TABLE; Schema: sys; Owner: beauty_user
--

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


ALTER TABLE sys.job_logs OWNER TO beauty_user;

--
-- Name: job_logs_job_id_seq; Type: SEQUENCE; Schema: sys; Owner: beauty_user
--

CREATE SEQUENCE sys.job_logs_job_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE sys.job_logs_job_id_seq OWNER TO beauty_user;

--
-- Name: job_logs_job_id_seq; Type: SEQUENCE OWNED BY; Schema: sys; Owner: beauty_user
--

ALTER SEQUENCE sys.job_logs_job_id_seq OWNED BY sys.job_logs.job_id;


--
-- Name: merge_candidates; Type: TABLE; Schema: sys; Owner: beauty_user
--

CREATE TABLE sys.merge_candidates (
    candidate_id bigint NOT NULL,
    app_company_id bigint NOT NULL,
    source_customer_id character varying(50) NOT NULL,
    target_customer_id character varying(50) NOT NULL,
    similarity_score integer,
    status character varying(20) DEFAULT 'PENDING'::character varying
);


ALTER TABLE sys.merge_candidates OWNER TO beauty_user;

--
-- Name: merge_candidates_candidate_id_seq; Type: SEQUENCE; Schema: sys; Owner: beauty_user
--

CREATE SEQUENCE sys.merge_candidates_candidate_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE sys.merge_candidates_candidate_id_seq OWNER TO beauty_user;

--
-- Name: merge_candidates_candidate_id_seq; Type: SEQUENCE OWNED BY; Schema: sys; Owner: beauty_user
--

ALTER SEQUENCE sys.merge_candidates_candidate_id_seq OWNED BY sys.merge_candidates.candidate_id;


--
-- Name: categories cat_id; Type: DEFAULT; Schema: raw; Owner: beauty_user
--

ALTER TABLE ONLY raw.categories ALTER COLUMN cat_id SET DEFAULT nextval('raw.categories_cat_id_seq'::regclass);


--
-- Name: category_groups cat_group_id; Type: DEFAULT; Schema: raw; Owner: beauty_user
--

ALTER TABLE ONLY raw.category_groups ALTER COLUMN cat_group_id SET DEFAULT nextval('raw.category_groups_cat_group_id_seq'::regclass);


--
-- Name: customers customer_id; Type: DEFAULT; Schema: raw; Owner: beauty_user
--

ALTER TABLE ONLY raw.customers ALTER COLUMN customer_id SET DEFAULT nextval('raw.customers_customer_id_seq'::regclass);


--
-- Name: products product_id; Type: DEFAULT; Schema: raw; Owner: beauty_user
--

ALTER TABLE ONLY raw.products ALTER COLUMN product_id SET DEFAULT nextval('raw.products_product_id_seq'::regclass);


--
-- Name: staffs staff_id; Type: DEFAULT; Schema: raw; Owner: beauty_user
--

ALTER TABLE ONLY raw.staffs ALTER COLUMN staff_id SET DEFAULT nextval('raw.staffs_staff_id_seq'::regclass);


--
-- Name: transaction_details detail_id; Type: DEFAULT; Schema: raw; Owner: beauty_user
--

ALTER TABLE ONLY raw.transaction_details ALTER COLUMN detail_id SET DEFAULT nextval('raw.transaction_details_detail_id_seq'::regclass);


--
-- Name: transactions transaction_id; Type: DEFAULT; Schema: raw; Owner: beauty_user
--

ALTER TABLE ONLY raw.transactions ALTER COLUMN transaction_id SET DEFAULT nextval('raw.transactions_transaction_id_seq'::regclass);


--
-- Name: app_company app_company_id; Type: DEFAULT; Schema: sys; Owner: beauty_user
--

ALTER TABLE ONLY sys.app_company ALTER COLUMN app_company_id SET DEFAULT nextval('sys.app_company_app_company_id_seq'::regclass);


--
-- Name: app_user app_user_id; Type: DEFAULT; Schema: sys; Owner: beauty_user
--

ALTER TABLE ONLY sys.app_user ALTER COLUMN app_user_id SET DEFAULT nextval('sys.app_user_app_user_id_seq'::regclass);


--
-- Name: job_logs job_id; Type: DEFAULT; Schema: sys; Owner: beauty_user
--

ALTER TABLE ONLY sys.job_logs ALTER COLUMN job_id SET DEFAULT nextval('sys.job_logs_job_id_seq'::regclass);


--
-- Name: merge_candidates candidate_id; Type: DEFAULT; Schema: sys; Owner: beauty_user
--

ALTER TABLE ONLY sys.merge_candidates ALTER COLUMN candidate_id SET DEFAULT nextval('sys.merge_candidates_candidate_id_seq'::regclass);


--
-- Data for Name: dim_category_groups; Type: TABLE DATA; Schema: dwh; Owner: beauty_user
--

COPY dwh.dim_category_groups (app_company_id, cat_group_id, cat_group_name, insert_data_time, update_data_time) FROM stdin;
4096	1	技術	2025-12-01 00:00:00+09	2025-12-01 00:00:00+09
4096	2	店販	2025-12-01 00:00:00+09	2025-12-01 00:00:00+09
\.


--
-- Data for Name: dim_customers; Type: TABLE DATA; Schema: dwh; Owner: beauty_user
--

COPY dwh.dim_customers (app_company_id, customer_id, customer_name, customer_kana, phone_number, mobile_number, store_id, first_visit_date, last_visit_date, visit_count, is_deleted, insert_data_time, update_data_time) FROM stdin;
4096	2	ヤマダ_ハナコ	ヤマダ_ハナコ	\N	090-1111-2222	0	\N	\N	\N	f	2025-12-01 00:00:00+09	2025-12-01 00:00:00+09
4096	1	山田_花子	ヤマダ_ハナコ	090-1111-2222	090-1111-2222	0	\N	2025-12-02	\N	f	2025-12-01 00:00:00+09	2025-12-01 00:00:00+09
4096	3	佐藤_健一	サトウ_ケンイチ	03-1234-5678	090-3333-4444	0	2023-05-10	2025-11-20	15	f	2023-05-10 10:00:00+09	2025-11-20 18:30:00+09
4096	4	鈴木_愛	スズキ_アイ	\N	080-5555-6666	0	2024-01-15	2025-10-30	8	f	2024-01-15 11:00:00+09	2025-10-30 14:00:00+09
4096	5	高橋_美咲	タカハシ_ミサキ	\N	070-9876-5432	0	2025-08-01	2025-08-01	1	f	2025-08-01 13:00:00+09	2025-08-01 13:00:00+09
4096	6	田中_健	タナカ_ケン	06-6666-7777	090-7777-8888	0	2022-11-11	2025-09-15	20	f	2022-11-11 09:00:00+09	2025-09-15 12:00:00+09
4096	7	タナカ_ケン	タナカ_ケン	\N	090-7777-8888	0	2024-03-20	2024-03-20	1	f	2024-03-20 15:00:00+09	2024-03-20 15:00:00+09
4096	8	伊藤_直人	イトウ_ナオト	\N	080-1122-3344	0	2023-12-05	2024-06-30	3	f	2023-12-05 14:30:00+09	2024-06-30 16:00:00+09
4096	9	渡辺_麻衣	ワタナベ_マイ	045-222-3333	090-9988-7766	0	2025-01-10	2025-11-01	6	f	2025-01-10 10:00:00+09	2025-11-01 19:00:00+09
4096	10	山本_さくら	ヤマモト_サクラ	\N	090-5678-1234	0	2025-04-01	2025-11-25	4	f	2025-04-01 11:00:00+09	2025-11-25 13:30:00+09
4096	11	中村_拓也	ナカムラ_タクヤ	03-5555-4444	\N	0	2021-06-15	2023-08-10	5	f	2021-06-15 12:00:00+09	2023-08-10 15:00:00+09
4096	12	小林_優子	コバヤシ_ユウコ	\N	080-4444-5555	0	2025-09-05	2025-10-20	2	f	2025-09-05 16:00:00+09	2025-10-20 11:00:00+09
4096	13	加藤_浩二	カトウ_コウジ	092-111-2222	090-2222-3333	0	2024-02-14	2025-11-28	12	f	2024-02-14 10:00:00+09	2025-11-28 17:00:00+09
4096	14	木村_里奈	キムラ_リナ	\N	070-1111-9999	0	2025-10-10	2025-10-10	1	f	2025-10-10 14:00:00+09	2025-10-10 14:00:00+09
4096	15	林_大輔	ハヤシ_ダイスケ	\N	090-8888-0000	0	2023-07-07	2024-12-25	5	f	2023-07-07 18:00:00+09	2024-12-25 12:00:00+09
\.


--
-- Data for Name: dim_products; Type: TABLE DATA; Schema: dwh; Owner: beauty_user
--

COPY dwh.dim_products (app_company_id, product_id, product_name, cat_group_id, price, store_id, insert_data_time, update_data_time) FROM stdin;
4096	8000021	洗い流さないTR(ミルク)	2	3080	0	2025-12-01 13:16:02+09	2025-12-04 11:25:03+09
4096	8000020	アロマキャンドル(ギフト)	2	3850	0	2025-12-01 13:16:02+09	2025-12-04 11:25:03+09
4096	8000019	グリースワックス(ハード)	2	2420	0	2025-12-01 13:16:02+09	2025-12-04 11:25:03+09
4096	8000018	カラーキープシャンプー(紫)	2	2200	0	2025-12-01 13:16:02+09	2025-12-04 11:25:03+09
4096	8000017	リフトアップ美顔器	2	29800	0	2025-12-01 13:16:02+09	2025-12-04 11:25:03+09
4096	8000016	ハードワックス	2	1980	0	2025-12-01 13:16:02+09	2025-12-04 11:25:03+09
4096	8000015	オーガニックヘアオイル	2	3850	0	2025-12-01 13:16:02+09	2025-12-04 11:25:03+09
4096	8000014	オリジナルTR(300g)	2	3300	0	2025-12-01 13:16:02+09	2025-12-04 11:25:02+09
4096	8000013	オリジナルシャンプー(300ml)	2	2750	0	2025-12-01 13:16:02+09	2025-12-04 11:25:02+09
4096	8000012	指名料	1	550	0	2025-12-01 13:16:02+09	2025-12-04 11:25:02+09
4096	8000011	髪質改善トリートメント	1	11000	0	2025-12-01 13:16:02+09	2025-12-04 11:25:02+09
4096	8000010	3Stepトリートメント	1	4400	0	2025-12-01 13:16:02+09	2025-12-04 11:25:02+09
4096	8000009	縮毛矯正	1	16500	0	2025-12-01 13:16:02+09	2025-12-04 11:25:02+09
4096	8000008	デジタルパーマ	1	13200	0	2025-12-01 13:16:02+09	2025-12-04 11:25:02+09
4096	8000007	コールドパーマ	1	7700	0	2025-12-01 13:16:02+09	2025-12-04 11:25:02+09
4096	8000006	イルミナカラー	1	9900	0	2025-12-01 13:16:02+09	2025-12-04 11:25:02+09
4096	8000005	リタッチカラー	1	5500	0	2025-12-01 13:16:02+09	2025-12-04 11:25:02+09
4096	8000004	フルカラー	1	7700	0	2025-12-01 10:26:52+09	2025-12-04 11:25:02+09
4096	8000003	前髪カット	1	1100	0	2025-12-01 10:06:32+09	2025-12-04 11:25:02+09
4096	8000002	スクールカット	1	3850	0	2025-11-27 16:12:37+09	2025-12-04 11:25:02+09
4096	8000001	デザインカット	1	5500	0	2025-11-27 16:09:51+09	2025-12-04 11:25:02+09
\.


--
-- Data for Name: dim_staffs; Type: TABLE DATA; Schema: dwh; Owner: beauty_user
--

COPY dwh.dim_staffs (app_company_id, staff_id, staff_name, rank, store_id, employ_flag, insert_data_time, update_data_time) FROM stdin;
4096	11	加藤 葵	アシスタント	0	1	2025-12-01 13:47:02+09	2025-12-04 11:43:22+09
4096	10	小林 大樹	アシスタント	0	1	2025-12-01 13:47:02+09	2025-12-04 11:43:30+09
4096	9	中村 陽菜	アシスタント	0	1	2025-12-01 13:47:02+09	2025-12-04 11:43:39+09
4096	8	山本 陸 	Jr.スタイリスト	0	1	2025-12-01 13:47:02+09	2025-12-04 11:43:59+09
4096	7	渡辺 結衣 	スタイリスト	0	1	2025-12-01 13:47:02+09	2025-12-04 11:44:06+09
4096	6	伊藤 蓮	スタイリスト	0	1	2025-12-01 13:47:02+09	2025-12-04 11:44:14+09
4096	5	高橋 優子 	トップスタイリスト	0	1	2025-12-01 13:47:02+09	2025-12-04 11:44:21+09
4096	4	鈴木 翔太	トップスタイリスト	0	1	2025-12-01 13:47:02+09	2025-12-04 11:44:33+09
4096	3	田中 美咲	店長 (Manager)	0	1	2025-12-01 13:47:02+09	2025-12-04 11:44:39+09
4096	2	佐藤 健一 	オーナー	0	1	2025-12-01 13:47:02+09	2025-12-04 11:44:45+09
4096	1	管理者		0	1	2025-11-25 05:46:22+09	2025-12-04 11:32:23+09
\.


--
-- Data for Name: fact_sales; Type: TABLE DATA; Schema: dwh; Owner: beauty_user
--

COPY dwh.fact_sales (app_company_id, transaction_head_id, transaction_date_time, transaction_date, customer_id, staff_id, store_id, amount_total, amount_subtotal, amount_tax_include, amount_tax_exclude, amount_subtotal_discount_price, amount_fee, amount_shipping, discount_point, discount_coupon, transaction_type, is_void) FROM stdin;
4096	2	2023-05-10 10:00:00+09	2023-05-10	3	2	1	13200	12000	1200	0	0	0	0	0	0	SALES	f
4096	3	2023-07-07 18:00:00+09	2023-07-07	15	10	2	3850	3500	350	0	0	0	0	0	0	SALES	f
4096	4	2024-01-15 11:00:00+09	2024-01-15	4	3	1	14300	13000	1300	0	0	0	0	0	0	SALES	f
4096	5	2024-02-14 10:00:00+09	2024-02-14	13	4	1	5500	5000	500	0	0	0	0	0	0	SALES	f
4096	6	2024-03-20 15:00:00+09	2024-03-20	7	5	2	7700	7000	700	0	0	0	0	0	0	SALES	f
4096	7	2025-08-01 13:00:00+09	2025-08-01	5	6	2	12100	11000	1100	0	0	0	0	0	0	SALES	f
4096	8	2025-09-05 16:00:00+09	2025-09-05	12	7	1	2750	2500	250	0	0	0	0	0	0	SALES	f
4096	9	2025-10-10 14:00:00+09	2025-10-10	14	8	2	4400	4000	400	0	0	0	0	0	0	SALES	f
4096	10	2025-11-01 19:00:00+09	2025-11-01	9	9	1	16500	15000	1500	0	0	0	0	0	0	SALES	f
4096	11	2025-11-20 18:30:00+09	2025-11-20	3	2	1	5500	5000	500	0	0	0	0	0	0	SALES	f
4096	12	2025-11-25 13:30:00+09	2025-11-25	10	10	1	9900	9000	900	0	0	0	0	0	0	SALES	f
4096	13	2025-11-28 17:00:00+09	2025-11-28	13	11	2	7700	7000	700	0	0	0	0	0	0	SALES	f
4096	14	2025-11-30 11:00:00+09	2025-11-30	6	3	2	-5500	-5000	-500	0	0	0	0	0	0	REFUND	f
\.


--
-- Data for Name: fact_sales_details; Type: TABLE DATA; Schema: dwh; Owner: beauty_user
--

COPY dwh.fact_sales_details (app_company_id, transaction_head_id, transaction_detail_id, product_id, product_name, category_group_name, quantity, sales_price, tax_division, category_type) FROM stdin;
4096	2	4	8000001	デザインカット	技術	1	5500	0	SALES
4096	2	5	8000004	フルカラー	技術	1	7700	0	SALES
4096	3	6	8000002	スクールカット	技術	1	3850	0	SALES
4096	4	7	8000008	デジタルパーマ	技術	1	13200	0	SALES
4096	4	8	8000003	前髪カット	技術	1	1100	0	SALES
4096	5	9	8000001	デザインカット	技術	1	5500	0	SALES
4096	6	10	8000007	コールドパーマ	技術	1	7700	0	SALES
4096	7	11	8000011	髪質改善トリートメント	技術	1	11000	0	SALES
4096	7	12	8000003	前髪カット	技術	1	1100	0	SALES
4096	8	13	8000013	オリジナルシャンプー(300ml)	店販	1	2750	0	SALES
4096	9	14	8000010	3Stepトリートメント	技術	1	4400	0	SALES
4096	10	15	8000009	縮毛矯正	技術	1	16500	0	SALES
4096	11	16	8000005	リタッチカラー	技術	1	5500	0	SALES
4096	12	17	8000006	イルミナカラー	技術	1	9900	0	SALES
4096	13	18	8000004	フルカラー	技術	1	7700	0	SALES
4096	14	19	8000001	デザインカット	技術	1	-5500	0	REFUND
\.


--
-- Data for Name: categories; Type: TABLE DATA; Schema: raw; Owner: beauty_user
--

COPY raw.categories (cat_id, fetched_at, json_body, app_company_id) FROM stdin;
\.


--
-- Data for Name: category_groups; Type: TABLE DATA; Schema: raw; Owner: beauty_user
--

COPY raw.category_groups (cat_group_id, fetched_at, json_body, app_company_id) FROM stdin;
\.


--
-- Data for Name: customers; Type: TABLE DATA; Schema: raw; Owner: beauty_user
--

COPY raw.customers (customer_id, fetched_at, json_body, app_company_id) FROM stdin;
\.


--
-- Data for Name: products; Type: TABLE DATA; Schema: raw; Owner: beauty_user
--

COPY raw.products (product_id, fetched_at, json_body, app_company_id) FROM stdin;
\.


--
-- Data for Name: staffs; Type: TABLE DATA; Schema: raw; Owner: beauty_user
--

COPY raw.staffs (staff_id, fetched_at, json_body, app_company_id) FROM stdin;
\.


--
-- Data for Name: transaction_details; Type: TABLE DATA; Schema: raw; Owner: beauty_user
--

COPY raw.transaction_details (detail_id, transaction_head_id, json_body, file_name, row_number, fetched_at, app_company_id) FROM stdin;
\.


--
-- Data for Name: transactions; Type: TABLE DATA; Schema: raw; Owner: beauty_user
--

COPY raw.transactions (transaction_id, fetched_at, json_body, app_company_id) FROM stdin;
\.


--
-- Data for Name: app_company; Type: TABLE DATA; Schema: sys; Owner: beauty_user
--

COPY sys.app_company (app_company_id, company_name) FROM stdin;
4096	Lua Beauty Works
\.


--
-- Data for Name: app_user; Type: TABLE DATA; Schema: sys; Owner: beauty_user
--

COPY sys.app_user (app_user_id, app_company_id, user_name, email, role) FROM stdin;
4194308096	4096	管理者	beauty@sale.com	ADMIN
\.


--
-- Data for Name: job_logs; Type: TABLE DATA; Schema: sys; Owner: beauty_user
--

COPY sys.job_logs (job_id, app_company_id, job_name, job_detail, status, start_time, end_time, message) FROM stdin;
\.


--
-- Data for Name: merge_candidates; Type: TABLE DATA; Schema: sys; Owner: beauty_user
--

COPY sys.merge_candidates (candidate_id, app_company_id, source_customer_id, target_customer_id, similarity_score, status) FROM stdin;
\.


--
-- Name: categories_cat_id_seq; Type: SEQUENCE SET; Schema: raw; Owner: beauty_user
--

SELECT pg_catalog.setval('raw.categories_cat_id_seq', 1, false);


--
-- Name: category_groups_cat_group_id_seq; Type: SEQUENCE SET; Schema: raw; Owner: beauty_user
--

SELECT pg_catalog.setval('raw.category_groups_cat_group_id_seq', 1, false);


--
-- Name: customers_customer_id_seq; Type: SEQUENCE SET; Schema: raw; Owner: beauty_user
--

SELECT pg_catalog.setval('raw.customers_customer_id_seq', 1, false);


--
-- Name: products_product_id_seq; Type: SEQUENCE SET; Schema: raw; Owner: beauty_user
--

SELECT pg_catalog.setval('raw.products_product_id_seq', 1, false);


--
-- Name: staffs_staff_id_seq; Type: SEQUENCE SET; Schema: raw; Owner: beauty_user
--

SELECT pg_catalog.setval('raw.staffs_staff_id_seq', 1, false);


--
-- Name: transaction_details_detail_id_seq; Type: SEQUENCE SET; Schema: raw; Owner: beauty_user
--

SELECT pg_catalog.setval('raw.transaction_details_detail_id_seq', 1, false);


--
-- Name: transactions_transaction_id_seq; Type: SEQUENCE SET; Schema: raw; Owner: beauty_user
--

SELECT pg_catalog.setval('raw.transactions_transaction_id_seq', 1, false);


--
-- Name: app_company_app_company_id_seq; Type: SEQUENCE SET; Schema: sys; Owner: beauty_user
--

SELECT pg_catalog.setval('sys.app_company_app_company_id_seq', 1, false);


--
-- Name: app_user_app_user_id_seq; Type: SEQUENCE SET; Schema: sys; Owner: beauty_user
--

SELECT pg_catalog.setval('sys.app_user_app_user_id_seq', 1, false);


--
-- Name: job_logs_job_id_seq; Type: SEQUENCE SET; Schema: sys; Owner: beauty_user
--

SELECT pg_catalog.setval('sys.job_logs_job_id_seq', 1, false);


--
-- Name: merge_candidates_candidate_id_seq; Type: SEQUENCE SET; Schema: sys; Owner: beauty_user
--

SELECT pg_catalog.setval('sys.merge_candidates_candidate_id_seq', 1, false);


--
-- Name: dim_category_groups dim_category_groups_pkey; Type: CONSTRAINT; Schema: dwh; Owner: beauty_user
--

ALTER TABLE ONLY dwh.dim_category_groups
    ADD CONSTRAINT dim_category_groups_pkey PRIMARY KEY (app_company_id, cat_group_id);


--
-- Name: dim_customers dim_customers_pkey; Type: CONSTRAINT; Schema: dwh; Owner: beauty_user
--

ALTER TABLE ONLY dwh.dim_customers
    ADD CONSTRAINT dim_customers_pkey PRIMARY KEY (app_company_id, customer_id);


--
-- Name: dim_products dim_products_pkey; Type: CONSTRAINT; Schema: dwh; Owner: beauty_user
--

ALTER TABLE ONLY dwh.dim_products
    ADD CONSTRAINT dim_products_pkey PRIMARY KEY (app_company_id, product_id);


--
-- Name: dim_staffs dim_staffs_pkey; Type: CONSTRAINT; Schema: dwh; Owner: beauty_user
--

ALTER TABLE ONLY dwh.dim_staffs
    ADD CONSTRAINT dim_staffs_pkey PRIMARY KEY (app_company_id, staff_id);


--
-- Name: fact_sales_details fact_sales_details_pkey; Type: CONSTRAINT; Schema: dwh; Owner: beauty_user
--

ALTER TABLE ONLY dwh.fact_sales_details
    ADD CONSTRAINT fact_sales_details_pkey PRIMARY KEY (app_company_id, transaction_head_id, transaction_detail_id);


--
-- Name: fact_sales fact_sales_pkey; Type: CONSTRAINT; Schema: dwh; Owner: beauty_user
--

ALTER TABLE ONLY dwh.fact_sales
    ADD CONSTRAINT fact_sales_pkey PRIMARY KEY (app_company_id, transaction_head_id);


--
-- Name: categories categories_pkey; Type: CONSTRAINT; Schema: raw; Owner: beauty_user
--

ALTER TABLE ONLY raw.categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (cat_id);


--
-- Name: category_groups category_groups_pkey; Type: CONSTRAINT; Schema: raw; Owner: beauty_user
--

ALTER TABLE ONLY raw.category_groups
    ADD CONSTRAINT category_groups_pkey PRIMARY KEY (cat_group_id);


--
-- Name: customers customers_pkey; Type: CONSTRAINT; Schema: raw; Owner: beauty_user
--

ALTER TABLE ONLY raw.customers
    ADD CONSTRAINT customers_pkey PRIMARY KEY (customer_id);


--
-- Name: products products_pkey; Type: CONSTRAINT; Schema: raw; Owner: beauty_user
--

ALTER TABLE ONLY raw.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (product_id);


--
-- Name: staffs staffs_pkey; Type: CONSTRAINT; Schema: raw; Owner: beauty_user
--

ALTER TABLE ONLY raw.staffs
    ADD CONSTRAINT staffs_pkey PRIMARY KEY (staff_id);


--
-- Name: transaction_details transaction_details_pkey; Type: CONSTRAINT; Schema: raw; Owner: beauty_user
--

ALTER TABLE ONLY raw.transaction_details
    ADD CONSTRAINT transaction_details_pkey PRIMARY KEY (detail_id);


--
-- Name: transactions transactions_pkey; Type: CONSTRAINT; Schema: raw; Owner: beauty_user
--

ALTER TABLE ONLY raw.transactions
    ADD CONSTRAINT transactions_pkey PRIMARY KEY (transaction_id);


--
-- Name: app_company app_company_pkey; Type: CONSTRAINT; Schema: sys; Owner: beauty_user
--

ALTER TABLE ONLY sys.app_company
    ADD CONSTRAINT app_company_pkey PRIMARY KEY (app_company_id);


--
-- Name: app_user app_user_email_key; Type: CONSTRAINT; Schema: sys; Owner: beauty_user
--

ALTER TABLE ONLY sys.app_user
    ADD CONSTRAINT app_user_email_key UNIQUE (email);


--
-- Name: app_user app_user_pkey; Type: CONSTRAINT; Schema: sys; Owner: beauty_user
--

ALTER TABLE ONLY sys.app_user
    ADD CONSTRAINT app_user_pkey PRIMARY KEY (app_user_id);


--
-- Name: job_logs job_logs_pkey; Type: CONSTRAINT; Schema: sys; Owner: beauty_user
--

ALTER TABLE ONLY sys.job_logs
    ADD CONSTRAINT job_logs_pkey PRIMARY KEY (job_id);


--
-- Name: merge_candidates merge_candidates_app_company_id_source_customer_id_target_c_key; Type: CONSTRAINT; Schema: sys; Owner: beauty_user
--

ALTER TABLE ONLY sys.merge_candidates
    ADD CONSTRAINT merge_candidates_app_company_id_source_customer_id_target_c_key UNIQUE (app_company_id, source_customer_id, target_customer_id);


--
-- Name: merge_candidates merge_candidates_pkey; Type: CONSTRAINT; Schema: sys; Owner: beauty_user
--

ALTER TABLE ONLY sys.merge_candidates
    ADD CONSTRAINT merge_candidates_pkey PRIMARY KEY (candidate_id);


--
-- Name: idx_sales_customer; Type: INDEX; Schema: dwh; Owner: beauty_user
--

CREATE INDEX idx_sales_customer ON dwh.fact_sales USING btree (app_company_id, customer_id);


--
-- Name: idx_sales_date; Type: INDEX; Schema: dwh; Owner: beauty_user
--

CREATE INDEX idx_sales_date ON dwh.fact_sales USING btree (transaction_date);


--
-- Name: idx_sales_staff; Type: INDEX; Schema: dwh; Owner: beauty_user
--

CREATE INDEX idx_sales_staff ON dwh.fact_sales USING btree (app_company_id, staff_id);


--
-- Name: idx_raw_details_head_id; Type: INDEX; Schema: raw; Owner: beauty_user
--

CREATE INDEX idx_raw_details_head_id ON raw.transaction_details USING btree (transaction_head_id);


--
-- Name: fact_sales_details fact_sales_details_app_company_id_transaction_head_id_fkey; Type: FK CONSTRAINT; Schema: dwh; Owner: beauty_user
--

ALTER TABLE ONLY dwh.fact_sales_details
    ADD CONSTRAINT fact_sales_details_app_company_id_transaction_head_id_fkey FOREIGN KEY (app_company_id, transaction_head_id) REFERENCES dwh.fact_sales(app_company_id, transaction_head_id) ON DELETE CASCADE;


--
-- Name: app_user app_user_app_company_id_fkey; Type: FK CONSTRAINT; Schema: sys; Owner: beauty_user
--

ALTER TABLE ONLY sys.app_user
    ADD CONSTRAINT app_user_app_company_id_fkey FOREIGN KEY (app_company_id) REFERENCES sys.app_company(app_company_id);


--
-- PostgreSQL database dump complete
--

\unrestrict JikfNbiFu8PD596j6UgoC5bOmh5ARSNlLUaAdhun4JyTUtdFoUqzy9ZIla4B1sG

