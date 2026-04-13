-- ============================================================
-- 论坛评论：显示/隐藏（与 news_comment.status 语义一致）
-- 执行后：用户端仅展示 status=1；管理端可隐藏违规内容而不删帖计数
-- 若提示 Duplicate column 'status'，说明已存在，可忽略
-- ============================================================

USE fire_recommendation;

ALTER TABLE forum_comment
  ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '1 显示 0 隐藏' AFTER content;

CREATE INDEX idx_fc_status ON forum_comment (status);
