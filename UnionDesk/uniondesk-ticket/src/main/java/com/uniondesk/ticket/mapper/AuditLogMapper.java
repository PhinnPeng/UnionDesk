package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.AuditLogPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper {

    void insert(AuditLogPo po);
}
