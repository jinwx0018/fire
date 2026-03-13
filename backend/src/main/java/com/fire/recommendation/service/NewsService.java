package com.fire.recommendation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.Map;

public interface NewsService {

    IPage<Map<String, Object>> list(Integer pageNum, Integer pageSize, String region, String title, String orderBy, String order);

    Map<String, Object> getDetail(Long id);

    Long adminSave(Long publisherId, Map<String, Object> body);

    void adminUpdate(Long id, Map<String, Object> body);

    void adminDelete(Long id);
}
