package com.uniondesk.domain.mapper;

import com.mybatisflex.core.BaseMapper;
import com.uniondesk.domain.entity.AuditLogPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLogPo> {
}
