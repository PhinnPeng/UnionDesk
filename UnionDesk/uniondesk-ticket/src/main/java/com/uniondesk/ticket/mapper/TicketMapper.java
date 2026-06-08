package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.TicketDetailPo;
import com.uniondesk.ticket.entity.TicketPo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TicketMapper {

    void insert(TicketPo po);

    TicketDetailPo findByIdAndDomainId(@Param("ticketId") long ticketId, @Param("domainId") long domainId);

    List<TicketDetailPo> listTickets(@Param("domainId") long domainId,
                                     @Param("customerId") Long customerId,
                                     @Param("status") String status,
                                     @Param("limit") int limit);

    Long findIdByTicketNo(@Param("ticketNo") String ticketNo);

    int updateStatus(@Param("ticketId") long ticketId,
                     @Param("newStatus") String newStatus,
                     @Param("version") long version,
                     @Param("now") LocalDateTime now);

    int updateClaim(@Param("ticketId") long ticketId,
                    @Param("domainId") long domainId,
                    @Param("assignee") long assignee,
                    @Param("version") long version,
                    @Param("now") LocalDateTime now);

    int updateAssign(@Param("ticketId") long ticketId,
                     @Param("domainId") long domainId,
                     @Param("assignee") long assignee,
                     @Param("version") long version,
                     @Param("now") LocalDateTime now);

    int updateOnReply(@Param("ticketId") long ticketId,
                      @Param("domainId") long domainId,
                      @Param("senderType") String senderType,
                      @Param("version") long version,
                      @Param("now") LocalDateTime now);

    int updateWithdraw(@Param("ticketId") long ticketId,
                       @Param("domainId") long domainId,
                       @Param("version") long version,
                       @Param("reason") String reason);

    int updateMerge(@Param("ticketId") long ticketId,
                    @Param("domainId") long domainId,
                    @Param("version") long version);

    String findDomainCodeById(@Param("domainId") long domainId);

    Long findNextTicketSequence(@Param("domainId") long domainId, @Param("prefix") String prefix);

    String findDefaultPriorityCode(@Param("domainId") long domainId);
}
