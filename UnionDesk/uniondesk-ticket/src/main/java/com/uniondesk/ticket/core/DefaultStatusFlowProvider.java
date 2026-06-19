package com.uniondesk.ticket.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DefaultStatusFlowProvider {

    private static final String DEFAULT_FLOW_JSON = """
            {
              "states": [
                { "code": "pending", "name": "待处理", "state_type": "in_progress", "allow_customer_withdraw": true, "is_resolved": false },
                { "code": "processing", "name": "处理中", "state_type": "in_progress", "allow_customer_withdraw": false, "is_resolved": false },
                { "code": "closed", "name": "已关闭", "state_type": "terminal", "allow_customer_withdraw": false, "is_resolved": false }
              ],
              "transitions": [
                { "from": "pending", "to": "processing" },
                { "from": "processing", "to": "closed" }
              ]
            }
            """;

    private DefaultStatusFlowProvider() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> defaultFlow(ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(DEFAULT_FLOW_JSON, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("invalid default status flow", ex);
        }
    }
}
