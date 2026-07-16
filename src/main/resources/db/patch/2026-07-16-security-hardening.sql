-- 已执行过旧版 init.sql 的数据库需要执行本补丁。
-- 执行前请先备份数据库，并确认不存在重复的未删除业务键。

BEGIN;

-- 权限编码统一为 {module}:{resource}:{action}。
UPDATE sys_permission SET code = 'organize:user:list' WHERE code = 'organize:user';
UPDATE sys_permission SET code = 'organize:role:list' WHERE code = 'organize:role';
UPDATE sys_permission SET code = 'organize:dept:list' WHERE code = 'organize:dept';
UPDATE sys_permission SET code = 'organize:menu:list' WHERE code = 'organize:menu';
UPDATE sys_permission SET code = 'organize:permission:list' WHERE code = 'organize:permission';

INSERT INTO sys_permission (name, code, type, create_by, update_by)
SELECT '日志查询', 'organize:log:list', 2, 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE code = 'organize:log:list' AND is_delete = 0
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.name = 'ROLE_ADMIN' AND r.is_delete = 0
  AND p.code = 'organize:log:list' AND p.is_delete = 0
ON CONFLICT DO NOTHING;

-- 全局唯一约束与逻辑删除冲突，改为仅约束未删除数据。
ALTER TABLE sys_user DROP CONSTRAINT IF EXISTS sys_user_username_key;
ALTER TABLE sys_permission DROP CONSTRAINT IF EXISTS sys_permission_code_key;

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_username_active
    ON sys_user(username) WHERE is_delete = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_role_name_active
    ON sys_role(name) WHERE is_delete = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_permission_code_active
    ON sys_permission(code) WHERE is_delete = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_dept_name_active
    ON sys_dept(name) WHERE is_delete = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_menu_name_active
    ON sys_menu(name) WHERE is_delete = 0;

-- NOT VALID 会立即约束新写入，同时允许先清理历史孤儿数据后再手动 VALIDATE。
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sys_user_dept') THEN
        ALTER TABLE sys_user ADD CONSTRAINT fk_sys_user_dept
            FOREIGN KEY (dept_id) REFERENCES sys_dept(id) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sys_user_role_user') THEN
        ALTER TABLE sys_user_role ADD CONSTRAINT fk_sys_user_role_user
            FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sys_user_role_role') THEN
        ALTER TABLE sys_user_role ADD CONSTRAINT fk_sys_user_role_role
            FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sys_role_permission_role') THEN
        ALTER TABLE sys_role_permission ADD CONSTRAINT fk_sys_role_permission_role
            FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sys_role_permission_permission') THEN
        ALTER TABLE sys_role_permission ADD CONSTRAINT fk_sys_role_permission_permission
            FOREIGN KEY (permission_id) REFERENCES sys_permission(id) ON DELETE CASCADE NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sys_role_menu_role') THEN
        ALTER TABLE sys_role_menu ADD CONSTRAINT fk_sys_role_menu_role
            FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sys_role_menu_menu') THEN
        ALTER TABLE sys_role_menu ADD CONSTRAINT fk_sys_role_menu_menu
            FOREIGN KEY (menu_id) REFERENCES sys_menu(id) ON DELETE CASCADE NOT VALID;
    END IF;
END $$;

COMMIT;
