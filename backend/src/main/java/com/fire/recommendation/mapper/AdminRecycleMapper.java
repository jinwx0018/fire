package com.fire.recommendation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 管理端统一回收站：知识、新闻、论坛帖子（逻辑删除 deleted=1）合并分页。
 */
@Mapper
public interface AdminRecycleMapper {

    @Select("""
            SELECT t.id, t.title, t.categoryName, t.viewCount, t.updateTime, t.moduleType FROM (
              SELECT k.id AS id, k.title AS title, c.name AS categoryName,
                     COALESCE(k.view_count, 0) AS viewCount, k.update_time AS updateTime, 'KNOWLEDGE' AS moduleType
              FROM knowledge_content k
              LEFT JOIN knowledge_category c ON c.id = k.category_id
              WHERE k.deleted = 1
              UNION ALL
              SELECT n.id, n.title, nc.name, COALESCE(n.view_count, 0), n.update_time, 'NEWS'
              FROM news n
              LEFT JOIN news_category nc ON nc.id = n.category_id AND nc.deleted = 0
              WHERE n.deleted = 1
              UNION ALL
              SELECT p.id, p.title, NULL, COALESCE(p.view_count, 0), p.update_time, 'FORUM'
              FROM forum_post p
              WHERE p.deleted = 1
            ) t
            ORDER BY t.updateTime DESC
            LIMIT #{offset}, #{limit}
            """)
    List<Map<String, Object>> selectUnifiedRecyclePage(@Param("offset") long offset, @Param("limit") long limit);

    @Select("""
            SELECT
              (SELECT COUNT(*) FROM knowledge_content WHERE deleted = 1)
            + (SELECT COUNT(*) FROM news WHERE deleted = 1)
            + (SELECT COUNT(*) FROM forum_post WHERE deleted = 1)
            """)
    long countUnifiedRecycle();
}
