package com.uniondesk.ticket.mapper;

import com.mybatisflex.core.BaseMapper;
import com.uniondesk.ticket.entity.SlaScanCandidatePo;
import com.uniondesk.ticket.entity.TicketDetailPo;
import com.uniondesk.ticket.entity.TicketPo;
import com.uniondesk.ticket.entity.TicketTypeInitialCountPo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TicketMapper extends BaseMapper<TicketPo> {

    TicketDetailPo findByIdAndDomainId(@Param("ticketId") long ticketId, @Param("domainId") long domainId);

    List<TicketDetailPo> listTickets(@Param("domainId") long domainId,
                                     @Param("customerId") Long customerId,
                                     @Param("status") String status,
                                     @Param("limit") int limit);

    long countTickets(@Param("domainId") long domainId,
                      @Param("customerId") Long customerId,
                      @Param("status") String status,
                      @Param("assignee") Long assignee,
                      @Param("priority") String priority,
                      @Param("keyword") String keyword,
                      @Param("slaStatus") String slaStatus,
                      @Param("ticketTypeId") Long ticketTypeId);

    List<TicketDetailPo> listTicketsPage(@Param("domainId") long domainId,
                                         @Param("customerId") Long customerId,
                                         @Param("status") String status,
                                         @Param("assignee") Long assignee,
                                         @Param("priority") String priority,
                                         @Param("keyword") String keyword,
                                         @Param("slaStatus") String slaStatus,
                                         @Param("ticketTypeId") Long ticketTypeId,
                                         @Param("limit") int limit,
                                         @Param("offset") long offset);

    /**
     * 按事项类型统计「未处理」工单数：类型处于起始状态（ticket_type_flow_status.is_initial=1）
     * 的工单；assignee 非空时限定受理人（我的待办视角）。
     */
    List<TicketTypeInitialCountPo> countInitialTicketsByType(@Param("domainId") long domainId,
                                                             @Param("assignee") Long assignee);

    Long findIdByTicketNoAndDomain(@Param("ticketNo") String ticketNo, @Param("domainId") long domainId);

    Long selectVersionByIdAndDomainId(@Param("ticketId") long ticketId, @Param("domainId") long domainId);

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
                     @Param("assignee") Long assignee,
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

    /**
     * SLA 违约强制指派：绕开版本乐观锁与领取状态校验，版本自增。
     */
    int forceAssign(@Param("ticketId") long ticketId,
                    @Param("domainId") long domainId,
                    @Param("assignee") long assignee,
                    @Param("now") LocalDateTime now);

    /**
     * SLA 定时扫描候选：tracking 且任一时限已过未完成。
     */
    List<SlaScanCandidatePo> selectSlaScanCandidates(@Param("limit") int limit);

    Long findNextTicketSequence(@Param("domainId") long domainId, @Param("prefix") String prefix);

    String findDefaultPriorityCode(@Param("domainId") long domainId);
}
