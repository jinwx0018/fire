package com.fire.recommendation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fire.recommendation.entity.NewsCategory;

import java.util.List;
import java.util.Map;

public interface NewsCategoryDictService {

    /** 用户端/管理端下拉：未删除，按 sort_order */
    List<Map<String, Object>> listOptions();

    List<NewsCategory> adminListAll();

    /** 管理端分类分页，按 sortOrder、id 升序 */
    IPage<NewsCategory> adminPage(Integer pageNum, Integer pageSize);

    Long adminCreate(Long operatorId, String name, Integer sortOrder);

    void adminUpdate(Long operatorId, Long id, String name, Integer sortOrder);

    /** 仍有新闻引用时禁止删除 */
    void adminDelete(Long operatorId, Long id);
}
