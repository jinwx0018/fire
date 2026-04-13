package com.fire.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fire.recommendation.common.PageResult;
import com.fire.recommendation.common.ResultCode;
import com.fire.recommendation.constant.CollectionTargetType;
import com.fire.recommendation.entity.Equipment;
import com.fire.recommendation.entity.EquipmentType;
import com.fire.recommendation.entity.ForumPost;
import com.fire.recommendation.entity.KnowledgeCategory;
import com.fire.recommendation.entity.KnowledgeContent;
import com.fire.recommendation.entity.News;
import com.fire.recommendation.entity.UserCollection;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.mapper.EquipmentMapper;
import com.fire.recommendation.mapper.EquipmentTypeMapper;
import com.fire.recommendation.mapper.ForumPostMapper;
import com.fire.recommendation.mapper.KnowledgeCategoryMapper;
import com.fire.recommendation.mapper.KnowledgeContentMapper;
import com.fire.recommendation.mapper.NewsMapper;
import com.fire.recommendation.mapper.UserCollectionMapper;
import com.fire.recommendation.service.RecommendService;
import com.fire.recommendation.service.UserCollectionService;
import com.fire.recommendation.util.NewsCoverUrlUtil;
import com.fire.recommendation.util.PlainTextSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserCollectionServiceImpl implements UserCollectionService {

    private static final int STATUS_PUBLISHED = 1;
    private static final int FORUM_APPROVED = 1;

    private final UserCollectionMapper collectionMapper;
    private final KnowledgeContentMapper contentMapper;
    private final KnowledgeCategoryMapper categoryMapper;
    private final ForumPostMapper forumPostMapper;
    private final NewsMapper newsMapper;
    private final EquipmentMapper equipmentMapper;
    private final EquipmentTypeMapper equipmentTypeMapper;
    private final RecommendService recommendService;

    private LambdaQueryWrapper<UserCollection> rowQuery(long userId, int targetType, long targetId) {
        return new LambdaQueryWrapper<UserCollection>()
                .eq(UserCollection::getUserId, userId)
                .eq(UserCollection::getTargetType, targetType)
                .eq(UserCollection::getContentId, targetId);
    }

    @Override
    public boolean isCollected(Long userId, int targetType, Long targetId) {
        if (userId == null || targetId == null) {
            return false;
        }
        return collectionMapper.selectCount(rowQuery(userId, targetType, targetId)) > 0;
    }

    @Override
    public void collectKnowledge(Long userId, Long contentId) {
        KnowledgeContent c = contentMapper.selectById(contentId);
        if (c == null || Integer.valueOf(1).equals(c.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        int cst = c.getStatus() != null ? c.getStatus() : 0;
        if (cst != STATUS_PUBLISHED) {
            throw new BusinessException("内容未通过审核或未上架，暂不可收藏");
        }
        if (collectionMapper.selectCount(rowQuery(userId, CollectionTargetType.KNOWLEDGE, contentId)) == 0) {
            UserCollection uc = new UserCollection();
            uc.setUserId(userId);
            uc.setTargetType(CollectionTargetType.KNOWLEDGE);
            uc.setContentId(contentId);
            collectionMapper.insert(uc);
            recommendService.recordBehavior(userId, "COLLECT", "CONTENT", contentId);
        }
    }

    @Override
    public void uncollectKnowledge(Long userId, Long contentId) {
        if (userId == null || contentId == null) {
            return;
        }
        /* 不校验知识是否仍存在或已下架，便于用户取消收藏 */
        collectionMapper.delete(rowQuery(userId, CollectionTargetType.KNOWLEDGE, contentId));
    }

    @Override
    public void collectForumPost(Long userId, Long postId) {
        ForumPost p = forumPostMapper.selectById(postId);
        if (p == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        int st = p.getStatus() != null ? p.getStatus() : 0;
        if (st != FORUM_APPROVED) {
            throw new BusinessException("帖子未通过审核，暂不可收藏");
        }
        if (collectionMapper.selectCount(rowQuery(userId, CollectionTargetType.FORUM_POST, postId)) == 0) {
            UserCollection uc = new UserCollection();
            uc.setUserId(userId);
            uc.setTargetType(CollectionTargetType.FORUM_POST);
            uc.setContentId(postId);
            collectionMapper.insert(uc);
            recommendService.recordBehavior(userId, "COLLECT", "FORUM_POST", postId);
        }
    }

    @Override
    public void uncollectForumPost(Long userId, Long postId) {
        if (userId == null || postId == null) {
            return;
        }
        collectionMapper.delete(rowQuery(userId, CollectionTargetType.FORUM_POST, postId));
    }

    @Override
    public void collectNews(Long userId, Long newsId) {
        News n = newsMapper.selectById(newsId);
        if (n == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (n.getStatus() == null || !Integer.valueOf(STATUS_PUBLISHED).equals(n.getStatus())) {
            throw new BusinessException("新闻未上架，暂不可收藏");
        }
        if (n.getPublishTime() != null && n.getPublishTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("新闻未上架，暂不可收藏");
        }
        if (collectionMapper.selectCount(rowQuery(userId, CollectionTargetType.NEWS, newsId)) == 0) {
            UserCollection uc = new UserCollection();
            uc.setUserId(userId);
            uc.setTargetType(CollectionTargetType.NEWS);
            uc.setContentId(newsId);
            collectionMapper.insert(uc);
            recommendService.recordBehavior(userId, "COLLECT", "NEWS", newsId);
        }
    }

    @Override
    public void uncollectNews(Long userId, Long newsId) {
        if (userId == null || newsId == null) {
            return;
        }
        collectionMapper.delete(rowQuery(userId, CollectionTargetType.NEWS, newsId));
    }

    @Override
    public void collectEquipment(Long userId, Long equipmentId) {
        Equipment e = equipmentMapper.selectById(equipmentId);
        if (e == null || Integer.valueOf(1).equals(e.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (e.getStatus() == null || !Integer.valueOf(STATUS_PUBLISHED).equals(e.getStatus())) {
            throw new BusinessException("器材未上架，暂不可收藏");
        }
        if (collectionMapper.selectCount(rowQuery(userId, CollectionTargetType.EQUIPMENT, equipmentId)) == 0) {
            UserCollection uc = new UserCollection();
            uc.setUserId(userId);
            uc.setTargetType(CollectionTargetType.EQUIPMENT);
            uc.setContentId(equipmentId);
            collectionMapper.insert(uc);
            recommendService.recordBehavior(userId, "COLLECT", "EQUIPMENT", equipmentId);
        }
    }

    @Override
    public void uncollectEquipment(Long userId, Long equipmentId) {
        if (userId == null || equipmentId == null) {
            return;
        }
        collectionMapper.delete(rowQuery(userId, CollectionTargetType.EQUIPMENT, equipmentId));
    }

    @Override
    public PageResult<Map<String, Object>> myCollectPage(Long userId, String module, Integer pageNum, Integer pageSize) {
        int type = CollectionTargetType.fromModuleParam(module);
        Page<UserCollection> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<UserCollection> q = new LambdaQueryWrapper<UserCollection>()
                .eq(UserCollection::getUserId, userId)
                .eq(UserCollection::getTargetType, type)
                .orderByDesc(UserCollection::getCreateTime);
        IPage<UserCollection> result = collectionMapper.selectPage(page, q);
        List<Map<String, Object>> list = result.getRecords().stream().map(uc -> mapOneCollectRow(type, uc)).collect(Collectors.toList());
        return PageResult.of(list, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    private Map<String, Object> mapOneCollectRow(int type, UserCollection uc) {
        Map<String, Object> m = new HashMap<>();
        m.put("collectTime", uc.getCreateTime());
        m.put("module", moduleKey(type));
        Long tid = uc.getContentId();
        m.put("id", tid);
        if (type == CollectionTargetType.KNOWLEDGE) {
            KnowledgeContent k = contentMapper.selectById(tid);
            if (k != null) {
                m.put("title", k.getTitle());
                KnowledgeCategory cat = k.getCategoryId() != null ? categoryMapper.selectById(k.getCategoryId()) : null;
                m.put("categoryName", cat != null ? cat.getName() : null);
            } else {
                m.put("title", "（内容已删除或不可见）");
                m.put("categoryName", null);
            }
        } else if (type == CollectionTargetType.FORUM_POST) {
            ForumPost p = forumPostMapper.selectById(tid);
            if (p != null) {
                m.put("title", p.getTitle());
            } else {
                m.put("title", "（帖子已删除或不可见）");
            }
        } else if (type == CollectionTargetType.EQUIPMENT) {
            Equipment eq = equipmentMapper.selectById(tid);
            if (eq != null && !Integer.valueOf(1).equals(eq.getDeleted())
                    && eq.getStatus() != null && Integer.valueOf(STATUS_PUBLISHED).equals(eq.getStatus())) {
                m.put("title", eq.getName());
                EquipmentType tp = eq.getTypeId() != null ? equipmentTypeMapper.selectById(eq.getTypeId()) : null;
                m.put("categoryName", tp != null ? tp.getName() : null);
            } else {
                m.put("title", "（器材已删除或不可见）");
                m.put("categoryName", null);
            }
        } else {
            News n = newsMapper.selectById(tid);
            if (n != null) {
                m.put("title", PlainTextSanitizer.sanitizeTitleOutput(n.getTitle()));
                m.put("coverUrl", NewsCoverUrlUtil.sanitize(n.getCoverUrl()));
                m.put("categoryName", null);
            } else {
                m.put("title", "（新闻已删除或不可见）");
                m.put("coverUrl", null);
                m.put("categoryName", null);
            }
        }
        return m;
    }

    private static String moduleKey(int type) {
        if (type == CollectionTargetType.FORUM_POST) {
            return "forum";
        }
        if (type == CollectionTargetType.NEWS) {
            return "news";
        }
        if (type == CollectionTargetType.EQUIPMENT) {
            return "equipment";
        }
        return "knowledge";
    }

    @Override
    public void deleteAllForKnowledgeContent(Long knowledgeContentId) {
        if (knowledgeContentId == null) {
            return;
        }
        collectionMapper.delete(new LambdaQueryWrapper<UserCollection>()
                .eq(UserCollection::getTargetType, CollectionTargetType.KNOWLEDGE)
                .eq(UserCollection::getContentId, knowledgeContentId));
    }
}
