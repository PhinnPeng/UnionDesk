package com.uniondesk.notification.repository;

import com.uniondesk.notification.entity.InboxMessagePo;
import com.uniondesk.notification.mapper.InboxMessageMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class InboxMessageRepository {

    private final InboxMessageMapper mapper;

    public InboxMessageRepository(InboxMessageMapper mapper) {
        this.mapper = mapper;
    }

    public void save(InboxMessagePo po) {
        mapper.insert(po);
    }

    public long countUnread(long recipientSubjectId) {
        Long count = mapper.countUnread(recipientSubjectId);
        return count == null ? 0L : count;
    }

    public List<InboxMessagePo> listByRecipient(long recipientSubjectId, boolean unreadOnly, int limit) {
        return mapper.listByRecipient(recipientSubjectId, unreadOnly, limit);
    }

    public int markRead(long recipientSubjectId, long inboxMessageId) {
        return mapper.markRead(recipientSubjectId, inboxMessageId);
    }

    public int markReadBatch(long recipientSubjectId, List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return 0;
        }
        return mapper.markReadBatch(recipientSubjectId, messageIds);
    }

    public long findLatestId(long recipientSubjectId, long notificationLogId) {
        Long id = mapper.findLatestId(recipientSubjectId, notificationLogId);
        if (id == null) {
            throw new IllegalStateException("inbox message not found");
        }
        return id;
    }
}
