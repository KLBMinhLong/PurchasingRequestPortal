# 03 - DATABASE SCHEMA (PostgreSQL)

Tai lieu nay dinh nghia schema du lieu cho Purchasing Request Portal voi 2 schema:

- identity: user, role, permission cho xac thuc va phan quyen.
- business: du lieu nghiep vu mua sam, workflow Camunda, audit.

Yeu cau dac thu duoc dap ung:

- User/Role/Permission khong nam trong DB mac dinh cua Keycloak.
- Keycloak se doc du lieu tu database portal thong qua Custom User Storage SPI.
- Mat khau luu dang hash (BCrypt/Argon2) trong truong password_hash.

## DDL Script

```sql
-- ============================================================
-- Purchasing Request Portal - PostgreSQL DDL
-- Database: portal
-- Schemas : identity, business
-- ============================================================

-- 1) Extensions
CREATE EXTENSION IF NOT EXISTS pgcrypto;  -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS citext;    -- case-insensitive text for username/email

-- 2) Schemas
CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS business;

-- ============================================================
-- IDENTITY SCHEMA
-- ============================================================

-- Users table for Keycloak User Storage SPI
CREATE TABLE IF NOT EXISTS identity.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username CITEXT NOT NULL,
    password_hash TEXT NOT NULL,
    email CITEXT NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(30),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts SMALLINT NOT NULL DEFAULT 0,
    last_login_at TIMESTAMPTZ,
    attributes JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_password_hash_format CHECK (
        password_hash LIKE '$2a$%' OR  -- BCrypt
        password_hash LIKE '$2b$%' OR  -- BCrypt
        password_hash LIKE '$2y$%' OR  -- BCrypt
        password_hash LIKE '$argon2%'
    )
);

CREATE TABLE IF NOT EXISTS identity.roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_roles_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS identity.permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_permissions_code UNIQUE (code),
    CONSTRAINT uq_permissions_resource_action UNIQUE (resource, action)
);

CREATE TABLE IF NOT EXISTS identity.user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    assigned_by UUID,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id)
        REFERENCES identity.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id)
        REFERENCES identity.roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_assigned_by FOREIGN KEY (assigned_by)
        REFERENCES identity.users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS identity.role_permissions (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    granted_by UUID,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id)
        REFERENCES identity.roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id)
        REFERENCES identity.permissions(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_granted_by FOREIGN KEY (granted_by)
        REFERENCES identity.users(id) ON DELETE SET NULL
);

-- Identity indexes for frequent lookups
CREATE INDEX IF NOT EXISTS idx_users_is_active ON identity.users (is_active);
CREATE INDEX IF NOT EXISTS idx_users_last_login_at ON identity.users (last_login_at);
CREATE INDEX IF NOT EXISTS idx_users_attributes_gin ON identity.users USING GIN (attributes);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON identity.user_roles (role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission_id ON identity.role_permissions (permission_id);

-- ============================================================
-- BUSINESS SCHEMA
-- ============================================================

CREATE TABLE IF NOT EXISTS business.categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    category_type VARCHAR(20) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_categories_code UNIQUE (code),
    CONSTRAINT ck_categories_type CHECK (category_type IN ('CAPEX', 'OPEX', 'SERVICE'))
);

CREATE TABLE IF NOT EXISTS business.products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL,
    sku VARCHAR(80) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    unit VARCHAR(30) NOT NULL DEFAULT 'item',
    default_unit_price NUMERIC(18,2) NOT NULL DEFAULT 0,
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    metadata JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_products_sku UNIQUE (sku),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id)
        REFERENCES business.categories(id) ON DELETE RESTRICT,
    CONSTRAINT ck_products_default_unit_price_non_negative CHECK (default_unit_price >= 0)
);

CREATE TABLE IF NOT EXISTS business.purchasing_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_no VARCHAR(40) NOT NULL,
    requester_user_id UUID NOT NULL,
    department_code VARCHAR(50),
    title VARCHAR(255) NOT NULL,
    reason TEXT,
    request_date DATE NOT NULL DEFAULT CURRENT_DATE,
    needed_by_date DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    camunda_process_instance_id VARCHAR(64),
    camunda_business_key VARCHAR(64),
    current_approver_role_code VARCHAR(50),
    total_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    metadata JSONB NOT NULL DEFAULT '{}'::JSONB,
    submitted_at TIMESTAMPTZ,
    approved_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uq_purchasing_requests_no UNIQUE (request_no),
    CONSTRAINT fk_purchasing_requests_requester FOREIGN KEY (requester_user_id)
        REFERENCES identity.users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_purchasing_requests_created_by FOREIGN KEY (created_by)
        REFERENCES identity.users(id) ON DELETE SET NULL,
    CONSTRAINT fk_purchasing_requests_updated_by FOREIGN KEY (updated_by)
        REFERENCES identity.users(id) ON DELETE SET NULL,
    CONSTRAINT ck_purchasing_requests_status CHECK (
        status IN ('DRAFT', 'SUBMITTED', 'IN_APPROVAL', 'APPROVED', 'REJECTED', 'CANCELLED', 'COMPLETED')
    ),
    CONSTRAINT ck_purchasing_requests_total_amount_non_negative CHECK (total_amount >= 0)
);

CREATE TABLE IF NOT EXISTS business.request_details (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL,
    line_no INTEGER NOT NULL,
    product_id UUID,
    item_name VARCHAR(255) NOT NULL,
    item_spec JSONB NOT NULL DEFAULT '{}'::JSONB,
    quantity NUMERIC(18,3) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    unit_price NUMERIC(18,2) NOT NULL,
    line_amount NUMERIC(18,2) GENERATED ALWAYS AS (quantity * unit_price) STORED,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_request_details_request FOREIGN KEY (request_id)
        REFERENCES business.purchasing_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_request_details_product FOREIGN KEY (product_id)
        REFERENCES business.products(id) ON DELETE SET NULL,
    CONSTRAINT uq_request_details_line UNIQUE (request_id, line_no),
    CONSTRAINT ck_request_details_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_request_details_unit_price_non_negative CHECK (unit_price >= 0)
);

CREATE TABLE IF NOT EXISTS business.audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID,
    action VARCHAR(50) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_id UUID,
    request_id UUID,
    trace_id VARCHAR(64),
    ip_address INET,
    user_agent TEXT,
    before_data JSONB,
    after_data JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_user_id)
        REFERENCES identity.users(id) ON DELETE SET NULL,
    CONSTRAINT fk_audit_logs_request FOREIGN KEY (request_id)
        REFERENCES business.purchasing_requests(id) ON DELETE SET NULL
);

-- Business indexes for search/reporting/workflow
CREATE INDEX IF NOT EXISTS idx_categories_type_active ON business.categories (category_type, is_active);
CREATE INDEX IF NOT EXISTS idx_products_category_active ON business.products (category_id, is_active);
CREATE INDEX IF NOT EXISTS idx_products_name ON business.products (name);
CREATE INDEX IF NOT EXISTS idx_products_metadata_gin ON business.products USING GIN (metadata);

CREATE INDEX IF NOT EXISTS idx_requests_requester_created_at ON business.purchasing_requests (requester_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_requests_status_updated_at ON business.purchasing_requests (status, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_requests_camunda_process_id ON business.purchasing_requests (camunda_process_instance_id);
CREATE INDEX IF NOT EXISTS idx_requests_department_code ON business.purchasing_requests (department_code);
CREATE INDEX IF NOT EXISTS idx_requests_metadata_gin ON business.purchasing_requests USING GIN (metadata);

CREATE INDEX IF NOT EXISTS idx_request_details_request_id ON business.request_details (request_id);
CREATE INDEX IF NOT EXISTS idx_request_details_product_id ON business.request_details (product_id);
CREATE INDEX IF NOT EXISTS idx_request_details_item_spec_gin ON business.request_details USING GIN (item_spec);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_created_at ON business.audit_logs (actor_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON business.audit_logs (entity_name, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_request_id ON business.audit_logs (request_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_trace_id ON business.audit_logs (trace_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_before_data_gin ON business.audit_logs USING GIN (before_data);
CREATE INDEX IF NOT EXISTS idx_audit_logs_after_data_gin ON business.audit_logs USING GIN (after_data);

-- Optional utility function to auto-update updated_at
CREATE OR REPLACE FUNCTION business.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trg_identity_users_updated_at'
    ) THEN
        CREATE TRIGGER trg_identity_users_updated_at
        BEFORE UPDATE ON identity.users
        FOR EACH ROW EXECUTE FUNCTION business.set_updated_at();
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trg_identity_roles_updated_at'
    ) THEN
        CREATE TRIGGER trg_identity_roles_updated_at
        BEFORE UPDATE ON identity.roles
        FOR EACH ROW EXECUTE FUNCTION business.set_updated_at();
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trg_identity_permissions_updated_at'
    ) THEN
        CREATE TRIGGER trg_identity_permissions_updated_at
        BEFORE UPDATE ON identity.permissions
        FOR EACH ROW EXECUTE FUNCTION business.set_updated_at();
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trg_business_categories_updated_at'
    ) THEN
        CREATE TRIGGER trg_business_categories_updated_at
        BEFORE UPDATE ON business.categories
        FOR EACH ROW EXECUTE FUNCTION business.set_updated_at();
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trg_business_products_updated_at'
    ) THEN
        CREATE TRIGGER trg_business_products_updated_at
        BEFORE UPDATE ON business.products
        FOR EACH ROW EXECUTE FUNCTION business.set_updated_at();
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trg_business_purchasing_requests_updated_at'
    ) THEN
        CREATE TRIGGER trg_business_purchasing_requests_updated_at
        BEFORE UPDATE ON business.purchasing_requests
        FOR EACH ROW EXECUTE FUNCTION business.set_updated_at();
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trg_business_request_details_updated_at'
    ) THEN
        CREATE TRIGGER trg_business_request_details_updated_at
        BEFORE UPDATE ON business.request_details
        FOR EACH ROW EXECUTE FUNCTION business.set_updated_at();
    END IF;
END $$;
```

