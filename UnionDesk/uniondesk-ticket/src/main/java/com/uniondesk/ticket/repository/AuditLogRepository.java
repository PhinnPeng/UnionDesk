package com.uniondesk.ticket.repository;

import com.uniondesk.ticket.entity.AuditLogPo;
import com.uniondesk.ticket.mapper.AuditLogMapper;
import org.springframework.stereotype.Repository;

@Repository("ticketAuditLogRepository")
public class AuditLogRepository {

    private final AuditLogMapper mapper;

    public AuditLogRepository(AuditLogMapper mapper) {
        this.mapper = mapper;
    }

    public void save(AuditLogPo po) {
        mapper.insert(po);
    }
}
