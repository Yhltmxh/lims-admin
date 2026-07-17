-- 前端对接与菜单标准化补丁。
-- 执行前请备份数据库。

BEGIN;

INSERT INTO sys_menu (parent_id, name, path, component, icon, sort_order, create_by, update_by)
VALUES (0, '系统管理', '/system', 'Layout', 'SettingOutlined', 10, 0, 0)
ON CONFLICT DO NOTHING;

-- 将旧版平级菜单归入“系统管理”，同时统一路由和图标。
UPDATE sys_menu child
SET parent_id = parent.id,
    path = mapping.path,
    component = mapping.component,
    icon = mapping.icon,
    sort_order = mapping.sort_order,
    update_time = CURRENT_TIMESTAMP
FROM sys_menu parent
JOIN (VALUES
    ('用户管理', '/system/users', 'system/users', 'UserOutlined', 10),
    ('角色管理', '/system/roles', 'system/roles', 'TeamOutlined', 20)
) AS mapping(name, path, component, icon, sort_order) ON TRUE
WHERE parent.name = '系统管理' AND parent.parent_id = 0 AND parent.is_delete = 0
  AND child.name = mapping.name AND child.is_delete = 0;

DROP INDEX IF EXISTS uk_sys_menu_name_active;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_menu_parent_name_active
    ON sys_menu(parent_id, name) WHERE is_delete = 0;

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

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.name = 'ROLE_ADMIN' AND r.is_delete = 0 AND m.is_delete = 0
ON CONFLICT DO NOTHING;

COMMIT;
