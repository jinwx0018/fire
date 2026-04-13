package com.fire.recommendation.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fire.recommendation.common.ResultCode;
import com.fire.recommendation.entity.UserCommentLike;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.mapper.UserCommentLikeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * 评论点赞：优先 Redis Set；无 Redis 或 Redis 异常时写 {@code user_comment_like}。
 * 有 Redis 时与明细表双写；Redis 为空而表有数据时从表恢复 Set，避免仅用 SCARD 覆盖评论行 like_count。
 */
@Slf4j
@Component
public class CommentLikeService {

    public static final String REDIS_FORUM_COMMENT = "forum:comment:like:";
    public static final String REDIS_NEWS_COMMENT = "news:comment:like:";
    public static final String REDIS_KNOWLEDGE_COMMENT = "knowledge:comment:like:";

    public static final String CHAN_FORUM = "FORUM_COMMENT";
    public static final String CHAN_NEWS = "NEWS_COMMENT";
    public static final String CHAN_KNOWLEDGE = "KNOWLEDGE_COMMENT";

    private final UserCommentLikeMapper userCommentLikeMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    public CommentLikeService(UserCommentLikeMapper userCommentLikeMapper) {
        this.userCommentLikeMapper = userCommentLikeMapper;
    }

    /**
     * 列表展示用点赞总数：与 {@link #toggle} 数据源一致。优先 Redis Set 大小；无 Redis 时用 {@code user_comment_like} 计数；
     * 与库字段 {@code persistedLikeCount} 取较大值，避免仅 Redis 有数据而 comment 行未同步时长期显示 0。
     */
    public int resolveLikeCount(String redisPrefix, String dbChannel, Long commentId, Integer persistedLikeCount) {
        int persisted = persistedLikeCount != null ? persistedLikeCount : 0;
        if (commentId == null) {
            return persisted;
        }
        if (redisTemplate != null) {
            try {
                Long sz = redisTemplate.opsForSet().size(redisPrefix + commentId);
                if (sz != null && sz == 0) {
                    ensureCommentLikeRedisFromTable(redisPrefix, dbChannel, commentId, null);
                    sz = redisTemplate.opsForSet().size(redisPrefix + commentId);
                }
                if (sz != null) {
                    return Math.max(sz.intValue(), persisted);
                }
            } catch (DataAccessException e) {
                log.warn("Redis 不可用，评论点赞数改读库: {}", e.getMessage());
            }
        }
        long dbCnt = userCommentLikeMapper.selectCount(new LambdaQueryWrapper<UserCommentLike>()
                .eq(UserCommentLike::getChannel, dbChannel)
                .eq(UserCommentLike::getCommentId, commentId));
        int fromDb = (int) Math.min(dbCnt, Integer.MAX_VALUE);
        return Math.max(fromDb, persisted);
    }

