package com.uniondesk.ticket.core;

import com.uniondesk.ticket.web.TicketConfigDtos;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 兼容门面：步骤规则已并入 {@link TicketTypeFlowService} / {@code ticket_type_flow_transition}。
 */
@Service
public class TicketTransitionRuleService {

    private final TicketTypeFlowService ticketTypeFlowService;

    public TicketTransitionRuleService(TicketTypeFlowService ticketTypeFlowService) {
        this.ticketTypeFlowService = ticketTypeFlowService;
    }

    public List<TicketConfigDtos.TransitionRuleView> findByDomainIdAndTypeId(long domainId, long ticketTypeId) {
        return ticketTypeFlowService.loadRules(domainId, ticketTypeId);
    }

    @Transactional
    public void saveWorkflowRules(
            long domainId,
            long ticketTypeId,
            List<TicketConfigDtos.SaveTransitionRuleRequest> rules) {
        Object statusFlow = ticketTypeFlowService.loadStatusFlow(domainId, ticketTypeId);
        ticketTypeFlowService.replaceAll(domainId, ticketTypeId, statusFlow, rules);
    }

    @Transactional
    public void deleteByDomainIdAndTypeId(long domainId, long ticketTypeId) {
        ticketTypeFlowService.deleteByDomainIdAndTypeId(domainId, ticketTypeId);
    }

    @Transactional
    public void deleteByDomainIdAndTypeIdAndStateCode(long domainId, long ticketTypeId, String stateCode) {
        ticketTypeFlowService.deleteTransitionsByStateCode(domainId, ticketTypeId, stateCode);
    }
}
