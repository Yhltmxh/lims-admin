-- ============================================

CREATE EXTENSION IF NOT EXISTS btree_gist;
-- MCMIS 基础设施数据库初始化脚本
-- IvorySQL (PostgreSQL compatible)
-- ============================================
--
-- 执行方式：
--   1. 先手动建库（以 superuser 连接默认库执行）：
--      psql -h 127.0.0.1 -U ivorysql -d postgres -c "CREATE DATABASE lims"
--   2. 再执行本脚本建表：
--      psql -h 127.0.0.1 -U ivorysql -d lims -f init.sql

-- ============================================

-- 部门表
CREATE TABLE IF NOT EXISTS sys_dept (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    parent_id   BIGINT        DEFAULT 0,
    name        VARCHAR(64)   NOT NULL,
    sort_order  INT           DEFAULT 0,
    leader      VARCHAR(32),
    phone       VARCHAR(20),
    status      SMALLINT      NOT NULL DEFAULT 1 CHECK (status IN (0, 1)),
    create_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    is_delete   SMALLINT      NOT NULL DEFAULT 0 CHECK (is_delete IN (0, 1)),
    version     INT           NOT NULL DEFAULT 0 CHECK (version >= 0)
);

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username    VARCHAR(32)   NOT NULL,
    password    VARCHAR(128)  NOT NULL,
    real_name   VARCHAR(32),
    phone       VARCHAR(20),
    email       VARCHAR(64),
    gender      SMALLINT      NOT NULL DEFAULT 0 CHECK (gender IN (0, 1, 2)),
    avatar      VARCHAR(256),
    dept_id     BIGINT REFERENCES sys_dept(id),
    status      SMALLINT      NOT NULL DEFAULT 1 CHECK (status IN (0, 1)),
    auth_version INT          NOT NULL DEFAULT 0 CHECK (auth_version >= 0),
    create_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    is_delete   SMALLINT      NOT NULL DEFAULT 0 CHECK (is_delete IN (0, 1)),
    version     INT           NOT NULL DEFAULT 0 CHECK (version >= 0)
);

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(32)   NOT NULL,
    label       VARCHAR(32)   NOT NULL,
    description VARCHAR(128),
    status      SMALLINT      NOT NULL DEFAULT 1 CHECK (status IN (0, 1)),
    create_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    is_delete   SMALLINT      NOT NULL DEFAULT 0 CHECK (is_delete IN (0, 1)),
    version     INT           NOT NULL DEFAULT 0 CHECK (version >= 0)
);

-- 权限表
CREATE TABLE IF NOT EXISTS sys_permission (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(64)   NOT NULL,
    code        VARCHAR(64)   NOT NULL,
    type        SMALLINT      NOT NULL CHECK (type IN (1, 2)),
    parent_id   BIGINT        DEFAULT 0,
    sort_order  INT           DEFAULT 0,
    status      SMALLINT      NOT NULL DEFAULT 1 CHECK (status IN (0, 1)),
    create_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    is_delete   SMALLINT      NOT NULL DEFAULT 0 CHECK (is_delete IN (0, 1)),
    version     INT           NOT NULL DEFAULT 0 CHECK (version >= 0)
);

-- 菜单表
CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    parent_id   BIGINT        DEFAULT 0,
    name        VARCHAR(32)   NOT NULL,
    path        VARCHAR(128),
    component   VARCHAR(128),
    icon        VARCHAR(32),
    sort_order  INT           DEFAULT 0,
    hidden      SMALLINT      NOT NULL DEFAULT 0 CHECK (hidden IN (0, 1)),
    status      SMALLINT      NOT NULL DEFAULT 1 CHECK (status IN (0, 1)),
    required_permission_id BIGINT REFERENCES sys_permission(id),
    create_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    is_delete   SMALLINT      NOT NULL DEFAULT 0 CHECK (is_delete IN (0, 1)),
    version     INT           NOT NULL DEFAULT 0 CHECK (version >= 0)
);

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    role_id     BIGINT NOT NULL REFERENCES sys_role(id) ON DELETE CASCADE,
    UNIQUE (user_id, role_id)
);

-- 角色-权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_id         BIGINT NOT NULL REFERENCES sys_role(id) ON DELETE CASCADE,
    permission_id   BIGINT NOT NULL REFERENCES sys_permission(id) ON DELETE CASCADE,
    UNIQUE (role_id, permission_id)
);

