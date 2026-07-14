package com.uniondesk.ticket.core;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StatusFlowValidatorTests {

    @Test
    void acceptsEmptyFlow() {
        Map<String, Object> flow = Map.of(
                "states", List.of(),
                "transitions", List.of());

        assertThatCode(() -> StatusFlowValidator.validate(flow)).doesNotThrowAnyException();
    }

    @Test
    void rejectsFlowWithoutTerminal() {
        Map<String, Object> flow = Map.of(
                "states", List.of(Map.of(
                        "code", "pending",
                        "name", "待处理",
                        "state_type", "in_progress",
                        "allow_customer_withdraw", true)),
                "transitions", List.of());

        assertThatThrownBy(() -> StatusFlowValidator.validate(flow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("终态");
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
                "transitions", List.of(Map.of("from", "pending", "to", "closed")));

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
                        Map.of("from", "*", "to", "closed")));

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
                "transitions", List.of(Map.of("from", "pending", "to", "*")));

        assertThatThrownBy(() -> StatusFlowValidator.validate(flow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("目标状态不能为任意状态");
    }
}
