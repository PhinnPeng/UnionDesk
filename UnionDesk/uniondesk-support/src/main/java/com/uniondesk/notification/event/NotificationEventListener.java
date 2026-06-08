package com.uniondesk.notification.event;

import com.uniondesk.common.event.TicketStatusChangedEvent;
import com.uniondesk.notification.core.NotificationCenterService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationEventListener {

    private final NotificationCenterService notificationCenterService;

    public NotificationEventListener(NotificationCenterService notificationCenterService) {
        this.notificationCenterService = notificationCenterService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketStatusChanged(TicketStatusChangedEvent event) {
        if (event.customerId() == null) {
            return;
        }
        notificationCenterService.notifyTicketStatusChanged(
                event.businessDomainId(),
                event.ticketId(),
                event.customerId(),
                event.actorUserId(),
                event.newStatus());
    }
}
