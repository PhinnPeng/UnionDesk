package com.uniondesk.audit.mapper;

import com.uniondesk.audit.entity.LoginLogViewPo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LoginLogMapper {

    List<LoginLogViewPo> selectLoginLogs(
            @Param("subjectId") Long subjectId,
            @Param("businessDomainId") Long businessDomainId,
            @Param("portalType") String portalType,
            @Param("clientCode") String clientCode,
            @Param("eventType") String eventType,
            @Param("result") String result,
            @Param("keyword") String keyword,
            @Param("ip") String ip,
            @Param("username") String username,
            @Param("nickname") String nickname,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("limit") int limit,
            @Param("offset") long offset);

    long countLoginLogs(
            @Param("subjectId") Long subjectId,
            @Param("businessDomainId") Long businessDomainId,
            @Param("portalType") String portalType,
            @Param("clientCode") String clientCode,
            @Param("eventType") String eventType,
            @Param("result") String result,
            @Param("keyword") String keyword,
            @Param("ip") String ip,
            @Param("username") String username,
            @Param("nickname") String nickname,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
