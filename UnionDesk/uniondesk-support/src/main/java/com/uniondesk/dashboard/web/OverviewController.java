package com.uniondesk.dashboard.web;

import com.uniondesk.common.web.ApiResponse;
import com.uniondesk.dashboard.core.OverviewService;
import com.uniondesk.dashboard.web.OverviewDtos.OverviewResponse;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台概览聚合端点（生产 profile，与 demo 的 DashboardController 并存）。
 */
@RestController
@RequestMapping("/api/v1/dashboard/overview")
public class OverviewController {

    private final OverviewService overviewService;

    public OverviewController(OverviewService overviewService) {
        this.overviewService = overviewService;
    }

    @GetMapping
    @RequirePermission(PermissionCodes.PLATFORM_DASHBOARD_READ)
    public ApiResponse<OverviewResponse> overview() {
        return ApiResponse.ok(overviewService.overview());
    }
}
