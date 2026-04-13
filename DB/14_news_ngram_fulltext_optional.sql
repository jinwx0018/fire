-- ============================================================
-- 可选：新闻 title+summary 全文索引，供 searchMode=ngram + yml 开关使用
--
-- 错误 1128「Function 'ngram' is not defined」
--   → 多为 MariaDB，或不支持 ngram 的 MySQL；请勿使用「方案甲」。
--
-- 方案乙（默认，推荐）：普通 FULLTEXT，无解析器
--   Oracle MySQL / MariaDB 均可执行；MATCH(title,summary) 可用，中文分词弱于 ngram。
--
-- 方案甲（仅 Oracle MySQL 5.7.6+ InnoDB，且 SHOW ENGINES 为 InnoDB）：
--   需要更好中文短语命中时再改用；执行前请先 DROP 方案乙的索引以免重复。
-- ============================================================

USE fire_recommendation;

-- 若重复执行报 Duplicate key name，请先：
-- ALTER TABLE news DROP INDEX ft_news_title_summary;
-- 或（若曾建过 ngram 索引）ALTER TABLE news DROP INDEX ft_news_ngram;

-- ---------- 方案乙：通用（出现 1128 时请只保留本句，不要执行方案甲）----------
ALTER TABLE news ADD FULLTEXT INDEX ft_news_title_summary (title, summary);

-- ---------- 方案甲：仅 Oracle MySQL 5.7.6+（不要用 MariaDB 执行）----------
-- 若已执行方案乙，需先：ALTER TABLE news DROP INDEX ft_news_title_summary;
-- ALTER TABLE news ADD FULLTEXT INDEX ft_news_ngram (title, summary) WITH PARSER ngram;
