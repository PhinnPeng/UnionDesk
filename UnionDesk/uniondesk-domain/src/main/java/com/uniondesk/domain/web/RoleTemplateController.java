package com.uniondesk.domain.web;

import com.uniondesk.domain.core.RoleTemplateService;
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
@RequestMapping("/api/v1/iam/role-templates")
public class RoleTemplateController {

    private final RoleTemplateService roleTemplateService;

    public RoleTemplateController(RoleTemplateService roleTemplateService) {
        this.roleTemplateService = roleTemplateService;
    }

    @GetMapping
    @RequirePermission(PermissionCodes.PLATFORM_ROLE_TEMPLATE_READ)
    public RoleTemplateDtos.RoleTemplateListView listTemplates() {
        List<RoleTemplateDtos.RoleTemplateView> items = roleTemplateService.listTemplates();
        return new RoleTemplateDtos.RoleTemplateListView(items.size(), items);
    }

    @GetMapping("/{templateId}")
    @RequirePermission(PermissionCodes.PLATFORM_ROLE_TEMPLATE_READ)
    public RoleTemplateDtos.RoleTemplateDetailView detail(@PathVariable("templateId") long templateId) {
        return roleTemplateService.detail(templateId);
    }

    @GetMapping("/permission-items")
    @RequirePermission(PermissionCodes.PLATFORM_ROLE_TEMPLATE_READ)
    public RoleTemplateDtos.PermissionItemListView listPermissionItems() {
        List<RoleTemplateDtos.PermissionItemView> items = roleTemplateService.listPermissionItems();
        return new RoleTemplateDtos.PermissionItemListView(items.size(), items);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.PLATFORM_ROLE_TEMPLATE_CREATE)
    public RoleTemplateDtos.RoleTemplateView createTemplate(
            @Valid @RequestBody RoleTemplateDtos.CreateRoleTemplateRequest request) {
        return roleTemplateService.createTemplate(request);
    }

    @PutMapping("/{templateId}")
    @RequirePermission(PermissionCodes.PLATFORM_ROLE_TEMPLATE_UPDATE)
    public RoleTemplateDtos.RoleTemplateView updateTemplate(
            @PathVariable("templateId") long templateId,
            @Valid @RequestBody RoleTemplateDtos.UpdateRoleTemplateRequest request) {
        return roleTemplateService.updateTemplate(templateId, request);
    }

    @DeleteMapping("/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.PLATFORM_ROLE_TEMPLATE_DELETE)
    public void deleteTemplate(@PathVariable("templateId") long templateId) {
        roleTemplateService.deleteTemplate(templateId);
    }

    @PostMapping("/{templateId}/apply")
    @RequirePermission(PermissionCodes.PLATFORM_ROLE_TEMPLATE_APPLY)
    public RoleTemplateDtos.BatchResult apply(
            @PathVariable("templateId") long templateId,
            @Valid @RequestBody RoleTemplateDtos.ApplyRequest request) {
        return roleTemplateService.apply(templateId, request);
    }

    @PostMapping("/{templateId}/sync")
    @RequirePermission(PermissionCodes.PLATFORM_ROLE_TEMPLATE_SYNC)
    public RoleTemplateDtos.BatchResult sync(
            @PathVariable("templateId") long templateId,
            @RequestBody(required = false) RoleTemplateDtos.SyncRequest request) {
        return roleTemplateService.sync(templateId, request);
    }

    @PostMapping("/{templateId}/unapply")
    @RequirePermission(PermissionCodes.PLATFORM_ROLE_TEMPLATE_APPLY)
    public RoleTemplateDtos.BatchResult unapply(
            @PathVariable("templateId") long templateId,
            @Valid @RequestBody RoleTemplateDtos.UnapplyRequest request) {
        return roleTemplateService.unapply(templateId, request);
    }

    @PostMapping("/{templateId}/bind-members")
    @RequirePermission(PermissionCodes.PLATFORM_ROLE_TEMPLATE_APPLY)
    public RoleTemplateDtos.BatchResult bindMembers(
            @PathVariable("templateId") long templateId,
            @Valid @RequestBody RoleTemplateDtos.BindMembersRequest request) {
        return roleTemplateService.bindMembers(templateId, request);
    }
}
