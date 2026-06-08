package com.uniondesk.domain.mapper;

import com.uniondesk.domain.entity.AuditLogPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper {

    void insert(AuditLogPo po);
}
