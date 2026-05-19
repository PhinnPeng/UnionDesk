package com.uniondesk.iam.web;

import com.uniondesk.iam.core.OrganizationService;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/iam/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    @RequirePermission(PermissionCodes.PLATFORM_ORGANIZATION_READ)
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
                        unit.remark(),
                        unit.createdAt()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.PLATFORM_ORGANIZATION_CREATE)
    public OrganizationDtos.OrganizationUnitView createOrganization(
            @Valid @RequestBody OrganizationDtos.CreateOrganizationRequest request) {
        OrganizationService.OrganizationUnit unit = organizationService.createOrganization(
                new OrganizationService.CreateOrganizationCommand(
                        request.code(),
                        request.name(),
                        request.parentId(),
                        request.leaderUserId(),
                        request.orderNo(),
                        request.status(),
                        request.remark()));
        return toView(unit);
    }

    @PutMapping("/{id}")
    @RequirePermission(PermissionCodes.PLATFORM_ORGANIZATION_UPDATE)
    public OrganizationDtos.OrganizationUnitView updateOrganization(
            @PathVariable long id,
            @Valid @RequestBody OrganizationDtos.UpdateOrganizationRequest request) {
        OrganizationService.OrganizationUnit unit = organizationService.updateOrganization(
                id,
                new OrganizationService.UpdateOrganizationCommand(
                        request.code(),
                        request.name(),
                        request.parentId(),
                        request.leaderUserId(),
                        request.orderNo(),
                        request.status(),
                        request.remark()));
        return toView(unit);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.PLATFORM_ORGANIZATION_DELETE)
    public void deleteOrganization(@PathVariable long id) {
        organizationService.deleteOrganization(id);
    }

    private OrganizationDtos.OrganizationUnitView toView(OrganizationService.OrganizationUnit unit) {
        return new OrganizationDtos.OrganizationUnitView(
                unit.id(),
                unit.code(),
                unit.name(),
                unit.parentId(),
                unit.parentName(),
                unit.leaderUserId(),
                unit.leaderName(),
                unit.orderNo(),
                unit.status(),
                unit.remark(),
                unit.createdAt());
    }
}
