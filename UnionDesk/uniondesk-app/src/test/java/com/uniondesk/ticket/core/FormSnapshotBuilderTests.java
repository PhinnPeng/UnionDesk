package com.uniondesk.ticket.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.ticket.entity.TicketAttributePo;
import com.uniondesk.ticket.entity.TicketTypeAttributeSlotPo;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class FormSnapshotBuilderTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void resolveLiteralDefault_parsesJsonLiteralText() {
        Object value = FormSnapshotBuilder.resolveLiteralDefault(
                "{\"mode\":\"literal\",\"value\":\"hello\"}",
                objectMapper);
        assertThat(value).isEqualTo("hello");
    }

    @Test
    void resolveLiteralDefault_parsesJsonLiteralBoolean() {
        Object value = FormSnapshotBuilder.resolveLiteralDefault(
                "{\"mode\":\"literal\",\"value\":true}",
                objectMapper);
        assertThat(value).isEqualTo(true);
    }

    @Test
    void resolveLiteralDefault_parsesJsonLiteralArray() {
        Object value = FormSnapshotBuilder.resolveLiteralDefault(
                "{\"mode\":\"literal\",\"value\":[\"a\",\"b\"]}",
                objectMapper);
        assertThat(value).isEqualTo(List.of("a", "b"));
    }

    @Test
    void resolveLiteralDefault_ignoresExpressionMode() {
        Object value = FormSnapshotBuilder.resolveLiteralDefault(
                "{\"mode\":\"expression\",\"value\":\"{{today()}}\"}",
                objectMapper);
        assertThat(value).isNull();
    }

    @Test
    void resolveLiteralDefault_fallsBackToLegacyPlainString() {
        Object value = FormSnapshotBuilder.resolveLiteralDefault("legacy", objectMapper);
        assertThat(value).isEqualTo("legacy");
    }

    @Test
    void buildFromSlotContexts_appliesLiteralDefaultForSelect() throws Exception {
        TicketAttributePo attribute = new TicketAttributePo();
        attribute.setId(10L);
        attribute.setName("优先级");
        attribute.setFieldType("select");
        attribute.setTypeConfig(objectMapper.writeValueAsString(Map.of(
                "options", List.of(
                        Map.of("label", "高", "value", "high"),
                        Map.of("label", "中", "value", "medium")
                )
        )));
        attribute.setStatus(TicketAttributePo.STATUS_ACTIVE);
        attribute.setSystem(false);

        TicketTypeAttributeSlotPo slot = new TicketTypeAttributeSlotPo();
        slot.setStatus(TicketTypeAttributeSlotPo.STATUS_ENABLED);
        slot.setSortOrder(0);
        slot.setSlotConfig(objectMapper.writeValueAsString(Map.of(
                "default_value", "{\"mode\":\"literal\",\"value\":\"medium\"}"
        )));

        Map<String, Object> schema = FormSnapshotBuilder.buildFromSlotContexts(
                "transaction",
                List.of(new FormSnapshotBuilder.SlotContext(slot, attribute)),
                objectMapper
        );
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        Map<String, Object> field = (Map<String, Object>) properties.get("attr_10");

        assertThat(field.get("default")).isEqualTo("medium");
    }

    @Test
    void buildFromSlotContexts_mapsSystemPriorityAndMemberKeys() throws Exception {
        TicketAttributePo priority = new TicketAttributePo();
        priority.setId(1L);
        priority.setName("优先级");
        priority.setFieldType("select");
        priority.setSystem(true);
        priority.setSystemKey("priority");
        priority.setStatus(TicketAttributePo.STATUS_ACTIVE);
        priority.setTypeConfig(objectMapper.writeValueAsString(Map.of("options_source", "priority_levels")));

        TicketAttributePo assignee = new TicketAttributePo();
        assignee.setId(2L);
        assignee.setName("处理人");
        assignee.setFieldType("member");
        assignee.setSystem(true);
        assignee.setSystemKey("assignee");
        assignee.setStatus(TicketAttributePo.STATUS_ACTIVE);
        assignee.setTypeConfig(objectMapper.writeValueAsString(Map.of("multiple", false, "scope_mode", "auto")));

        TicketTypeAttributeSlotPo prioritySlot = new TicketTypeAttributeSlotPo();
        prioritySlot.setStatus(TicketTypeAttributeSlotPo.STATUS_ENABLED);
        prioritySlot.setSortOrder(0);
        prioritySlot.setSlotConfig("{}");

        TicketTypeAttributeSlotPo assigneeSlot = new TicketTypeAttributeSlotPo();
        assigneeSlot.setStatus(TicketTypeAttributeSlotPo.STATUS_ENABLED);
        assigneeSlot.setSortOrder(1);
        assigneeSlot.setSlotConfig("{}");

        Map<String, Object> schema = FormSnapshotBuilder.buildFromSlotContexts(
                "transaction",
                List.of(
                        new FormSnapshotBuilder.SlotContext(prioritySlot, priority),
                        new FormSnapshotBuilder.SlotContext(assigneeSlot, assignee)
                ),
                objectMapper
        );
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertThat(properties).containsKeys("priority", "assignee");
        Map<String, Object> priorityField = (Map<String, Object>) properties.get("priority");
        Map<String, Object> assigneeField = (Map<String, Object>) properties.get("assignee");
        assertThat(priorityField.get("x-system-field")).isEqualTo(true);
        assertThat(priorityField.get("x-options-source")).isEqualTo("priority_levels");
        assertThat(assigneeField.get("x-component")).isEqualTo("MemberPicker");
    }
}
