package com.fire.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fire.recommendation.common.PageResult;
import com.fire.recommendation.entity.KnowledgeCategory;
import com.fire.recommendation.entity.KnowledgeContent;
import com.fire.recommendation.entity.KnowledgeContentTag;
import com.fire.recommendation.entity.KnowledgeTag;
import com.fire.recommendation.entity.UserCollection;
import com.fire.recommendation.entity.SysUser;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.common.ResultCode;
import com.fire.recommendation.mapper.KnowledgeCategoryMapper;
import com.fire.recommendation.mapper.KnowledgeContentMapper;
import com.fire.recommendation.mapper.KnowledgeContentTagMapper;
import com.fire.recommendation.mapper.KnowledgeTagMapper;
import com.fire.recommendation.mapper.UserCollectionMapper;
import com.fire.recommendation.mapper.SysUserMapper;
import com.fire.recommendation.service.ContentService;
import com.fire.recommendation.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private final KnowledgeContentMapper contentMapper;
    private final KnowledgeCategoryMapper categoryMapper;
    private final KnowledgeContentTagMapper contentTagMapper;
    private final KnowledgeTagMapper tagMapper;
    private final UserCollectionMapper collectionMapper;
    private final SysUserMapper userMapper;
    private final RecommendService recommendService;

    @Value("${app.client-base-url:http://localhost:5173}")
    private String clientBaseUrl;

    @Override
    public IPage<Map<String, Object>> list(Integer pageNum, Integer pageSize, Long categoryId, String title, Integer status) {
        Page<KnowledgeContent> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<KnowledgeContent> q = new LambdaQueryWrapper<>();
        q.eq(KnowledgeContent::getDeleted, 0);
        if (categoryId != null) q.eq(KnowledgeContent::getCategoryId, categoryId);
        if (StringUtils.hasText(title)) q.like(KnowledgeContent::getTitle, title);
        if (status != null) q.eq(KnowledgeContent::getStatus, status);
        q.orderByDesc(KnowledgeContent::getCreateTime);
        IPage<KnowledgeContent> result = contentMapper.selectPage(page, q);
        return result.convert(this::toListMap);
    }

    @Override
    public Map<String, Object> getDetail(Long id, Long userId) {
        KnowledgeContent c = contentMapper.selectById(id);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        c.setViewCount((c.getViewCount() != null ? c.getViewCount() : 0) + 1);
        contentMapper.updateById(c);
        if (userId != null) {
            recommendService.recordBehavior(userId, "VIEW", "CONTENT", id);
        }
        Map<String, Object> map = toDetailMap(c);
        List<String> tagNames = listTagNamesByContentId(id);
        map.put("tags", tagNames);
        if (userId != null) {
            long cnt = collectionMapper.selectCount(new LambdaQueryWrapper<UserCollection>()
                    .eq(UserCollection::getUserId, userId).eq(UserCollection::getContentId, id));
            map.put("collected", cnt > 0);
        } else {
            map.put("collected", false);
        }
        return map;
    }

    @Override
    public Long save(Long authorId, Map<String, Object> body) {
        KnowledgeContent c = new KnowledgeContent();
        c.setTitle((String) body.get("title"));
        c.setCategoryId(Long.valueOf(body.get("categoryId").toString()));
        c.setContent((String) body.get("content"));
        c.setCover((String) body.get("cover"));
        c.setSummary((String) body.get("summary"));
        c.setAuthorId(authorId);
        c.setStatus(body.get("status") != null ? Integer.valueOf(body.get("status").toString()) : 1);
        contentMapper.insert(c);
        bindTags(c.getId(), body.get("tags"));
        return c.getId();
    }

    @Override
    public void update(Long id, Long operatorId, Map<String, Object> body) {
        KnowledgeContent c = contentMapper.selectById(id);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (body.containsKey("title")) c.setTitle((String) body.get("title"));
        if (body.containsKey("categoryId")) c.setCategoryId(Long.valueOf(body.get("categoryId").toString()));
        if (body.containsKey("content")) c.setContent((String) body.get("content"));
        if (body.containsKey("cover")) c.setCover((String) body.get("cover"));
        if (body.containsKey("summary")) c.setSummary((String) body.get("summary"));
        if (body.containsKey("status")) c.setStatus(Integer.valueOf(body.get("status").toString()));
        contentMapper.updateById(c);
        if (body.containsKey("tags")) {
            contentTagMapper.delete(new LambdaQueryWrapper<KnowledgeContentTag>().eq(KnowledgeContentTag::getContentId, id));
            bindTags(id, body.get("tags"));
        }
    }

    @Override
    public void delete(Long id, Long operatorId) {
        KnowledgeContent c = contentMapper.selectById(id);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        contentMapper.deleteById(id);
    }

    @Override
    public void collect(Long userId, Long contentId) {
        long cnt = collectionMapper.selectCount(new LambdaQueryWrapper<UserCollection>()
                .eq(UserCollection::getUserId, userId).eq(UserCollection::getContentId, contentId));
        if (cnt == 0) {
            UserCollection uc = new UserCollection();
            uc.setUserId(userId);
            uc.setContentId(contentId);
            collectionMapper.insert(uc);
        }
    }

    @Override
    public void uncollect(Long userId, Long contentId) {
        collectionMapper.delete(new LambdaQueryWrapper<UserCollection>()
                .eq(UserCollection::getUserId, userId).eq(UserCollection::getContentId, contentId));
    }

    @Override
    public PageResult<Map<String, Object>> collectList(Long userId, Integer pageNum, Integer pageSize) {
        Page<UserCollection> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<UserCollection> q = new LambdaQueryWrapper<>();
        q.eq(UserCollection::getUserId, userId).orderByDesc(UserCollection::getCreateTime);
        IPage<UserCollection> result = collectionMapper.selectPage(page, q);
        List<Map<String, Object>> list = result.getRecords().stream().map(uc -> {
            KnowledgeContent k = contentMapper.selectById(uc.getContentId());
            Map<String, Object> m = new HashMap<>();
            if (k != null) {
                m.put("id", k.getId());
                m.put("title", k.getTitle());
                m.put("cover", k.getCover());
                m.put("collectTime", uc.getCreateTime());
            }
            KnowledgeCategory cat = k != null ? categoryMapper.selectById(k.getCategoryId()) : null;
            m.put("categoryName", cat != null ? cat.getName() : null);
            return m;
        }).collect(Collectors.toList());
        return PageResult.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public Map<String, Object> shareUrl(Long contentId) {
        Map<String, Object> m = new HashMap<>();
        m.put("contentId", contentId);
        m.put("shareUrl", clientBaseUrl + "/article/" + contentId);
        return m;
    }

    @Override
    public List<Map<String, Object>> categoryList() {
        List<KnowledgeCategory> list = categoryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeCategory>().eq(KnowledgeCategory::getStatus, 1).orderByAsc(KnowledgeCategory::getSort));
        return list.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("sort", c.getSort());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public Long adminSaveCategory(String name, Integer sort) {
        KnowledgeCategory c = new KnowledgeCategory();
        c.setName(name);
        c.setSort(sort != null ? sort : 0);
        c.setStatus(1);
        categoryMapper.insert(c);
        return c.getId();
    }

    @Override
    public void adminUpdateCategory(Long id, String name, Integer sort) {
        KnowledgeCategory c = categoryMapper.selectById(id);
        if (c != null) {
            if (name != null) c.setName(name);
            if (sort != null) c.setSort(sort);
            categoryMapper.updateById(c);
        }
    }

    @Override
    public void adminDeleteCategory(Long id) {
        categoryMapper.deleteById(id);
    }

    private Map<String, Object> toListMap(KnowledgeContent c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("title", c.getTitle());
        m.put("categoryId", c.getCategoryId());
        KnowledgeCategory cat = categoryMapper.selectById(c.getCategoryId());
        m.put("categoryName", cat != null ? cat.getName() : null);
        m.put("cover", c.getCover());
        m.put("summary", c.getSummary());
        m.put("viewCount", c.getViewCount());
        m.put("likeCount", c.getLikeCount());
        m.put("createTime", c.getCreateTime());
        return m;
    }

    private Map<String, Object> toDetailMap(KnowledgeContent c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("title", c.getTitle());
        m.put("categoryId", c.getCategoryId());
        KnowledgeCategory cat = categoryMapper.selectById(c.getCategoryId());
        m.put("categoryName", cat != null ? cat.getName() : null);
        m.put("content", c.getContent());
        m.put("cover", c.getCover());
        m.put("summary", c.getSummary());
        m.put("viewCount", c.getViewCount());
        m.put("likeCount", c.getLikeCount());
        m.put("authorId", c.getAuthorId());
        SysUser author = userMapper.selectById(c.getAuthorId());
        m.put("authorName", author != null ? author.getUsername() : null);
        m.put("createTime", c.getCreateTime());
        m.put("status", c.getStatus());
        return m;
    }

    /** 根据内容ID查询关联的标签名列表 */
    private List<String> listTagNamesByContentId(Long contentId) {
        List<KnowledgeContentTag> ctList = contentTagMapper.selectList(
                new LambdaQueryWrapper<KnowledgeContentTag>().eq(KnowledgeContentTag::getContentId, contentId));
        if (ctList.isEmpty()) return Collections.emptyList();
        List<Long> tagIds = ctList.stream().map(KnowledgeContentTag::getTagId).distinct().toList();
        List<KnowledgeTag> tags = tagMapper.selectBatchIds(tagIds);
        return tags.stream().map(KnowledgeTag::getName).toList();
    }

    /** 将 body 中的 tags（可为 tag 名列表或 id 列表）绑定到 contentId */
    @SuppressWarnings("unchecked")
    private void bindTags(Long contentId, Object tagsObj) {
        if (tagsObj == null) return;
        List<?> list = tagsObj instanceof List ? (List<?>) tagsObj : Collections.emptyList();
        for (Object item : list) {
            if (item == null) continue;
            String name = item.toString().trim();
            if (!StringUtils.hasText(name)) continue;
            KnowledgeTag tag = tagMapper.selectOne(new LambdaQueryWrapper<KnowledgeTag>().eq(KnowledgeTag::getName, name));
            if (tag == null) {
                tag = new KnowledgeTag();
                tag.setName(name);
                tagMapper.insert(tag);
            }
            long cnt = contentTagMapper.selectCount(new LambdaQueryWrapper<KnowledgeContentTag>()
                    .eq(KnowledgeContentTag::getContentId, contentId).eq(KnowledgeContentTag::getTagId, tag.getId()));
            if (cnt == 0) {
                KnowledgeContentTag ct = new KnowledgeContentTag();
                ct.setContentId(contentId);
                ct.setTagId(tag.getId());
                contentTagMapper.insert(ct);
            }
        }
    }
}
