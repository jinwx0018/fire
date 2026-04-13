-- ============================================================
-- 新闻表：封面图、分类（模块六扩展）
-- 在已有库上执行：ALTER 幂等（重复执行需手动忽略「重复列」错误）
-- ============================================================

USE fire_recommendation;

-- 封面图 URL：http(s) 或本站以 / 开头的路径（如 /uploads/xxx）
ALTER TABLE news ADD COLUMN cover_url VARCHAR(512) DEFAULT NULL COMMENT '封面图URL' AFTER summary;

-- 分类标签（自由文本，如「政策法规」「演练通报」）
ALTER TABLE news ADD COLUMN category VARCHAR(64) DEFAULT NULL COMMENT '新闻分类' AFTER region;

ALTER TABLE news ADD KEY idx_news_category (category);
