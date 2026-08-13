package com.uniondesk.dashboard.core;

import com.uniondesk.dashboard.mapper.OverviewMapper;
import com.uniondesk.dashboard.web.OverviewDtos.OverviewResponse;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 平台概览聚合服务：各模块表轻量 count(*) 汇总。
 */
@Service
@Transactional(readOnly = true)
public class OverviewService {

    private static final int RECENT_AUDIT_HOURS = 24;

    private final OverviewMapper overviewMapper;

    public OverviewService(OverviewMapper overviewMapper) {
        this.overviewMapper = overviewMapper;
    }

    public OverviewResponse overview() {
        LocalDateTime since = LocalDateTime.now().minusHours(RECENT_AUDIT_HOURS);
        return new OverviewResponse(
                overviewMapper.countBusinessDomains(),
                overviewMapper.countStaff(),
                overviewMapper.countActiveStaff(),
                overviewMapper.countDisabledStaff(),
                overviewMapper.countOffboardedStaff(),
                overviewMapper.countCustomers(),
                overviewMapper.countTickets(),
                overviewMapper.countConsultationSessions(),
                overviewMapper.countLoginLogsSince(since));
    }
}
