-- ============================================================
-- 知识内容审核驳回原因字段
-- 依赖：02_knowledge.sql（knowledge_content 表已存在）
-- 作用：为 knowledge_content 增加 reject_reason，用于保存管理员驳回时的原因，作者在「我的内容」中可见
-- ============================================================

USE fire_recommendation;

-- 仅当列不存在时添加（MySQL 5.7 兼容，可重复执行）
SET @dbname = DATABASE();
SET @tablename = 'knowledge_content';
SET @columnname = 'reject_reason';
SET @prepared = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname);

SET @sql = IF(@prepared = 0,
  'ALTER TABLE knowledge_content ADD COLUMN reject_reason VARCHAR(512) DEFAULT NULL COMMENT ''最近一次审核驳回原因，仅作者可见''',
  'SELECT 1');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
