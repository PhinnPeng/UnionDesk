package com.uniondesk.audit.repository;

import com.uniondesk.audit.entity.AuditLogViewPo;
import com.uniondesk.audit.entity.LoginLogViewPo;
import com.uniondesk.audit.mapper.AuditLogMapper;
import com.uniondesk.audit.mapper.LoginLogMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogRepository {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogMapper auditLogMapper;
    private final LoginLogMapper loginLogMapper;

    public AuditLogRepository(AuditLogMapper auditLogMapper, LoginLogMapper loginLogMapper) {
        this.auditLogMapper = auditLogMapper;
        this.loginLogMapper = loginLogMapper;
    }

    public long countAuditLogs(Long domainId, String operator, String action,
                               String module, String keyword, String ip,
                               String username, String nickname,
                               LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogMapper.countAuditLogs(domainId, operator, action, module,
                keyword, ip, username, nickname, startTime, endTime);
    }

    public List<AuditLogViewPo> findAuditLogs(Long domainId, String operator, String action,
                                               String module, String keyword, String ip,
                                               String username, String nickname,
                                               LocalDateTime startTime, LocalDateTime endTime,
                                               int page, int pageSize) {
        int normalizedPageSize = normalizePageSize(pageSize);
        long offset = (long) (Math.max(page, 1) - 1) * normalizedPageSize;
        return auditLogMapper.selectAuditLogs(domainId, operator, action, module,
                keyword, ip, username, nickname, startTime, endTime,
                normalizedPageSize, offset);
    }

    public long countLoginLogs(Long subjectId, Long businessDomainId,
                               String portalType, String clientCode, String eventType,
                               String result, String keyword, String ip,
                               String username, String nickname,
                               LocalDateTime startTime, LocalDateTime endTime) {
        return loginLogMapper.countLoginLogs(subjectId, businessDomainId,
                portalType, clientCode, eventType, result,
                keyword, ip, username, nickname, startTime, endTime);
    }

    public List<LoginLogViewPo> findLoginLogs(Long subjectId, Long businessDomainId,
                                               String portalType, String clientCode,
                                               String eventType, String result,
                                               String keyword, String ip,
                                               String username, String nickname,
                                               LocalDateTime startTime, LocalDateTime endTime,
                                               int page, int pageSize) {
        int normalizedPageSize = normalizePageSize(pageSize);
        long offset = (long) (Math.max(page, 1) - 1) * normalizedPageSize;
        return loginLogMapper.selectLoginLogs(subjectId, businessDomainId,
                portalType, clientCode, eventType, result,
                keyword, ip, username, nickname, startTime, endTime,
                normalizedPageSize, offset);
    }

    private int normalizePageSize(int pageSize) {
        return Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
    }
}
