package com.fire.recommendation.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按用户 ID 管理 WebSocket 会话，用于站内通知实时推送。
 */
@Slf4j
@Component
public class NotificationWebSocketSessionRegistry {

    private final ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    public void register(Long userId, WebSocketSession session) {
        if (userId == null || session == null) {
            return;
        }
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(Long userId, WebSocketSession session) {
        if (userId == null || session == null) {
            return;
        }
        Set<WebSocketSession> set = userSessions.get(userId);
        if (set == null) {
            return;
        }
        set.remove(session);
        if (set.isEmpty()) {
            userSessions.remove(userId);
        }
    }

    /**
     * 向该用户所有在线连接广播一条文本（通常为 JSON）。
     */
    public void sendToUser(Long userId, String jsonPayload) {
        if (userId == null || jsonPayload == null) {
            return;
        }
        Set<WebSocketSession> set = userSessions.get(userId);
        if (set == null || set.isEmpty()) {
            return;
        }
        TextMessage msg = new TextMessage(jsonPayload);
        for (WebSocketSession s : Set.copyOf(set)) {
            if (s == null || !s.isOpen()) {
                set.remove(s);
                continue;
            }
            try {
                synchronized (s) {
                    s.sendMessage(msg);
                }
            } catch (IOException e) {
                log.debug("WebSocket 推送失败 userId={}: {}", userId, e.getMessage());
                try {
                    s.close();
                } catch (IOException ignored) {
                }
                set.remove(s);
            }
        }
        if (set.isEmpty()) {
            userSessions.remove(userId);
        }
    }
}
