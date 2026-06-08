package com.uniondesk.audit.mapper;

import com.uniondesk.audit.entity.AuditLogViewPo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuditLogMapper {

    List<AuditLogViewPo> selectAuditLogs(
            @Param("domainId") Long domainId,
            @Param("operator") String operator,
            @Param("action") String action,
            @Param("module") String module,
            @Param("keyword") String keyword,
            @Param("ip") String ip,
            @Param("username") String username,
            @Param("nickname") String nickname,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("limit") int limit,
            @Param("offset") long offset);

    long countAuditLogs(
            @Param("domainId") Long domainId,
            @Param("operator") String operator,
            @Param("action") String action,
            @Param("module") String module,
            @Param("keyword") String keyword,
            @Param("ip") String ip,
            @Param("username") String username,
            @Param("nickname") String nickname,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
