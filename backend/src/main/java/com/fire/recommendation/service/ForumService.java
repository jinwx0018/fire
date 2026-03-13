package com.fire.recommendation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.Map;

public interface ForumService {

    IPage<Map<String, Object>> postList(Integer pageNum, Integer pageSize, Integer status, String keyword);

    Map<String, Object> postDetail(Long postId, Long userId);

    Long publishPost(Long userId, String title, String content);

    void updatePost(Long postId, Long userId, String title, String content);

    void deletePost(Long postId, Long userId);

    Map<String, Object> likePost(Long postId, Long userId);

    IPage<Map<String, Object>> commentList(Long postId, Integer pageNum, Integer pageSize);

    Long addComment(Long userId, Long postId, String content, Long parentId);

    void deleteComment(Long commentId, Long userId);

    void auditPost(Long postId, Integer status, String rejectReason);
}
