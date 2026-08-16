package com.uniondesk.realtime;

/**
 * 实时通道常量：事件类型、actor 维度、Redis 频道。
 */
public final class RealtimeConstants {

    private RealtimeConstants() {
    }

    /** Redis Pub/Sub 频道：实时推送广播（多实例） */
    public static final String REDIS_CHANNEL = "ud:rt:events";

    /** 身份维度 */
    public static final String ACTOR_CUSTOMER = "customer";
    public static final String ACTOR_STAFF = "staff";

    /** 下行事件类型 */
    public static final String EVT_HELLO = "hello";
    public static final String EVT_CHAT_MESSAGE = "chat.message";
    public static final String EVT_CHAT_SESSION = "chat.session";
    public static final String EVT_CHAT_QUEUE = "chat.queue";
    public static final String EVT_TICKET_CREATED = "ticket.created";
    public static final String EVT_TICKET_REPLIED = "ticket.replied";
    public static final String EVT_TICKET_UPDATED = "ticket.updated";
    public static final String EVT_INBOX_NEW = "inbox.new";

    /** 上行帧类型（仅心跳） */
    public static final String REQ_PING = "ping";
    public static final String REQ_PONG = "pong";

    /** 心跳：客户端 30s ping；服务端 90s 无帧判定失联 */
    public static final long PING_INTERVAL_SECONDS = 30;
    public static final long SESSION_IDLE_TIMEOUT_SECONDS = 90;

    /** 单用户最大连接数（多标签页），超限关最旧 */
    public static final int MAX_SESSIONS_PER_USER = 5;
}
