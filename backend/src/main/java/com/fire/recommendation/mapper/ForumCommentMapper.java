package com.fire.recommendation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fire.recommendation.entity.ForumComment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface ForumCommentMapper extends BaseMapper<ForumComment> {

    /** 仅统计所属帖子为已通过且未删的评论 */
    @Select("SELECT COUNT(*) FROM forum_comment c INNER JOIN forum_post p ON c.post_id = p.id "
            + "WHERE c.deleted = 0 AND p.deleted = 0 AND p.status = 1")
    Long countOnApprovedPostsTotal();

    @Select("<script>SELECT COUNT(*) FROM forum_comment c INNER JOIN forum_post p ON c.post_id = p.id "
            + "WHERE c.deleted = 0 AND p.deleted = 0 AND p.status = 1 "
            + "<if test='start != null'>AND c.create_time &gt;= #{start}</if>"
            + "<if test='end != null'>AND c.create_time &lt; #{end}</if></script>")
    Long countOnApprovedPostsInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Delete("DELETE FROM forum_comment WHERE post_id = #{postId}")
    int physicalDeleteByPostId(@Param("postId") Long postId);
}
