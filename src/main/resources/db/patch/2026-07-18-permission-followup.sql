-- 用户权限首轮上线后的约束收尾。
BEGIN;

ALTER TABLE sys_menu VALIDATE CONSTRAINT fk_sys_menu_required_permission;

COMMIT;
