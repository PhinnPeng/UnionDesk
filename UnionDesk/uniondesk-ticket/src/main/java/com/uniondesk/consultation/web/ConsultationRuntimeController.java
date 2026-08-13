package com.uniondesk.consultation.web;

import com.uniondesk.auth.core.UserContext;
import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.common.web.PageResult;
import com.uniondesk.consultation.core.ConsultationService;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 在线咨询生产端点（demo 版 ConsultationController 保留不动）。
 * 客户端点按 customer_account 归属校验，管理端点按业务域隔离。
 */
@RestController
@RequestMapping("/api/v1")
public class ConsultationRuntimeController {

    public record CreateConsultationRequest(@NotBlank(message = "咨询内容不能为空") String content) {
    }

    public record SendMessageRequest(@NotBlank(message = "消息内容不能为空") String content) {
    }

    private final ConsultationService consultationService;

    public ConsultationRuntimeController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    // --- 客户端点 ---

    /** 客户发起咨询：创建会话并写入首条消息 */
    @PostMapping("/domains/{domain_id}/consultations")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(value = PermissionCodes.CONSULTATION_CUSTOMER, domainIdParam = "domain_id")
    public ConsultationService.ConsultationSessionRow create(
            @PathVariable("domain_id") long domainId,
            @Valid @RequestBody CreateConsultationRequest request) {
        return consultationService.createSession(requireCurrent(), domainId, request.content());
    }

    /** 客户我的咨询会话列表 */
    @GetMapping("/domains/{domain_id}/consultations/my")
    @RequirePermission(value = PermissionCodes.CONSULTATION_CUSTOMER, domainIdParam = "domain_id")
    public List<ConsultationService.ConsultationSessionRow> listMy(
            @PathVariable("domain_id") long domainId) {
        return consultationService.listMySessions(requireCurrent(), domainId);
    }

    /** 客户查看我的会话消息（归属校验） */
    @GetMapping("/domains/{domain_id}/consultations/my/{session_no}/messages")
    @RequirePermission(value = PermissionCodes.CONSULTATION_CUSTOMER, domainIdParam = "domain_id")
    public List<ConsultationService.ConsultationMessageRow> listMyMessages(
            @PathVariable("domain_id") long domainId,
            @PathVariable("session_no") String sessionNo) {
        return consultationService.listMessagesOwned(requireCurrent(), domainId, sessionNo);
    }

    /** 客户在我的会话中发送消息 */
    @PostMapping("/domains/{domain_id}/consultations/my/{session_no}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(value = PermissionCodes.CONSULTATION_CUSTOMER, domainIdParam = "domain_id")
    public ConsultationService.ConsultationMessageRow sendMyMessage(
            @PathVariable("domain_id") long domainId,
            @PathVariable("session_no") String sessionNo,
            @Valid @RequestBody SendMessageRequest request) {
        return consultationService.sendMessageOwned(requireCurrent(), domainId, sessionNo, request.content());
    }

    // --- 管理端点 ---

    /** 客服会话列表（按业务域） */
    @GetMapping("/admin/domains/{domain_id}/consultations")
    @RequirePermission(value = PermissionCodes.CONSULTATION_VIEW, domainIdParam = "domain_id")
    public PageResult<ConsultationService.ConsultationSessionRow> listAdmin(
            @PathVariable("domain_id") long domainId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status) {
        return consultationService.listAdminSessions(requireCurrent(), domainId, page, pageSize, status);
    }

    /** 客服查看会话消息 */
    @GetMapping("/admin/domains/{domain_id}/consultations/{session_no}/messages")
    @RequirePermission(value = PermissionCodes.CONSULTATION_VIEW, domainIdParam = "domain_id")
    public List<ConsultationService.ConsultationMessageRow> listAdminMessages(
            @PathVariable("domain_id") long domainId,
            @PathVariable("session_no") String sessionNo) {
        return consultationService.listAdminMessages(requireCurrent(), domainId, sessionNo);
    }

    /** 客服回复会话 */
    @PostMapping("/admin/domains/{domain_id}/consultations/{session_no}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(value = PermissionCodes.CONSULTATION_REPLY, domainIdParam = "domain_id")
    public ConsultationService.ConsultationMessageRow replyAdmin(
            @PathVariable("domain_id") long domainId,
            @PathVariable("session_no") String sessionNo,
            @Valid @RequestBody SendMessageRequest request) {
        return consultationService.replyAdmin(requireCurrent(), domainId, sessionNo, request.content());
    }

    /** 客服将会话转为工单（带客户信息与会话摘要） */
    @PostMapping("/admin/domains/{domain_id}/consultations/{session_no}/ticket")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(value = PermissionCodes.CONSULTATION_CONVERT, domainIdParam = "domain_id")
    public ConsultationService.ConsultationConvertResult convertToTicket(
            @PathVariable("domain_id") long domainId,
            @PathVariable("session_no") String sessionNo,
            @Valid @RequestBody(required = false) ConsultationService.ConvertTicketRequest request) {
        ConsultationService.ConvertTicketRequest effective = request == null
                ? new ConsultationService.ConvertTicketRequest(null, null, null, null)
                : request;
        return consultationService.convertToTicket(requireCurrent(), domainId, sessionNo, effective);
    }

    private UserContext requireCurrent() {
        return UserContextHolder.current()
                .orElseThrow(() -> new IllegalStateException("user context is not available"));
    }
}
