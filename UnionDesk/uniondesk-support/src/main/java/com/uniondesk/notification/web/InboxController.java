package com.uniondesk.notification.web;

import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import com.uniondesk.notification.core.NotificationCenterService;
import jakarta.validation.Valid;
import java.util.List;
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

    // --- View records ---

    public record InboxMessageListView(long total, List<NotificationCenterService.InboxMessageView> items) {}

    public record UnreadCountView(long unreadCount) {}

    public record MarkReadResultView(boolean ok, int updated) {}

    @GetMapping
    @RequirePermission(PermissionCodes.INBOX_READ)
    public InboxMessageListView listInbox(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "100") int limit) {
        List<NotificationCenterService.InboxMessageView> items = notificationCenterService.listInboxMessages(UserContextHolder.requireCurrent().userId(), unreadOnly, limit);
        return new InboxMessageListView(items.size(), items);
    }

    @GetMapping("/unread-count")
    @RequirePermission(PermissionCodes.INBOX_READ)
    public UnreadCountView unreadCount() {
        long unreadCount = notificationCenterService.unreadCount(UserContextHolder.requireCurrent().userId());
        return new UnreadCountView(unreadCount);
    }

    @PostMapping("/{message_id}/read")
    @RequirePermission(PermissionCodes.INBOX_MARK_READ)
    public MarkReadResultView markRead(@PathVariable("message_id") long messageId) {
        int updated = notificationCenterService.markRead(UserContextHolder.requireCurrent().userId(), messageId);
        return new MarkReadResultView(updated > 0, updated);
    }

    @PostMapping("/read-batch")
    @RequirePermission(PermissionCodes.INBOX_MARK_READ)
    public MarkReadResultView markReadBatch(@Valid @RequestBody List<Long> messageIds) {
        int updated = notificationCenterService.markReadBatch(UserContextHolder.requireCurrent().userId(), messageIds);
        return new MarkReadResultView(true, updated);
    }
}
