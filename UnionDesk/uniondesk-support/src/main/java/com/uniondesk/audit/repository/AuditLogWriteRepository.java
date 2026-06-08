package com.uniondesk.audit.repository;

import com.uniondesk.audit.entity.AuditLogWritePo;
import com.uniondesk.audit.mapper.AuditLogWriteMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogWriteRepository {

    private final AuditLogWriteMapper mapper;

    public AuditLogWriteRepository(AuditLogWriteMapper mapper) {
        this.mapper = mapper;
    }

    public void save(AuditLogWritePo po) {
        mapper.insert(po);
    }
}
