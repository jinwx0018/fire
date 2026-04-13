-- 知识评论支持表情与插入图片（富文本），需扩大 content 字段；已建库执行本脚本一次即可
USE fire_recommendation;

ALTER TABLE knowledge_content_comment
  MODIFY COLUMN content TEXT NOT NULL COMMENT '评论正文（富文本：Unicode 表情与受控 img 等）';
