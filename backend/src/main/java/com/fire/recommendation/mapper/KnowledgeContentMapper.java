package com.fire.recommendation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fire.recommendation.entity.KnowledgeContent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface KnowledgeContentMapper extends BaseMapper<KnowledgeContent> {

    /** 按分类统计发布量：category_id, cnt */
    @Select("SELECT category_id AS categoryId, COUNT(*) AS cnt FROM knowledge_content WHERE deleted = 0 GROUP BY category_id")
    List<Map<String, Object>> selectCategoryPublishCount();

    /** 按年按月汇总浏览量：month(YYYY-MM), viewCount */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m') AS month, COALESCE(SUM(view_count), 0) AS viewCount " +
            "FROM knowledge_content WHERE deleted = 0 AND create_time >= CONCAT(#{year}, '-01-01') AND create_time < DATE_ADD(CONCAT(#{year}, '-01-01'), INTERVAL 1 YEAR) " +
            "GROUP BY month ORDER BY month")
    List<Map<String, Object>> selectViewTrendByYear(@Param("year") String year);
}
