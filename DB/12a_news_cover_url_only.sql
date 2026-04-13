-- ============================================================
-- 仅补全 news.cover_url（旧库缺列时执行）
-- 若提示 Duplicate column 'cover_url'，说明已有该列，可忽略
-- 补列后：将 backend 中 News.coverUrl 上的 @TableField(exist = false) 去掉，封面才可写入数据库
-- ============================================================

USE fire_recommendation;

ALTER TABLE news ADD COLUMN cover_url VARCHAR(512) DEFAULT NULL COMMENT '封面图URL' AFTER summary;
