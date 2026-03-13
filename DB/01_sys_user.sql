-- ============================================================
-- 模块一：用户管理 - 用户表、角色、密码重置 Token 等（兼容 MySQL 5.6）
-- 依赖：00_init_database.sql（需先 USE fire_recommendation）
-- ============================================================

USE fire_recommendation;

-- 角色表（可选：若角色固定可只用枚举，不建表）
CREATE TABLE IF NOT EXISTS sys_role (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
  role_code   VARCHAR(32)  NOT NULL COMMENT '角色编码：USER/AUTHOR/ADMIN',
  role_name   VARCHAR(64)  NOT NULL COMMENT '角色名称',
  create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
  id          BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  username    VARCHAR(64)  NOT NULL COMMENT '用户名',
  password    VARCHAR(128) NOT NULL COMMENT '密码（BCrypt 等加密）',
  phone       VARCHAR(20)  NOT NULL COMMENT '手机号',
  email       VARCHAR(128)          DEFAULT NULL COMMENT '邮箱',
  avatar      VARCHAR(512)          DEFAULT NULL COMMENT '头像URL',
  role_id     BIGINT       NOT NULL DEFAULT 1 COMMENT '角色ID，关联 sys_role.id',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0 禁用 1 正常',
  create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_username (username),
  UNIQUE KEY uk_phone (phone),
  UNIQUE KEY uk_email (email),
  KEY idx_role_id (role_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 密码重置 Token（邮箱链接找回）
CREATE TABLE IF NOT EXISTS sys_password_reset_token (
  id          BIGINT    PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT    NOT NULL COMMENT '用户ID',
  token       VARCHAR(128) NOT NULL COMMENT '重置 Token',
  expire_time DATETIME  NOT NULL COMMENT '过期时间',
  used        TINYINT   NOT NULL DEFAULT 0 COMMENT '0 未使用 1 已使用',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_token (token),
  KEY idx_user_id (user_id),
  KEY idx_expire (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='密码重置Token表';

-- 初始化角色数据
INSERT INTO sys_role (id, role_code, role_name) VALUES
(1, 'USER', '普通用户'),
(2, 'AUTHOR', '作者'),
(3, 'ADMIN', '管理员')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);
