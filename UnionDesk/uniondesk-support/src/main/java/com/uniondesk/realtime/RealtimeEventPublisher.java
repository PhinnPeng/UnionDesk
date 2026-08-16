package com.uniondesk.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

/**
 * 推送出口（多实例）：本地投递 + Redis Pub/Sub 广播（ud:rt:events）。
 * 单实例部署同样走广播路径（本实例订阅即投递），保证多实例行为一致。
 */
@Component
public class RealtimeEventPublisher implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RealtimeEventPublisher.class);

    private final RealtimeSessionRegistry registry;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RealtimeEventPublisher(RealtimeSessionRegistry registry, StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.registry = registry;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /** 定向推送：actorType+userId 的全部在线连接 */
    public void publishToUser(String actorType, long userId, RealtimeEnvelope envelope) {
        dispatch(new RealtimeDispatch(actorType, userId, null, envelope));
    }

    /** 域广播：domainId 下全部在线 staff 连接 */
    public void publishToDomainStaff(long domainId, RealtimeEnvelope envelope) {
        dispatch(new RealtimeDispatch(RealtimeConstants.ACTOR_STAFF, null, domainId, envelope));
    }

    private void dispatch(RealtimeDispatch dispatch) {
        String body;
        try {
            body = objectMapper.writeValueAsString(dispatch);
        } catch (JsonProcessingException ex) {
            log.warn("实时推送序列化失败：{}", ex.getMessage());
            return;
        }
        // 统一走 Redis Pub/Sub 广播：本实例订阅回环后投递本地连接。
        // 不做「本地先投 + 广播」双路径，避免同实例重复投递；单实例部署同样经此路径。
        redis.convertAndSend(RealtimeConstants.REDIS_CHANNEL, body);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8);
        try {
            RealtimeDispatch dispatch = objectMapper.readValue(body, RealtimeDispatch.class);
            deliver(dispatch);
        } catch (JsonProcessingException ex) {
            log.warn("实时推送广播解析失败，忽略：{}", ex.getMessage());
        }
    }

    /** 本地投递：按接收人维度查本机连接注册表（已在线才推；离线由重连拉取兜底） */
    private void deliver(RealtimeDispatch dispatch) {
        TextMessage message = new TextMessage(serializeEnvelope(dispatch.envelope()));
        int delivered;
        if (dispatch.userId() != null) {
            delivered = registry.sendToUser(dispatch.actorType(), dispatch.userId(), message);
        } else if (dispatch.domainId() != null) {
            delivered = registry.sendToDomainStaff(dispatch.domainId(), message);
        } else {
            delivered = 0;
        }
        if (delivered > 0) {
            log.debug("实时推送投递：type={}, delivered={}", dispatch.envelope().type(), delivered);
        }
    }

    private String serializeEnvelope(RealtimeEnvelope envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException ex) {
            // 理论不可达（envelope 为简单结构）
            return "{}";
        }
    }
}
