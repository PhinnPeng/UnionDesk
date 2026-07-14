package com.uniondesk.ticket.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.ticket.entity.TicketTypeFlowStatusPo;
import com.uniondesk.ticket.entity.TicketTypeFlowTransitionPo;
import com.uniondesk.ticket.entity.TicketTypePo;
import com.uniondesk.ticket.repository.TicketTypeFlowStatusRepository;
import com.uniondesk.ticket.repository.TicketTypeFlowTransitionRepository;
import com.uniondesk.ticket.repository.TicketTypeRepository;
import com.uniondesk.ticket.web.TicketConfigDtos;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TicketTypeFlowService {

    private final TicketTypeFlowStatusRepository statusRepository;
    private final TicketTypeFlowTransitionRepository transitionRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final ObjectMapper objectMapper;

    public TicketTypeFlowService(
            TicketTypeFlowStatusRepository statusRepository,
            TicketTypeFlowTransitionRepository transitionRepository,
            TicketTypeRepository ticketTypeRepository,
            ObjectMapper objectMapper) {
        this.statusRepository = statusRepository;
        this.transitionRepository = transitionRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.objectMapper = objectMapper;
    }

    public TicketConfigDtos.WorkflowConfigView loadAssembled(long domainId, long ticketTypeId) {
        List<TicketTypeFlowStatusPo> statuses = statusRepository.findByDomainIdAndTypeId(domainId, ticketTypeId);
        List<TicketTypeFlowTransitionPo> transitions = transitionRepository.findByDomainIdAndTypeId(domainId, ticketTypeId);
        return new TicketConfigDtos.WorkflowConfigView(toStatusFlow(statuses, transitions), toRuleViews(transitions));
    }

    public Object loadStatusFlow(long domainId, long ticketTypeId) {
        return loadAssembled(domainId, ticketTypeId).status_flow();
    }

    public List<TicketConfigDtos.TransitionRuleView> loadRules(long domainId, long ticketTypeId) {
        return loadAssembled(domainId, ticketTypeId).transition_rules();
    }

    @Transactional
    public void replaceAll(
            long domainId,
            long ticketTypeId,
            Object statusFlow,
            List<TicketConfigDtos.SaveTransitionRuleRequest> rules) {
        requireTicketType(domainId, ticketTypeId);
        StatusFlowValidator.validate(statusFlow == null ? DefaultStatusFlowProvider.emptyFlow() : statusFlow);

        List<TicketTypeFlowStatusPo> statusPos = toStatusPos(domainId, ticketTypeId, statusFlow);
        List<TicketTypeFlowTransitionPo> transitionPos = rules != null
                ? toTransitionPosFromRules(domainId, ticketTypeId, rules)
                : toTransitionPosFromFlow(domainId, ticketTypeId, statusFlow);

        // 用边回写 transitions，保证组装一致性，并再校验一次图
        StatusFlowValidator.validate(toStatusFlow(statusPos, transitionPos));

        statusRepository.deleteByDomainIdAndTypeId(domainId, ticketTypeId);
        transitionRepository.deleteByDomainIdAndTypeId(domainId, ticketTypeId);
        statusRepository.batchInsert(statusPos);
        transitionRepository.batchInsert(transitionPos);
    }

    @Transactional
    public void deleteByDomainIdAndTypeId(long domainId, long ticketTypeId) {
        transitionRepository.deleteByDomainIdAndTypeId(domainId, ticketTypeId);
        statusRepository.deleteByDomainIdAndTypeId(domainId, ticketTypeId);
    }

    @Transactional
    public void deleteTransitionsByStateCode(long domainId, long ticketTypeId, String stateCode) {
        transitionRepository.deleteByDomainIdAndTypeIdAndStateCode(domainId, ticketTypeId, stateCode);
    }

    private void requireTicketType(long domainId, long ticketTypeId) {
        if (domainId == 0L) {
            ticketTypeRepository.findRequiredPlatformById(ticketTypeId);
            return;
        }
        ticketTypeRepository.findRequiredByIdAndDomainId(ticketTypeId, domainId);
    }

    private List<TicketTypeFlowStatusPo> toStatusPos(long domainId, long ticketTypeId, Object statusFlow) {
        if (!(statusFlow instanceof Map<?, ?> flowMap)) {
            return List.of();
        }
        Object statesObj = flowMap.get("states");
        if (!(statesObj instanceof List<?> states) || states.isEmpty()) {
            return List.of();
        }
        List<TicketTypeFlowStatusPo> result = new ArrayList<>();
        int index = 0;
        for (Object stateObj : states) {
            if (!(stateObj instanceof Map<?, ?> state)) {
                continue;
            }
            String code = stringValue(state.get("code"));
            TicketTypeFlowStatusPo po = new TicketTypeFlowStatusPo();
            po.setDomainId(domainId);
            po.setTicketTypeId(ticketTypeId);
            po.setStateCode(code);
            po.setName(stringOrDefault(state.get("name"), code));
            po.setStateType(stringValue(state.get("state_type")));
            po.setAllowCustomerWithdraw(Boolean.TRUE.equals(state.get("allow_customer_withdraw")));
            po.setResolved(Boolean.TRUE.equals(state.get("is_resolved")));
            po.setSortOrder(index++);
            Object sourceId = state.get("source_status_id");
            if (sourceId instanceof Number number) {
                po.setSourceStatusId(number.longValue());
            }
            result.add(po);
        }
        return result;
    }

    private List<TicketTypeFlowTransitionPo> toTransitionPosFromRules(
            long domainId,
            long ticketTypeId,
            List<TicketConfigDtos.SaveTransitionRuleRequest> rules) {
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        List<TicketTypeFlowTransitionPo> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        int index = 0;
        for (TicketConfigDtos.SaveTransitionRuleRequest rule : rules) {
            String from = trimRequired(rule.from_state_code(), "from_state_code");
            String to = trimRequired(rule.to_state_code(), "to_state_code");
            String key = from + "->" + to;
            if (!seen.add(key)) {
                continue;
            }
            TicketTypeFlowTransitionPo po = new TicketTypeFlowTransitionPo();
            po.setDomainId(domainId);
            po.setTicketTypeId(ticketTypeId);
            po.setFromStateCode(from);
            po.setToStateCode(to);
            po.setStepName(StringUtils.hasText(rule.step_name()) ? rule.step_name().trim() : to);
            po.setPermissionMode(rule.permission_mode() != null
                    ? rule.permission_mode()
                    : TicketTypeFlowTransitionPo.PERMISSION_MODE_NONE);
            po.setMemberIdsJson(toJson(rule.member_ids()));
            po.setRoleIdsJson(toJson(rule.role_ids()));
            po.setRequiredSlotIdsJson(toJson(rule.required_slot_ids()));
            po.setAttributeUpdatesJson(toJson(rule.attribute_updates()));
            po.setSortOrder(index++);
            result.add(po);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<TicketTypeFlowTransitionPo> toTransitionPosFromFlow(
            long domainId,
            long ticketTypeId,
            Object statusFlow) {
        if (!(statusFlow instanceof Map<?, ?> flowMap)) {
            return List.of();
        }
        Object transitionsObj = flowMap.get("transitions");
        if (!(transitionsObj instanceof List<?> transitions) || transitions.isEmpty()) {
            return List.of();
        }
        List<TicketTypeFlowTransitionPo> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        int index = 0;
        for (Object transitionObj : transitions) {
            if (!(transitionObj instanceof Map<?, ?> transition)) {
                continue;
            }
            String from = stringValue(transition.get("from"));
            String to = stringValue(transition.get("to"));
            String key = from + "->" + to;
            if (!seen.add(key)) {
                continue;
            }
            TicketTypeFlowTransitionPo po = new TicketTypeFlowTransitionPo();
            po.setDomainId(domainId);
            po.setTicketTypeId(ticketTypeId);
            po.setFromStateCode(from);
            po.setToStateCode(to);
            Object label = transition.get("label");
            po.setStepName(label != null && StringUtils.hasText(String.valueOf(label))
                    ? String.valueOf(label).trim()
                    : to);
            po.setPermissionMode(TicketTypeFlowTransitionPo.PERMISSION_MODE_NONE);
            po.setMemberIdsJson("[]");
            po.setRoleIdsJson("[]");
            po.setRequiredSlotIdsJson("[]");
            po.setAttributeUpdatesJson("[]");
            po.setSortOrder(index++);
            result.add(po);
        }
        return result;
    }

    private Map<String, Object> toStatusFlow(
            List<TicketTypeFlowStatusPo> statuses,
            List<TicketTypeFlowTransitionPo> transitions) {
        Map<String, Object> flow = new LinkedHashMap<>();
        List<Map<String, Object>> stateMaps = new ArrayList<>();
        for (TicketTypeFlowStatusPo status : statuses) {
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("code", status.getStateCode());
            state.put("name", status.getName());
            state.put("state_type", status.getStateType());
            state.put("allow_customer_withdraw", status.isAllowCustomerWithdraw());
            state.put("is_resolved", status.isResolved());
            stateMaps.add(state);
        }
        List<Map<String, Object>> transitionMaps = new ArrayList<>();
        for (TicketTypeFlowTransitionPo transition : transitions) {
            Map<String, Object> edge = new LinkedHashMap<>();
            edge.put("from", transition.getFromStateCode());
            edge.put("to", transition.getToStateCode());
            transitionMaps.add(edge);
        }
        flow.put("states", stateMaps);
        flow.put("transitions", transitionMaps);
        return flow;
    }

    private List<TicketConfigDtos.TransitionRuleView> toRuleViews(List<TicketTypeFlowTransitionPo> transitions) {
        return transitions.stream().map(this::toRuleView).toList();
    }

    private TicketConfigDtos.TransitionRuleView toRuleView(TicketTypeFlowTransitionPo po) {
        return new TicketConfigDtos.TransitionRuleView(
                String.valueOf(po.getId()),
                po.getFromStateCode(),
                po.getToStateCode(),
                po.getStepName(),
                po.getPermissionMode(),
                parseLongList(po.getMemberIdsJson()),
                parseLongList(po.getRoleIdsJson()),
                parseStringList(po.getRequiredSlotIdsJson()),
                parseAttributeUpdates(po.getAttributeUpdatesJson()));
    }

    private String toJson(Object value) {
        if (value == null) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JSON", e);
        }
    }

    private List<Long> parseLongList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {
            });
        }
        catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        }
        catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private List<TicketConfigDtos.AttributeUpdateItemView> parseAttributeUpdates(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
            return list.stream().map(m -> new TicketConfigDtos.AttributeUpdateItemView(
                    (String) m.get("slot_id"),
                    m.get("value"),
                    (String) m.get("value_type")
            )).toList();
        }
        catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private static String stringValue(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException("状态流格式无效");
        }
        return String.valueOf(value).trim();
    }

    private static String stringOrDefault(Object value, String defaultValue) {
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        return String.valueOf(value).trim();
    }

    private static String trimRequired(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(field + " required");
        }
        return value.trim();
    }

    /** Resolve domain_id used by flow tables for a ticket type row. */
    public static long resolveDomainId(TicketTypePo po) {
        if (po == null || TicketTypePo.SCOPE_PLATFORM.equals(po.getScope()) || po.getBusinessDomainId() == null) {
            return 0L;
        }
        return po.getBusinessDomainId();
    }
}
