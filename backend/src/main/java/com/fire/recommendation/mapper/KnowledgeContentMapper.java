package com.fire.recommendation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fire.recommendation.entity.KnowledgeContent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface KnowledgeContentMapper extends BaseMapper<KnowledgeContent> {

    /** 按分类统计已发布知识（status=1）发布量：category_id, cnt */
    @Select("SELECT category_id AS categoryId, COUNT(*) AS cnt FROM knowledge_content WHERE deleted = 0 AND status = 1 GROUP BY category_id")
    List<Map<String, Object>> selectCategoryPublishCount();

    /**
     * 同上，限定 create_time ∈ [start, end)（任一为 null 则该侧不限制；通常成对传入）
     */
    @Select("<script>SELECT category_id AS categoryId, COUNT(*) AS cnt FROM knowledge_content WHERE deleted = 0 AND status = 1 " +
            "<if test='start != null'>AND create_time &gt;= #{start}</if>" +
            "<if test='end != null'>AND create_time &lt; #{end}</if>" +
            "GROUP BY category_id</script>")
    List<Map<String, Object>> selectCategoryPublishCountRange(@Param("start") java.time.LocalDateTime start,
                                                               @Param("end") java.time.LocalDateTime end);

    /**
     * 按年按月汇总：已发布知识按创建月份分组，对该月创建条目的 view_count 求和（非行为日志口径）。
     */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m') AS month, COALESCE(SUM(view_count), 0) AS viewCount " +
            "FROM knowledge_content WHERE deleted = 0 AND status = 1 AND create_time >= CONCAT(#{year}, '-01-01') AND create_time < DATE_ADD(CONCAT(#{year}, '-01-01'), INTERVAL 1 YEAR) " +
            "GROUP BY month ORDER BY month")
    List<Map<String, Object>> selectViewTrendByYear(@Param("year") String year);

    /**
     * 区间内按月：已发布知识按创建月分组，对该月创建条目的 view_count 求和（与按年接口口径一致，仅时间条件不同）。
     */
    @Select("<script>SELECT DATE_FORMAT(create_time, '%Y-%m') AS month, COALESCE(SUM(view_count), 0) AS viewCount " +
            "FROM knowledge_content WHERE deleted = 0 AND status = 1 " +
            "<if test='start != null'>AND create_time &gt;= #{start}</if>" +
            "<if test='end != null'>AND create_time &lt; #{end}</if>" +
            "GROUP BY month ORDER BY month</script>")
    List<Map<String, Object>> selectViewTrendInRange(@Param("start") java.time.LocalDateTime start,
                                                     @Param("end") java.time.LocalDateTime end);

    @Update("UPDATE knowledge_content SET deleted = 0 WHERE id = #{id}")
    int restoreById(@Param("id") Long id);

    @Select("SELECT id FROM knowledge_content WHERE deleted = 1")
    List<Long> selectRecycleIds();

    @Select("SELECT COUNT(*) FROM knowledge_content WHERE id = #{id} AND deleted = 1")
    int countInRecycle(@Param("id") Long id);

    /** 物理删除：仅允许删除已在回收站（deleted=1）的记录 */
    @Delete("DELETE FROM knowledge_content WHERE id = #{id} AND deleted = 1")
    int physicalDeleteById(@Param("id") Long id);

    /** 清理内容与标签关联行（知识仅按分类组织时仍用于回收站物理删除等） */
    @Delete("DELETE FROM knowledge_content_tag WHERE content_id = #{contentId}")
    int deleteContentTagsByContentId(@Param("contentId") Long contentId);
}
