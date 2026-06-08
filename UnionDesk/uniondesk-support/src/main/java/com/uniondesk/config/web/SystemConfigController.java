package com.uniondesk.config.web;

import com.uniondesk.config.core.SystemConfigService;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/system-config")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @GetMapping
    @RequirePermission(PermissionCodes.PLATFORM_SYSTEM_CONFIG_READ)
    public SystemConfigService.SystemConfigView getSystemConfig() {
        return systemConfigService.load();
    }

    @PutMapping
    @RequirePermission(PermissionCodes.PLATFORM_SYSTEM_CONFIG_UPDATE)
    public SystemConfigService.SystemConfigView updateSystemConfig(
            @Valid @RequestBody SystemConfigService.SystemConfigUpdateCommand request) {
        return systemConfigService.update(request);
    }
}
