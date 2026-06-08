package com.uniondesk.sla.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.common.web.PageResult;
import com.uniondesk.sla.entity.SlaCalendarPo;
import com.uniondesk.sla.entity.SlaRulePo;
import com.uniondesk.sla.entity.SlaTicketPo;
import com.uniondesk.sla.entity.TicketSlaPolicyPo;
import com.uniondesk.sla.repository.SlaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SlaService {

    private final SlaRepository slaRepository;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public SlaService(SlaRepository slaRepository, Clock clock, ObjectMapper objectMapper) {
        this.slaRepository = slaRepository;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void applyOnCreate(long businessDomainId, long ticketId, long ticketTypeId) {
        TicketSlaPolicy policy = loadPolicy(businessDomainId, ticketId, ticketTypeId);
        slaRepository.updateSlaDeadlines(
                ticketId,
                policy.firstResponseMinutes(),
                policy.resolutionMinutes());
    }

    @Transactional(readOnly = true)
    public PageResult<SlaRuleView> listSlaRules(long businessDomainId, int page, int pageSize) {
        long total = slaRepository.countRulesByDomainId(businessDomainId);
        List<SlaRuleView> items = slaRepository.findRulesByDomainId(businessDomainId, page, pageSize)
                .stream()
                .map(this::toSlaRuleView)
                .toList();
        return new PageResult<>(total, items);
    }

    @Transactional
    public SlaRuleView createSlaRule(long businessDomainId, SlaRuleCommand command) {
        SlaRulePo po = new SlaRulePo();
        po.setBusinessDomainId(businessDomainId);
        po.setName(normalizeText(command.name(), "SLA规则"));
        po.setTicketTypeId(command.ticketTypeId());
        po.setPriorityLevelId(command.priorityLevelId());
        po.setCalendarId(command.calendarId());
        po.setFirstResponseMinutes(command.firstResponseMinutes());
        po.setResolutionMinutes(command.resolutionMinutes());
        po.setIsUrgentConfig(command.isUrgentConfig() != null && command.isUrgentConfig());
        po.setBreachActionJson(serializeMap(command.breachAction()));
        slaRepository.saveRule(po);
        return toSlaRuleView(slaRepository.findRuleByIdAndDomainId(po.getId(), businessDomainId));
    }

    @Transactional
    public SlaRuleView updateSlaRule(long businessDomainId, long ruleId, SlaRuleCommand command) {
        SlaRulePo po = new SlaRulePo();
        po.setId(ruleId);
        po.setBusinessDomainId(businessDomainId);
        po.setName(normalizeText(command.name(), "SLA规则"));
        po.setTicketTypeId(command.ticketTypeId());
        po.setPriorityLevelId(command.priorityLevelId());
        po.setCalendarId(command.calendarId());
        po.setFirstResponseMinutes(command.firstResponseMinutes());
        po.setResolutionMinutes(command.resolutionMinutes());
        po.setIsUrgentConfig(command.isUrgentConfig() != null && command.isUrgentConfig());
        po.setBreachActionJson(serializeMap(command.breachAction()));
        slaRepository.updateRule(po);
        return toSlaRuleView(slaRepository.findRuleByIdAndDomainId(ruleId, businessDomainId));
    }

    @Transactional(readOnly = true)
    public PageResult<SlaCalendarView> listSlaCalendars(long businessDomainId, int page, int pageSize) {
        long total = slaRepository.countCalendarsByDomainId(businessDomainId);
        List<SlaCalendarView> items = slaRepository.findCalendarsByDomainId(businessDomainId, page, pageSize)
                .stream()
                .map(this::toSlaCalendarView)
                .toList();
        return new PageResult<>(total, items);
    }

    @Transactional
    public SlaCalendarView createSlaCalendar(long businessDomainId, SlaCalendarCommand command) {
        SlaCalendarPo po = new SlaCalendarPo();
        po.setBusinessDomainId(businessDomainId);
        po.setName(normalizeText(command.name(), "SLA工作日历"));
        po.setConfig(serializeMap(command.config()));
        slaRepository.saveCalendar(po);
        return toSlaCalendarView(slaRepository.findCalendarByIdAndDomainId(po.getId(), businessDomainId));
    }

    @Transactional
    public SlaCalendarView updateSlaCalendar(long businessDomainId, long calendarId, SlaCalendarCommand command) {
        SlaCalendarPo po = new SlaCalendarPo();
        po.setId(calendarId);
        po.setBusinessDomainId(businessDomainId);
        po.setName(normalizeText(command.name(), "SLA工作日历"));
        po.setConfig(serializeMap(command.config()));
        slaRepository.updateCalendar(po);
        return toSlaCalendarView(slaRepository.findCalendarByIdAndDomainId(calendarId, businessDomainId));
    }

    @Transactional
    public void deleteSlaRule(long businessDomainId, long ruleId) {
        int updated = slaRepository.deleteRuleByIdAndDomainId(ruleId, businessDomainId);
        if (updated == 0) {
            throw new IllegalArgumentException("sla rule not found");
        }
    }

    @Transactional
    public void recordFirstResponse(long businessDomainId, long ticketId) {
        LocalDateTime now = LocalDateTime.now(clock);
        slaRepository.updateFirstResponse(now, ticketId, businessDomainId);
    }

    @Transactional
    public void recordResolution(long businessDomainId, long ticketId) {
        LocalDateTime now = LocalDateTime.now(clock);
        slaRepository.updateResolution(now, ticketId, businessDomainId);
    }

    @Transactional
    public SlaBreachDecision evaluateTicket(long businessDomainId, long ticketId) {
        SlaTicketPo snapshot = slaRepository.findSlaSnapshot(ticketId, businessDomainId);
        LocalDateTime now = LocalDateTime.now(clock);
        boolean firstResponseBreached = snapshot.getSlaFirstResponseDeadline() != null
                && snapshot.getSlaFirstRespondedAt() == null
                && now.isAfter(snapshot.getSlaFirstResponseDeadline());
        boolean resolutionBreached = snapshot.getSlaResolutionDeadline() != null
                && snapshot.getSlaResolvedAt() == null
                && now.isAfter(snapshot.getSlaResolutionDeadline());
        if (!firstResponseBreached && !resolutionBreached) {
            return new SlaBreachDecision(false, false, snapshot.getPriority(), snapshot.getSlaStatus());
        }

        String nextPriority = snapshot.getPriority();
        String nextStatus = "breached";
        Map<String, Object> breachAction = snapshot.getBreachActionJson() == null
                ? Map.of()
                : parseMap(snapshot.getBreachActionJson());

        if (firstResponseBreached || resolutionBreached) {
            Object priorityValue = breachAction.get("raise_priority_to");
            if (priorityValue != null && String.valueOf(priorityValue).isBlank() == false) {
                nextPriority = String.valueOf(priorityValue);
            }
            Object statusValue = breachAction.get("sla_status");
            if (statusValue != null && String.valueOf(statusValue).isBlank() == false) {
                nextStatus = String.valueOf(statusValue);
            }
        }

        slaRepository.updatePriorityAndSlaStatus(nextPriority, nextStatus, ticketId);

        return new SlaBreachDecision(true, firstResponseBreached, nextPriority, nextStatus);
    }

    private TicketSlaPolicy loadPolicy(long businessDomainId, long ticketId, long ticketTypeId) {
        String priorityCode = slaRepository.findTicketPriority(ticketId);
        if (priorityCode == null) {
            priorityCode = "";
        }
        TicketSlaPolicyPo policyPo = slaRepository.findPolicy(businessDomainId, ticketTypeId, priorityCode);
        if (policyPo == null) {
            return new TicketSlaPolicy(null, null, null);
        }
        return new TicketSlaPolicy(
                policyPo.getFirstResponseMinutes(),
                policyPo.getResolutionMinutes(),
                policyPo.getBreachActionJson());
    }

    private SlaRuleView toSlaRuleView(SlaRulePo po) {
        return new SlaRuleView(
                po.getId(),
                po.getBusinessDomainId(),
                po.getName(),
                po.getTicketTypeId(),
                po.getPriorityLevelId(),
                po.getCalendarId(),
                po.getFirstResponseMinutes(),
                po.getResolutionMinutes(),
                po.getIsUrgentConfig() != null && po.getIsUrgentConfig(),
                po.getBreachActionJson() == null ? Map.of() : parseMap(po.getBreachActionJson()),
                po.getCreatedAt(),
                po.getUpdatedAt());
    }

    private SlaCalendarView toSlaCalendarView(SlaCalendarPo po) {
        return new SlaCalendarView(
                po.getId(),
                po.getBusinessDomainId(),
                po.getName(),
                po.getConfig() == null ? Map.of() : parseMap(po.getConfig()),
                po.getCreatedAt(),
                po.getUpdatedAt());
    }

    private String normalizeText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String serializeMap(Map<String, Object> value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("invalid json payload", ex);
        }
    }

    private Map<String, Object> parseMap(String json) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            return new LinkedHashMap<>(map);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    public record SlaBreachDecision(
            boolean breached,
            boolean firstResponseBreached,
            String nextPriority,
            String nextStatus) {
    }

    public record SlaRuleCommand(
            String name,
            Long ticketTypeId,
            Long priorityLevelId,
            Long calendarId,
            Integer firstResponseMinutes,
            Integer resolutionMinutes,
            Boolean isUrgentConfig,
            Map<String, Object> breachAction) {
    }

    public record SlaRuleView(
            long id,
            long businessDomainId,
            String name,
            Long ticketTypeId,
            Long priorityLevelId,
            Long calendarId,
            Integer firstResponseMinutes,
            Integer resolutionMinutes,
            boolean isUrgentConfig,
            Map<String, Object> breachAction,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record SlaCalendarCommand(
            String name,
            Map<String, Object> config) {
    }

    public record SlaCalendarView(
            long id,
            long businessDomainId,
            String name,
            Map<String, Object> config,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    private record TicketSlaPolicy(Integer firstResponseMinutes, Integer resolutionMinutes, String breachActionJson) {
    }
}
