package com.uniondesk.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/**
 * 实时推送信封帧（下行 JSON：{v, id, type, ts, payload}）。
 */
public record RealtimeEnvelope(
        int v,
        String id,
        String type,
        long ts,
        Map<String, Object> payload) {

    public static RealtimeEnvelope of(String id, String type, long ts, Map<String, Object> payload) {
        return new RealtimeEnvelope(1, id, type, ts, payload == null ? Map.of() : payload);
    }
}
