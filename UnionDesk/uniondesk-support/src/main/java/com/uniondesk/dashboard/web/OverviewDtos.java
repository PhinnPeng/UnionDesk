package com.uniondesk.dashboard.web;

/**
 * 平台概览聚合 DTO（生产 profile，与 demo 的 DashboardDtos 并存）。
 */
public final class OverviewDtos {

    private OverviewDtos() {
    }

    /**
     * 平台概览聚合结果：各模块实时 count 汇总，与前端 /platform/home 概览卡一一对应。
     */
    public record OverviewResponse(
            long domainCount,
            long staffCount,
            long activeUserCount,
            long disabledUserCount,
            long offboardUserCount,
            long customerCount,
            long ticketCount,
            long consultationCount,
            long recentAuditCount) {
    }
}
