package com.uniondesk.notification.repository;

import com.uniondesk.notification.entity.NotificationLogPo;
import com.uniondesk.notification.mapper.NotificationLogMapper;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationLogRepository {

    private final NotificationLogMapper mapper;

    public NotificationLogRepository(NotificationLogMapper mapper) {
        this.mapper = mapper;
    }

    public void save(NotificationLogPo po) {
        mapper.insert(po);
    }

    public long findLatestId(long recipientSubjectId, long sourceId, String templateCode) {
        Long id = mapper.findLatestId(recipientSubjectId, sourceId, templateCode);
        if (id == null) {
            throw new IllegalStateException("notification log not found");
        }
        return id;
    }
}
