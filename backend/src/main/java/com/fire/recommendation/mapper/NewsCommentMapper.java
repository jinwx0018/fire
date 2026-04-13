package com.fire.recommendation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fire.recommendation.entity.NewsComment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NewsCommentMapper extends BaseMapper<NewsComment> {

    @Delete("DELETE FROM news_comment WHERE news_id = #{newsId}")
    int physicalDeleteByNewsId(@Param("newsId") Long newsId);
}
