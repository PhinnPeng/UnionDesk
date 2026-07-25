package com.uniondesk.ticket.core;

import com.uniondesk.ticket.entity.TicketWatcherPo;
import com.uniondesk.ticket.mapper.TicketWatcherMapper;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketWatcherService {

    private final TicketWatcherMapper ticketWatcherMapper;

    public TicketWatcherService(TicketWatcherMapper ticketWatcherMapper) {
        this.ticketWatcherMapper = ticketWatcherMapper;
    }

    public List<Long> listStaffIds(long ticketId) {
        return ticketWatcherMapper.findStaffIdsByTicketId(ticketId);
    }

    @Transactional
    public void replaceWatchers(long ticketId, List<Long> staffAccountIds) {
        ticketWatcherMapper.deleteByTicketId(ticketId);
        if (staffAccountIds == null || staffAccountIds.isEmpty()) {
            return;
        }
        LinkedHashSet<Long> unique = new LinkedHashSet<>(staffAccountIds);
        for (Long staffId : unique) {
            if (staffId == null) {
                continue;
            }
            TicketWatcherPo po = new TicketWatcherPo();
            po.setTicketId(ticketId);
            po.setStaffAccountId(staffId);
            ticketWatcherMapper.insert(po);
        }
    }
}
