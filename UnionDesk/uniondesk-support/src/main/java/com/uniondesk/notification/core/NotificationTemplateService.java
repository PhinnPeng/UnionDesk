package com.uniondesk.notification.core;

import com.uniondesk.common.web.PageResult;
import com.uniondesk.notification.entity.NotificationTemplatePo;
import com.uniondesk.notification.repository.NotificationTemplateRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class NotificationTemplateService {

    private final NotificationTemplateRepository notificationTemplateRepository;

    public NotificationTemplateService(NotificationTemplateRepository notificationTemplateRepository) {
        this.notificationTemplateRepository = notificationTemplateRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<NotificationTemplateView> list(long domainId, int page, int pageSize) {
        long total = notificationTemplateRepository.countByDomainId(domainId);
        List<NotificationTemplatePo> rows = notificationTemplateRepository.findByDomainId(domainId, page, pageSize);
        return new PageResult<>(total, rows.stream().map(this::toView).toList());
    }

    @Transactional
    public NotificationTemplateView update(long domainId, long templateId, UpdateNotificationTemplateCommand command) {
        notificationTemplateRepository.updateByIdAndDomainId(
                templateId,
                domainId,
                normalizeText(command.eventCategory(), "notification"),
                normalizeText(command.channel(), "email"),
                normalizeText(command.code(), "template"),
                normalizeText(command.titleTemplate(), ""),
                normalizeText(command.contentTemplate(), ""),
                command.isSecurity() != null && command.isSecurity(),
                command.isUnsubscribable() != null && command.isUnsubscribable(),
                normalizeText(command.status(), "active"));
        return load(domainId, templateId);
    }

    @Transactional(readOnly = true)
    public NotificationTemplateView load(long domainId, long templateId) {
        NotificationTemplatePo po = notificationTemplateRepository.findByIdAndDomainId(templateId, domainId);
        return toView(po);
    }

    private NotificationTemplateView toView(NotificationTemplatePo po) {
        return new NotificationTemplateView(
                po.getId(),
                po.getScopeType(),
                po.getScopeId(),
                po.getEventCategory(),
                po.getChannel(),
                po.getCode(),
                po.getTitleTemplate(),
                po.getContentTemplate(),
                po.getIsSecurity() != null && po.getIsSecurity(),
                po.getIsUnsubscribable() != null && po.getIsUnsubscribable(),
                po.getStatus(),
                po.getCreatedAt(),
                po.getUpdatedAt());
    }

    private String normalizeText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    public record NotificationTemplateView(
            long id,
            String scopeType,
            long scopeId,
            String eventCategory,
            String channel,
            String code,
            String titleTemplate,
            String contentTemplate,
            boolean isSecurity,
            boolean isUnsubscribable,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record UpdateNotificationTemplateCommand(
            String eventCategory,
            String channel,
            String code,
            String titleTemplate,
            String contentTemplate,
            Boolean isSecurity,
            Boolean isUnsubscribable,
            String status) {
    }
}
