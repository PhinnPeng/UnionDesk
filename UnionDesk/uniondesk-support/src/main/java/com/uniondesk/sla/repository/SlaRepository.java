package com.uniondesk.sla.repository;

import com.mybatisflex.core.paginate.Page;
import com.uniondesk.sla.entity.SlaCalendarPo;
import com.uniondesk.sla.entity.SlaConfigPo;
import com.uniondesk.sla.entity.SlaRulePo;
import com.uniondesk.sla.entity.SlaTicketPo;
import com.uniondesk.sla.entity.TicketSlaPolicyPo;
import com.uniondesk.sla.mapper.SlaCalendarMapper;
import com.uniondesk.sla.mapper.SlaConfigMapper;
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
    private final SlaConfigMapper slaConfigMapper;

    public SlaRepository(SlaRuleMapper slaRuleMapper,
                         SlaCalendarMapper slaCalendarMapper,
                         SlaTicketMapper slaTicketMapper,
                         SlaConfigMapper slaConfigMapper) {
        this.slaRuleMapper = slaRuleMapper;
        this.slaCalendarMapper = slaCalendarMapper;
        this.slaTicketMapper = slaTicketMapper;
        this.slaConfigMapper = slaConfigMapper;
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

    public TicketSlaPolicyPo findGlobalPolicy() {
        return slaRuleMapper.selectGlobalPolicy();
    }

    public List<String> findActivePriorityCodes(long domainId) {
        return slaRuleMapper.selectActivePriorityCodes(domainId);
    }

    public List<SlaRulePo> findGlobalRules(int page, int pageSize) {
        int normalizedPageSize = normalizePageSize(pageSize);
        long offset = (long) (Math.max(page, 1) - 1) * normalizedPageSize;
        return slaRuleMapper.selectGlobalRules(normalizedPageSize, offset);
    }

    public long countGlobalRules() {
        return slaRuleMapper.countGlobalRules();
    }

    public SlaRulePo findGlobalRuleById(long ruleId) {
        return slaRuleMapper.selectGlobalRuleById(ruleId);
    }

    public int updateGlobalRule(SlaRulePo po) {
        return slaRuleMapper.updateGlobalRuleById(po);
    }

    public int deleteGlobalRuleById(long ruleId) {
        return slaRuleMapper.deleteGlobalRuleById(ruleId);
    }

    public String findTicketPriority(long ticketId) {
        return slaRuleMapper.selectTicketPriority(ticketId);
    }

    public String findPriorityCodeById(long priorityLevelId) {
        return slaRuleMapper.selectPriorityCodeById(priorityLevelId);
    }

    // --- SLA Config（每域一行，ADR-005） ---

    public SlaConfigPo findSlaConfigByDomainId(long domainId) {
        return slaConfigMapper.selectByDomainId(domainId);
    }

    public void saveSlaConfig(SlaConfigPo po) {
        slaConfigMapper.insert(po);
    }

    public int updateSlaConfigByDomainId(SlaConfigPo po) {
        return slaConfigMapper.updateByDomainId(po);
    }

    // --- SLA Calendar ---

    public Page<SlaCalendarPo> findPageByCalendars(Page<SlaCalendarPo> page, long domainId) {
        return slaCalendarMapper.selectPageByDomainId(page, domainId);
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

    public int deleteCalendarByIdAndDomainId(long calendarId, long domainId) {
        return slaCalendarMapper.deleteByIdAndDomainId(calendarId, domainId);
    }

    // --- Ticket SLA ---

    public SlaTicketPo findSlaSnapshot(long ticketId, long domainId) {
        return slaTicketMapper.selectSlaSnapshot(ticketId, domainId);
    }

    public void updateSlaDeadlines(long ticketId, LocalDateTime firstResponseDeadline, LocalDateTime resolutionDeadline) {
        slaTicketMapper.updateSlaDeadlines(ticketId, firstResponseDeadline, resolutionDeadline);
    }

    public LocalDateTime findCreatedAtById(long ticketId, long domainId) {
        return slaTicketMapper.selectCreatedAtById(ticketId, domainId);
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

    public int claimBreachAction(long ticketId) {
        return slaTicketMapper.claimBreachAction(ticketId);
    }

    public void updateSlaStatus(long ticketId, long domainId, String status) {
        slaTicketMapper.updateSlaStatus(ticketId, domainId, status);
    }

    private int normalizePageSize(int pageSize) {
        return Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE));
    }
}