-- 角色-菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_id     BIGINT NOT NULL REFERENCES sys_role(id) ON DELETE CASCADE,
    menu_id     BIGINT NOT NULL REFERENCES sys_menu(id) ON DELETE CASCADE,
    UNIQUE (role_id, menu_id)
);

-- 用户直接权限表（ALLOW=1，DENY=2；时间区间为左闭右开）
CREATE TABLE IF NOT EXISTS sys_user_permission (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES sys_user(id),
    permission_id   BIGINT       NOT NULL REFERENCES sys_permission(id),
    effect          SMALLINT     NOT NULL CHECK (effect IN (1, 2)),
    valid_from      TIMESTAMP,
    expires_at      TIMESTAMP,
    reason          VARCHAR(256) NOT NULL,
    grant_by        BIGINT       NOT NULL REFERENCES sys_user(id),
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_by       BIGINT,
    update_by       BIGINT,
    is_delete       SMALLINT     NOT NULL DEFAULT 0 CHECK (is_delete IN (0, 1)),
    version         INT          NOT NULL DEFAULT 0 CHECK (version >= 0),
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

-- 用户直接权限不可变审计表
CREATE TABLE IF NOT EXISTS sys_user_permission_audit (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    grant_id          BIGINT,
    user_id           BIGINT       NOT NULL,
    permission_id     BIGINT       NOT NULL,
    operation         VARCHAR(16)  NOT NULL,
    before_data       JSONB,
    after_data        JSONB,
    reason            VARCHAR(256) NOT NULL,
    operator_id       BIGINT       NOT NULL,
    operator_username VARCHAR(32)  NOT NULL,
    request_id        VARCHAR(64),
    ip                VARCHAR(45),
    create_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sys_user_permission_audit_user
    ON sys_user_permission_audit(user_id, create_time DESC);

-- 操作日志表
CREATE TABLE IF NOT EXISTS sys_log (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT,
    username    VARCHAR(32),
    module      VARCHAR(32),
    action      VARCHAR(64),
    method      VARCHAR(128),
    params      TEXT,
    result      TEXT,
    ip          VARCHAR(45),
    duration    INT,
    status      SMALLINT      DEFAULT 1,
    create_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    is_delete   SMALLINT      DEFAULT 0
);

-- ============================================
-- 索引
-- ============================================
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_username_active ON sys_user(username) WHERE is_delete = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_role_name_active ON sys_role(name) WHERE is_delete = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_permission_code_active ON sys_permission(code) WHERE is_delete = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_dept_name_active ON sys_dept(name) WHERE is_delete = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_menu_parent_name_active
    ON sys_menu(parent_id, name) WHERE is_delete = 0;
CREATE INDEX IF NOT EXISTS idx_sys_user_dept_id  ON sys_user(dept_id)  WHERE is_delete = 0;
CREATE INDEX IF NOT EXISTS idx_sys_user_status   ON sys_user(status)   WHERE is_delete = 0;
CREATE INDEX IF NOT EXISTS idx_sys_log_user_id   ON sys_log(user_id)   WHERE is_delete = 0;
CREATE INDEX IF NOT EXISTS idx_sys_log_create_time ON sys_log(create_time) WHERE is_delete = 0;

-- ============================================
-- 模拟数据
-- ============================================

-- 部门
INSERT INTO sys_dept (name, sort_order, leader) VALUES
('总公司', 0, '张总'),
('检测部', 1, '李经理'),
('质控部', 2, '王经理'),
('综合部', 3, '赵主任')
ON CONFLICT DO NOTHING;

-- 角色
INSERT INTO sys_role (name, label, description) VALUES
('ROLE_ADMIN',   '超级管理员', '拥有系统全部权限'),
('ROLE_MANAGER', '部门经理',   '管理本部门数据和人员'),
('ROLE_SAMPLER', '采样员',     '执行采样任务'),
('ROLE_ANALYST', '检测员',     '执行检测分析')
ON CONFLICT DO NOTHING;

-- 用户（密码均为 BCrypt 加密的 "123456"）
INSERT INTO sys_user (username, password, real_name, dept_id, create_by, update_by) VALUES
('admin',    '$2a$10$qo5hAj3rXMlzt3KWyLmHgOBKUVo1BxJaxsY6y/jHokOTmMJ1N8vmq', '系统管理员', 1, 0, 0),
('liming',   '$2a$10$qo5hAj3rXMlzt3KWyLmHgOBKUVo1BxJaxsY6y/jHokOTmMJ1N8vmq', '李明',       2, 0, 0),
('wangfang', '$2a$10$qo5hAj3rXMlzt3KWyLmHgOBKUVo1BxJaxsY6y/jHokOTmMJ1N8vmq', '王芳',       3, 0, 0),
('zhaoqiang','$2a$10$qo5hAj3rXMlzt3KWyLmHgOBKUVo1BxJaxsY6y/jHokOTmMJ1N8vmq', '赵强',       4, 0, 0)
ON CONFLICT DO NOTHING;

-- 权限
INSERT INTO sys_permission (name, code, type, create_by, update_by) VALUES
('用户管理', 'organize:user:list',       1, 0, 0),
('新增用户', 'organize:user:add',        2, 0, 0),
('编辑用户', 'organize:user:edit',       2, 0, 0),
('删除用户', 'organize:user:del',        2, 0, 0),
('角色管理', 'organize:role:list',       1, 0, 0),
('新增角色', 'organize:role:add',        2, 0, 0),
('编辑角色', 'organize:role:edit',       2, 0, 0),
('删除角色', 'organize:role:del',        2, 0, 0),
('部门管理', 'organize:dept:list',       1, 0, 0),
('新增部门', 'organize:dept:add',        2, 0, 0),
('编辑部门', 'organize:dept:edit',       2, 0, 0),
('删除部门', 'organize:dept:del',        2, 0, 0),
('菜单管理', 'organize:menu:list',       1, 0, 0),
('新增菜单', 'organize:menu:add',        2, 0, 0),
('编辑菜单', 'organize:menu:edit',       2, 0, 0),
('删除菜单', 'organize:menu:del',        2, 0, 0),
('权限管理', 'organize:permission:list', 1, 0, 0),
('新增权限', 'organize:permission:add',  2, 0, 0),
('编辑权限', 'organize:permission:edit', 2, 0, 0),
('删除权限', 'organize:permission:del',  2, 0, 0),
('日志查询', 'organize:log:list',        1, 0, 0),
('查询用户直接权限', 'organize:user-permission:list', 2, 0, 0),
('新增用户直接权限', 'organize:user-permission:add', 2, 0, 0),
('编辑用户直接权限', 'organize:user-permission:edit', 2, 0, 0),
('撤销用户直接权限', 'organize:user-permission:del', 2, 0, 0),
('查询权限审计', 'organize:user-permission:audit', 2, 0, 0),
('强制用户下线', 'organize:user:force-logout', 2, 0, 0)
ON CONFLICT DO NOTHING;

-- 菜单（运行时仅使用 path/name/icon，component 为前端静态路由注册键）
INSERT INTO sys_menu (parent_id, name, path, component, icon, sort_order, create_by, update_by)
VALUES (0, '系统管理', '/system', 'Layout', 'SettingOutlined', 10, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (parent_id, name, path, component, icon, sort_order, create_by, update_by)
SELECT p.id, v.name, v.path, v.component, v.icon, v.sort_order, 0, 0
FROM sys_menu p
CROSS JOIN (VALUES
    ('用户管理', '/system/users',       'system/users',       'UserOutlined',       10),
    ('角色管理', '/system/roles',       'system/roles',       'TeamOutlined',       20),
    ('部门管理', '/system/departments', 'system/departments', 'ApartmentOutlined',  30),
    ('权限管理', '/system/permissions', 'system/permissions', 'SafetyOutlined',     40),
    ('菜单管理', '/system/menus',       'system/menus',       'MenuOutlined',       50),
    ('操作日志', '/system/logs',        'system/logs',        'FileSearchOutlined', 60)
) AS v(name, path, component, icon, sort_order)
WHERE p.name = '系统管理' AND p.parent_id = 0 AND p.is_delete = 0
ON CONFLICT DO NOTHING;

UPDATE sys_menu m
SET required_permission_id = p.id
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

-- admin 用户拥有 ROLE_ADMIN 角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1)
ON CONFLICT DO NOTHING;

-- ROLE_ADMIN 拥有所有权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission
ON CONFLICT DO NOTHING;

-- ROLE_ADMIN 拥有所有菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.name = 'ROLE_ADMIN' AND r.is_delete = 0 AND m.is_delete = 0
ON CONFLICT DO NOTHING;
