package com.uniondesk.dashboard.web;

import com.uniondesk.common.web.ApiResponse;
import com.uniondesk.common.demo.DemoDataService;
import com.uniondesk.common.demo.DemoDtos.DashboardResponse;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Profile("demo")
public class DashboardController {

    private final DemoDataService demoDataService;

    public DashboardController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @GetMapping
    @RequirePermission(PermissionCodes.PLATFORM_DASHBOARD_READ)
    public ApiResponse<DashboardResponse> dashboard(@RequestParam(required = false) Long businessDomainId) {
        return ApiResponse.ok(demoDataService.dashboard(businessDomainId));
    }
}
