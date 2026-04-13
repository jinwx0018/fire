package com.fire.recommendation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fire.recommendation.entity.News;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface NewsMapper extends BaseMapper<News> {

    /** 与用户端列表一致：已上架、未删除、已到发布时间、地区非空 */
    @Select("SELECT DISTINCT region FROM news WHERE deleted = 0 AND status = 1 "
            + "AND (publish_time IS NULL OR publish_time <= NOW()) "
            + "AND region IS NOT NULL AND TRIM(region) <> '' ORDER BY region")
    List<String> selectDistinctRegionsForUser();

    /** 管理端：未删除新闻中出现过的地区（含下架、定时未上线稿） */
    @Select("SELECT DISTINCT region FROM news WHERE deleted = 0 "
            + "AND region IS NOT NULL AND TRIM(region) <> '' ORDER BY region")
    List<String> selectDistinctRegionsForAdmin();

    @Update("UPDATE news SET deleted = 0 WHERE id = #{id} AND deleted = 1")
    int restoreByIdFromRecycle(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM news WHERE id = #{id} AND deleted = 1")
    int countInRecycle(@Param("id") Long id);

    @Delete("DELETE FROM news WHERE id = #{id} AND deleted = 1")
    int physicalDeleteFromRecycle(@Param("id") Long id);

    @Select("SELECT id FROM news WHERE deleted = 1")
    List<Long> selectRecycleIds();
}
