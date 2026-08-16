package com.uniondesk.realtime;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * 连接注册表：actorType+userId → 连接集合（多标签页 fan-out）；domainId → staff 连接集合（域广播）。
 * 并发安全（ConcurrentHashMap + 同步发送）；单用户连接数上限，超限关最旧。
 */
@Component
public class RealtimeSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(RealtimeSessionRegistry.class);

    private final Map<String, Map<Long, Map<String, WebSocketSession>>> byUser = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, WebSocketSession>> staffByDomain = new ConcurrentHashMap<>();

    public record SessionIdentity(String actorType, long userId, Long domainId) {
    }

    /** 注册连接；返回是否达到上限被拒绝（超出 MAX_SESSIONS_PER_USER 时关闭最旧并注册新连接） */
    public void register(SessionIdentity identity, WebSocketSession session) {
        Map<String, WebSocketSession> userSessions = byUser
                .computeIfAbsent(identity.actorType(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(identity.userId(), k -> new ConcurrentHashMap<>());
        if (userSessions.size() >= RealtimeConstants.MAX_SESSIONS_PER_USER) {
            // 关最旧（按注册序近似：取首个），然后继续注册新连接
            String oldestKey = userSessions.keySet().iterator().next();
            WebSocketSession oldest = userSessions.remove(oldestKey);
            if (oldest != null) {
                closeQuietly(oldest, "连接数超限");
            }
        }
        userSessions.put(session.getId(), session);
        if (identity.actorType().equals(RealtimeConstants.ACTOR_STAFF) && identity.domainId() != null) {
            staffByDomain
                    .computeIfAbsent(identity.domainId(), k -> new ConcurrentHashMap<>())
                    .put(session.getId(), session);
        }
    }

    public void unregister(SessionIdentity identity, String sessionId) {
        Map<Long, Map<String, WebSocketSession>> byActor = byUser.get(identity.actorType());
        if (byActor != null) {
            Map<String, WebSocketSession> userSessions = byActor.get(identity.userId());
            if (userSessions != null) {
                userSessions.remove(sessionId);
                if (userSessions.isEmpty()) {
                    byActor.remove(identity.userId());
                }
            }
        }
        if (identity.actorType().equals(RealtimeConstants.ACTOR_STAFF) && identity.domainId() != null) {
            Map<String, WebSocketSession> domainSessions = staffByDomain.get(identity.domainId());
            if (domainSessions != null) {
                domainSessions.remove(sessionId);
                if (domainSessions.isEmpty()) {
                    staffByDomain.remove(identity.domainId());
                }
            }
        }
    }

    /** 定向推送：actorType+userId 的全部在线连接 */
    public int sendToUser(String actorType, long userId, TextMessage message) {
        Map<Long, Map<String, WebSocketSession>> byActor = byUser.get(actorType);
        if (byActor == null) {
            return 0;
        }
        Map<String, WebSocketSession> sessions = byActor.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return 0;
        }
        return sendAll(sessions.values(), message);
    }

    /** 域广播：domainId 下全部在线 staff 连接 */
    public int sendToDomainStaff(long domainId, TextMessage message) {
        Map<String, WebSocketSession> sessions = staffByDomain.get(domainId);
        if (sessions == null || sessions.isEmpty()) {
            return 0;
        }
        return sendAll(sessions.values(), message);
    }

    private int sendAll(Collection<WebSocketSession> sessions, TextMessage message) {
        int delivered = 0;
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(message);
                }
                delivered++;
            } catch (IOException ex) {
                log.debug("实时推送发送失败（连接可能已断开）：sessionId={}", session.getId());
            }
        }
        return delivered;
    }

    private void closeQuietly(WebSocketSession session, String reason) {
        try {
            session.close(org.springframework.web.socket.CloseStatus.POLICY_VIOLATION.withReason(reason));
        } catch (IOException ex) {
            log.debug("关闭旧连接失败：sessionId={}", session.getId());
        }
    }
}
