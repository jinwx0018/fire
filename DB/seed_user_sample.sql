-- ============================================================
-- 用户模块测试数据（管理员、作者、普通用户）
-- 依赖：01_sys_user.sql 已执行（角色 1/2/3 已存在）
-- 执行：mysql -u root -p fire_recommendation < seed_user_sample.sql
-- 说明：
--   admin / author1 / user1～user3 密码均为 password
--   jk 用户密码为 123456（若已存在 jk 会更新密码）
-- ============================================================

USE fire_recommendation;

-- BCrypt 哈希：password（与 Spring Security BCryptPasswordEncoder 兼容）
SET @pwd_hash = '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/B.G';
-- BCrypt 哈希：123456（用于 jk 等）
SET @pwd_123456 = '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.';

-- 插入测试用户
INSERT INTO sys_user (id, username, password, phone, email, avatar, role_id, status, create_time, update_time) VALUES
(1, 'admin',   @pwd_hash,   '13800000001', 'admin@example.com',   NULL, 3, 1, NOW(), NOW()),
(2, 'author1', @pwd_hash,   '13800000002', 'author1@example.com', NULL, 2, 1, NOW(), NOW()),
(3, 'user1',   @pwd_hash,   '13800000003', 'user1@example.com',   NULL, 1, 1, NOW(), NOW()),
(4, 'user2',   @pwd_hash,   '13800000004', 'user2@example.com',   NULL, 1, 1, NOW(), NOW()),
(5, 'user3',   @pwd_hash,   '13800000005', 'user3@example.com',   NULL, 1, 1, NOW(), NOW()),
(6, 'jk',      @pwd_123456, '13800000006', 'jk@example.com',     NULL, 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  phone = VALUES(phone),
  email = VALUES(email),
  role_id = VALUES(role_id),
  status = VALUES(status);

-- 若库中已有 jk 但 id 不是 6，上面 INSERT 可能因 uk_username 冲突而变成 UPDATE，密码会更新为 123456。
-- 若 jk 已存在且上面未更新到，可单独执行：UPDATE sys_user SET password = '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.' WHERE username = 'jk';

-- 可选：密码重置 Token 测试数据（1 条未使用、1 条已使用）；重复执行时相同 token 会忽略
INSERT IGNORE INTO sys_password_reset_token (user_id, token, expire_time, used, create_time) VALUES
(1, 'test-token-unused-001', DATE_ADD(NOW(), INTERVAL 1 HOUR), 0, NOW()),
(2, 'test-token-used-002',   DATE_ADD(NOW(), INTERVAL -1 HOUR), 1, NOW());
