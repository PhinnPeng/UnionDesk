package com.uniondesk.ticket.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.common.web.PageResult;
import com.uniondesk.ticket.entity.ClaimRulePo;
import com.uniondesk.ticket.entity.ClaimRulePolicyPo;
import com.uniondesk.ticket.entity.TicketHistoryPo;
import com.uniondesk.ticket.repository.ClaimRuleRepository;
import com.uniondesk.ticket.repository.TicketHistoryRepository;
import com.uniondesk.ticket.repository.TicketPriorityLevelRepository;
import com.uniondesk.ticket.repository.TicketRepository;
import com.uniondesk.ticket.repository.TicketTypeRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 工单领取规则：域级规则 CRUD 与客户提单后的自动领取执行（系统动作，无用户上下文）。
 * 匹配口径与 sla_rule 的 selectPolicy 同构：具体度优先、同度取 id 大。
 */
@Service
public class ClaimRuleService {

    private static final Logger log = LoggerFactory.getLogger(ClaimRuleService.class);

    public static final String STRATEGY_LEAST_LOADED = "least_loaded";
    public static final String STRATEGY_FIXED = "fixed";

    private final ClaimRuleRepository claimRuleRepository;
    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final TicketPriorityLevelRepository ticketPriorityLevelRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ClaimRuleService(
            ClaimRuleRepository claimRuleRepository,
            TicketRepository ticketRepository,
            TicketHistoryRepository ticketHistoryRepository,
            TicketTypeRepository ticketTypeRepository,
            TicketPriorityLevelRepository ticketPriorityLevelRepository,
            ObjectMapper objectMapper,
            Clock clock) {
        this.claimRuleRepository = claimRuleRepository;
        this.ticketRepository = ticketRepository;
        this.ticketHistoryRepository = ticketHistoryRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.ticketPriorityLevelRepository = ticketPriorityLevelRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResult<ClaimRuleView> listClaimRules(long businessDomainId, int page, int pageSize) {
        long total = claimRuleRepository.countRulesByDomainId(businessDomainId);
        List<ClaimRuleView> items = claimRuleRepository.findRulesByDomainId(businessDomainId, page, pageSize)
                .stream()
                .map(this::toClaimRuleView)
                .toList();
        return new PageResult<>(total, items);
    }

    @Transactional
    public ClaimRuleView createClaimRule(long businessDomainId, ClaimRuleCommand command) {
        validate(businessDomainId, command);
        ClaimRulePo po = new ClaimRulePo();
        po.setBusinessDomainId(businessDomainId);
        po.setName(normalizeText(command.name(), "领取规则"));
        po.setEnabled(command.enabled() == null || command.enabled());
        po.setMatchTicketTypeId(command.matchTicketTypeId());
        po.setMatchPriorityLevelId(command.matchPriorityLevelId());
        po.setStrategy(normalizeText(command.strategy(), STRATEGY_LEAST_LOADED));
        po.setAssigneeStaffAccountId(command.assigneeStaffAccountId());
        po.setGraceMinutes(command.graceMinutes() == null ? 0 : command.graceMinutes());
        claimRuleRepository.saveRule(po);
        return toClaimRuleView(claimRuleRepository.findRequiredRuleByIdAndDomainId(po.getId(), businessDomainId));
    }

    @Transactional
    public ClaimRuleView updateClaimRule(long businessDomainId, long ruleId, ClaimRuleCommand command) {
        ClaimRulePo po = claimRuleRepository.findRequiredRuleByIdAndDomainId(ruleId, businessDomainId);
        validate(businessDomainId, command);
        po.setName(normalizeText(command.name(), "领取规则"));
        po.setEnabled(command.enabled() == null || command.enabled());
        po.setMatchTicketTypeId(command.matchTicketTypeId());
        po.setMatchPriorityLevelId(command.matchPriorityLevelId());
        po.setStrategy(normalizeText(command.strategy(), STRATEGY_LEAST_LOADED));
        po.setAssigneeStaffAccountId(command.assigneeStaffAccountId());
        po.setGraceMinutes(command.graceMinutes() == null ? 0 : command.graceMinutes());
        claimRuleRepository.updateRule(po);
        return toClaimRuleView(claimRuleRepository.findRequiredRuleByIdAndDomainId(ruleId, businessDomainId));
    }

    @Transactional
    public void deleteClaimRule(long businessDomainId, long ruleId) {
        int updated = claimRuleRepository.deleteRuleByIdAndDomainId(ruleId, businessDomainId);
        if (updated == 0) {
            throw new IllegalArgumentException("领取规则不存在");
        }
    }

    /**
     * 自动领取（系统动作）：匹配规则 → 候选池选人 → 乐观锁领取 + 历史记录（context=null，payload auto:true）。
     * 任何一步不满足均静默跳过（保持未领取），由调用方 try-catch 包裹，失败仅日志不回滚提单。
     * 注意：须在提单事务内调用（同事务可见新建工单），本方法不做 @Transactional 声明。
     */
    public void tryAutoClaim(long businessDomainId, long ticketId, long ticketTypeId, String priority) {
        String priorityCode = StringUtils.hasText(priority) ? priority.trim() : null;
        ClaimRulePolicyPo policy = claimRuleRepository.findPolicy(businessDomainId, ticketTypeId, priorityCode);
        if (policy == null) {
            return;
        }
        Long assignee = resolveAssignee(businessDomainId, policy);
        if (assignee == null) {
            return;
        }
        Long version = ticketRepository.selectVersionByIdAndDomainId(ticketId, businessDomainId);
        if (version == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        int updated = ticketRepository.updateClaim(ticketId, businessDomainId, assignee, version, now);
        if (updated == 0) {
            log.info("自动领取跳过（工单已被领取或状态不允许）：domainId={}, ticketId={}, assignee={}",
                    businessDomainId, ticketId, assignee);
            return;
        }
        recordAutoClaimHistory(ticketId, businessDomainId, assignee);
        log.info("自动领取成功：domainId={}, ticketId={}, assignee={}, ruleId={}",
                businessDomainId, ticketId, assignee, policy.getId());
    }

    /**
     * 按策略选人：fixed 指定人须在候选池，否则跳过记日志（不回退其他策略）；least_loaded 取受理最少者。
     */
    private Long resolveAssignee(long businessDomainId, ClaimRulePolicyPo policy) {
        if (STRATEGY_FIXED.equals(policy.getStrategy())) {
            Long fixed = policy.getAssigneeStaffAccountId();
            if (fixed == null || !claimRuleRepository.isCandidate(businessDomainId, fixed)) {
                log.info("自动领取跳过（指定人不在候选池）：domainId={}, ruleId={}, assignee={}",
                        businessDomainId, policy.getId(), fixed);
                return null;
            }
            return fixed;
        }
        Long assignee = claimRuleRepository.findLeastLoadedAssignee(businessDomainId);
        if (assignee == null) {
            log.info("自动领取跳过（无候选员工）：domainId={}, ruleId={}", businessDomainId, policy.getId());
        }
        return assignee;
    }

    /**
     * 自动领取历史（系统动作）：operator 为空，payload 标记 auto。
     */
    private void recordAutoClaimHistory(long ticketId, long businessDomainId, long assignee) {
        TicketHistoryPo historyPo = new TicketHistoryPo();
        historyPo.setTicketId(ticketId);
        historyPo.setBusinessDomainId(businessDomainId);
        historyPo.setAction("claim");
        historyPo.setFromValue(null);
        historyPo.setToValue(String.valueOf(assignee));
        historyPo.setOperatorSubjectId(null);
        historyPo.setOperatorActorType(null);
        historyPo.setPayload(serializeJson(Map.of("auto", true)));
        ticketHistoryRepository.save(historyPo);
    }

    private String serializeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("invalid json payload", ex);
        }
    }

