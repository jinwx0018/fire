package com.fire.recommendation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fire.recommendation.common.PageResult;

import java.util.List;
import java.util.Map;

public interface ContentService {

    IPage<Map<String, Object>> list(Integer pageNum, Integer pageSize, Long categoryId, String title, Integer status);

    Map<String, Object> getDetail(Long id, Long userId);

    Long save(Long authorId, Map<String, Object> body);

    void update(Long id, Long operatorId, Map<String, Object> body);

    void delete(Long id, Long operatorId);

    void collect(Long userId, Long contentId);

    void uncollect(Long userId, Long contentId);

    PageResult<Map<String, Object>> collectList(Long userId, Integer pageNum, Integer pageSize);

    Map<String, Object> shareUrl(Long contentId);

    List<Map<String, Object>> categoryList();

    Long adminSaveCategory(String name, Integer sort);

    void adminUpdateCategory(Long id, String name, Integer sort);

    void adminDeleteCategory(Long id);
}
