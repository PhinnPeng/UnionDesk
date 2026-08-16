package com.uniondesk.sla.mapper;

import com.uniondesk.sla.entity.SlaTicketPo;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SlaTicketMapper {

    SlaTicketPo selectSlaSnapshot(
            @Param("ticketId") long ticketId,
            @Param("domainId") long domainId);

    LocalDateTime selectCreatedAtById(
            @Param("ticketId") long ticketId,
            @Param("domainId") long domainId);

    void updateSlaDeadlines(
            @Param("ticketId") long ticketId,
            @Param("firstResponseDeadline") LocalDateTime firstResponseDeadline,
            @Param("resolutionDeadline") LocalDateTime resolutionDeadline);

    void updateFirstResponse(
            @Param("now") LocalDateTime now,
            @Param("ticketId") long ticketId,
            @Param("domainId") long domainId);

    void updateResolution(
            @Param("now") LocalDateTime now,
            @Param("ticketId") long ticketId,
            @Param("domainId") long domainId);

    void updatePriorityAndSlaStatus(
            @Param("priority") String priority,
            @Param("slaStatus") String slaStatus,
            @Param("ticketId") long ticketId);

    int claimBreachAction(@Param("ticketId") long ticketId);

    void updateSlaStatus(
            @Param("ticketId") long ticketId,
            @Param("domainId") long domainId,
            @Param("status") String status);
}
