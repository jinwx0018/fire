-- 新闻评论富文本（表情、换行、图片）易超过 VARCHAR(2000)，截断会破坏 <img> 等标签导致发布后图片不显示。
-- 与 forum_comment / knowledge_content_comment 一致改为 TEXT。已建库执行一次即可。
USE fire_recommendation;

ALTER TABLE news_comment
  MODIFY COLUMN content TEXT NOT NULL COMMENT '评论正文（富文本：换行、Unicode 表情、受控 img 等）';
