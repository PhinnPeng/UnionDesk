package com.uniondesk.notification.web;

import com.uniondesk.common.web.PageResult;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import com.uniondesk.notification.core.NotificationTemplateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/domains/{domainId}/notification-templates")
public class NotificationTemplateController {

    private final NotificationTemplateService notificationTemplateService;

    public NotificationTemplateController(NotificationTemplateService notificationTemplateService) {
        this.notificationTemplateService = notificationTemplateService;
    }

    @GetMapping
    @RequirePermission(PermissionCodes.DOMAIN_NOTIFICATION_TEMPLATE_READ)
    public PageResult<NotificationTemplateService.NotificationTemplateView> list(
            @PathVariable long domainId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return notificationTemplateService.list(domainId, page, pageSize);
    }

    @PutMapping("/{templateId}")
    @RequirePermission(PermissionCodes.DOMAIN_NOTIFICATION_TEMPLATE_UPDATE)
    public NotificationTemplateService.NotificationTemplateView update(
            @PathVariable long domainId,
            @PathVariable long templateId,
            @Valid @RequestBody NotificationTemplateService.UpdateNotificationTemplateCommand request) {
        return notificationTemplateService.update(domainId, templateId, request);
    }
}
