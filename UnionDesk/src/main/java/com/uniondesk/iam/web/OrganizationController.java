package com.uniondesk.iam.web;

import com.uniondesk.iam.core.OrganizationService;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/iam/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    @RequirePermission(PermissionCodes.PLATFORM_USER_READ)
    public List<OrganizationDtos.OrganizationUnitView> listOrganizations() {
        return organizationService.listOrganizations().stream()
                .map(unit -> new OrganizationDtos.OrganizationUnitView(
                        unit.id(),
                        unit.code(),
                        unit.name(),
                        unit.parentId(),
                        unit.parentName(),
                        unit.leaderUserId(),
                        unit.leaderName(),
                        unit.orderNo(),
                        unit.status(),
                        unit.remark()))
                .toList();
    }
}
