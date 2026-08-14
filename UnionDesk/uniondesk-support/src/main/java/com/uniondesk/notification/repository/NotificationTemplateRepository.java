package com.uniondesk.notification.repository;

import com.mybatisflex.core.paginate.Page;
import com.uniondesk.notification.entity.NotificationTemplatePo;
import com.uniondesk.notification.mapper.NotificationTemplateMapper;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationTemplateRepository {

    private final NotificationTemplateMapper mapper;

    public NotificationTemplateRepository(NotificationTemplateMapper mapper) {
        this.mapper = mapper;
    }

    public Page<NotificationTemplatePo> findPageByDomainId(Page<NotificationTemplatePo> page, long domainId) {
        return mapper.selectPageByDomainId(page, domainId);
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
}
