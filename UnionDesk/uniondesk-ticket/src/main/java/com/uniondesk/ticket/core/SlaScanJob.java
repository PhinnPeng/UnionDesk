package com.uniondesk.ticket.core;

import com.uniondesk.ticket.entity.SlaScanCandidatePo;
import com.uniondesk.ticket.repository.TicketRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * SLA 定时扫描：每分钟兜底处置超时工单（与工单事件触点 refreshTicketSla 共享幂等 evaluateTicket）。
 * 单实例部署，无分布式锁（MVP）；单条失败隔离，不中断整批。
 */
@Component
public class SlaScanJob {

    private static final Logger log = LoggerFactory.getLogger(SlaScanJob.class);

    private static final int BATCH_LIMIT = 100;

    private final TicketRepository ticketRepository;
    private final TicketService ticketService;

    public SlaScanJob(TicketRepository ticketRepository, TicketService ticketService) {
        this.ticketRepository = ticketRepository;
        this.ticketService = ticketService;
    }

    @Scheduled(cron = "0 * * * * *")
    public void scanOverdueTickets() {
        List<SlaScanCandidatePo> candidates = ticketRepository.findSlaScanCandidates(BATCH_LIMIT);
        if (candidates.isEmpty()) {
            return;
        }
        log.info("SLA 扫描候选工单数：{}", candidates.size());
        for (SlaScanCandidatePo candidate : candidates) {
            try {
                ticketService.processSlaBreach(candidate.getBusinessDomainId(), candidate.getId());
            } catch (Exception ex) {
                log.warn("SLA 扫描处置失败（跳过）：ticketId={}, domainId={}",
                        candidate.getId(), candidate.getBusinessDomainId(), ex);
            }
        }
    }
}
