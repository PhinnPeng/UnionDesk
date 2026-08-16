package com.uniondesk.sla.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.uniondesk.common.web.PageResult;
import com.uniondesk.sla.entity.SlaCalendarPo;
import com.uniondesk.sla.entity.SlaRulePo;
import com.uniondesk.sla.entity.SlaTicketPo;
import com.uniondesk.sla.entity.TicketSlaPolicyPo;
import com.uniondesk.sla.repository.SlaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SlaService {

    private static final int MAX_PAGE_SIZE = 100;

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
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE));
        Page<SlaCalendarPo> result =
                slaRepository.findPageByCalendars(Page.of(normalizedPage, normalizedPageSize), businessDomainId);
        return new PageResult<>(result.getTotalRow(), result.getRecords().stream().map(this::toSlaCalendarView).toList());
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
    public void deleteSlaCalendar(long businessDomainId, long calendarId) {
        int updated = slaRepository.deleteCalendarByIdAndDomainId(calendarId, businessDomainId);
        if (updated == 0) {
            throw new IllegalArgumentException("sla calendar not found");
        }
    }

    @Transactional
    public void deleteSlaRule(long businessDomainId, long ruleId) {
        int updated = slaRepository.deleteRuleByIdAndDomainId(ruleId, businessDomainId);
        if (updated == 0) {
            throw new IllegalArgumentException("sla rule not found");
        }
    }

    // --- 平台全局规则（business_domain_id IS NULL） ---

    @Transactional(readOnly = true)
    public PageResult<SlaRuleView> listGlobalSlaRules(int page, int pageSize) {
        long total = slaRepository.countGlobalRules();
        List<SlaRuleView> items = slaRepository.findGlobalRules(page, pageSize)
                .stream()
                .map(this::toSlaRuleView)
                .toList();
        return new PageResult<>(total, items);
    }

    @Transactional
    public SlaRuleView createGlobalSlaRule(SlaRuleCommand command) {
        validateGlobalRuleCommand(command);
        SlaRulePo po = new SlaRulePo();
        po.setBusinessDomainId(null);
        po.setName(normalizeText(command.name(), "SLA规则"));
        po.setTicketTypeId(null);
        po.setPriorityLevelId(null);
        po.setCalendarId(null);
        po.setFirstResponseMinutes(command.firstResponseMinutes());
        po.setResolutionMinutes(command.resolutionMinutes());
        po.setIsUrgentConfig(command.isUrgentConfig() != null && command.isUrgentConfig());
        po.setBreachActionJson(serializeMap(command.breachAction()));
        slaRepository.saveRule(po);
        return toSlaRuleView(slaRepository.findGlobalRuleById(po.getId()));
    }

    @Transactional
    public SlaRuleView updateGlobalSlaRule(long ruleId, SlaRuleCommand command) {
        validateGlobalRuleCommand(command);
        SlaRulePo po = new SlaRulePo();
        po.setId(ruleId);
        po.setName(normalizeText(command.name(), "SLA规则"));
        po.setFirstResponseMinutes(command.firstResponseMinutes());
        po.setResolutionMinutes(command.resolutionMinutes());
        po.setIsUrgentConfig(command.isUrgentConfig() != null && command.isUrgentConfig());
        po.setBreachActionJson(serializeMap(command.breachAction()));
        int updated = slaRepository.updateGlobalRule(po);
        if (updated == 0) {
            throw new IllegalArgumentException("sla rule not found");
        }
        return toSlaRuleView(slaRepository.findGlobalRuleById(ruleId));
    }

    @Transactional
    public void deleteGlobalSlaRule(long ruleId) {
        int updated = slaRepository.deleteGlobalRuleById(ruleId);
        if (updated == 0) {
            throw new IllegalArgumentException("sla rule not found");
        }
    }

    private void validateGlobalRuleCommand(SlaRuleCommand command) {
        if (command.ticketTypeId() != null) {
            throw new IllegalArgumentException("全局 SLA 规则不允许关联事项类型");
        }
        if (command.priorityLevelId() != null) {
            throw new IllegalArgumentException("全局 SLA 规则不允许关联优先级");
        }
        if (command.calendarId() != null) {
            throw new IllegalArgumentException("全局 SLA 规则不允许关联工作日历");
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

        // 终态保护：stopped 为唯一最终态，直接返回（不覆盖、不违约）
        if ("stopped".equals(snapshot.getSlaStatus())) {
            return new SlaBreachDecision(false, false, snapshot.getPriority(), "stopped", List.of());
        }

        boolean firstResponseBreached = snapshot.getSlaFirstResponseDeadline() != null
                && snapshot.getSlaFirstRespondedAt() == null
                && now.isAfter(snapshot.getSlaFirstResponseDeadline());
        boolean resolutionBreached = snapshot.getSlaResolutionDeadline() != null
                && snapshot.getSlaResolvedAt() == null
                && now.isAfter(snapshot.getSlaResolutionDeadline());

        // 违约条件全消除：自愈为 tracking（首响后违约消除等场景）
        if (!firstResponseBreached && !resolutionBreached) {
            if (!"tracking".equals(snapshot.getSlaStatus())) {
                slaRepository.updateSlaStatus(ticketId, businessDomainId, "tracking");
            }
            return new SlaBreachDecision(false, false, snapshot.getPriority(), "tracking", List.of());
        }

        Map<String, Object> breachAction = snapshot.getBreachActionJson() == null
                ? Map.of()
                : parseMap(snapshot.getBreachActionJson());
        String nextPriority = snapshot.getPriority();
        // 置状态：旧键 sla_status 兼容覆盖，缺省 breached
        String nextStatus = readText(breachAction.get("sla_status"), "breached");

        // 原子认领动作：受影响行数 = 1 才是认领者；认领者执行动作与升级，非认领者只翻状态
        boolean claimed = slaRepository.claimBreachAction(ticketId) == 1;
        List<BreachAction> pendingActions = List.of();
        if (claimed) {
            // 旧键 raise_priority_to（绝对目标）并存时优先于按序升级
            Object raiseTo = breachAction.get("raise_priority_to");
            if (raiseTo != null && StringUtils.hasText(String.valueOf(raiseTo))) {
                nextPriority = String.valueOf(raiseTo);
            } else if (Boolean.TRUE.equals(breachAction.get("escalate_priority"))) {
                nextPriority = escalatePriority(businessDomainId, snapshot.getPriority());
            }
            pendingActions = buildPendingActions(breachAction);
            slaRepository.updatePriorityAndSlaStatus(nextPriority, nextStatus, ticketId);
        } else {
            slaRepository.updateSlaStatus(ticketId, businessDomainId, nextStatus);
        }

        return new SlaBreachDecision(true, firstResponseBreached, nextPriority, nextStatus, pendingActions);
    }

    /**
     * 按序升级：域优先级按 sort_order 升序（小=更紧急），当前 code 对应行取更紧急的下一行；已是最高级或未匹配则不动。
     */
    private String escalatePriority(long businessDomainId, String currentCode) {
        if (!StringUtils.hasText(currentCode)) {
            return currentCode;
        }
        List<String> codes = slaRepository.findActivePriorityCodes(businessDomainId);
        int index = codes.indexOf(currentCode);
        if (index <= 0) {
            return currentCode;
        }
        return codes.get(index - 1);
    }

    private List<BreachAction> buildPendingActions(Map<String, Object> breachAction) {
        List<BreachAction> actions = new ArrayList<>();
        Object assignValue = breachAction.get("assign_to_staff_account_id");
        Long assignee = parseLong(assignValue);
        if (assignee != null) {
            actions.add(new AssignAction(assignee));
        }
        Object watchersValue = breachAction.get("add_watcher_staff_account_ids");
        if (watchersValue instanceof List<?> list) {
            List<Long> watcherIds = list.stream()
                    .map(this::parseLong)
                    .filter(Objects::nonNull)
                    .toList();
            if (!watcherIds.isEmpty()) {
                actions.add(new AddWatchersAction(watcherIds));
            }
        }
        return List.copyOf(actions);
    }

    private String readText(Object value, String defaultValue) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return defaultValue;
        }
        return String.valueOf(value);
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private TicketSlaPolicy loadPolicy(long businessDomainId, long ticketId, long ticketTypeId) {
        String priorityCode = slaRepository.findTicketPriority(ticketId);
        if (priorityCode == null) {
            priorityCode = "";
        }
        // ① 域内规则（事项 SLA 优先）：类型+优先级精确 > 仅类型 > 仅优先级 > 域默认
        TicketSlaPolicyPo policyPo = slaRepository.findPolicy(businessDomainId, ticketTypeId, priorityCode);
        if (policyPo == null) {
            // ② 全局默认规则兜底
            policyPo = slaRepository.findGlobalPolicy();
        }
        if (policyPo == null) {
            // ③ 未配置 → 不设 SLA
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
            String nextStatus,
            List<BreachAction> pendingActions) {

        public SlaBreachDecision {
            pendingActions = pendingActions == null ? List.of() : List.copyOf(pendingActions);
        }

        public SlaBreachDecision(
                boolean breached, boolean firstResponseBreached, String nextPriority, String nextStatus) {
            this(breached, firstResponseBreached, nextPriority, nextStatus, List.of());
        }
    }

    /** 违约待执行动作清单：由 ticket 模块执行（换处理人 → 加关注人） */
    public sealed interface BreachAction permits AssignAction, AddWatchersAction {
    }

    /** 更换处理人（超时强制指派） */
    public record AssignAction(long staffAccountId) implements BreachAction {
    }

    /** 添加关注人（追加语义） */
    public record AddWatchersAction(List<Long> staffAccountIds) implements BreachAction {

        public AddWatchersAction {
            staffAccountIds = List.copyOf(staffAccountIds);
        }
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
            Long businessDomainId,
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
