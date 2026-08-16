package com.uniondesk.realtime;

/**
 * 推送指令：接收人维度（actorType+userId 定向，或 domainId 域广播）+ 信封。
 * 经 Redis Pub/Sub 广播（多实例），各实例按本机连接注册表投递。
 */
public record RealtimeDispatch(
        String actorType,
        Long userId,
        Long domainId,
        RealtimeEnvelope envelope) {

    public static RealtimeDispatch toUser(String actorType, long userId, RealtimeEnvelope envelope) {
        return new RealtimeDispatch(actorType, userId, null, envelope);
    }

    public static RealtimeDispatch toDomain(long domainId, RealtimeEnvelope envelope) {
        return new RealtimeDispatch(RealtimeConstants.ACTOR_STAFF, null, domainId, envelope);
    }
}
