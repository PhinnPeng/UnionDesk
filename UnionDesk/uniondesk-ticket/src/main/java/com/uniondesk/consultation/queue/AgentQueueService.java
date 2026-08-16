package com.uniondesk.consultation.queue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 咨询客服在线状态与排队队列（Redis 热路径）。
 *
 * <p>在线状态：String {@code agent:online:{domainId}:{staffId}} = JSON{"status":"online","mode":...}，
 * 心跳 SETEX 90s（TTL 到期即离线）；Hash {@code agent:online:{domainId}}（field=staffId）供域内在线列表查询。
 *
 * <p>排队队列：List {@code queue:consult:{domainId}}——客户发起时无在线 auto 客服则 LPUSH 入队；
 * 客服开启自动模式时 RPOP 原子取队（多客服并发无锁竞争）。
 */
@Service
public class AgentQueueService {

    private static final Logger log = LoggerFactory.getLogger(AgentQueueService.class);

    /** 客服在线有效期（秒）：心跳间隔内的在线时长，超时自动离线 */
    public static final long ONLINE_TTL_SECONDS = 90;

    /** 客服状态：上线 */
    public static final String STATUS_ONLINE = "online";
    /** 客服状态：隐身（不参与接入，仅心跳保活） */
    public static final String STATUS_INVISIBLE = "invisible";

    /** 接入模式：自动（排队会话自动分配） */
    public static final String MODE_AUTO = "auto";
    /** 接入模式：手动（客服主动接入） */
    public static final String MODE_MANUAL = "manual";

    /** 在线状态快照（status + mode），供切换前判断 */
    public record PresenceState(String status, String mode) {
    }

    /** Redis key 前缀：客服在线状态（含接入模式）agent:online:{domainId}:{staffId} */
    public static final String KEY_PREFIX_ONLINE = "agent:online:";
    /** Redis key 前缀：域内在线客服哈希（field=staffId）agent:online:{domainId} */
    public static final String KEY_PREFIX_ONLINE_DOMAIN = "agent:online:";
    /** Redis key 前缀：咨询排队队列 queue:consult:{domainId} */
    public static final String KEY_PREFIX_QUEUE = "queue:consult:";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final StringRedisTemplate redis;

    public AgentQueueService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 客服在线心跳：写入/刷新在线状态（状态+接入模式一体），并登记到域内在线哈希 */
    public void heartbeat(long domainId, long staffId, String status, String mode) {
        String key = onlineKey(domainId, staffId);
        redis.opsForValue().set(key, buildValue(status, mode), Duration.ofSeconds(ONLINE_TTL_SECONDS));
        redis.opsForHash().put(domainOnlineKey(domainId), String.valueOf(staffId), String.valueOf(staffId));
    }

    /** 在线判定：在线状态 key 是否存在（TTL 过期即离线） */
    public boolean isOnline(long domainId, long staffId) {
        return Boolean.TRUE.equals(redis.hasKey(onlineKey(domainId, staffId)));
    }

    /** 隐身判定：key 存在且状态为隐身（离线视为非隐身） */
    public boolean isInvisible(long domainId, long staffId) {
        return STATUS_INVISIBLE.equals(parseStatus(redis.opsForValue().get(onlineKey(domainId, staffId))));
    }

    /** 读取当前在线状态（含模式）；离线返回 null */
    public PresenceState readPresence(long domainId, long staffId) {
        String value = redis.opsForValue().get(onlineKey(domainId, staffId));
        if (value == null) {
            return null;
        }
        return new PresenceState(parseStatus(value), parseMode(value));
    }

    /** 切换状态/接入模式：先校验在线（离线拒绝），再写入新状态并刷新心跳；返回切换前快照 */
    public PresenceState updatePresence(long domainId, long staffId, String status, String mode) {
        String key = onlineKey(domainId, staffId);
        if (!isOnline(domainId, staffId)) {
            throw new IllegalArgumentException("客服不在线，请先上线（心跳）再切换状态/接入模式");
        }
        PresenceState previous = readPresence(domainId, staffId);
        heartbeat(domainId, staffId, status, mode);
        return previous;
    }

    /** 域内在线且自动模式的客服 staffId 列表（HGETALL + 逐 key 查状态模式，域内量小）；隐身不参与 */
    public List<Long> listOnlineAutoStaffIds(long domainId) {
        Map<Object, Object> entries = redis.opsForHash().entries(domainOnlineKey(domainId));
        List<Long> staffIds = new ArrayList<>(entries.size());
        for (Object field : entries.keySet()) {
            Long staffId = parseStaffId(field);
            if (staffId == null) {
                continue;
            }
            String value = redis.opsForValue().get(onlineKey(domainId, staffId));
            if (value != null
                    && STATUS_ONLINE.equals(parseStatus(value))
                    && MODE_AUTO.equals(parseMode(value))) {
                staffIds.add(staffId);
            }
        }
        return staffIds;
    }

    /** 会话入队（LPUSH 队头，RPOP 队尾先出） */
    public void enqueue(long domainId, String sessionNo) {
        redis.opsForList().leftPush(queueKey(domainId), sessionNo);
    }

    /** 取队（RPOP）：返回会话编号；队列为空返回 null */
    public String poll(long domainId) {
        return redis.opsForList().rightPop(queueKey(domainId));
    }

    /** 从队列移除指定会话（清队/手动接入兜底，幂等） */
    public void removeFromQueue(long domainId, String sessionNo) {
        redis.opsForList().remove(queueKey(domainId), 0, sessionNo);
    }

    /** 域内是否有在线客服（仅统计「上线」状态；供客户侧「当前无坐席」提示，隐身客服不算坐席） */
    public boolean hasOnlineAgent(long domainId) {
        Map<Object, Object> entries = redis.opsForHash().entries(domainOnlineKey(domainId));
        for (Object field : entries.keySet()) {
            Long staffId = parseStaffId(field);
            if (staffId == null) {
                continue;
            }
            String value = redis.opsForValue().get(onlineKey(domainId, staffId));
            if (value != null && STATUS_ONLINE.equals(parseStatus(value))) {
                return true;
            }
        }
        return false;
    }

    /** 当前排队会话数（LLEN） */
    public long queueSize(long domainId) {
        Long size = redis.opsForList().size(queueKey(domainId));
        return size == null ? 0 : size;
    }

    private String onlineKey(long domainId, long staffId) {
        return KEY_PREFIX_ONLINE + domainId + ":" + staffId;
    }

    private String domainOnlineKey(long domainId) {
        return KEY_PREFIX_ONLINE_DOMAIN + domainId;
    }

    private String queueKey(long domainId) {
        return KEY_PREFIX_QUEUE + domainId;
    }

    private static String buildValue(String status, String mode) {
        return "{\"status\":\"" + status + "\",\"mode\":\"" + mode + "\"}";
    }

    private static String parseStatus(String value) {
        if (value == null) {
            return null;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(value);
            return node.path("status").asText(null);
        }
        catch (Exception ex) {
            log.warn("解析客服在线状态失败，视为离线：value={}", value);
            return null;
        }
    }

    private static String parseMode(String value) {
        if (value == null) {
            return null;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(value);
            return node.path("mode").asText(null);
        }
        catch (Exception ex) {
            log.warn("解析客服在线状态失败，视为离线：value={}", value);
            return null;
        }
    }

    private static Long parseStaffId(Object field) {
        try {
            return Long.valueOf(String.valueOf(field));
        }
        catch (NumberFormatException ex) {
            return null;
        }
    }
}
