package com.uniondesk.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WS 端点处理：连接建立后注册 + 发 hello；上行仅接受 ping/pong 心跳；断开注销。
 */
@Component
public class RealtimeWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(RealtimeWebSocketHandler.class);

    private final RealtimeSessionRegistry registry;
    private final ObjectMapper objectMapper;

    public RealtimeWebSocketHandler(RealtimeSessionRegistry registry, ObjectMapper objectMapper) {
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        RealtimeSessionRegistry.SessionIdentity identity = identity(session);
        if (identity == null) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("unauthorized"));
            return;
        }
        registry.register(identity, session);
        RealtimeEnvelope hello = RealtimeEnvelope.of(
                "hello-" + session.getId(),
                RealtimeConstants.EVT_HELLO,
                System.currentTimeMillis(),
                Map.of("actorType", identity.actorType(), "userId", identity.userId()));
        sendJson(session, hello);
        log.info("WS 连接建立：actorType={}, userId={}, sessionId={}", identity.actorType(), identity.userId(), session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 上行仅心跳：ping/pong；其余忽略（业务一律 REST）
        try {
            Map<?, ?> frame = objectMapper.readValue(message.getPayload(), Map.class);
            String type = frame.get("type") == null ? "" : String.valueOf(frame.get("type"));
            if (RealtimeConstants.REQ_PING.equals(type)) {
                RealtimeEnvelope pong = RealtimeEnvelope.of(
                        "pong-" + session.getId(),
                        RealtimeConstants.REQ_PONG,
                        System.currentTimeMillis(),
                        Map.of());
                sendJson(session, pong);
            }
        } catch (JsonProcessingException ex) {
            log.debug("WS 上行非 JSON 帧，忽略：{}", message.getPayload());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        RealtimeSessionRegistry.SessionIdentity identity = identity(session);
        if (identity != null) {
            registry.unregister(identity, session.getId());
            log.info("WS 连接关闭：actorType={}, userId={}, sessionId={}", identity.actorType(), identity.userId(), session.getId());
        }
    }

    private RealtimeSessionRegistry.SessionIdentity identity(WebSocketSession session) {
        Object raw = session.getAttributes().get(RealtimeHandshakeInterceptor.ATTR_IDENTITY);
        return raw instanceof RealtimeSessionRegistry.SessionIdentity identity ? identity : null;
    }

    private void sendJson(WebSocketSession session, RealtimeEnvelope envelope) throws Exception {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(envelope)));
    }
}
