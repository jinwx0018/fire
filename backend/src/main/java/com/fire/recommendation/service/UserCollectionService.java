package com.fire.recommendation.service;

import com.fire.recommendation.common.PageResult;

import java.util.Map;

public interface UserCollectionService {

    boolean isCollected(Long userId, int targetType, Long targetId);

    void collectKnowledge(Long userId, Long contentId);

    void uncollectKnowledge(Long userId, Long contentId);

    void collectForumPost(Long userId, Long postId);

    void uncollectForumPost(Long userId, Long postId);

    void collectNews(Long userId, Long newsId);

    void uncollectNews(Long userId, Long newsId);

    void collectEquipment(Long userId, Long equipmentId);

    void uncollectEquipment(Long userId, Long equipmentId);

    /**
     * module：knowledge | forum | news | equipment
     */
    PageResult<Map<String, Object>> myCollectPage(Long userId, String module, Integer pageNum, Integer pageSize);

    /** 知识物理删除/回收站清理时移除该知识下的收藏（仅 target_type=知识） */
    void deleteAllForKnowledgeContent(Long knowledgeContentId);
}
