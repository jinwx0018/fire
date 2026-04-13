package com.fire.recommendation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fire.recommendation.entity.UserBehavior;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserBehaviorMapper extends BaseMapper<UserBehavior> {

    /**
     * 基于物品的协同过滤：查询与 contentId 共同被同一批用户行为过的其他内容及共现得分。
     * 用于 Item-CF 推荐。
     */
    List<Map<String, Object>> findSimilarContentIds(@Param("contentId") Long contentId, @Param("limit") int limit);

    @Select("SELECT COUNT(DISTINCT user_id) FROM user_behavior WHERE create_time >= #{since}")
    Long countDistinctUsersSince(@Param("since") LocalDateTime since);

    @Select("<script>SELECT COUNT(DISTINCT user_id) FROM user_behavior WHERE 1=1 " +
            "<if test='start != null'>AND create_time &gt;= #{start}</if>" +
            "<if test='end != null'>AND create_time &lt; #{end}</if></script>")
    Long countDistinctUsersInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 行为日志 VIEW 条数，按行为发生月份分组（区间内） */
    @Select("<script>SELECT DATE_FORMAT(create_time, '%Y-%m') AS month, COUNT(*) AS cnt FROM user_behavior " +
            "WHERE behavior_type = 'VIEW' " +
            "<if test='start != null'>AND create_time &gt;= #{start}</if>" +
            "<if test='end != null'>AND create_time &lt; #{end}</if>" +
            "GROUP BY month ORDER BY month</script>")
    List<Map<String, Object>> selectViewBehaviorCountByMonth(@Param("start") LocalDateTime start,
                                                             @Param("end") LocalDateTime end);

    /**
     * 兴趣分析：用户对 CONTENT 行为所涉知识分类出现次数 Top1（已发布、未删）。
     */
    @Select("SELECT k.category_id AS categoryId FROM user_behavior ub "
            + "INNER JOIN knowledge_content k ON ub.target_id = k.id AND ub.target_type = 'CONTENT' "
            + "WHERE ub.user_id = #{userId} AND k.deleted = 0 AND k.status = 1 AND k.category_id IS NOT NULL "
            + "GROUP BY k.category_id ORDER BY COUNT(*) DESC LIMIT 1")
    Long selectTop1ContentCategoryIdByUser(@Param("userId") Long userId);

    /** 兴趣分析：用户对知识内容互动涉及最多的分类 Top3（已发布、未删），用于智能推荐多分类加权 */
    @Select("SELECT k.category_id FROM user_behavior ub "
            + "INNER JOIN knowledge_content k ON ub.target_id = k.id AND ub.target_type = 'CONTENT' "
            + "WHERE ub.user_id = #{userId} AND k.deleted = 0 AND k.status = 1 AND k.category_id IS NOT NULL "
            + "GROUP BY k.category_id ORDER BY COUNT(*) DESC LIMIT 3")
    List<Long> selectTop3ContentCategoryIdsByUser(@Param("userId") Long userId);

    /**
     * 兴趣画像：行为类型加权（VIEW/LIKE/COLLECT/COMMENT）× 时间衰减，分类 Top3。
     */
    List<Long> selectWeightedTop3CategoryIds(@Param("userId") Long userId);

    /**
     * 近期浏览过的知识标题+摘要（用于字二元组语义相似加分）。
     */
    List<Map<String, Object>> selectRecentViewedContentTexts(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 用户在论坛互动过的帖子标题（跨域反哺知识推荐）。
     */
    List<String> selectForumCrossDomainTitles(@Param("userId") Long userId, @Param("limit") int limit);
}
