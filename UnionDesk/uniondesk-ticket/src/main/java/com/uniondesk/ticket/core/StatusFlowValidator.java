package com.uniondesk.ticket.core;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unchecked")
public final class StatusFlowValidator {

    public static final String ANY_STATE_CODE = "*";

    private StatusFlowValidator() {
    }

    public static void validate(Object statusFlow) {
        if (!(statusFlow instanceof Map<?, ?> flowMap)) {
            throw new IllegalArgumentException("状态流格式无效");
        }
        Object statesObj = flowMap.get("states");
        if (!(statesObj instanceof List<?> states)) {
            throw new IllegalArgumentException("状态流格式无效");
        }
        // 空工作流合法
        if (states.isEmpty()) {
            return;
        }
        Set<String> codes = new HashSet<>();
        boolean hasTerminal = false;
        for (Object stateObj : states) {
            if (!(stateObj instanceof Map<?, ?> state)) {
                throw new IllegalArgumentException("状态流格式无效");
            }
            String code = stringValue(state.get("code"));
            if (ANY_STATE_CODE.equals(code)) {
                throw new IllegalArgumentException("状态编码不能为任意状态通配符");
            }
            if (!codes.add(code)) {
                throw new IllegalArgumentException("状态编码不能重复");
            }
            String stateType = stringValue(state.get("state_type"));
            if (!List.of("in_progress", "paused", "terminal").contains(stateType)) {
                throw new IllegalArgumentException("状态类型无效");
            }
            if ("terminal".equals(stateType)) {
                hasTerminal = true;
            }
            Object allowWithdraw = state.get("allow_customer_withdraw");
            if (Boolean.TRUE.equals(allowWithdraw) && !"in_progress".equals(stateType)) {
                throw new IllegalArgumentException("仅进行中的状态允许客户撤回");
            }
        }
        if (!hasTerminal) {
            throw new IllegalArgumentException("状态流至少需要一个终态");
        }
        Object transitionsObj = flowMap.get("transitions");
        List<?> transitions = transitionsObj instanceof List<?> list ? list : List.of();
        Set<String> connected = new HashSet<>();
        for (Object transitionObj : transitions) {
            if (!(transitionObj instanceof Map<?, ?> transition)) {
                throw new IllegalArgumentException("流转配置格式无效");
            }
            String from = stringValue(transition.get("from"));
            String to = stringValue(transition.get("to"));
            if (ANY_STATE_CODE.equals(to)) {
                throw new IllegalArgumentException("目标状态不能为任意状态");
            }
            if (!ANY_STATE_CODE.equals(from) && !codes.contains(from)) {
                throw new IllegalArgumentException("流转源状态不存在");
            }
            if (!codes.contains(to)) {
                throw new IllegalArgumentException("流转目标状态不存在");
            }
            if (!ANY_STATE_CODE.equals(from)) {
                connected.add(from);
            }
            connected.add(to);
        }
        for (Object stateObj : states) {
            Map<?, ?> state = (Map<?, ?>) stateObj;
            String code = stringValue(state.get("code"));
            String stateType = stringValue(state.get("state_type"));
            if ("terminal".equals(stateType)) {
                continue;
            }
            if (!connected.contains(code)) {
                throw new IllegalArgumentException("存在孤立状态：" + code);
            }
        }
    }

    private static String stringValue(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException("状态流格式无效");
        }
        return String.valueOf(value).trim();
    }
}
