package com.fire.recommendation.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fire.recommendation.common.ResultCode;
import com.fire.recommendation.entity.ForumPost;
import com.fire.recommendation.entity.ForumPostLike;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.mapper.ForumPostMapper;
import com.fire.recommendation.mapper.ForumPostLikeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 论坛帖子点赞：优先 Redis Set（key {@value #REDIS_KEY_PREFIX}{postId}）；无 Redis 或 Redis 异常时改走 {@code forum_post_like} 表。
 * 当 Redis 中 Set 为空而 {@code forum_post.like_count>0} 且明细表有行时，从表重建 Set 并回写 {@code like_count}（与行数对齐）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ForumPostLikeService {

    public static final String REDIS_KEY_PREFIX = "forum:post:like:";

    private final ForumPostLikeMapper forumPostLikeMapper;
    private final ForumPostMapper postMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    public boolean isLiked(Long postId, Long userId) {
        if (userId == null || postId == null) {
            return false;
        }
        if (redisTemplate != null) {
            try {
                ensureForumPostLikeRedisSynced(postId);
                return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(REDIS_KEY_PREFIX + postId, String.valueOf(userId)));
            } catch (DataAccessException e) {
                log.warn("Redis 不可用，帖子点赞状态改读库: {}", e.getMessage());
            }
        }
        return forumPostLikeMapper.selectCount(new LambdaQueryWrapper<ForumPostLike>()
                .eq(ForumPostLike::getPostId, postId)
                .eq(ForumPostLike::getUserId, userId)) > 0;
    }

    /**
     * Redis Set 为空且 {@code forum_post_like} 有行时从表恢复 Set；无明细仅 {@code like_count>0} 时无法还原，交给 toggle 孤儿分支。
     */
    private void ensureForumPostLikeRedisSynced(Long postId) {
        if (redisTemplate == null || postId == null) {
            return;
        }
        ForumPost post = postMapper.selectById(postId);
        if (post == null) {
            return;
        }
        int dbCnt = post.getLikeCount() != null ? post.getLikeCount() : 0;
        String key = REDIS_KEY_PREFIX + postId;
        Long sc;
        try {
            sc = redisTemplate.opsForSet().size(key);
        } catch (DataAccessException e) {
            log.warn("ensureForumPostLikeRedisSynced Redis 读失败: {}", e.getMessage());
            return;
        }
        int redisCnt = sc != null ? sc.intValue() : 0;
        if (redisCnt > 0) {
            return;
        }
        long tableCnt = forumPostLikeMapper.selectCount(new LambdaQueryWrapper<ForumPostLike>()
                .eq(ForumPostLike::getPostId, postId));
        // 明细表有行则必须从表恢复（即使 like_count 被误写成 0）
        if (tableCnt <= 0) {
            if (dbCnt <= 0) {
                return;
            }
            // Redis 空、无明细、仅 like_count>0：无法还原 Set，留给 toggle 孤儿分支
            return;
        }
        List<ForumPostLike> rows = forumPostLikeMapper.selectList(new LambdaQueryWrapper<ForumPostLike>()
                .eq(ForumPostLike::getPostId, postId));
        try {
            redisTemplate.delete(key);
            for (ForumPostLike r : rows) {
                if (r.getUserId() != null) {
                    redisTemplate.opsForSet().add(key, String.valueOf(r.getUserId()));
                }
            }
        } catch (DataAccessException e) {
            log.warn("ensureForumPostLikeRedisSynced Redis 写失败: {}", e.getMessage());
            return;
        }
        int n = rows.size();
        if (n != dbCnt) {
            log.warn("forum post {} like_count {} 与 forum_post_like 行数 {} 不一致，以明细为准回写", postId, dbCnt, n);
        }
        persistPostLikeCount(postId, n);
    }

    /**
     * 切换点赞并同步 {@code forum_post.like_count}。
     */
    public Map<String, Object> toggle(Long postId, Long userId) {
        if (postId == null || postId <= 0) {
            throw new BusinessException("帖子 ID 无效");
        }
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (redisTemplate != null) {
            try {
                ensureForumPostLikeRedisSynced(postId);

                ForumPost post = postMapper.selectById(postId);
                if (post == null) {
                    throw new BusinessException("帖子不存在");
                }
                int dbCnt = post.getLikeCount() != null ? post.getLikeCount() : 0;
                String key = REDIS_KEY_PREFIX + postId;
                Long sc = redisTemplate.opsForSet().size(key);
                int redisCnt = sc != null ? sc.intValue() : 0;
                long tableCnt = forumPostLikeMapper.selectCount(new LambdaQueryWrapper<ForumPostLike>()
                        .eq(ForumPostLike::getPostId, postId));

                // 历史仅 Redis、未落表：无法还原 Set，首次点赞在 DB 上 +1 并双写，避免用 size==1 覆盖大 like_count
                boolean orphan = redisCnt == 0 && tableCnt == 0 && dbCnt > 0;
                if (orphan) {
                    redisTemplate.opsForSet().add(key, String.valueOf(userId));
                    try {
                        ForumPostLike row = new ForumPostLike();
                        row.setPostId(postId);
                        row.setUserId(userId);
                        forumPostLikeMapper.insert(row);
                    } catch (DuplicateKeyException e) {
                        long cnt = forumPostLikeMapper.selectCount(new LambdaQueryWrapper<ForumPostLike>()
                                .eq(ForumPostLike::getPostId, postId));
                        int ic = (int) Math.min(cnt, Integer.MAX_VALUE);
                        persistPostLikeCount(postId, ic);
                        redisTemplate.opsForSet().add(key, String.valueOf(userId));
                        return result(true, ic);
                    }
                    int newCnt = dbCnt + 1;
                    persistPostLikeCount(postId, newCnt);
                    return result(true, newCnt);
                }

                Boolean isMember = redisTemplate.opsForSet().isMember(key, String.valueOf(userId));
                boolean liked;
                if (Boolean.TRUE.equals(isMember)) {
                    redisTemplate.opsForSet().remove(key, String.valueOf(userId));
                    forumPostLikeMapper.delete(new LambdaQueryWrapper<ForumPostLike>()
                            .eq(ForumPostLike::getPostId, postId)
                            .eq(ForumPostLike::getUserId, userId));
                    liked = false;
                } else {
                    redisTemplate.opsForSet().add(key, String.valueOf(userId));
                    try {
                        ForumPostLike row = new ForumPostLike();
                        row.setPostId(postId);
                        row.setUserId(userId);
                        forumPostLikeMapper.insert(row);
                    } catch (DuplicateKeyException e) {
                        Long sz = redisTemplate.opsForSet().size(key);
                        int cnt = sz != null ? sz.intValue() : 0;
                        long fromTable = forumPostLikeMapper.selectCount(new LambdaQueryWrapper<ForumPostLike>()
                                .eq(ForumPostLike::getPostId, postId));
                        int ic = (int) Math.min(Math.max(cnt, fromTable), Integer.MAX_VALUE);
                        persistPostLikeCount(postId, ic);
                        return result(true, ic);
                    }
                    liked = true;
                }
                Long sz = redisTemplate.opsForSet().size(key);
                int cnt = sz != null ? sz.intValue() : 0;
                persistPostLikeCount(postId, cnt);
                return result(liked, cnt);
            } catch (DataAccessException e) {
                log.warn("Redis 不可用，论坛帖子点赞改走数据库: {}", e.getMessage());
            }
        }
        LambdaQueryWrapper<ForumPostLike> q = new LambdaQueryWrapper<ForumPostLike>()
                .eq(ForumPostLike::getPostId, postId)
                .eq(ForumPostLike::getUserId, userId);
        ForumPostLike existing = forumPostLikeMapper.selectOne(q);
        boolean liked;
        if (existing != null) {
            forumPostLikeMapper.deleteById(existing.getId());
            liked = false;
        } else {
            ForumPostLike row = new ForumPostLike();
            row.setPostId(postId);
            row.setUserId(userId);
            forumPostLikeMapper.insert(row);
            liked = true;
        }
        long cnt = forumPostLikeMapper.selectCount(new LambdaQueryWrapper<ForumPostLike>()
                .eq(ForumPostLike::getPostId, postId));
        int ic = (int) Math.min(cnt, Integer.MAX_VALUE);
        persistPostLikeCount(postId, ic);
        return result(liked, ic);
    }

    private void persistPostLikeCount(Long postId, int count) {
        postMapper.update(null, new LambdaUpdateWrapper<ForumPost>()
                .eq(ForumPost::getId, postId)
                .set(ForumPost::getLikeCount, count));
    }

    private static Map<String, Object> result(boolean liked, int likeCount) {
        Map<String, Object> m = new HashMap<>();
        m.put("liked", liked);
        m.put("likeCount", likeCount);
        return m;
    }
}
