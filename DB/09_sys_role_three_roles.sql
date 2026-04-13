-- ============================================================
-- 强制对齐三角色：普通用户(1)、作者(2)、管理员(3)
-- 依赖：01_sys_user.sql（sys_role 表已存在）
-- 说明：
--   设计约定：id=1 USER，id=2 AUTHOR，id=3 ADMIN（与 seed_user_sample.sql 一致）
--   若你库中曾只有两条角色或 id 与编码错乱，执行本脚本可统一三条记录。
--   执行后请检查管理员账号：SELECT username, role_id FROM sys_user WHERE username='admin';
--   管理员应为 role_id=3；若仍为 2，请执行：UPDATE sys_user SET role_id=3 WHERE username='admin';
-- ============================================================

USE fire_recommendation;

INSERT INTO sys_role (id, role_code, role_name) VALUES
(1, 'USER', '普通用户'),
(2, 'AUTHOR', '作者'),
(3, 'ADMIN', '管理员')
ON DUPLICATE KEY UPDATE
  role_code = VALUES(role_code),
  role_name = VALUES(role_name);
