-- ============================================================
-- 将指定用户的密码重置为 123456（BCrypt）
-- 用于：种子脚本曾把密码覆盖成 password，现改回 123456 以便测试登录
-- 执行：mysql -u root -p fire_recommendation < fix_password_to_123456.sql
-- ============================================================

USE fire_recommendation;

-- BCrypt 哈希，明文密码：123456（与 Spring Security BCryptPasswordEncoder 兼容）
SET @pwd_123456 = '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.';

UPDATE sys_user SET password = @pwd_123456
WHERE username IN ('admin', 'user1', 'testuser', 'jk');

-- 若 6k测试员 也要用 123456 登录，取消下面注释：
-- UPDATE sys_user SET password = @pwd_123456 WHERE username = '6k测试员';