## Interaction Model: Spring Boot va Keycloak

- Keycloak (Custom User Storage SPI) se ket noi vao database portal, schema identity.
- SPI se doc identity.users de tim username/email, doc password_hash de verify BCrypt/Argon2, va doc identity.user_roles + identity.roles + identity.role_permissions de map role/permission vao token claims.
- Spring Security JWT o Backend se validate token Keycloak, sau do dung role/claim de authorize API.
- Spring Boot dung JPA/Hibernate cho CRUD thong thuong tren business.categories, business.products, business.purchasing_requests, business.request_details, business.audit_logs.
- Spring Boot dung MyBatis cho cac truy van bao cao phuc tap (tong hop chi tieu, thong ke theo phong ban, pivot theo danh muc).
- Camunda process instance id va business key duoc luu trong business.purchasing_requests de dong bo trang thai workflow va truy vet nghiep vu.
- Audit log tu JPA callback/aspect ghi vao business.audit_logs (before_data/after_data JSONB) de dam bao kha nang tra soat.

## Ghi chu trien khai

- Chay script tren DB portal ngay sau khi khoi tao PostgreSQL.
- Nen cap quyen schema theo principle of least privilege:
- app_user cho Spring Boot (CRUD business + read identity can thiet).
- keycloak_spi_user cho Keycloak SPI (read identity la chinh).
- Khong bao gio luu plaintext password; chi luu hash hop le BCrypt/Argon2.
