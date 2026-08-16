package com.uniondesk.consultation.repository;

import com.uniondesk.consultation.entity.ConsultationMessagePo;
import com.uniondesk.consultation.entity.ConsultationSessionPo;
import com.uniondesk.consultation.mapper.ConsultationMapper;
import com.uniondesk.consultation.mapper.ConsultationMessageMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ConsultationRepository {

    private final ConsultationMapper mapper;
    private final ConsultationMessageMapper messageMapper;

    public ConsultationRepository(ConsultationMapper mapper, ConsultationMessageMapper messageMapper) {
        this.mapper = mapper;
        this.messageMapper = messageMapper;
    }

    public ConsultationSessionPo findBySessionNoAndDomain(String sessionNo, long domainId) {
        return mapper.selectBySessionNoAndDomain(sessionNo, domainId);
    }

    public List<ConsultationSessionPo> findPageByDomain(long domainId, String status, Long assignedToMe, Boolean archived, int limit, long offset) {
        return mapper.selectPageByDomain(domainId, status, assignedToMe, archived, limit, offset);
    }

    public long countByDomain(long domainId, String status, Long assignedToMe, Boolean archived) {
        return mapper.countByDomain(domainId, status, assignedToMe, archived);
    }

    public List<ConsultationSessionPo> findByCustomerId(long domainId, long customerId) {
        return mapper.selectByCustomerId(domainId, customerId);
    }

    public long nextSessionSequence(long domainId, String prefix) {
        return mapper.nextSessionSequence(domainId, prefix);
    }

    public void saveSession(ConsultationSessionPo po) {
        mapper.insertSession(po);
    }

    public List<ConsultationMessagePo> findMessagesBySession(long sessionId) {
        return mapper.selectMessagesBySession(sessionId);
    }

    public ConsultationMessagePo findMessageByIdAndSession(long messageId, long sessionId) {
        return mapper.selectMessageByIdAndSession(messageId, sessionId);
    }

    public int nextSeqNo(long sessionId) {
        return mapper.nextSeqNo(sessionId);
    }

    public void saveMessage(ConsultationMessagePo po) {
        messageMapper.insert(po);
    }

    public int updateLastMessageAt(long sessionId, LocalDateTime lastMessageAt) {
        return mapper.updateLastMessageAt(sessionId, lastMessageAt);
    }

    public int assignSessionIfUnassigned(long sessionId, long assignedTo) {
        return mapper.assignSessionIfUnassigned(sessionId, assignedTo);
    }

    public int assignSessionByNoIfUnassigned(String sessionNo, long domainId, long assignedTo) {
        return mapper.assignSessionByNoIfUnassigned(sessionNo, domainId, assignedTo);
    }

    public Long selectLeastLoadedOnlineAssignee(long domainId, List<Long> staffIds) {
        return mapper.selectLeastLoadedOnlineAssignee(domainId, staffIds);
    }

    public int updateMessageRetracted(long messageId, long retractedBy, LocalDateTime retractedAt) {
        return mapper.updateMessageRetracted(messageId, retractedBy, retractedAt);
    }

    public int closeSession(long sessionId, LocalDateTime closedAt) {
        return mapper.closeSession(sessionId, closedAt);
    }

    public int updateArchived(long sessionId, LocalDateTime archivedAt) {
        return mapper.updateArchived(sessionId, archivedAt);
    }

    public List<ConsultationSessionPo> findAutoArchiveCandidates(long domainId, LocalDateTime closedBefore, int limit) {
        return mapper.selectAutoArchiveCandidates(domainId, closedBefore, limit);
    }

    public List<ConsultationMapper.AutoArchiveConfigRow> findAutoArchiveConfigs() {
        return mapper.selectAutoArchiveConfigs();
    }

    public String findLinkedTicketNo(long sessionId) {
        return mapper.selectLinkedTicketNo(sessionId);
    }

    public void saveTicketLink(long sessionId, long ticketId, long domainId, long convertedBy) {
        mapper.insertTicketLink(sessionId, ticketId, domainId, convertedBy);
    }
}
