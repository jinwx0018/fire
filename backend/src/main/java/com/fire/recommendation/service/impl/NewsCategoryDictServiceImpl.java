package com.fire.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fire.recommendation.entity.News;
import com.fire.recommendation.entity.NewsCategory;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.mapper.NewsCategoryMapper;
import com.fire.recommendation.mapper.NewsMapper;
import com.fire.recommendation.service.AuditLogService;
import com.fire.recommendation.service.NewsCategoryDictService;
import com.fire.recommendation.util.PlainTextSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsCategoryDictServiceImpl implements NewsCategoryDictService {

    private final NewsCategoryMapper newsCategoryMapper;
    private final NewsMapper newsMapper;
    private final AuditLogService auditLogService;

    @Override
    public List<Map<String, Object>> listOptions() {
        List<NewsCategory> list = newsCategoryMapper.selectList(
                new LambdaQueryWrapper<NewsCategory>()
                        .orderByAsc(NewsCategory::getSortOrder)
                        .orderByAsc(NewsCategory::getId));
        return list.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("sortOrder", c.getSortOrder());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public List<NewsCategory> adminListAll() {
        return newsCategoryMapper.selectList(
                new LambdaQueryWrapper<NewsCategory>()
                        .orderByAsc(NewsCategory::getSortOrder)
                        .orderByAsc(NewsCategory::getId));
    }

    @Override
    public IPage<NewsCategory> adminPage(Integer pageNum, Integer pageSize) {
        int pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int ps = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Page<NewsCategory> page = new Page<>(pn, ps);
        LambdaQueryWrapper<NewsCategory> q = new LambdaQueryWrapper<>();
        q.orderByAsc(NewsCategory::getSortOrder).orderByAsc(NewsCategory::getId);
        return newsCategoryMapper.selectPage(page, q);
    }

    @Override
    public Long adminCreate(Long operatorId, String name, Integer sortOrder) {
        String n = PlainTextSanitizer.sanitizeCategory(name);
        if (!StringUtils.hasText(n)) {
            throw new BusinessException("分类名称不能为空");
        }
        NewsCategory c = new NewsCategory();
        c.setName(n);
        c.setSortOrder(sortOrder != null ? sortOrder : 0);
        newsCategoryMapper.insert(c);
        auditLogService.log(null, "NEWS_CATEGORY_CREATE", "NEWS_CATEGORY", c.getId(), "新增新闻分类: " + n);
        return c.getId();
    }

    @Override
    public void adminUpdate(Long operatorId, Long id, String name, Integer sortOrder) {
        NewsCategory c = newsCategoryMapper.selectById(id);
        if (c == null) {
            throw new BusinessException("分类不存在");
        }
        if (StringUtils.hasText(name)) {
            String n = PlainTextSanitizer.sanitizeCategory(name);
            if (!StringUtils.hasText(n)) {
                throw new BusinessException("分类名称不能为空");
            }
            c.setName(n);
        }
        if (sortOrder != null) {
            c.setSortOrder(sortOrder);
        }
        newsCategoryMapper.updateById(c);
        auditLogService.log(operatorId, "NEWS_CATEGORY_UPDATE", "NEWS_CATEGORY", id, "修改新闻分类");
    }

    @Override
    public void adminDelete(Long operatorId, Long id) {
        NewsCategory c = newsCategoryMapper.selectById(id);
        if (c == null) {
            throw new BusinessException("分类不存在");
        }
        long cnt = newsMapper.selectCount(new LambdaQueryWrapper<News>().eq(News::getCategoryId, id).eq(News::getDeleted, 0));
        if (cnt > 0) {
            throw new BusinessException("仍有新闻使用该分类，无法删除");
        }
        newsCategoryMapper.deleteById(id);
        auditLogService.log(operatorId, "NEWS_CATEGORY_DELETE", "NEWS_CATEGORY", id, "删除新闻分类: " + c.getName());
    }
}
