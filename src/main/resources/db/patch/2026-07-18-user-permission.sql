-- 用户直接权限、最终权限菜单和强制下线升级。
-- 依赖：2026-07-16-security-hardening.sql、2026-07-17-frontend-integration.sql。
BEGIN;

CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS auth_version INT NOT NULL DEFAULT 0;
ALTER TABLE sys_user DROP CONSTRAINT IF EXISTS ck_sys_user_auth_version;
ALTER TABLE sys_user ADD CONSTRAINT ck_sys_user_auth_version CHECK (auth_version >= 0);

ALTER TABLE sys_menu ADD COLUMN IF NOT EXISTS required_permission_id BIGINT;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sys_menu_required_permission') THEN
        ALTER TABLE sys_menu ADD CONSTRAINT fk_sys_menu_required_permission
            FOREIGN KEY (required_permission_id) REFERENCES sys_permission(id) NOT VALID;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS sys_user_permission (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES sys_user(id),
    permission_id BIGINT NOT NULL REFERENCES sys_permission(id),
    effect SMALLINT NOT NULL CHECK (effect IN (1, 2)),
    valid_from TIMESTAMP,
    expires_at TIMESTAMP,
    reason VARCHAR(256) NOT NULL,
    grant_by BIGINT NOT NULL REFERENCES sys_user(id),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    is_delete SMALLINT NOT NULL DEFAULT 0 CHECK (is_delete IN (0, 1)),
    version INT NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT ck_sys_user_permission_time
        CHECK (expires_at IS NULL OR valid_from IS NULL OR expires_at > valid_from)
);

ALTER TABLE sys_user_permission DROP CONSTRAINT IF EXISTS ex_sys_user_permission_time;
ALTER TABLE sys_user_permission ADD CONSTRAINT ex_sys_user_permission_time
    EXCLUDE USING gist (
        user_id WITH =,
        permission_id WITH =,
        tsrange(COALESCE(valid_from, '-infinity'::timestamp),
                COALESCE(expires_at, 'infinity'::timestamp), '[)') WITH &&
    ) WHERE (is_delete = 0);

CREATE INDEX IF NOT EXISTS idx_sys_user_permission_user
    ON sys_user_permission(user_id) WHERE is_delete = 0;
CREATE INDEX IF NOT EXISTS idx_sys_user_permission_boundary
    ON sys_user_permission(user_id, valid_from, expires_at) WHERE is_delete = 0;

CREATE TABLE IF NOT EXISTS sys_user_permission_audit (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    grant_id BIGINT,
    user_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    operation VARCHAR(16) NOT NULL,
    before_data JSONB,
    after_data JSONB,
    reason VARCHAR(256) NOT NULL,
    operator_id BIGINT NOT NULL,
    operator_username VARCHAR(32) NOT NULL,
    request_id VARCHAR(64),
    ip VARCHAR(45),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sys_user_permission_audit_user
    ON sys_user_permission_audit(user_id, create_time DESC);

UPDATE sys_permission SET type = 1 WHERE code = 'organize:log:list' AND is_delete = 0;

INSERT INTO sys_permission (name, code, type, create_by, update_by)
SELECT v.name, v.code, 2, 0, 0
FROM (VALUES
    ('查询用户直接权限', 'organize:user-permission:list'),
    ('新增用户直接权限', 'organize:user-permission:add'),
    ('编辑用户直接权限', 'organize:user-permission:edit'),
    ('撤销用户直接权限', 'organize:user-permission:del'),
    ('查询权限审计', 'organize:user-permission:audit'),
    ('强制用户下线', 'organize:user:force-logout')
) AS v(name, code)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.code = v.code AND p.is_delete = 0);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.name = 'ROLE_ADMIN' AND r.is_delete = 0 AND p.is_delete = 0
ON CONFLICT DO NOTHING;

UPDATE sys_menu m
SET required_permission_id = p.id, update_time = CURRENT_TIMESTAMP
FROM sys_permission p
WHERE p.code = CASE m.name
    WHEN '用户管理' THEN 'organize:user:list'
    WHEN '角色管理' THEN 'organize:role:list'
    WHEN '部门管理' THEN 'organize:dept:list'
    WHEN '权限管理' THEN 'organize:permission:list'
    WHEN '菜单管理' THEN 'organize:menu:list'
    WHEN '操作日志' THEN 'organize:log:list'
END
AND m.name IN ('用户管理', '角色管理', '部门管理', '权限管理', '菜单管理', '操作日志')
AND m.is_delete = 0 AND p.is_delete = 0;

COMMIT;
