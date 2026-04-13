-- 站内通知增强：支持点击直达目标页面（如对应评论）
USE fire_recommendation;

-- 列已存在时会报错，可忽略对应语句
ALTER TABLE user_notification
  ADD COLUMN action_url VARCHAR(512) DEFAULT NULL COMMENT '点击跳转地址（前端路由）' AFTER content;

ALTER TABLE user_notification
  ADD COLUMN action_text VARCHAR(64) DEFAULT NULL COMMENT '跳转文案（如去查看）' AFTER action_url;
