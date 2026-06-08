package com.uniondesk.audit.mapper;

import com.uniondesk.audit.entity.AuditLogWritePo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogWriteMapper {

    void insert(AuditLogWritePo po);
}
