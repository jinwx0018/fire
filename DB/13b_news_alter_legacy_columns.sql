-- ============================================================
-- 旧版 news 表升级：补充 category_id、like_count（列已存在则报错，可忽略）
-- ============================================================

USE fire_recommendation;

ALTER TABLE news ADD COLUMN category_id BIGINT DEFAULT NULL COMMENT '分类字典 id' AFTER region;
ALTER TABLE news ADD COLUMN like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数冗余' AFTER view_count;
ALTER TABLE news ADD KEY idx_news_category_id (category_id);
