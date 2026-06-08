package com.uniondesk.audit.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.audit.entity.AuditLogWritePo;
import com.uniondesk.audit.repository.AuditLogWriteRepository;
import com.uniondesk.common.event.DomainMemberStatusChangedEvent;
import com.uniondesk.common.repository.IdentityResolutionRepository;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AuditLogEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditLogEventListener.class);

    private final AuditLogWriteRepository auditLogWriteRepository;
    private final IdentityResolutionRepository identityResolutionRepository;
    private final ObjectMapper objectMapper;

    public AuditLogEventListener(
            AuditLogWriteRepository auditLogWriteRepository,
            IdentityResolutionRepository identityResolutionRepository,
            ObjectMapper objectMapper) {
        this.auditLogWriteRepository = auditLogWriteRepository;
        this.identityResolutionRepository = identityResolutionRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainMemberStatusChanged(DomainMemberStatusChangedEvent event) {
        AuditLogWritePo po = new AuditLogWritePo();
        po.setBusinessDomainId(event.businessDomainId());
        po.setOperatorSubjectId(identityResolutionRepository.ensureIdentitySubject(event.operatorUserId()));
        po.setOperatorActorType("staff");
        po.setTarget("domain_member:" + event.memberId());
        po.setAction("domain.member.update_status");
        po.setResult("success");
        try {
            po.setDetail(objectMapper.writeValueAsString(Map.of(
                    "member_id", event.memberId(),
                    "staff_account_id", event.staffAccountId(),
                    "previous_status", event.previousStatus(),
                    "new_status", event.newStatus())));
        } catch (JsonProcessingException ex) {
            log.warn("序列化成员状态审计详情失败: memberId={}", event.memberId(), ex);
            po.setDetail("{}");
        }
        auditLogWriteRepository.save(po);
    }
}
