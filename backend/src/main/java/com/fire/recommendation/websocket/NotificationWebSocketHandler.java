package com.fire.recommendation.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 站内通知 WebSocket：连接建立后按用户注册会话；具体推送由 {@link com.fire.recommendation.component.NotificationRealtimePublisher} 触发。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final NotificationWebSocketSessionRegistry registry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Object uid = session.getAttributes().get(JwtHandshakeInterceptor.ATTR_USER_ID);
        if (!(uid instanceof Long userId)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("missing user"));
            return;
        }
        registry.register(userId, session);
        try {
            session.sendMessage(new TextMessage("{\"type\":\"CONNECTED\"}"));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object uid = session.getAttributes().get(JwtHandshakeInterceptor.ATTR_USER_ID);
        if (uid instanceof Long userId) {
            registry.unregister(userId, session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 客户端可发 ping，服务端简单回应，用于保活
        String p = message.getPayload();
        if ("ping".equalsIgnoreCase(p) || "\"ping\"".equals(p)) {
            try {
                session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
            } catch (Exception ignored) {
            }
        }
    }
}
