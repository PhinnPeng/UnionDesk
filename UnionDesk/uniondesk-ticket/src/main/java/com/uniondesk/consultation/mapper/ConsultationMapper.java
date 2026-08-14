package com.uniondesk.consultation.mapper;

import com.mybatisflex.core.BaseMapper;
import com.uniondesk.consultation.entity.ConsultationMessagePo;
import com.uniondesk.consultation.entity.ConsultationSessionPo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ConsultationMapper extends BaseMapper<ConsultationSessionPo> {

    ConsultationSessionPo selectBySessionNoAndDomain(@Param("sessionNo") String sessionNo, @Param("domainId") long domainId);

    List<ConsultationSessionPo> selectPageByDomain(
            @Param("domainId") long domainId,
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") long offset);

    long countByDomain(@Param("domainId") long domainId, @Param("status") String status);

    List<ConsultationSessionPo> selectByCustomerId(@Param("domainId") long domainId, @Param("customerId") long customerId);

    long nextSessionSequence(@Param("prefix") String prefix);

    default void insertSession(ConsultationSessionPo po) {
        insert(po);
    }

    List<ConsultationMessagePo> selectMessagesBySession(@Param("sessionId") long sessionId);

    int nextSeqNo(@Param("sessionId") long sessionId);

    int updateLastMessageAt(@Param("sessionId") long sessionId, @Param("lastMessageAt") LocalDateTime lastMessageAt);

    int updateAssignedToIfNull(@Param("sessionId") long sessionId, @Param("assignedTo") long assignedTo);

    int closeSession(@Param("sessionId") long sessionId, @Param("closedAt") LocalDateTime closedAt);

    String selectLinkedTicketNo(@Param("sessionId") long sessionId);

    void insertTicketLink(
            @Param("sessionId") long sessionId,
            @Param("ticketId") long ticketId,
            @Param("domainId") long domainId,
            @Param("convertedBy") long convertedBy);
}
