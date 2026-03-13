-- ============================================================
-- 消防科普推荐系统 - 数据库初始化（兼容 MySQL 5.6）
-- 执行顺序：先执行本文件创建数据库，再按 01~06 顺序执行建表脚本
-- ============================================================

CREATE DATABASE IF NOT EXISTS fire_recommendation
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE fire_recommendation;