    public boolean isLiked(String redisPrefix, String dbChannel, Long commentId, Long userId) {
        if (userId == null || commentId == null) {
            return false;
        }
        if (redisTemplate != null) {
            try {
                ensureCommentLikeRedisFromTable(redisPrefix, dbChannel, commentId, null);
                return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(redisPrefix + commentId, String.valueOf(userId)));
            } catch (DataAccessException e) {
                log.warn("Redis 不可用，评论点赞状态改读库: {}", e.getMessage());
            }
        }
        return userCommentLikeMapper.selectCount(new LambdaQueryWrapper<UserCommentLike>()
                .eq(UserCommentLike::getChannel, dbChannel)
                .eq(UserCommentLike::getCommentId, commentId)
                .eq(UserCommentLike::getUserId, userId)) > 0;
    }

    /**
     * Redis Set 为空且 {@code user_comment_like} 有行时，从表重建 Set；若传入 {@code persistLikeCount}，则同步评论行 like_count。
     */
    private void ensureCommentLikeRedisFromTable(String redisPrefix, String dbChannel, Long commentId,
                                                 IntConsumer persistLikeCountOrNull) {
        if (redisTemplate == null || commentId == null) {
            return;
        }
        String key = redisPrefix + commentId;
        Long sc;
        try {
            sc = redisTemplate.opsForSet().size(key);
        } catch (DataAccessException e) {
            log.warn("ensureCommentLikeRedisFromTable Redis 读失败: {}", e.getMessage());
            return;
        }
        if (sc != null && sc > 0) {
            return;
        }
        List<UserCommentLike> rows = userCommentLikeMapper.selectList(new LambdaQueryWrapper<UserCommentLike>()
                .eq(UserCommentLike::getChannel, dbChannel)
                .eq(UserCommentLike::getCommentId, commentId));
        if (rows.isEmpty()) {
            return;
        }
        try {
            redisTemplate.delete(key);
            for (UserCommentLike r : rows) {
                if (r.getUserId() != null) {
                    redisTemplate.opsForSet().add(key, String.valueOf(r.getUserId()));
                }
            }
        } catch (DataAccessException e) {
            log.warn("ensureCommentLikeRedisFromTable Redis 写失败: {}", e.getMessage());
            return;
        }
        int n = rows.size();
        if (persistLikeCountOrNull != null) {
            persistLikeCountOrNull.accept(n);
        }
    }

    /**
     * 切换点赞并返回最新状态；persistCount 写入评论行 like_count。
     *
     * @param persistedLikeCountSupplier 每次在同步 Redis 后取当前评论行上的 like_count（用于孤儿分支与一致性）
     */
    public Map<String, Object> toggle(String redisPrefix, String dbChannel, Long commentId, Long userId,
                                      Supplier<Integer> persistedLikeCountSupplier,
                                      IntConsumer persistLikeCount) {
        if (commentId == null || commentId <= 0) {
            throw new BusinessException("评论 ID 无效");
        }
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (redisTemplate != null) {
            try {
                ensureCommentLikeRedisFromTable(redisPrefix, dbChannel, commentId, persistLikeCount);

                int dbCnt = persistedLikeCountSupplier.get() != null ? persistedLikeCountSupplier.get() : 0;
                String key = redisPrefix + commentId;
                Long sc = redisTemplate.opsForSet().size(key);
                int redisCnt = sc != null ? sc.intValue() : 0;
                long tableCnt = userCommentLikeMapper.selectCount(new LambdaQueryWrapper<UserCommentLike>()
                        .eq(UserCommentLike::getChannel, dbChannel)
                        .eq(UserCommentLike::getCommentId, commentId));

                boolean orphan = redisCnt == 0 && tableCnt == 0 && dbCnt > 0;
                if (orphan) {
                    redisTemplate.opsForSet().add(key, String.valueOf(userId));
                    try {
                        UserCommentLike row = new UserCommentLike();
                        row.setChannel(dbChannel);
                        row.setCommentId(commentId);
                        row.setUserId(userId);
                        userCommentLikeMapper.insert(row);
                    } catch (DuplicateKeyException e) {
                        long cnt = userCommentLikeMapper.selectCount(new LambdaQueryWrapper<UserCommentLike>()
                                .eq(UserCommentLike::getChannel, dbChannel)
                                .eq(UserCommentLike::getCommentId, commentId));
                        int ic = (int) Math.min(cnt, Integer.MAX_VALUE);
                        persistLikeCount.accept(ic);
                        redisTemplate.opsForSet().add(key, String.valueOf(userId));
                        return result(true, ic);
                    }
                    int newCnt = dbCnt + 1;
                    persistLikeCount.accept(newCnt);
                    return result(true, newCnt);
                }

                Boolean isMember = redisTemplate.opsForSet().isMember(key, String.valueOf(userId));
                boolean liked;
                if (Boolean.TRUE.equals(isMember)) {
                    redisTemplate.opsForSet().remove(key, String.valueOf(userId));
                    userCommentLikeMapper.delete(new LambdaQueryWrapper<UserCommentLike>()
                            .eq(UserCommentLike::getChannel, dbChannel)
                            .eq(UserCommentLike::getCommentId, commentId)
                            .eq(UserCommentLike::getUserId, userId));
                    liked = false;
                } else {
                    redisTemplate.opsForSet().add(key, String.valueOf(userId));
                    try {
                        UserCommentLike row = new UserCommentLike();
                        row.setChannel(dbChannel);
                        row.setCommentId(commentId);
                        row.setUserId(userId);
                        userCommentLikeMapper.insert(row);
                    } catch (DuplicateKeyException e) {
                        Long sz = redisTemplate.opsForSet().size(key);
                        int cnt = sz != null ? sz.intValue() : 0;
                        long fromTable = userCommentLikeMapper.selectCount(new LambdaQueryWrapper<UserCommentLike>()
                                .eq(UserCommentLike::getChannel, dbChannel)
                                .eq(UserCommentLike::getCommentId, commentId));
                        int ic = (int) Math.min(Math.max(cnt, fromTable), Integer.MAX_VALUE);
                        persistLikeCount.accept(ic);
                        return result(true, ic);
                    }
                    liked = true;
                }
                Long sz = redisTemplate.opsForSet().size(key);
                int cnt = sz != null ? sz.intValue() : 0;
                persistLikeCount.accept(cnt);
                return result(liked, cnt);
            } catch (DataAccessException e) {
                log.warn("Redis 不可用，评论点赞改走数据库: {}", e.getMessage());
            }
        }
        LambdaQueryWrapper<UserCommentLike> q = new LambdaQueryWrapper<UserCommentLike>()
                .eq(UserCommentLike::getChannel, dbChannel)
                .eq(UserCommentLike::getCommentId, commentId)
                .eq(UserCommentLike::getUserId, userId);
        UserCommentLike existing = userCommentLikeMapper.selectOne(q);
        boolean liked;
        if (existing != null) {
            userCommentLikeMapper.deleteById(existing.getId());
            liked = false;
        } else {
            UserCommentLike row = new UserCommentLike();
            row.setChannel(dbChannel);
            row.setCommentId(commentId);
            row.setUserId(userId);
            userCommentLikeMapper.insert(row);
            liked = true;
        }
        long cnt = userCommentLikeMapper.selectCount(new LambdaQueryWrapper<UserCommentLike>()
                .eq(UserCommentLike::getChannel, dbChannel)
                .eq(UserCommentLike::getCommentId, commentId));
        int ic = (int) Math.min(cnt, Integer.MAX_VALUE);
        persistLikeCount.accept(ic);
        return result(liked, ic);
    }

    private static Map<String, Object> result(boolean liked, int likeCount) {
        Map<String, Object> m = new HashMap<>();
        m.put("liked", liked);
        m.put("likeCount", likeCount);
        return m;
    }
}
