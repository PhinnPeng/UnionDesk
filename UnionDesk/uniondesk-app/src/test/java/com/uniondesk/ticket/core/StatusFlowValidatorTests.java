package com.uniondesk.ticket.core;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StatusFlowValidatorTests {

    @Test
    void acceptsEmptyFlow() {
        Map<String, Object> flow = new LinkedHashMap<>();
        flow.put("states", List.of());
        flow.put("transitions", List.of());
        flow.put("initial_state_code", null);

        assertThatCode(() -> StatusFlowValidator.validate(flow)).doesNotThrowAnyException();
    }

    @Test
    void rejectsEmptyFlowWithInitial() {
        Map<String, Object> flow = Map.of(
                "states", List.of(),
                "transitions", List.of(),
                "initial_state_code", "pending");

        assertThatThrownBy(() -> StatusFlowValidator.validate(flow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("空工作流不能指定初始状态");
    }

    @Test
    void rejectsFlowWithoutTerminal() {
        Map<String, Object> flow = Map.of(
                "states", List.of(Map.of(
                        "code", "pending",
                        "name", "待处理",
                        "state_type", "in_progress",
                        "allow_customer_withdraw", true)),
                "transitions", List.of(),
                "initial_state_code", "pending");

        assertThatThrownBy(() -> StatusFlowValidator.validate(flow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("终态");
    }

    @Test
    void rejectsNonEmptyFlowWithoutInitial() {
        Map<String, Object> flow = Map.of(
                "states", List.of(
                        Map.of(
                                "code", "pending",
                                "name", "待处理",
                                "state_type", "in_progress",
                                "allow_customer_withdraw", true),
                        Map.of(
                                "code", "closed",
                                "name", "已关闭",
                                "state_type", "terminal",
                                "allow_customer_withdraw", false)),
                "transitions", List.of(Map.of("from", "pending", "to", "closed")));

        assertThatThrownBy(() -> StatusFlowValidator.validate(flow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("请指定初始状态");
    }

    @Test
    void rejectsInitialNotInStates() {
        Map<String, Object> flow = Map.of(
                "states", List.of(
                        Map.of(
                                "code", "pending",
                                "name", "待处理",
                                "state_type", "in_progress",
                                "allow_customer_withdraw", true),
                        Map.of(
                                "code", "closed",
                                "name", "已关闭",
                                "state_type", "terminal",
                                "allow_customer_withdraw", false)),
                "transitions", List.of(Map.of("from", "pending", "to", "closed")),
                "initial_state_code", "missing");

        assertThatThrownBy(() -> StatusFlowValidator.validate(flow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("初始状态不存在于工作流中");
    }

    @Test
    void acceptsMinimalValidFlow() {
        Map<String, Object> flow = Map.of(
                "states", List.of(
                        Map.of(
                                "code", "pending",
                                "name", "待处理",
                                "state_type", "in_progress",
                                "allow_customer_withdraw", true),
                        Map.of(
                                "code", "closed",
                                "name", "已关闭",
                                "state_type", "terminal",
                                "allow_customer_withdraw", false)),
                "transitions", List.of(Map.of("from", "pending", "to", "closed")),
                "initial_state_code", "pending");

        StatusFlowValidator.validate(flow);
    }

    @Test
    void acceptsWildcardFromAnyState() {
        Map<String, Object> flow = Map.of(
                "states", List.of(
                        Map.of(
                                "code", "pending",
                                "name", "待处理",
                                "state_type", "in_progress",
                                "allow_customer_withdraw", true),
                        Map.of(
                                "code", "closed",
                                "name", "已关闭",
                                "state_type", "terminal",
                                "allow_customer_withdraw", false)),
                "transitions", List.of(
                        Map.of("from", "pending", "to", "closed"),
                        Map.of("from", "*", "to", "closed")),
                "initial_state_code", "pending");

        assertThatCode(() -> StatusFlowValidator.validate(flow)).doesNotThrowAnyException();
    }

    @Test
    void rejectsWildcardAsTarget() {
        Map<String, Object> flow = Map.of(
                "states", List.of(
                        Map.of(
                                "code", "pending",
                                "name", "待处理",
                                "state_type", "in_progress",
                                "allow_customer_withdraw", true),
                        Map.of(
                                "code", "closed",
                                "name", "已关闭",
                                "state_type", "terminal",
                                "allow_customer_withdraw", false)),
                "transitions", List.of(Map.of("from", "pending", "to", "*")),
                "initial_state_code", "pending");

        assertThatThrownBy(() -> StatusFlowValidator.validate(flow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("目标状态不能为任意状态");
    }

    @Test
    void acceptsIsolatedNonTerminalStates() {
        Map<String, Object> flow = Map.of(
                "states", List.of(
                        Map.of(
                                "code", "pending",
                                "name", "待处理",
                                "state_type", "in_progress",
                                "allow_customer_withdraw", true),
                        Map.of(
                                "code", "paused",
                                "name", "暂停",
                                "state_type", "paused",
                                "allow_customer_withdraw", false),
                        Map.of(
                                "code", "closed",
                                "name", "已关闭",
                                "state_type", "terminal",
                                "allow_customer_withdraw", false)),
                "transitions", List.of(),
                "initial_state_code", "pending");

        assertThatCode(() -> StatusFlowValidator.validate(flow)).doesNotThrowAnyException();
    }
}
