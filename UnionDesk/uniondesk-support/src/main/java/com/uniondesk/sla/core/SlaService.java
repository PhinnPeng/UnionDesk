package com.uniondesk.sla.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.uniondesk.common.web.PageResult;
import com.uniondesk.sla.entity.SlaCalendarPo;
import com.uniondesk.sla.entity.SlaConfigPo;
import com.uniondesk.sla.entity.SlaRulePo;
import com.uniondesk.sla.entity.SlaTicketPo;
import com.uniondesk.sla.entity.TicketSlaPolicyPo;
import com.uniondesk.sla.repository.SlaRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
        LocalDateTime createdAt = slaRepository.findCreatedAtById(ticketId, businessDomainId);
        Map<String, Object> calendar = policy.calendarJson() == null
                ? Map.of()
                : parseMap(policy.calendarJson());
        LocalDateTime firstResponseDeadline = policy.firstResponseMinutes() == null
                ? null
                : plusWorkingMinutes(createdAt, policy.firstResponseMinutes(), calendar);
        LocalDateTime resolutionDeadline = policy.resolutionMinutes() == null
                ? null
                : plusWorkingMinutes(createdAt, policy.resolutionMinutes(), calendar);
        slaRepository.updateSlaDeadlines(ticketId, firstResponseDeadline, resolutionDeadline);
    }

    // --- 域 SLA 配置（每域一行，ADR-005） ---

    @Transactional(readOnly = true)
    public SlaConfigView getSlaConfig(long businessDomainId) {
        SlaConfigPo po = slaRepository.findSlaConfigByDomainId(businessDomainId);
        return po == null ? null : toSlaConfigView(po);
    }

    @Transactional
    public SlaConfigView saveSlaConfig(long businessDomainId, SlaConfigCommand command) {
        SlaConfigPo po = new SlaConfigPo();
        po.setBusinessDomainId(businessDomainId);
        po.setFirstResponseMinutes(command.firstResponseMinutes());
        po.setResolutionMinutes(command.resolutionMinutes());
        po.setBreachActionJson(serializeMap(normalizeBreachAction(command.breachAction())));
        po.setCalendarJson(serializeMap(command.calendar()));
        int updated = slaRepository.updateSlaConfigByDomainId(po);
        if (updated == 0) {
            slaRepository.saveSlaConfig(po);
        }
        return toSlaConfigView(slaRepository.findSlaConfigByDomainId(businessDomainId));
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
            } else {
                // escalate_priority：兼容旧 bool（true=按序升下一档）与对象形态 {enabled, to_priority_level_id, to_priority_code}
                Object escalate = breachAction.get("escalate_priority");
                if (Boolean.TRUE.equals(escalate)) {
                    nextPriority = escalatePriority(businessDomainId, snapshot.getPriority());
                } else if (escalate instanceof Map<?, ?> escalateMap
                        && Boolean.TRUE.equals(escalateMap.get("enabled"))) {
                    nextPriority = resolveEscalateTarget(businessDomainId, snapshot.getPriority(), escalateMap);
                }
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

    /**
     * 解析升级目标档：保存时已把 to_priority_level_id 对应 code 持久化为 to_priority_code，执行时优先直接用；缺省按序升下一档。
     */
    private String resolveEscalateTarget(long businessDomainId, String currentCode, Map<?, ?> escalateMap) {
        Object codeValue = escalateMap.get("to_priority_code");
        if (codeValue != null && StringUtils.hasText(String.valueOf(codeValue))) {
            return String.valueOf(codeValue);
        }
        return escalatePriority(businessDomainId, currentCode);
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
        // ① 优先读域内单份 sla_config（ADR-005）：存在且至少一项时限非空则直接使用，不再走旧规则链
        SlaConfigPo configPo = slaRepository.findSlaConfigByDomainId(businessDomainId);
        if (configPo != null
                && (configPo.getFirstResponseMinutes() != null || configPo.getResolutionMinutes() != null)) {
            return new TicketSlaPolicy(
                    configPo.getFirstResponseMinutes(),
                    configPo.getResolutionMinutes(),
                    configPo.getBreachActionJson(),
                    configPo.getCalendarJson());
        }
        // ② 旧规则链兜底（兼容回滚）：域内规则（事项 SLA 优先）> 全局默认规则
        String priorityCode = slaRepository.findTicketPriority(ticketId);
        if (priorityCode == null) {
            priorityCode = "";
        }
        TicketSlaPolicyPo policyPo = slaRepository.findPolicy(businessDomainId, ticketTypeId, priorityCode);
        if (policyPo == null) {
            // ③ 全局默认规则兜底
            policyPo = slaRepository.findGlobalPolicy();
        }
        if (policyPo == null) {
            // ④ 未配置 → 不设 SLA
            return new TicketSlaPolicy(null, null, null, null);
        }
        return new TicketSlaPolicy(
                policyPo.getFirstResponseMinutes(),
                policyPo.getResolutionMinutes(),
                policyPo.getBreachActionJson(),
                null);
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

    private SlaConfigView toSlaConfigView(SlaConfigPo po) {
        return new SlaConfigView(
                po.getBusinessDomainId(),
                po.getFirstResponseMinutes(),
                po.getResolutionMinutes(),
                po.getBreachActionJson() == null ? Map.of() : parseMap(po.getBreachActionJson()),
                po.getCalendarJson() == null ? Map.of() : parseMap(po.getCalendarJson()),
                po.getUpdatedAt());
    }

    /**
     * 超时动作规范化：escalate_priority 对象形态下，校验 to_priority_level_id 属于该域优先级，
     * 并把对应 code 持久化为 to_priority_code（执行时优先读 code，免二次查询）。
     */
    private Map<String, Object> normalizeBreachAction(Map<String, Object> breachAction) {
        if (breachAction == null) {
            return null;
        }
        Map<String, Object> normalized = new LinkedHashMap<>(breachAction);
        Object escalate = normalized.get("escalate_priority");
        if (escalate instanceof Map<?, ?> escalateMap) {
            Map<String, Object> normalizedEscalate = new LinkedHashMap<>();
            escalateMap.forEach((key, value) -> normalizedEscalate.put(String.valueOf(key), value));
            Long priorityLevelId = parseLong(normalizedEscalate.get("to_priority_level_id"));
            if (priorityLevelId != null) {
                String code = slaRepository.findPriorityCodeById(priorityLevelId);
                if (code == null) {
                    throw new IllegalArgumentException("目标优先级不存在");
                }
                normalizedEscalate.put("to_priority_code", code);
            }
            normalized.put("escalate_priority", normalizedEscalate);
        }
        return normalized;
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

    /**
     * 工作分钟折算：把 minutes 从 from 起按 calendar 折算为工作分钟后的时间点。
     * calendar 为空（或分钟为空）时按自然分钟累加，等价旧 SQL TIMESTAMPADD(MINUTE, minutes, created_at)。
     */
    private LocalDateTime plusWorkingMinutes(LocalDateTime from, Integer minutes, Map<String, Object> calendar) {
        if (minutes == null) {
            return from;
        }
        if (calendar == null || calendar.isEmpty()) {
            return from.plusMinutes(minutes);
        }
        List<Integer> workingDays = parseWorkingDays(calendar.get("working_days"));
        boolean weekendWork = parseWeekendWork(calendar.get("weekend_work"));
        Set<LocalDate> holidays = parseHolidays(calendar.get("holidays"));
        LocalDateTime cursor = from;
        int remaining = minutes;
        while (remaining > 0) {
            if (isWorkingDay(cursor.toLocalDate(), workingDays, weekendWork, holidays)) {
                remaining--;
                cursor = cursor.plusMinutes(1);
            } else {
                // 非工作日整天不计时，直接跳到次日零点
                cursor = cursor.toLocalDate().plusDays(1).atStartOfDay();
            }
        }
        return cursor;
    }

    private boolean isWorkingDay(LocalDate date, List<Integer> workingDays, boolean weekendWork, Set<LocalDate> holidays) {
        if (!workingDays.contains(date.getDayOfWeek().getValue())) {
            return false;
        }
        if (holidays.contains(date)) {
            return false;
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        boolean weekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        return !weekend || weekendWork;
    }

    private List<Integer> parseWorkingDays(Object value) {
        List<Integer> days = new ArrayList<>(List.of(1, 2, 3, 4, 5));
        if (value instanceof List<?> list) {
            List<Integer> parsed = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Number number) {
                    int day = number.intValue();
                    if (day >= 1 && day <= 7) {
                        parsed.add(day);
                    }
                }
            }
            if (!parsed.isEmpty()) {
                return parsed;
            }
        }
        return days;
    }

    private boolean parseWeekendWork(Object value) {
        return Boolean.TRUE.equals(value);
    }

    private Set<LocalDate> parseHolidays(Object value) {
        Set<LocalDate> holidays = new HashSet<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                try {
                    holidays.add(LocalDate.parse(String.valueOf(item)));
                } catch (DateTimeParseException ex) {
                    // 无法解析的日期项忽略
                }
            }
        }
        return holidays;
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

    public record SlaConfigView(
            long businessDomainId,
            Integer firstResponseMinutes,
            Integer resolutionMinutes,
            Map<String, Object> breachAction,
            Map<String, Object> calendar,
            LocalDateTime updatedAt) {
    }

    public record SlaConfigCommand(
            Integer firstResponseMinutes,
            Integer resolutionMinutes,
            Map<String, Object> breachAction,
            Map<String, Object> calendar) {
    }

    private record TicketSlaPolicy(
            Integer firstResponseMinutes,
            Integer resolutionMinutes,
            String breachActionJson,
            String calendarJson) {
    }
}