    private void validate(long businessDomainId, ClaimRuleCommand command) {
        String strategy = normalizeText(command.strategy(), STRATEGY_LEAST_LOADED);
        if (!List.of(STRATEGY_LEAST_LOADED, STRATEGY_FIXED).contains(strategy)) {
            throw new IllegalArgumentException("领取策略不合法");
        }
        if (STRATEGY_FIXED.equals(strategy)) {
            if (command.assigneeStaffAccountId() == null) {
                throw new IllegalArgumentException("指定人策略必须选择受理人");
            }
            if (!claimRuleRepository.isCandidate(businessDomainId, command.assigneeStaffAccountId())) {
                throw new IllegalArgumentException("指定人必须为域内启用状态的客服成员");
            }
        }
        if (command.matchTicketTypeId() != null
                && ticketTypeRepository.findByIdAndDomainId(command.matchTicketTypeId(), businessDomainId) == null) {
            throw new IllegalArgumentException("事项类型不属于当前域");
        }
        if (command.matchPriorityLevelId() != null
                && ticketPriorityLevelRepository.findByIdAndDomainId(command.matchPriorityLevelId(), businessDomainId) == null) {
            throw new IllegalArgumentException("优先级不属于当前域");
        }
    }

    private ClaimRuleView toClaimRuleView(ClaimRulePo po) {
        return new ClaimRuleView(
                po.getId(),
                po.getBusinessDomainId(),
                po.getName(),
                po.getEnabled() != null && po.getEnabled(),
                po.getMatchTicketTypeId(),
                po.getMatchPriorityLevelId(),
                po.getStrategy(),
                po.getAssigneeStaffAccountId(),
                po.getGraceMinutes() == null ? 0 : po.getGraceMinutes(),
                po.getCreatedAt(),
                po.getUpdatedAt());
    }

    private String normalizeText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    public record ClaimRuleCommand(
            String name,
            Boolean enabled,
            Long matchTicketTypeId,
            Long matchPriorityLevelId,
            String strategy,
            Long assigneeStaffAccountId,
            Integer graceMinutes) {
    }

    public record ClaimRuleView(
            long id,
            long businessDomainId,
            String name,
            boolean enabled,
            Long matchTicketTypeId,
            Long matchPriorityLevelId,
            String strategy,
            Long assigneeStaffAccountId,
            int graceMinutes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}
