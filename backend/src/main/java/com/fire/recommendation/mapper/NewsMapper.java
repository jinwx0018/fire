package com.fire.recommendation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fire.recommendation.entity.News;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NewsMapper extends BaseMapper<News> {
}
