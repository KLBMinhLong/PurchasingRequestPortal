-- Bootstrap identity schema and seed IAM data.
-- Safe to re-run: uses IF NOT EXISTS / ON CONFLICT patterns.

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;

CREATE SCHEMA IF NOT EXISTS identity;

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
        password_hash LIKE '$2a$%' OR
        password_hash LIKE '$2b$%' OR
        password_hash LIKE '$2y$%' OR
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

INSERT INTO identity.roles (code, name, description, is_system)
VALUES
    ('ADMIN', 'Administrator', 'Full system administration privileges', TRUE),
    ('USER', 'User', 'Basic authenticated portal access', TRUE),
    ('REQUESTOR', 'Requestor', 'Create and manage purchasing requests', TRUE),
    ('APPROVER', 'Approver', 'Review and approve purchasing requests', TRUE)
ON CONFLICT (code) DO UPDATE
SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    is_system = EXCLUDED.is_system,
    updated_at = NOW();

INSERT INTO identity.permissions (code, name, resource, action, description)
VALUES
    ('USER_READ', 'Read users', 'user', 'read', 'View user profiles and list users'),
    ('USER_CREATE', 'Create users', 'user', 'create', 'Create new user accounts'),
    ('USER_UPDATE', 'Update users', 'user', 'update', 'Update user information and statuses'),
    ('USER_DELETE', 'Delete users', 'user', 'delete', 'Soft delete user accounts'),

    ('ROLE_READ', 'Read roles', 'role', 'read', 'View role definitions'),
    ('ROLE_ASSIGN', 'Assign roles', 'role', 'assign', 'Assign and revoke role mappings'),

    ('PERMISSION_READ', 'Read permissions', 'permission', 'read', 'View permission catalog'),
    ('PERMISSION_ASSIGN', 'Assign permissions', 'permission', 'assign', 'Map permissions to roles'),

    ('REQUEST_CREATE', 'Create request', 'purchasing_request', 'create', 'Create purchasing request drafts'),
    ('REQUEST_READ', 'Read request', 'purchasing_request', 'read', 'View purchasing requests'),
    ('REQUEST_UPDATE', 'Update request', 'purchasing_request', 'update', 'Update draft requests'),
    ('REQUEST_DELETE', 'Delete request', 'purchasing_request', 'delete', 'Cancel or delete requests'),
    ('REQUEST_SUBMIT', 'Submit request', 'purchasing_request', 'submit', 'Submit request into workflow'),

    ('APPROVAL_READ', 'Read approval queue', 'approval', 'read', 'View pending approvals'),
    ('APPROVAL_APPROVE', 'Approve request', 'approval', 'approve', 'Approve workflow tasks'),
    ('APPROVAL_REJECT', 'Reject request', 'approval', 'reject', 'Reject workflow tasks'),

    ('CATEGORY_READ', 'Read categories', 'category', 'read', 'View category master data'),
    ('CATEGORY_MANAGE', 'Manage categories', 'category', 'manage', 'Create/update category master data'),

    ('PRODUCT_READ', 'Read products', 'product', 'read', 'View product master data'),
    ('PRODUCT_MANAGE', 'Manage products', 'product', 'manage', 'Create/update product master data'),

    ('AUDIT_READ', 'Read audit logs', 'audit', 'read', 'View security and business audit logs'),
    ('SYSTEM_CONFIG', 'System configuration', 'system', 'manage', 'Manage system-level configuration')
ON CONFLICT (code) DO UPDATE
SET
    name = EXCLUDED.name,
    resource = EXCLUDED.resource,
    action = EXCLUDED.action,
    description = EXCLUDED.description,
    updated_at = NOW();

-- Map role -> permission matrix.
WITH role_permission_pairs AS (
    SELECT * FROM (VALUES
        ('ADMIN', 'USER_READ'), ('ADMIN', 'USER_CREATE'), ('ADMIN', 'USER_UPDATE'), ('ADMIN', 'USER_DELETE'),
        ('ADMIN', 'ROLE_READ'), ('ADMIN', 'ROLE_ASSIGN'),
        ('ADMIN', 'PERMISSION_READ'), ('ADMIN', 'PERMISSION_ASSIGN'),
        ('ADMIN', 'REQUEST_CREATE'), ('ADMIN', 'REQUEST_READ'), ('ADMIN', 'REQUEST_UPDATE'), ('ADMIN', 'REQUEST_DELETE'), ('ADMIN', 'REQUEST_SUBMIT'),
        ('ADMIN', 'APPROVAL_READ'), ('ADMIN', 'APPROVAL_APPROVE'), ('ADMIN', 'APPROVAL_REJECT'),
        ('ADMIN', 'CATEGORY_READ'), ('ADMIN', 'CATEGORY_MANAGE'), ('ADMIN', 'PRODUCT_READ'), ('ADMIN', 'PRODUCT_MANAGE'),
        ('ADMIN', 'AUDIT_READ'), ('ADMIN', 'SYSTEM_CONFIG'),

        ('USER', 'REQUEST_READ'), ('USER', 'CATEGORY_READ'), ('USER', 'PRODUCT_READ'),

        ('REQUESTOR', 'REQUEST_CREATE'), ('REQUESTOR', 'REQUEST_READ'), ('REQUESTOR', 'REQUEST_UPDATE'), ('REQUESTOR', 'REQUEST_DELETE'), ('REQUESTOR', 'REQUEST_SUBMIT'),
        ('REQUESTOR', 'CATEGORY_READ'), ('REQUESTOR', 'PRODUCT_READ'),

        ('APPROVER', 'REQUEST_READ'), ('APPROVER', 'APPROVAL_READ'), ('APPROVER', 'APPROVAL_APPROVE'), ('APPROVER', 'APPROVAL_REJECT'),
        ('APPROVER', 'CATEGORY_READ'), ('APPROVER', 'PRODUCT_READ')
    ) AS t(role_code, permission_code)
), resolved_pairs AS (
    SELECT r.id AS role_id, p.id AS permission_id
    FROM role_permission_pairs rp
    JOIN identity.roles r ON r.code = rp.role_code
    JOIN identity.permissions p ON p.code = rp.permission_code
)
INSERT INTO identity.role_permissions (role_id, permission_id)
SELECT role_id, permission_id
FROM resolved_pairs
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Seed default admin user.
INSERT INTO identity.users (
    username,
    password_hash,
    email,
    first_name,
    last_name,
    phone,
    is_active,
    is_locked,
    failed_login_attempts,
    attributes,
    created_by,
    updated_by
)
VALUES (
    'admin',
    crypt('Admin@123456', gen_salt('bf', 10)),
    'admin@prp.local',
    'System',
    'Administrator',
    '0900000000',
    TRUE,
    FALSE,
    0,
    '{"displayName": "System Administrator", "seeded": true}'::jsonb,
    'system-seed',
    'system-seed'
)
ON CONFLICT (username) DO UPDATE
SET
    password_hash = crypt('Admin@123456', gen_salt('bf', 10)),
    email = EXCLUDED.email,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    phone = EXCLUDED.phone,
    is_active = TRUE,
    is_locked = FALSE,
    failed_login_attempts = 0,
    deleted_at = NULL,
    attributes = EXCLUDED.attributes,
    updated_by = 'system-seed',
    updated_at = NOW();

-- Ensure admin role assignment.
INSERT INTO identity.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM identity.users u
JOIN identity.roles r ON r.code = 'ADMIN'
WHERE u.username = 'admin'
ON CONFLICT (user_id, role_id) DO NOTHING;
