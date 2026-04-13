package com.fire.recommendation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fire.recommendation.entity.ForumPost;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ForumPostMapper extends BaseMapper<ForumPost> {

    /** 仅已通过审核帖子（status=1），与前台可见口径一致 */
    @Select("SELECT COALESCE(SUM(like_count), 0) FROM forum_post WHERE deleted = 0 AND status = 1")
    Long sumLikeCountTotal();

    @Select("<script>SELECT COALESCE(SUM(like_count), 0) FROM forum_post WHERE deleted = 0 AND status = 1 " +
            "<if test='start != null'>AND create_time &gt;= #{start}</if>" +
            "<if test='end != null'>AND create_time &lt; #{end}</if></script>")
    Long sumLikeCountInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Update("UPDATE forum_post SET deleted = 0 WHERE id = #{id} AND deleted = 1")
    int restoreByIdFromRecycle(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM forum_post WHERE id = #{id} AND deleted = 1")
    int countInRecycle(@Param("id") Long id);

    @Delete("DELETE FROM forum_post WHERE id = #{id} AND deleted = 1")
    int physicalDeleteFromRecycle(@Param("id") Long id);

    @Select("SELECT id FROM forum_post WHERE deleted = 1")
    List<Long> selectRecycleIds();
}
