package com.fire.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fire.recommendation.entity.News;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.common.ResultCode;
import com.fire.recommendation.mapper.NewsMapper;
import com.fire.recommendation.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsMapper newsMapper;

    @Override
    public IPage<Map<String, Object>> list(Integer pageNum, Integer pageSize, String region, String title, String orderBy, String order) {
        Page<News> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<News> q = new LambdaQueryWrapper<>();
        q.eq(News::getDeleted, 0).eq(News::getStatus, 1);
        if (StringUtils.hasText(region)) q.eq(News::getRegion, region);
        if (StringUtils.hasText(title)) q.like(News::getTitle, title);
        boolean asc = "asc".equalsIgnoreCase(order);
        if ("urgency".equalsIgnoreCase(orderBy)) {
            q.orderBy(true, asc, News::getUrgencyLevel).orderByDesc(News::getPublishTime);
        } else {
            q.orderByDesc(News::getPublishTime);
        }
        IPage<News> result = newsMapper.selectPage(page, q);
        return result.convert(n -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", n.getId());
            m.put("title", n.getTitle());
            m.put("region", n.getRegion());
            m.put("urgencyLevel", n.getUrgencyLevel());
            m.put("publishTime", n.getPublishTime());
            m.put("summary", n.getSummary());
            return m;
        });
    }

    @Override
    public Map<String, Object> getDetail(Long id) {
        News n = newsMapper.selectById(id);
        if (n == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Map<String, Object> m = new HashMap<>();
        m.put("id", n.getId());
        m.put("title", n.getTitle());
        m.put("content", n.getContent());
        m.put("region", n.getRegion());
        m.put("urgencyLevel", n.getUrgencyLevel());
        m.put("publisherId", n.getPublisherId());
        m.put("summary", n.getSummary());
        m.put("viewCount", n.getViewCount());
        m.put("publishTime", n.getPublishTime());
        m.put("createTime", n.getCreateTime());
        return m;
    }

    @Override
    public Long adminSave(Long publisherId, Map<String, Object> body) {
        News n = new News();
        n.setTitle((String) body.get("title"));
        n.setContent(body.get("content") != null ? body.get("content").toString() : "");
        n.setRegion((String) body.get("region"));
        n.setUrgencyLevel(body.get("urgencyLevel") != null ? Integer.valueOf(body.get("urgencyLevel").toString()) : 1);
        n.setPublisherId(publisherId);
        n.setSummary((String) body.get("summary"));
        n.setStatus(1);
        if (body.get("publishTime") != null) {
            Object pt = body.get("publishTime");
            if (pt instanceof Long) {
                n.setPublishTime(LocalDateTime.ofEpochSecond((Long) pt / 1000, 0, java.time.ZoneOffset.ofHours(8)));
            } else if (pt instanceof String) {
                n.setPublishTime(LocalDateTime.parse((String) pt));
            }
        }
        newsMapper.insert(n);
        return n.getId();
    }

    @Override
    public void adminUpdate(Long id, Map<String, Object> body) {
        News n = newsMapper.selectById(id);
        if (n == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (body.containsKey("title")) n.setTitle((String) body.get("title"));
        if (body.containsKey("content")) n.setContent(body.get("content") != null ? body.get("content").toString() : n.getContent());
        if (body.containsKey("region")) n.setRegion((String) body.get("region"));
        if (body.containsKey("urgencyLevel")) n.setUrgencyLevel(Integer.valueOf(body.get("urgencyLevel").toString()));
        if (body.containsKey("summary")) n.setSummary((String) body.get("summary"));
        if (body.containsKey("publishTime")) {
            Object pt = body.get("publishTime");
            if (pt instanceof Long) {
                n.setPublishTime(LocalDateTime.ofEpochSecond((Long) pt / 1000, 0, java.time.ZoneOffset.ofHours(8)));
            } else if (pt instanceof String && StringUtils.hasText((String) pt)) {
                n.setPublishTime(LocalDateTime.parse((String) pt));
            }
        }
        newsMapper.updateById(n);
    }

    @Override
    public void adminDelete(Long id) {
        News n = newsMapper.selectById(id);
        if (n == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        newsMapper.deleteById(id);
    }
}
