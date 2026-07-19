-- 新增所有已登录用户可见的欢迎页菜单。
BEGIN;

INSERT INTO sys_menu (parent_id, name, path, component, icon, sort_order, create_by, update_by)
VALUES (0, '欢迎', '/welcome', 'Welcome', 'HomeOutlined', 0, 0, 0)
ON CONFLICT DO NOTHING;

-- 旧库中的系统管理图标使用过前端无法识别的 system 标识，统一为组件名。
UPDATE sys_menu
SET icon = 'SettingOutlined',
    sort_order = 10,
    update_time = CURRENT_TIMESTAMP
WHERE parent_id = 0
  AND path = '/system'
  AND is_delete = 0
  AND (icon IS DISTINCT FROM 'SettingOutlined' OR sort_order IS DISTINCT FROM 10);

COMMIT;
