package com.fire.recommendation.component;

import com.fire.recommendation.websocket.NotificationWebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 新站内通知写入后向在线用户 WebSocket 推送，前端收到后刷新未读角标与列表（与轮询互补）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRealtimePublisher {

    private static final String PAYLOAD = "{\"type\":\"NOTIFICATION\",\"event\":\"NEW\"}";

    private final NotificationWebSocketSessionRegistry registry;

    public void notifyNewNotification(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            registry.sendToUser(userId, PAYLOAD);
        } catch (Exception e) {
            log.debug("通知 WebSocket 推送异常 userId={}: {}", userId, e.getMessage());
        }
    }
}
