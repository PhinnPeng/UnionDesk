package com.uniondesk.consultation.core;

import com.uniondesk.auth.core.UserContext;
import com.uniondesk.common.event.ConsultationMessageSentEvent;
import com.uniondesk.common.event.ConsultationQueuedEvent;
import com.uniondesk.common.event.ConsultationSessionChangedEvent;
import com.uniondesk.common.event.UnionDeskEventPublisher;
import com.uniondesk.common.web.ErrorCodes;
import com.uniondesk.common.web.PageResult;
import com.uniondesk.consultation.entity.ConsultationMessagePo;
import com.uniondesk.consultation.entity.ConsultationSessionPo;
import com.uniondesk.consultation.queue.AgentQueueService;
import com.uniondesk.consultation.repository.ConsultationRepository;
import com.uniondesk.domain.repository.DomainCustomerRepository;
import com.uniondesk.ticket.core.TicketService;
import com.uniondesk.ticket.entity.TicketTypePo;
import com.uniondesk.ticket.repository.TicketRepository;
import com.uniondesk.ticket.repository.TicketTypeRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConsultationService {

    public static final String SESSION_STATUS_QUEUED = "queued";
    public static final String SESSION_STATUS_OPEN = "open";
    public static final String SESSION_STATUS_CLOSED = "closed";
    public static final String SENDER_ROLE_CUSTOMER = "customer";
    public static final String SENDER_ROLE_AGENT = "agent";

    /** 客服消息可撤回时限（发送后 2 分钟内） */
    private static final Duration RETRACT_WINDOW = Duration.ofMinutes(2);

    private static final DateTimeFormatter SESSION_NO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 会话编号生成并发冲突最大重试次数（MAX+1 撞唯一索引后重新取号） */
    private static final int SESSION_NO_CREATE_RETRY_MAX = 3;

    private static final Logger log = LoggerFactory.getLogger(ConsultationService.class);

    public record ConsultationSessionRow(
            long id,
            String sessionNo,
            long businessDomainId,
            String businessDomainName,
            long customerId,
            String sessionStatus,
            Long assignedTo,
            String linkedTicketNo,
            LocalDateTime lastMessageAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            long messageCount,
            LocalDateTime archivedAt) {
    }

    public record ConsultationMessageRow(
            long id,
            String sessionNo,
            int seqNo,
            long businessDomainId,
            String senderRole,
            String messageType,
            String content,
            String payloadJson,
            LocalDateTime createdAt,
            boolean retracted) {
    }

    public record ConsultationConvertResult(
            ConsultationSessionRow session,
            long ticketId,
            String ticketNo) {
    }

    /** 客服状态 + 接入模式一体结果（status 为 null 表示无在线记录/离线） */
    public record AgentPresenceResult(String status, String mode) {
    }

    private final ConsultationRepository consultationRepository;
    private final TicketService ticketService;
    private final TicketTypeRepository ticketTypeRepository;
    private final TicketRepository ticketRepository;
    private final DomainCustomerRepository domainCustomerRepository;
    private final AgentQueueService agentQueueService;
    private final UnionDeskEventPublisher eventPublisher;
    private final Clock clock;

    public ConsultationService(
            ConsultationRepository consultationRepository,
            TicketService ticketService,
            TicketTypeRepository ticketTypeRepository,
            TicketRepository ticketRepository,
            DomainCustomerRepository domainCustomerRepository,
            AgentQueueService agentQueueService,
            UnionDeskEventPublisher eventPublisher,
            Clock clock) {
        this.consultationRepository = consultationRepository;
        this.ticketService = ticketService;
        this.ticketTypeRepository = ticketTypeRepository;
        this.ticketRepository = ticketRepository;
        this.domainCustomerRepository = domainCustomerRepository;
        this.agentQueueService = agentQueueService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /** 客户侧坐席可用性（「当前无坐席」提示）：域内是否有在线客服 + 当前排队数 */
    public AvailabilityResult availability(long domainId) {
        return new AvailabilityResult(agentQueueService.hasOnlineAgent(domainId), agentQueueService.queueSize(domainId));
    }

    public record AvailabilityResult(boolean hasOnlineAgent, long queueSize) {
    }

    @Transactional
    public ConsultationSessionRow createSession(UserContext context, long domainId, String content) {
        requireCustomer(context);
        ensureCustomerInDomain(context.userId(), domainId);
        LocalDateTime now = LocalDateTime.now(clock);

        ConsultationSessionPo session = new ConsultationSessionPo();
        session.setBusinessDomainId(domainId);
        session.setCustomerId(context.userId());
        session.setSessionStatus(SESSION_STATUS_QUEUED);
        session.setLastMessageAt(now);
        insertSessionWithRetry(domainId, session);

        // 快速路径：域内有在线自动模式客服则 least_loaded 分配；否则入 Redis 队列（会话保持 queued）
        assignOrEnqueue(domainId, session.getSessionNo(), session.getId());

        ConsultationMessagePo message = new ConsultationMessagePo();
        message.setConsultationSessionId(session.getId());
        message.setBusinessDomainId(domainId);
        message.setSeqNo(1);
        message.setSenderUserId(context.userId());
        message.setSenderRole(SENDER_ROLE_CUSTOMER);
        message.setMessageType("text");
        message.setContent(content);
        consultationRepository.saveMessage(message);

        publishQueueChanged(domainId);
        return toSessionRow(consultationRepository.findBySessionNoAndDomain(session.getSessionNo(), domainId));
    }

    public List<ConsultationSessionRow> listMySessions(UserContext context, long domainId) {
        requireCustomer(context);
        return consultationRepository.findByCustomerId(domainId, context.userId())
                .stream()
                .map(this::toSessionRow)
                .toList();
    }

    public PageResult<ConsultationSessionRow> listAdminSessions(
            UserContext context, long domainId, int page, int pageSize, String status, boolean assignedToMe, Boolean archived) {
        requireStaff(context);
        Long assignedTo = assignedToMe ? context.userId() : null;
        long offset = (long) (page - 1) * pageSize;
        List<ConsultationSessionRow> rows = consultationRepository.findPageByDomain(domainId, status, assignedTo, archived, pageSize, offset)
                .stream()
                .map(this::toSessionRow)
                .toList();
        return new PageResult<>(consultationRepository.countByDomain(domainId, status, assignedTo, archived), rows);
    }

    public List<ConsultationMessageRow> listMessagesOwned(UserContext context, long domainId, String sessionNo) {
        requireCustomer(context);
        ConsultationSessionPo session = requireSession(domainId, sessionNo);
        requireOwner(session, context.userId());
        return listMessages(session);
    }

    public List<ConsultationMessageRow> listAdminMessages(UserContext context, long domainId, String sessionNo) {
        requireStaff(context);
        return listMessages(requireSession(domainId, sessionNo));
    }

    @Transactional
    public ConsultationMessageRow sendMessageOwned(UserContext context, long domainId, String sessionNo, String content) {
        requireCustomer(context);
        ConsultationSessionPo session = requireSession(domainId, sessionNo);
        requireOwner(session, context.userId());
        return sendMessage(session, context.userId(), SENDER_ROLE_CUSTOMER, content);
    }

    @Transactional
    public ConsultationMessageRow replyAdmin(UserContext context, long domainId, String sessionNo, String content) {
        requireStaff(context);
        ConsultationSessionPo session = requireSession(domainId, sessionNo);
        ConsultationMessageRow row = sendMessage(session, context.userId(), SENDER_ROLE_AGENT, content);
        // 首次回复隐式认领：未分配会话（含排队态）接入并置 open，同时移出排队队列
        if (session.getAssignedTo() == null) {
            consultationRepository.assignSessionIfUnassigned(session.getId(), context.userId());
            agentQueueService.removeFromQueue(domainId, sessionNo);
        }
        return row;
    }

    /**
     * 客服接入（领取）会话：queued 或 open 且未分配可接；已分配他人拒绝。
     */
    @Transactional
    public ConsultationSessionRow claimSession(UserContext context, long domainId, String sessionNo) {
        requireStaff(context);
        if (agentQueueService.isInvisible(domainId, context.userId())) {
            throw new IllegalArgumentException("隐身状态无法接入会话，请先上线");
        }
        ConsultationSessionPo session = requireSession(domainId, sessionNo);
        if (SESSION_STATUS_CLOSED.equals(session.getSessionStatus())) {
            throw new IllegalArgumentException("会话已结束，无法接入");
        }
        if (session.getAssignedTo() != null) {
            if (session.getAssignedTo() == context.userId()) {
                return toSessionRow(session);
            }
            throw new IllegalArgumentException("会话已被其他客服接入");
        }
        int updated = consultationRepository.assignSessionIfUnassigned(session.getId(), context.userId());
        if (updated == 0) {
            throw new IllegalArgumentException("会话已被其他客服接入，请刷新后重试");
        }
        // 手动接入后从排队队列移除，避免后续自动取队重复分配
        agentQueueService.removeFromQueue(domainId, sessionNo);
        ConsultationSessionPo latest = consultationRepository.findBySessionNoAndDomain(sessionNo, domainId);
        publishSessionChanged(latest);
        publishQueueChanged(domainId);
        return toSessionRow(latest);
    }

    /**
     * 客服结束会话：open/queued 均可结束（排队中结束即清队）；closed/已转单重复结束拒绝。
     */
    @Transactional
    public ConsultationSessionRow endSession(UserContext context, long domainId, String sessionNo, String reason) {
        requireStaff(context);
        ConsultationSessionPo session = requireSession(domainId, sessionNo);
        if (SESSION_STATUS_CLOSED.equals(session.getSessionStatus())) {
            throw new IllegalArgumentException("会话已结束，请勿重复操作");
        }
        if (StringUtils.hasText(reason)) {
            log.info("咨询会话结束原因：domainId={}, sessionNo={}, reason={}, endedBy={}", domainId, sessionNo, reason, context.userId());
        }
        consultationRepository.closeSession(session.getId(), LocalDateTime.now(clock));
        agentQueueService.removeFromQueue(domainId, sessionNo);
        ConsultationSessionPo latest = consultationRepository.findBySessionNoAndDomain(sessionNo, domainId);
        publishSessionChanged(latest);
        publishQueueChanged(domainId);
        return toSessionRow(latest);
    }

    /**
     * 归档会话：仅已关闭会话可归档（终态标记，状态不变）；重复归档幂等返回。
     */
    @Transactional
    public ConsultationSessionRow archiveSession(UserContext context, long domainId, String sessionNo) {
        requireStaff(context);
        ConsultationSessionPo session = requireSession(domainId, sessionNo);
        if (!SESSION_STATUS_CLOSED.equals(session.getSessionStatus())) {
            throw new IllegalArgumentException("仅已结束的会话可归档");
        }
        if (session.getArchivedAt() == null) {
            consultationRepository.updateArchived(session.getId(), LocalDateTime.now(clock));
        }
        return toSessionRow(consultationRepository.findBySessionNoAndDomain(sessionNo, domainId));
    }

    /**
     * 取消归档：仅已归档会话可恢复；恢复后回到默认列表（仍为已关闭）。
     */
    @Transactional
    public ConsultationSessionRow unarchiveSession(UserContext context, long domainId, String sessionNo) {
        requireStaff(context);
        ConsultationSessionPo session = requireSession(domainId, sessionNo);
        if (session.getArchivedAt() == null) {
            throw new IllegalArgumentException("会话未归档，无需取消");
        }
        consultationRepository.updateArchived(session.getId(), null);
        return toSessionRow(consultationRepository.findBySessionNoAndDomain(sessionNo, domainId));
    }

    /**
     * 客服撤回本人消息：2 分钟内、agent 角色、当前会话内；撤回后消息带 retracted 标记。
     */
    @Transactional
    public ConsultationMessageRow retractMessage(UserContext context, long domainId, String sessionNo, long messageId) {
        requireStaff(context);
        ConsultationSessionPo session = requireSession(domainId, sessionNo);
        ConsultationMessagePo message = consultationRepository.findMessageByIdAndSession(messageId, session.getId());
        if (message == null) {
            throw new IllegalArgumentException("消息不存在或不属于当前会话");
        }
        if (!SENDER_ROLE_AGENT.equals(message.getSenderRole())) {
            throw new IllegalArgumentException("仅客服消息可撤回");
        }
        if (message.getSenderUserId() == null || message.getSenderUserId() != context.userId()) {
            throw new IllegalArgumentException("仅可撤回本人发送的消息");
        }
        if (message.getRetractedAt() != null) {
            throw new IllegalArgumentException("消息已撤回，请勿重复操作");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (message.getCreatedAt().isBefore(now.minus(RETRACT_WINDOW))) {
            throw new IllegalArgumentException("消息发送超过2分钟，无法撤回");
        }
        consultationRepository.updateMessageRetracted(messageId, context.userId(), now);
        return toMessageRow(consultationRepository.findMessageByIdAndSession(messageId, session.getId()));
    }

    /**
     * 客服状态 + 接入模式一体：心跳注册/刷新在线；仅当「进入 上线+自动」且此前非该组合时
     * RPOP 原子取队并分配排队会话（首次上线 auto、隐身切回上线 auto 均触发）。
     */
    public AgentPresenceResult agentPresence(UserContext context, long domainId, String status, String mode) {
        requireStaff(context);
        if (!AgentQueueService.STATUS_ONLINE.equals(status)
                && !AgentQueueService.STATUS_INVISIBLE.equals(status)) {
            throw new IllegalArgumentException("客服状态参数错误（仅支持 online/invisible）");
        }
        if (!AgentQueueService.MODE_AUTO.equals(mode) && !AgentQueueService.MODE_MANUAL.equals(mode)) {
            throw new IllegalArgumentException("接入模式参数错误（仅支持 auto/manual）");
        }
        // updatePresence 内部先读旧快照再写新状态并刷新心跳；此处不得先 heartbeat 覆盖旧值，
        // 否则 manual→auto 转换判断恒 false，自动拉取永不触发
        AgentQueueService.PresenceState previous = agentQueueService.updatePresence(
                domainId, context.userId(), status, mode);
        boolean nowOnlineAuto = AgentQueueService.STATUS_ONLINE.equals(status)
                && AgentQueueService.MODE_AUTO.equals(mode);
        boolean beforeOnlineAuto = previous != null
                && AgentQueueService.STATUS_ONLINE.equals(previous.status())
                && AgentQueueService.MODE_AUTO.equals(previous.mode());
        if (nowOnlineAuto && !beforeOnlineAuto) {
            assignFromQueue(domainId, context.userId());
        }
        return new AgentPresenceResult(status, mode);
    }

    /** 查询当前客服状态（只读）：无在线记录返回 status/mode 均为 null（前端据此决定默认上报） */
    public AgentPresenceResult getAgentPresence(UserContext context, long domainId) {
        requireStaff(context);
        AgentQueueService.PresenceState state = agentQueueService.readPresence(domainId, context.userId());
        return state == null ? new AgentPresenceResult(null, null) : new AgentPresenceResult(state.status(), state.mode());
    }

    @Transactional
    public ConsultationConvertResult convertToTicket(UserContext context, long domainId, String sessionNo, ConvertTicketRequest request) {
        requireStaff(context);
        ConsultationSessionPo session = requireSession(domainId, sessionNo);

        String linkedTicketNo = consultationRepository.findLinkedTicketNo(session.getId());
        if (linkedTicketNo != null) {
            return new ConsultationConvertResult(
                    toSessionRow(session), ticketRepository.findIdByTicketNoAndDomain(linkedTicketNo, domainId), linkedTicketNo);
        }

        long ticketTypeId = request.ticketTypeId() != null
                ? request.ticketTypeId()
                : resolveDefaultTicketTypeId(domainId);
        String title = StringUtils.hasText(request.title()) ? request.title().trim() : "咨询转工单";
        String description = StringUtils.hasText(request.description())
                ? request.description().trim()
                : buildSessionSummary(session);
        String priority = StringUtils.hasText(request.priority()) ? request.priority().trim() : null;

        TicketService.TicketSubmissionResult result = ticketService.createTicketForCustomer(
                context,
                domainId,
                session.getCustomerId(),
                new TicketService.CreateTicketCommand(
                        ticketTypeId,
                        title,
                        description,
                        java.util.Map.of(),
                        java.util.List.of(),
                        null,
                        priority,
                        "consultation",
                        null,
                        java.util.List.of()));

        consultationRepository.saveTicketLink(session.getId(), result.id(), domainId, context.userId());
        consultationRepository.closeSession(session.getId(), LocalDateTime.now(clock));

        ConsultationSessionPo closed = consultationRepository.findBySessionNoAndDomain(sessionNo, domainId);
        publishSessionChanged(closed);
        publishQueueChanged(domainId);
        return new ConsultationConvertResult(toSessionRow(closed), result.id(), result.ticketNo());
    }

    public record ConvertTicketRequest(Long ticketTypeId, String title, String description, String priority) {
    }

    /** 发布会话状态变化事件（客户感知接入/结束/转单） */
    private void publishSessionChanged(ConsultationSessionPo session) {
        eventPublisher.publish(new ConsultationSessionChangedEvent(
                session.getBusinessDomainId(),
                session.getSessionNo(),
                session.getCustomerId(),
                session.getSessionStatus(),
                session.getAssignedTo(),
                consultationRepository.findLinkedTicketNo(session.getId())));
    }

    /** 发布排队数变化事件（域内在线客服感知） */
    private void publishQueueChanged(long domainId) {
        eventPublisher.publish(new ConsultationQueuedEvent(domainId, agentQueueService.queueSize(domainId)));
    }

    /**
     * 会话快速分配或入队：在线 auto 客服非空则 least_loaded 分配（条件更新，0 行视为已被接入）；
     * 无可用客服或分配失败则入 Redis 队列，会话保持 queued。Redis 异常降级：不阻塞客户发起（可手动接入兜底）。
     */
    private void assignOrEnqueue(long domainId, String sessionNo, long sessionId) {
        try {
            List<Long> autoStaffIds = agentQueueService.listOnlineAutoStaffIds(domainId);
            Long assigneeId = autoStaffIds.isEmpty()
                    ? null
                    : consultationRepository.selectLeastLoadedOnlineAssignee(domainId, autoStaffIds);
            if (assigneeId != null
                    && consultationRepository.assignSessionIfUnassigned(sessionId, assigneeId) == 1) {
                return;
            }
            agentQueueService.enqueue(domainId, sessionNo);
        }
        catch (RuntimeException ex) {
            log.warn("咨询快速分配/入队失败（Redis 异常降级，可手动接入兜底）：domainId={}, sessionNo={}",
                    domainId, sessionNo, ex);
        }
    }

    /**
     * 自动取队分配：RPOP 原子取队直到队列为空；会话已结束/已被接入则跳过。
     */
    private void assignFromQueue(long domainId, long staffId) {
        String sessionNo;
        while ((sessionNo = agentQueueService.poll(domainId)) != null) {
            consultationRepository.assignSessionByNoIfUnassigned(sessionNo, domainId, staffId);
        }
    }

    private List<ConsultationMessageRow> listMessages(ConsultationSessionPo session) {
        return consultationRepository.findMessagesBySession(session.getId())
                .stream()
                .map(this::toMessageRow)
                .toList();
    }

    private ConsultationMessageRow sendMessage(ConsultationSessionPo session, long senderUserId, String senderRole, String content) {
        if (SESSION_STATUS_CLOSED.equals(session.getSessionStatus())) {
            throw new IllegalArgumentException("会话已关闭，无法发送消息");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        int seqNo = consultationRepository.nextSeqNo(session.getId());

        ConsultationMessagePo message = new ConsultationMessagePo();
        message.setConsultationSessionId(session.getId());
        message.setBusinessDomainId(session.getBusinessDomainId());
        message.setSeqNo(seqNo);
        message.setSenderUserId(senderUserId);
        message.setSenderRole(senderRole);
        message.setMessageType("text");
        message.setContent(content);
        consultationRepository.saveMessage(message);

        consultationRepository.updateLastMessageAt(session.getId(), now);

        // 实时推送（AFTER_COMMIT 后由 RealtimeEventListener 投递）：消息接收方 = 会话另一方
        Long recipientUserId = SENDER_ROLE_CUSTOMER.equals(senderRole)
                ? session.getAssignedTo()
                : session.getCustomerId();
        String recipientRole = SENDER_ROLE_CUSTOMER.equals(senderRole) ? SENDER_ROLE_AGENT : SENDER_ROLE_CUSTOMER;
        eventPublisher.publish(new ConsultationMessageSentEvent(
                session.getBusinessDomainId(),
                session.getSessionNo(),
                senderUserId,
                senderRole,
                recipientUserId,
                recipientRole,
                message.getId(),
                content,
                now));

        return new ConsultationMessageRow(
                message.getId(),
                session.getSessionNo(),
                seqNo,
                session.getBusinessDomainId(),
                senderRole,
                "text",
                content,
                null,
                now,
                false);
    }

    private ConsultationSessionPo requireSession(long domainId, String sessionNo) {
        ConsultationSessionPo session = consultationRepository.findBySessionNoAndDomain(sessionNo, domainId);
        if (session == null) {
            throw new IllegalArgumentException("咨询会话不存在");
        }
        return session;
    }

    private void requireOwner(ConsultationSessionPo session, long customerUserId) {
        if (session.getCustomerId() != customerUserId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN.message());
        }
    }

    private void requireStaff(UserContext context) {
        if (!isStaffRole(context)) {
            throw new IllegalArgumentException("仅客服人员可执行该操作");
        }
    }

    private void requireCustomer(UserContext context) {
        if (!isCustomerRole(context)) {
            throw new IllegalArgumentException("仅客户可执行该操作");
        }
    }

    private boolean isStaffRole(UserContext context) {
        return context != null
                && List.of("agent", "domain_admin", "super_admin").contains(normalize(context.role()));
    }

    private boolean isCustomerRole(UserContext context) {
        return context != null && "customer".equals(normalize(context.role()));
    }

    private String normalize(String role) {
        return role == null ? "" : role.trim().toLowerCase();
    }

    private void ensureCustomerInDomain(long customerUserId, long domainId) {
        if (domainCustomerRepository.countActiveByDomainAndCustomer(domainId, customerUserId) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN.message());
        }
    }

    private String nextSessionNo(long domainId) {
        String day = LocalDate.now(clock).format(SESSION_NO_DATE_FORMAT);
        long sequence = consultationRepository.nextSessionSequence(domainId, "CS-" + day + "-%");
        return "CS-" + day + "-" + String.format("%04d", sequence);
    }

    /**
     * 生成会话编号并保存；并发建会话撞唯一索引（MAX+1 竞态）时重新取号重试。
     */
    private void insertSessionWithRetry(long domainId, ConsultationSessionPo session) {
        int attempt = 0;
        while (true) {
            attempt++;
            String sessionNo = nextSessionNo(domainId);
            session.setSessionNo(sessionNo);
            try {
                consultationRepository.saveSession(session);
                return;
            }
            catch (DuplicateKeyException ex) {
                if (attempt >= SESSION_NO_CREATE_RETRY_MAX) {
                    throw new IllegalStateException("会话编号生成冲突，请重试", ex);
                }
            }
        }
    }

    private long resolveDefaultTicketTypeId(long domainId) {
        return ticketTypeRepository.findByDomainId(domainId).stream()
                .filter(type -> TicketTypePo.STATUS_ACTIVE.equals(type.getStatus()))
                .min(java.util.Comparator.comparingInt(TicketTypePo::getSortOrder))
                .map(TicketTypePo::getId)
                .orElseThrow(() -> new IllegalArgumentException("业务域未配置事项类型，无法转工单"));
    }

    private String buildSessionSummary(ConsultationSessionPo session) {
        StringBuilder summary = new StringBuilder();
        summary.append("【咨询转工单】会话 ").append(session.getSessionNo()).append('\n');
        summary.append("客户 ID：").append(session.getCustomerId()).append('\n');
        summary.append("会话消息：\n");
        List<ConsultationMessagePo> messages = consultationRepository.findMessagesBySession(session.getId());
        for (ConsultationMessagePo message : messages) {
            String roleName = SENDER_ROLE_AGENT.equals(message.getSenderRole()) ? "客服" : "客户";
            summary.append("  · (").append(roleName).append(") ").append(message.getContent()).append('\n');
        }
        return summary.toString().trim();
    }

    private ConsultationSessionRow toSessionRow(ConsultationSessionPo po) {
        return new ConsultationSessionRow(
                po.getId(),
                po.getSessionNo(),
                po.getBusinessDomainId(),
                po.getBusinessDomainName(),
                po.getCustomerId(),
                po.getSessionStatus(),
                po.getAssignedTo(),
                po.getLinkedTicketNo(),
                po.getLastMessageAt(),
                po.getCreatedAt(),
                po.getUpdatedAt(),
                po.getMessageCount() == null ? 0 : po.getMessageCount(),
                po.getArchivedAt());
    }

    private ConsultationMessageRow toMessageRow(ConsultationMessagePo po) {
        return new ConsultationMessageRow(
                po.getId(),
                po.getSessionNo(),
                po.getSeqNo(),
                po.getBusinessDomainId(),
                po.getSenderRole(),
                po.getMessageType(),
                po.getContent(),
                po.getPayloadJson(),
                po.getCreatedAt(),
                po.getRetractedAt() != null);
    }
}
