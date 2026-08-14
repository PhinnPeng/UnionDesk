package com.uniondesk.consultation.core;

import com.uniondesk.auth.core.UserContext;
import com.uniondesk.common.web.ErrorCodes;
import com.uniondesk.common.web.PageResult;
import com.uniondesk.consultation.entity.ConsultationMessagePo;
import com.uniondesk.consultation.entity.ConsultationSessionPo;
import com.uniondesk.consultation.repository.ConsultationRepository;
import com.uniondesk.domain.repository.DomainCustomerRepository;
import com.uniondesk.ticket.core.TicketService;
import com.uniondesk.ticket.entity.TicketTypePo;
import com.uniondesk.ticket.repository.TicketRepository;
import com.uniondesk.ticket.repository.TicketTypeRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConsultationService {

    public static final String SESSION_STATUS_OPEN = "open";
    public static final String SESSION_STATUS_CLOSED = "closed";
    public static final String SENDER_ROLE_CUSTOMER = "customer";
    public static final String SENDER_ROLE_AGENT = "agent";

    private static final DateTimeFormatter SESSION_NO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 会话编号生成并发冲突最大重试次数（MAX+1 撞唯一索引后重新取号） */
    private static final int SESSION_NO_CREATE_RETRY_MAX = 3;

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
            long messageCount) {
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
            LocalDateTime createdAt) {
    }

    public record ConsultationConvertResult(
            ConsultationSessionRow session,
            long ticketId,
            String ticketNo) {
    }

    private final ConsultationRepository consultationRepository;
    private final TicketService ticketService;
    private final TicketTypeRepository ticketTypeRepository;
    private final TicketRepository ticketRepository;
    private final DomainCustomerRepository domainCustomerRepository;
    private final Clock clock;

    public ConsultationService(
            ConsultationRepository consultationRepository,
            TicketService ticketService,
            TicketTypeRepository ticketTypeRepository,
            TicketRepository ticketRepository,
            DomainCustomerRepository domainCustomerRepository,
            Clock clock) {
        this.consultationRepository = consultationRepository;
        this.ticketService = ticketService;
        this.ticketTypeRepository = ticketTypeRepository;
        this.ticketRepository = ticketRepository;
        this.domainCustomerRepository = domainCustomerRepository;
        this.clock = clock;
    }

    @Transactional
    public ConsultationSessionRow createSession(UserContext context, long domainId, String content) {
        requireCustomer(context);
        ensureCustomerInDomain(context.userId(), domainId);
        LocalDateTime now = LocalDateTime.now(clock);

        ConsultationSessionPo session = new ConsultationSessionPo();
        session.setBusinessDomainId(domainId);
        session.setCustomerId(context.userId());
        session.setSessionStatus(SESSION_STATUS_OPEN);
        session.setLastMessageAt(now);
        insertSessionWithRetry(domainId, session);

        ConsultationMessagePo message = new ConsultationMessagePo();
        message.setConsultationSessionId(session.getId());
        message.setBusinessDomainId(domainId);
        message.setSeqNo(1);
        message.setSenderUserId(context.userId());
        message.setSenderRole(SENDER_ROLE_CUSTOMER);
        message.setMessageType("text");
        message.setContent(content);
        consultationRepository.saveMessage(message);

        return toSessionRow(consultationRepository.findBySessionNoAndDomain(session.getSessionNo(), domainId));
    }

    public List<ConsultationSessionRow> listMySessions(UserContext context, long domainId) {
        requireCustomer(context);
        return consultationRepository.findByCustomerId(domainId, context.userId())
                .stream()
                .map(this::toSessionRow)
                .toList();
    }

    public PageResult<ConsultationSessionRow> listAdminSessions(UserContext context, long domainId, int page, int pageSize, String status) {
        requireStaff(context);
        long offset = (long) (page - 1) * pageSize;
        List<ConsultationSessionRow> rows = consultationRepository.findPageByDomain(domainId, status, pageSize, offset)
                .stream()
                .map(this::toSessionRow)
                .toList();
        return new PageResult<>(consultationRepository.countByDomain(domainId, status), rows);
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
        consultationRepository.updateAssignedToIfNull(session.getId(), context.userId());
        return row;
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
        return new ConsultationConvertResult(toSessionRow(closed), result.id(), result.ticketNo());
    }

    public record ConvertTicketRequest(Long ticketTypeId, String title, String description, String priority) {
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

        return new ConsultationMessageRow(
                message.getId(),
                session.getSessionNo(),
                seqNo,
                session.getBusinessDomainId(),
                senderRole,
                "text",
                content,
                null,
                now);
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
                po.getMessageCount() == null ? 0 : po.getMessageCount());
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
                po.getCreatedAt());
    }
}
