# 消防科普推荐系统 - 数据库脚本说明

## 适用版本

- **MySQL 5.6.x**（建议 5.6.5 及以上；含 FULLTEXT 的表需 **5.6.4** 及以上，以支持 InnoDB 全文索引）

## 数据库

- 数据库名：`fire_recommendation`
- 字符集：`utf8mb4`
- 排序规则：`utf8mb4_unicode_ci`

## 执行顺序

在 MySQL 5.6 中按以下顺序执行（若已创建库且 `USE fire_recommendation`，可只执行 01～06）：

| 序号 | 文件 | 说明 |
|------|------|------|
| 0 | `00_init_database.sql` | 创建数据库并 USE |
| 1 | `01_sys_user.sql` | 用户、角色、密码重置 Token（邮箱找回） |
| 2 | `02_knowledge.sql` | 知识分类、内容、标签、用户收藏 |
| 3 | `03_forum.sql` | 论坛帖子、评论 |
| 4 | `04_user_behavior.sql` | 用户行为（推荐用） |
| 5 | `05_equipment.sql` | 器材类型、器材 |
| 6 | `06_news.sql` | 新闻 |

## 表与模块对应关系

| 模块 | 主要表 |
|------|--------|
| 用户管理 | sys_role, sys_user, sys_password_reset_token |
| 知识内容 | knowledge_category, knowledge_content, knowledge_tag, knowledge_content_tag, user_collection |
| 论坛 | forum_post, forum_comment（点赞用 Redis Set） |
| 推荐 | user_behavior |
| 器材 | equipment_type, equipment |
| 新闻 | news |

## MySQL 5.6 兼容说明

- 使用 **TIMESTAMP** 作为 `create_time` / `update_time`，以兼容 5.6.0 的 `DEFAULT CURRENT_TIMESTAMP` 与 `ON UPDATE CURRENT_TIMESTAMP`。
- 不含默认时间的字段（如 `expire_time`、`publish_time`）仍使用 **DATETIME**。
- 含 **FULLTEXT** 索引的表（knowledge_content、forum_post、equipment、news）需 **MySQL 5.6.4+**（InnoDB 全文索引）。

## 一键执行（命令行示例）

```bash
mysql -u root -p < 00_init_database.sql
mysql -u root -p fire_recommendation < 01_sys_user.sql
mysql -u root -p fire_recommendation < 02_knowledge.sql
mysql -u root -p fire_recommendation < 03_forum.sql
mysql -u root -p fire_recommendation < 04_user_behavior.sql
mysql -u root -p fire_recommendation < 05_equipment.sql
mysql -u root -p fire_recommendation < 06_news.sql
```

或在 MySQL 5.6 客户端中依次执行各文件内容即可。
