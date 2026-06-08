package com.uniondesk.notification.web;

import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import com.uniondesk.notification.core.NotificationCenterService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inbox")
public class InboxController {

    private final NotificationCenterService notificationCenterService;

    public InboxController(NotificationCenterService notificationCenterService) {
        this.notificationCenterService = notificationCenterService;
    }

    @GetMapping
    @RequirePermission(PermissionCodes.INBOX_READ)
    public List<NotificationCenterService.InboxMessageView> listInbox(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "100") int limit) {
        return notificationCenterService.listInboxMessages(UserContextHolder.requireCurrent().userId(), unreadOnly, limit);
    }

    @GetMapping("/unread-count")
    @RequirePermission(PermissionCodes.INBOX_READ)
    public Map<String, Long> unreadCount() {
        long unreadCount = notificationCenterService.unreadCount(UserContextHolder.requireCurrent().userId());
        return Map.of("unreadCount", unreadCount);
    }

    @PostMapping("/{message_id}/read")
    @RequirePermission(PermissionCodes.INBOX_MARK_READ)
    public Map<String, Object> markRead(@PathVariable("message_id") long messageId) {
        int updated = notificationCenterService.markRead(UserContextHolder.requireCurrent().userId(), messageId);
        return Map.of("ok", updated > 0, "updated", updated);
    }

    @PostMapping("/read-batch")
    @RequirePermission(PermissionCodes.INBOX_MARK_READ)
    public Map<String, Object> markReadBatch(@Valid @RequestBody List<Long> messageIds) {
        int updated = notificationCenterService.markReadBatch(UserContextHolder.requireCurrent().userId(), messageIds);
        return Map.of("ok", true, "updated", updated);
    }
}
