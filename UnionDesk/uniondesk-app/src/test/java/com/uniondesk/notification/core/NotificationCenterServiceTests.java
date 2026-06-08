package com.uniondesk.notification.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.audit.repository.AuditLogWriteRepository;
import com.uniondesk.common.repository.IdentityResolutionRepository;
import com.uniondesk.notification.entity.InboxMessagePo;
import com.uniondesk.notification.entity.NotificationLogPo;
import com.uniondesk.notification.repository.InboxMessageRepository;
import com.uniondesk.notification.repository.NotificationLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationCenterServiceTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-03T08:00:00Z"), ZoneOffset.UTC);

    @Mock
    private NotificationLogRepository notificationLogRepository;
    @Mock
    private InboxMessageRepository inboxMessageRepository;
    @Mock
    private AuditLogWriteRepository auditLogWriteRepository;
    @Mock
    private IdentityResolutionRepository identityResolutionRepository;

    private final AtomicLong notificationLogSequence = new AtomicLong();
    private final AtomicLong inboxMessageSequence = new AtomicLong();
    private final AtomicLong unreadMessageCount = new AtomicLong();

    private NotificationCenterService notificationCenterService;

    @BeforeEach
    void setUp() {
        notificationCenterService = new NotificationCenterService(
                notificationLogRepository,
                inboxMessageRepository,
                auditLogWriteRepository,
                identityResolutionRepository,
                new ObjectMapper(),
                CLOCK,
                false);
        stubIdentityLookups();
        stubPersistence();
    }

    @Test
    void notifyTicketCreatedCreatesUnreadInboxMessageAndUnreadCount() {
        NotificationCenterService.NotificationDispatchResult result =
                notificationCenterService.notifyTicketCreated(1L, 101L, 1001L, 2002L);

        assertThat(result.status()).isEqualTo("degraded");
        assertThat(result.notificationLogId()).isEqualTo(1L);
        assertThat(result.inboxMessageId()).isEqualTo(1L);
        assertThat(notificationCenterService.unreadCount(1001L)).isEqualTo(1L);

        assertThat(notificationCenterService.markRead(1001L, result.inboxMessageId())).isEqualTo(1);
        unreadMessageCount.set(0L);
        assertThat(notificationCenterService.unreadCount(1001L)).isEqualTo(0L);
    }

    private void stubIdentityLookups() {
        when(identityResolutionRepository.ensureIdentitySubject(anyLong())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubPersistence() {
        doAnswer(invocation -> {
            NotificationLogPo po = invocation.getArgument(0);
            long id = notificationLogSequence.incrementAndGet();
            po.setId(id);
            return null;
        }).when(notificationLogRepository).save(any(NotificationLogPo.class));

        doAnswer(invocation -> {
            InboxMessagePo po = invocation.getArgument(0);
            long id = inboxMessageSequence.incrementAndGet();
            po.setId(id);
            unreadMessageCount.incrementAndGet();
            return null;
        }).when(inboxMessageRepository).save(any(InboxMessagePo.class));

        when(inboxMessageRepository.countUnread(anyLong())).thenAnswer(invocation -> unreadMessageCount.get());
        when(inboxMessageRepository.markRead(anyLong(), anyLong())).thenAnswer(invocation -> {
            unreadMessageCount.set(0L);
            return 1;
        });
    }
}
