package com.fire.recommendation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fire.recommendation.entity.UserBehavior;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserBehaviorMapper extends BaseMapper<UserBehavior> {

    /**
     * 基于物品的协同过滤：查询与 contentId 共同被同一批用户行为过的其他内容及共现得分。
     * 用于 Item-CF 推荐。
     */
    List<Map<String, Object>> findSimilarContentIds(@Param("contentId") Long contentId, @Param("limit") int limit);
}
