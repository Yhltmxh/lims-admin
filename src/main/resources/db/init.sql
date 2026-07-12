-- ============================================
-- LIMS 基础设施数据库初始化脚本
-- IvorySQL (PostgreSQL compatible)
-- ============================================

-- 部门表
CREATE TABLE IF NOT EXISTS sys_dept (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    parent_id   BIGINT        DEFAULT 0,
    name        VARCHAR(64)   NOT NULL,
    sort_order  INT           DEFAULT 0,
    leader      VARCHAR(32),
    phone       VARCHAR(20),
    status      SMALLINT      DEFAULT 1,
    create_time TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    is_delete   SMALLINT      DEFAULT 0,
    version     INT           DEFAULT 0
);

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username    VARCHAR(32)   NOT NULL UNIQUE,
    password    VARCHAR(128)  NOT NULL,
    real_name   VARCHAR(32),
    phone       VARCHAR(20),
    email       VARCHAR(64),
    gender      SMALLINT      DEFAULT 0,
    avatar      VARCHAR(256),
    dept_id     BIGINT,
    status      SMALLINT      DEFAULT 1,
    create_time TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    is_delete   SMALLINT      DEFAULT 0,
    version     INT           DEFAULT 0
);

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(32)   NOT NULL,
    label       VARCHAR(32)   NOT NULL,
    description VARCHAR(128),
    status      SMALLINT      DEFAULT 1,
    create_time TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    is_delete   SMALLINT      DEFAULT 0,
    version     INT           DEFAULT 0
);

-- 权限表
CREATE TABLE IF NOT EXISTS sys_permission (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(64)   NOT NULL,
    code        VARCHAR(64)   NOT NULL UNIQUE,
    type        SMALLINT      NOT NULL,
    parent_id   BIGINT        DEFAULT 0,
    sort_order  INT           DEFAULT 0,
    status      SMALLINT      DEFAULT 1,
    create_time TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    is_delete   SMALLINT      DEFAULT 0,
    version     INT           DEFAULT 0
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
    hidden      SMALLINT      DEFAULT 0,
    status      SMALLINT      DEFAULT 1,
    create_time TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    is_delete   SMALLINT      DEFAULT 0,
    version     INT           DEFAULT 0
);

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    role_id     BIGINT NOT NULL,
    UNIQUE (user_id, role_id)
);

-- 角色-权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_id         BIGINT NOT NULL,
    permission_id   BIGINT NOT NULL,
    UNIQUE (role_id, permission_id)
);

-- 角色-菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_id     BIGINT NOT NULL,
    menu_id     BIGINT NOT NULL,
    UNIQUE (role_id, menu_id)
);

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
    create_time TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
    is_delete   SMALLINT      DEFAULT 0
);

-- ============================================
-- 索引
-- ============================================
CREATE INDEX IF NOT EXISTS idx_sys_user_username ON sys_user(username) WHERE is_delete = 0;
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
('综合部', 3, '赵主任');

-- 角色
INSERT INTO sys_role (name, label, description) VALUES
('ROLE_ADMIN',   '超级管理员', '拥有系统全部权限'),
('ROLE_MANAGER', '部门经理',   '管理本部门数据和人员'),
('ROLE_SAMPLER', '采样员',     '执行采样任务'),
('ROLE_ANALYST', '检测员',     '执行检测分析');

-- 用户（密码均为 BCrypt 加密的 "123456"）
INSERT INTO sys_user (username, password, real_name, dept_id, create_by, update_by) VALUES
('admin',    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '系统管理员', 1, 0, 0),
('liming',   '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '李明',       2, 0, 0),
('wangfang', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '王芳',       3, 0, 0),
('zhaoqiang','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '赵强',       4, 0, 0);

-- 权限
INSERT INTO sys_permission (name, code, type, create_by, update_by) VALUES
('用户管理', 'organize:user',      1, 0, 0),
('新增用户', 'organize:user:add',  2, 0, 0),
('编辑用户', 'organize:user:edit', 2, 0, 0),
('删除用户', 'organize:user:del',  2, 0, 0),
('角色管理', 'organize:role',      1, 0, 0),
('部门管理', 'organize:dept',      1, 0, 0),
('菜单管理', 'organize:menu',      1, 0, 0),
('权限管理', 'organize:permission', 1, 0, 0);

-- 菜单
INSERT INTO sys_menu (name, path, component, icon, create_by, update_by) VALUES
('系统管理', '/system',     'Layout',          'system',  0, 0),
('用户管理', '/system/user','system/user/index','user',   0, 0),
('角色管理', '/system/role','system/role/index','role',   0, 0);

-- admin 用户拥有 ROLE_ADMIN 角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- ROLE_ADMIN 拥有所有权限
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8);

-- ROLE_ADMIN 拥有所有菜单
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 2), (1, 3);
