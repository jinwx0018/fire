package com.fire.recommendation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fire.recommendation.entity.NewsLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NewsLikeMapper extends BaseMapper<NewsLike> {

    @Delete("DELETE FROM news_like WHERE news_id = #{newsId}")
    int physicalDeleteByNewsId(@Param("newsId") Long newsId);
}
