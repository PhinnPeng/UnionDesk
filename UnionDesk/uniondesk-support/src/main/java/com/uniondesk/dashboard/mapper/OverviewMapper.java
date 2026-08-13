package com.uniondesk.dashboard.mapper;

import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 平台概览计数查询（轻量 count(*) 聚合）。
 */
@Mapper
public interface OverviewMapper {

    long countBusinessDomains();

    long countStaff();

    long countActiveStaff();

    long countDisabledStaff();

    long countOffboardedStaff();

    long countCustomers();

    long countTickets();

    long countConsultationSessions();

    long countLoginLogsSince(@Param("since") LocalDateTime since);
}
