package com.fire.recommendation.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 用户端详情浏览量去重：同模块、同资源、同一访客在时间窗口内多次打开（含刷新）只应计 1 次。
 * 未配置 Redis 时不做去重（仍可通过作者/管理员排除等非去重规则控制口径）。
 */
@Slf4j
@Component
public class DetailViewDedupHelper {

    private static final String PREFIX = "app:detail:view:";
    private static final Duration TTL = Duration.ofMinutes(30);

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    /**
     * @param module   如 forum、news、knowledge
     * @param userId   已登录用户 ID；未登录传 null，此时用 clientIp 区分访客
     * @param clientIp {@link com.fire.recommendation.util.HttpClientIp#resolve} 的返回值
     * @return true 表示本次应对库里的 view_count 执行 +1
     */
    public boolean shouldIncrement(String module, long entityId, Long userId, String clientIp) {
        if (redisTemplate == null) {
            return true;
        }
        String safeMod = module != null && !module.isEmpty() ? module : "x";
        String viewerKey;
        if (userId != null) {
            viewerKey = "u:" + userId;
        } else {
            String ip = clientIp != null ? clientIp.trim() : "";
            if (ip.isEmpty()) {
                ip = "_na_";
            }
            viewerKey = "ip:" + ip.replace(':', '_');
        }
        String key = PREFIX + safeMod + ":" + entityId + ":" + viewerKey;
        try {
            Boolean first = redisTemplate.opsForValue().setIfAbsent(key, "1", TTL);
            return Boolean.TRUE.equals(first);
        } catch (DataAccessException e) {
            log.warn("详情浏览去重 Redis 不可用，本次仍计浏览: {}", e.getMessage());
            return true;
        }
    }
}
