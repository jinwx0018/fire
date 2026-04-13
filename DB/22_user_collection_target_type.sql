-- 用户收藏支持多模块：1 知识 2 论坛帖 3 新闻（content_id 存对应实体 ID）
-- 仅「旧版 user_collection 无 target_type」的存量库执行本脚本一次。
-- 若已用当前 DB/02_knowledge.sql 全新建表（已含 target_type），请勿执行，否则会报重复列/索引错误。

ALTER TABLE user_collection
  ADD COLUMN target_type TINYINT NOT NULL DEFAULT 1 COMMENT '1知识 2论坛帖 3新闻' AFTER user_id;

ALTER TABLE user_collection DROP INDEX uk_user_content;

ALTER TABLE user_collection ADD UNIQUE KEY uk_user_type_content (user_id, target_type, content_id);
