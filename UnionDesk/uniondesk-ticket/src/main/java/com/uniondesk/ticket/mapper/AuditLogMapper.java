package com.uniondesk.ticket.mapper;

import com.mybatisflex.core.BaseMapper;
import com.uniondesk.ticket.entity.AuditLogPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLogPo> {
}
