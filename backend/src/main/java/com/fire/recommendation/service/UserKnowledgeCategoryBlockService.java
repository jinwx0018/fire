package com.fire.recommendation.service;

import java.util.List;

/**
 * 用户屏蔽的知识分类：仅影响智能推荐 {@code /recommend/list} 的筛选阶段。
 */
public interface UserKnowledgeCategoryBlockService {

    List<Long> listBlockedCategoryIds(Long userId);

    /** 全量替换当前用户屏蔽列表；categoryIds 去重、校验分类存在，最多 30 个 */
    void replaceBlockedCategories(Long userId, List<Long> categoryIds);
}
