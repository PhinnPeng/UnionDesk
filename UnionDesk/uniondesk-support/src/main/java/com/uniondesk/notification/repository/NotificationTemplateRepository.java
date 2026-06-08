package com.uniondesk.notification.repository;

import com.uniondesk.notification.entity.NotificationTemplatePo;
import com.uniondesk.notification.mapper.NotificationTemplateMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationTemplateRepository {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationTemplateMapper mapper;

    public NotificationTemplateRepository(NotificationTemplateMapper mapper) {
        this.mapper = mapper;
    }

    public List<NotificationTemplatePo> findByDomainId(long domainId, int page, int pageSize) {
        int normalizedPageSize = normalizePageSize(pageSize);
        long offset = (long) (Math.max(page, 1) - 1) * normalizedPageSize;
        return mapper.selectByDomainId(domainId, normalizedPageSize, offset);
    }

    public long countByDomainId(long domainId) {
        return mapper.countByDomainId(domainId);
    }

    public NotificationTemplatePo findByIdAndDomainId(long templateId, long domainId) {
        return mapper.selectByIdAndDomainId(templateId, domainId);
    }

    public void updateByIdAndDomainId(long templateId, long domainId,
                                       String eventCategory, String channel, String code,
                                       String titleTemplate, String contentTemplate,
                                       boolean isSecurity, boolean isUnsubscribable,
                                       String status) {
        mapper.updateByIdAndDomainId(templateId, domainId,
                eventCategory, channel, code, titleTemplate, contentTemplate,
                isSecurity, isUnsubscribable, status);
    }

    private int normalizePageSize(int pageSize) {
        return Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
    }
}
