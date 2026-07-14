package com.uniondesk.ticket.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 新建事项类型默认空工作流。
 */
public final class DefaultStatusFlowProvider {

    private DefaultStatusFlowProvider() {
    }

    public static Map<String, Object> emptyFlow() {
        Map<String, Object> flow = new LinkedHashMap<>();
        flow.put("states", List.of());
        flow.put("transitions", List.of());
        return flow;
    }

    /** @deprecated use {@link #emptyFlow()} */
    @Deprecated
    public static Map<String, Object> defaultFlow(ObjectMapper objectMapper) {
        return emptyFlow();
    }
}
