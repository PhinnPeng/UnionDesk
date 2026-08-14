package com.uniondesk.audit.mapper;

import com.mybatisflex.core.BaseMapper;
import com.uniondesk.audit.entity.AuditLogWritePo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogWriteMapper extends BaseMapper<AuditLogWritePo> {
}
