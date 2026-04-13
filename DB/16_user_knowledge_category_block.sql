-- 用户屏蔽的知识分类：智能推荐 /recommend/list 筛选阶段排除（与全站 recommend.exclude-category-ids 合并）
-- 执行：mysql -u root -p fire_recommendation < DB/16_user_knowledge_category_block.sql

USE fire_recommendation;

CREATE TABLE IF NOT EXISTS user_knowledge_category_block (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  user_id       BIGINT NOT NULL COMMENT '用户 ID',
  category_id   BIGINT NOT NULL COMMENT '知识分类 ID',
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_user_category (user_id, category_id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户屏蔽的知识分类（推荐筛选）';
  