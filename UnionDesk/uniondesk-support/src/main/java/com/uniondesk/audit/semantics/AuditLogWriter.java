package com.uniondesk.audit.semantics;

import com.uniondesk.audit.repository.AuditLogWriteRepository;
import com.uniondesk.audit.entity.AuditLogWritePo;
import com.uniondesk.common.repository.IdentityResolutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuditLogWriter {

    private static final Logger log = LoggerFactory.getLogger(AuditLogWriter.class);

    private final AuditLogWriteRepository auditLogWriteRepository;
    private final IdentityResolutionRepository identityResolutionRepository;

    public AuditLogWriter(
            AuditLogWriteRepository auditLogWriteRepository,
            IdentityResolutionRepository identityResolutionRepository) {
        this.auditLogWriteRepository = auditLogWriteRepository;
        this.identityResolutionRepository = identityResolutionRepository;
    }

    public void write(
            Long businessDomainId,
            long operatorUserId,
            String operatorActorType,
            String target,
            String action,
            String detail,
            String result,
            String requestId) {
        try {
            AuditLogWritePo po = new AuditLogWritePo();
            po.setBusinessDomainId(businessDomainId == null ? 0L : businessDomainId);
            po.setOperatorSubjectId(identityResolutionRepository.ensureIdentitySubject(operatorUserId));
            po.setOperatorActorType(operatorActorType);
            po.setTarget(target);
            po.setAction(action);
            po.setDetail(detail);
            po.setResult(result);
            po.setRequestId(requestId);
            auditLogWriteRepository.save(po);
        } catch (RuntimeException ex) {
            log.warn("审计写入失败: action={}, target={}", action, target, ex);
        }
    }
}
