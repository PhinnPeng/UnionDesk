package com.uniondesk.sla.repository;

import com.uniondesk.sla.entity.SlaCalendarPo;
import com.uniondesk.sla.entity.SlaRulePo;
import com.uniondesk.sla.entity.SlaTicketPo;
import com.uniondesk.sla.entity.TicketSlaPolicyPo;
import com.uniondesk.sla.mapper.SlaCalendarMapper;
import com.uniondesk.sla.mapper.SlaRuleMapper;
import com.uniondesk.sla.mapper.SlaTicketMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class SlaRepository {

    private static final int MAX_PAGE_SIZE = 100;

    private final SlaRuleMapper slaRuleMapper;
    private final SlaCalendarMapper slaCalendarMapper;
    private final SlaTicketMapper slaTicketMapper;

    public SlaRepository(SlaRuleMapper slaRuleMapper,
                         SlaCalendarMapper slaCalendarMapper,
                         SlaTicketMapper slaTicketMapper) {
        this.slaRuleMapper = slaRuleMapper;
        this.slaCalendarMapper = slaCalendarMapper;
        this.slaTicketMapper = slaTicketMapper;
    }

    // --- SLA Rule ---

    public List<SlaRulePo> findRulesByDomainId(long domainId, int page, int pageSize) {
        int normalizedPageSize = normalizePageSize(pageSize);
        long offset = (long) (Math.max(page, 1) - 1) * normalizedPageSize;
        return slaRuleMapper.selectByDomainId(domainId, normalizedPageSize, offset);
    }

    public long countRulesByDomainId(long domainId) {
        return slaRuleMapper.countByDomainId(domainId);
    }

    public SlaRulePo findRuleByIdAndDomainId(long ruleId, long domainId) {
        return slaRuleMapper.selectByIdAndDomainId(ruleId, domainId);
    }

    public void saveRule(SlaRulePo po) {
        slaRuleMapper.insert(po);
    }

    public void updateRule(SlaRulePo po) {
        slaRuleMapper.updateByIdAndDomainId(po);
    }

    public int deleteRuleByIdAndDomainId(long ruleId, long domainId) {
        return slaRuleMapper.deleteByIdAndDomainId(ruleId, domainId);
    }

    public TicketSlaPolicyPo findPolicy(long domainId, long ticketTypeId, String priorityCode) {
        return slaRuleMapper.selectPolicy(domainId, ticketTypeId, priorityCode);
    }

    public String findTicketPriority(long ticketId) {
        return slaRuleMapper.selectTicketPriority(ticketId);
    }

    // --- SLA Calendar ---

    public List<SlaCalendarPo> findCalendarsByDomainId(long domainId, int page, int pageSize) {
        int normalizedPageSize = normalizePageSize(pageSize);
        long offset = (long) (Math.max(page, 1) - 1) * normalizedPageSize;
        return slaCalendarMapper.selectByDomainId(domainId, normalizedPageSize, offset);
    }

    public long countCalendarsByDomainId(long domainId) {
        return slaCalendarMapper.countByDomainId(domainId);
    }

    public SlaCalendarPo findCalendarByIdAndDomainId(long calendarId, long domainId) {
        return slaCalendarMapper.selectByIdAndDomainId(calendarId, domainId);
    }

    public void saveCalendar(SlaCalendarPo po) {
        slaCalendarMapper.insert(po);
    }

    public void updateCalendar(SlaCalendarPo po) {
        slaCalendarMapper.updateByIdAndDomainId(po);
    }

    // --- Ticket SLA ---

    public SlaTicketPo findSlaSnapshot(long ticketId, long domainId) {
        return slaTicketMapper.selectSlaSnapshot(ticketId, domainId);
    }

    public void updateSlaDeadlines(long ticketId, Integer firstResponseMinutes, Integer resolutionMinutes) {
        slaTicketMapper.updateSlaDeadlines(ticketId, firstResponseMinutes, resolutionMinutes);
    }

    public void updateFirstResponse(LocalDateTime now, long ticketId, long domainId) {
        slaTicketMapper.updateFirstResponse(now, ticketId, domainId);
    }

    public void updateResolution(LocalDateTime now, long ticketId, long domainId) {
        slaTicketMapper.updateResolution(now, ticketId, domainId);
    }

    public void updatePriorityAndSlaStatus(String priority, String slaStatus, long ticketId) {
        slaTicketMapper.updatePriorityAndSlaStatus(priority, slaStatus, ticketId);
    }

    private int normalizePageSize(int pageSize) {
        return Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE));
    }
}
