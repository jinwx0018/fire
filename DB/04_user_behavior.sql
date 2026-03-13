-- ============================================================
-- 模块四：个性化推荐 - 用户行为表（兼容 MySQL 5.6）
-- 依赖：01_sys_user.sql，02_knowledge.sql，03_forum.sql
-- ============================================================

USE fire_recommendation;

-- 用户行为表（用于兴趣分析与推荐计算）
CREATE TABLE IF NOT EXISTS user_behavior (
  id            BIGINT   PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT   NOT NULL COMMENT '用户ID',
  behavior_type VARCHAR(32) NOT NULL COMMENT 'VIEW/LIKE/COMMENT/COLLECT',
  target_type   VARCHAR(32) NOT NULL COMMENT 'CONTENT/POST',
  target_id     BIGINT   NOT NULL COMMENT '知识ID或帖子ID',
  extra         VARCHAR(256) DEFAULT NULL COMMENT '扩展：如分类ID、标签，便于统计',
  create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user_id (user_id),
  KEY idx_behavior_type (behavior_type),
  KEY idx_target (target_type, target_id),
  KEY idx_create_time (create_time),
  KEY idx_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户行为表';
