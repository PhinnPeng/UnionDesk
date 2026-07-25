package com.uniondesk.ticket.core;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

@SuppressWarnings("unchecked")
public final class StatusFlowValidator {

    public static final String ANY_STATE_CODE = "*";
    public static final String INITIAL_STATE_CODE_KEY = "initial_state_code";

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
        Object initialObj = flowMap.get(INITIAL_STATE_CODE_KEY);
        String initialStateCode = initialObj == null ? null : String.valueOf(initialObj).trim();
        if (initialStateCode != null && initialStateCode.isEmpty()) {
            initialStateCode = null;
        }

        // 空工作流合法，且不得带初始状态
        if (states.isEmpty()) {
            if (StringUtils.hasText(initialStateCode)) {
                throw new IllegalArgumentException("空工作流不能指定初始状态");
            }
            return;
        }

        if (!StringUtils.hasText(initialStateCode)) {
            throw new IllegalArgumentException("请指定初始状态");
        }
        if (ANY_STATE_CODE.equals(initialStateCode)) {
            throw new IllegalArgumentException("初始状态不能为任意状态通配符");
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
        if (!codes.contains(initialStateCode)) {
            throw new IllegalArgumentException("初始状态不存在于工作流中");
        }
        if (!hasTerminal) {
            throw new IllegalArgumentException("状态流至少需要一个终态");
        }
        Object transitionsObj = flowMap.get("transitions");
        List<?> transitions = transitionsObj instanceof List<?> list ? list : List.of();
        // 允许孤立状态（未参与任何边）；连通性留给业务流转时再约束
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
        }
    }

    private static String stringValue(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException("状态流格式无效");
        }
        return String.valueOf(value).trim();
    }
}
