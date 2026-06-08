package com.uniondesk.ticket.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.ticket.entity.QuickReplyTemplatePo;
import com.uniondesk.ticket.entity.TicketPriorityLevelPo;
import com.uniondesk.ticket.entity.TicketTemplatePo;
import com.uniondesk.ticket.entity.TicketTypePo;
import com.uniondesk.ticket.repository.QuickReplyTemplateRepository;
import com.uniondesk.ticket.repository.TicketPriorityLevelRepository;
import com.uniondesk.ticket.repository.TicketTemplateRepository;
import com.uniondesk.ticket.repository.TicketTypeRepository;
import com.uniondesk.ticket.web.TicketConfigDtos;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TicketConfigService {

    private final TicketTypeRepository ticketTypeRepository;
    private final TicketTemplateRepository ticketTemplateRepository;
    private final QuickReplyTemplateRepository quickReplyTemplateRepository;
    private final TicketPriorityLevelRepository ticketPriorityLevelRepository;
    private final ObjectMapper objectMapper;

    public TicketConfigService(
            TicketTypeRepository ticketTypeRepository,
            TicketTemplateRepository ticketTemplateRepository,
            QuickReplyTemplateRepository quickReplyTemplateRepository,
            TicketPriorityLevelRepository ticketPriorityLevelRepository,
            ObjectMapper objectMapper) {
        this.ticketTypeRepository = ticketTypeRepository;
        this.ticketTemplateRepository = ticketTemplateRepository;
        this.quickReplyTemplateRepository = quickReplyTemplateRepository;
        this.ticketPriorityLevelRepository = ticketPriorityLevelRepository;
        this.objectMapper = objectMapper;
    }

    public List<TicketConfigDtos.TicketTypeView> listTicketTypes(long domainId) {
        return ticketTypeRepository.findByDomainId(domainId).stream()
                .map(this::toTicketTypeView)
                .toList();
    }

    @Transactional
    public TicketConfigDtos.TicketTypeView createTicketType(long domainId, TicketConfigDtos.CreateTicketTypeRequest request) {
        String code = requiredText(request.code(), "code");
        String name = requiredText(request.name(), "name");
        Object dynamicFields = request.dynamic_fields();
        TicketTypePo po = new TicketTypePo();
        po.setBusinessDomainId(domainId);
        po.setCode(code);
        po.setName(name);
        po.setStatusFlowConfig(toJson(dynamicFields));
        ticketTypeRepository.save(po);
        return new TicketConfigDtos.TicketTypeView(String.valueOf(po.getId()), String.valueOf(domainId), code, name, dynamicFields, "active");
    }

    @Transactional
    public TicketConfigDtos.TicketTypeView updateTicketType(long domainId, long typeId, TicketConfigDtos.UpdateTicketTypeRequest request) {
        TicketTypePo existing = ticketTypeRepository.findRequiredByIdAndDomainId(typeId, domainId);
        String name = StringUtils.hasText(request.name()) ? request.name().trim() : existing.getName();
        Object dynamicFields = request.dynamic_fields() == null ? readJsonObject(existing.getStatusFlowConfig()) : request.dynamic_fields();
        ticketTypeRepository.update(typeId, domainId, name, toJson(dynamicFields));
        return new TicketConfigDtos.TicketTypeView(
                String.valueOf(existing.getId()),
                String.valueOf(domainId),
                existing.getCode(),
                name,
                dynamicFields,
                "active");
    }

    @Transactional
    public void deleteTicketType(long domainId, long typeId) {
        int updated = ticketTypeRepository.deleteByIdAndDomainId(typeId, domainId);
        if (updated == 0) {
            throw new IllegalArgumentException("ticket type not found");
        }
    }

    public List<TicketConfigDtos.TicketTemplateView> listTicketTemplates(long domainId) {
        return ticketTemplateRepository.findByDomainId(domainId).stream()
                .map(this::toTicketTemplateView)
                .toList();
    }

    @Transactional
    public TicketConfigDtos.TicketTemplateView createTicketTemplate(long domainId, TicketConfigDtos.CreateTicketTemplateRequest request) {
        String name = requiredText(request.name(), "name");
        String scope = normalizeTemplateType(request.type());
        long ticketTypeId = request.type_id() == null ? findDefaultTicketTypeId(domainId) : parseLong(request.type_id(), "type_id");
        int sortOrder = request.sort_order() == null ? 0 : request.sort_order();
        Map<String, Object> contentJson = buildTicketTemplateContent(request.fields_snapshot(), request.content());
        TicketTemplatePo po = new TicketTemplatePo();
        po.setBusinessDomainId(domainId);
        po.setTicketTypeId(ticketTypeId);
        po.setScope(scope);
        po.setName(name);
        po.setContentJson(toJson(contentJson));
        po.setStatus("active");
        po.setSortOrder(sortOrder);
        ticketTemplateRepository.save(po);
        return new TicketConfigDtos.TicketTemplateView(
                String.valueOf(po.getId()),
                String.valueOf(domainId),
                name,
                toResponseTemplateType(scope),
                String.valueOf(ticketTypeId),
                request.fields_snapshot(),
                request.content(),
                sortOrder);
    }

    @Transactional
    public TicketConfigDtos.TicketTemplateView updateTicketTemplate(long domainId, long templateId, TicketConfigDtos.UpdateTicketTemplateRequest request) {
        TicketTemplatePo existing = ticketTemplateRepository.findRequiredByIdAndDomainId(templateId, domainId);
        String name = StringUtils.hasText(request.name()) ? request.name().trim() : existing.getName();
        String scope = StringUtils.hasText(request.type()) ? normalizeTemplateType(request.type()) : existing.getScope();
        long ticketTypeId = request.type_id() == null ? existing.getTicketTypeId() : parseLong(request.type_id(), "type_id");
        TicketTemplateContent existingContent = readTicketTemplateContent(existing.getContentJson());
        Object fieldsSnapshot = request.fields_snapshot() == null ? existingContent.fieldsSnapshot() : request.fields_snapshot();
        String content = request.content() == null ? existingContent.content() : request.content();
        int sortOrder = request.sort_order() == null ? existing.getSortOrder() : request.sort_order();
        ticketTemplateRepository.update(
                templateId,
                domainId,
                ticketTypeId,
                scope,
                name,
                toJson(buildTicketTemplateContent(fieldsSnapshot, content)),
                sortOrder);
        return new TicketConfigDtos.TicketTemplateView(
                String.valueOf(existing.getId()),
                String.valueOf(domainId),
                name,
                toResponseTemplateType(scope),
                String.valueOf(ticketTypeId),
                fieldsSnapshot,
                content,
                sortOrder);
    }

    @Transactional
    public void deleteTicketTemplate(long domainId, long templateId) {
        int updated = ticketTemplateRepository.deleteByIdAndDomainId(templateId, domainId);
        if (updated == 0) {
            throw new IllegalArgumentException("ticket template not found");
        }
    }

    public List<TicketConfigDtos.QuickReplyView> listQuickReplies(long domainId) {
        return quickReplyTemplateRepository.findByDomainId(domainId).stream()
                .map(this::toQuickReplyView)
                .toList();
    }

    @Transactional
    public TicketConfigDtos.QuickReplyView createQuickReply(long domainId, TicketConfigDtos.CreateQuickReplyRequest request) {
        String title = requiredText(request.title(), "title");
        String content = requiredText(request.content(), "content");
        String scope = normalizeQuickReplyScope(request.scope());
        int sortOrder = request.sort_order() == null ? 0 : request.sort_order();
        QuickReplyTemplatePo po = new QuickReplyTemplatePo();
        po.setBusinessDomainId(domainId);
        po.setScopeType(scope);
        po.setTitle(title);
        po.setContent(content);
        po.setSortOrder(sortOrder);
        quickReplyTemplateRepository.save(po);
        return toQuickReplyView(po);
    }

    @Transactional
    public TicketConfigDtos.QuickReplyView updateQuickReply(long domainId, long replyId, TicketConfigDtos.UpdateQuickReplyRequest request) {
        QuickReplyTemplatePo existing = quickReplyTemplateRepository.findRequiredByIdAndDomainId(replyId, domainId);
        String title = StringUtils.hasText(request.title()) ? request.title().trim() : existing.getTitle();
        String content = StringUtils.hasText(request.content()) ? request.content().trim() : existing.getContent();
        String scope = StringUtils.hasText(request.scope()) ? normalizeQuickReplyScope(request.scope()) : existing.getScopeType();
        int sortOrder = request.sort_order() == null ? existing.getSortOrder() : request.sort_order();
        quickReplyTemplateRepository.update(replyId, domainId, scope, title, content, sortOrder);
        return new TicketConfigDtos.QuickReplyView(
                String.valueOf(existing.getId()),
                String.valueOf(domainId),
                title,
                content,
                scope,
                sortOrder,
                toDateTimeString(existing.getCreatedAt()));
    }

    @Transactional
    public void deleteQuickReply(long domainId, long replyId) {
        int updated = quickReplyTemplateRepository.deleteByIdAndDomainId(replyId, domainId);
        if (updated == 0) {
            throw new IllegalArgumentException("quick reply not found");
        }
    }

    public List<TicketConfigDtos.PriorityLevelView> listPriorityLevels(long domainId) {
        return ticketPriorityLevelRepository.findByDomainId(domainId).stream()
                .map(this::toPriorityLevelView)
                .toList();
    }

    @Transactional
    public TicketConfigDtos.PriorityLevelView createPriorityLevel(long domainId, TicketConfigDtos.CreatePriorityLevelRequest request) {
        String label = resolvePriorityLabel(request.name(), request.display_label());
        int sortOrder = request.sort_order() == null ? 0 : request.sort_order();
        boolean isDefault = Boolean.TRUE.equals(request.is_default());
        if (isDefault) {
            ticketPriorityLevelRepository.clearDefaults(domainId);
        }
        TicketPriorityLevelPo po = new TicketPriorityLevelPo();
        po.setBusinessDomainId(domainId);
        po.setCode(label);
        po.setName(label);
        po.setSortOrder(sortOrder);
        po.setIsDefault(isDefault);
        po.setStatus("active");
        ticketPriorityLevelRepository.save(po);
        return new TicketConfigDtos.PriorityLevelView(
                String.valueOf(po.getId()),
                String.valueOf(domainId),
                label,
                label,
                request.color(),
                sortOrder,
                isDefault);
    }

    @Transactional
    public TicketConfigDtos.PriorityLevelView updatePriorityLevel(long domainId, long levelId, TicketConfigDtos.UpdatePriorityLevelRequest request) {
        TicketPriorityLevelPo existing = ticketPriorityLevelRepository.findRequiredByIdAndDomainId(levelId, domainId);
        String label = StringUtils.hasText(request.display_label())
                ? request.display_label().trim()
                : (StringUtils.hasText(request.name()) ? request.name().trim() : existing.getName());
        int sortOrder = request.sort_order() == null ? existing.getSortOrder() : request.sort_order();
        boolean isDefault = request.is_default() == null ? existing.getIsDefault() : request.is_default();
        if (isDefault) {
            ticketPriorityLevelRepository.clearDefaultsExcept(domainId, levelId);
        }
        ticketPriorityLevelRepository.update(levelId, domainId, label, label, sortOrder, isDefault ? 1 : 0);
        return new TicketConfigDtos.PriorityLevelView(
                String.valueOf(existing.getId()),
                String.valueOf(domainId),
                label,
                label,
                request.color(),
                sortOrder,
                isDefault);
    }

    @Transactional
    public void deletePriorityLevel(long domainId, long levelId) {
        int updated = ticketPriorityLevelRepository.deleteByIdAndDomainId(levelId, domainId);
        if (updated == 0) {
            throw new IllegalArgumentException("priority level not found");
        }
    }

    private TicketConfigDtos.TicketTypeView toTicketTypeView(TicketTypePo po) {
        return new TicketConfigDtos.TicketTypeView(
                String.valueOf(po.getId()),
                String.valueOf(po.getBusinessDomainId()),
                po.getCode(),
                po.getName(),
                readJsonObject(po.getStatusFlowConfig()),
                "active");
    }

    private TicketConfigDtos.TicketTemplateView toTicketTemplateView(TicketTemplatePo po) {
        TicketTemplateContent content = readTicketTemplateContent(po.getContentJson());
        return new TicketConfigDtos.TicketTemplateView(
                String.valueOf(po.getId()),
                String.valueOf(po.getBusinessDomainId()),
                po.getName(),
                toResponseTemplateType(po.getScope()),
                String.valueOf(po.getTicketTypeId()),
                content.fieldsSnapshot(),
                content.content(),
                po.getSortOrder());
    }

    private TicketConfigDtos.QuickReplyView toQuickReplyView(QuickReplyTemplatePo po) {
        return new TicketConfigDtos.QuickReplyView(
                String.valueOf(po.getId()),
                String.valueOf(po.getBusinessDomainId()),
                po.getTitle(),
                po.getContent(),
                po.getScopeType(),
                po.getSortOrder(),
                toDateTimeString(po.getCreatedAt()));
    }

    private TicketConfigDtos.PriorityLevelView toPriorityLevelView(TicketPriorityLevelPo po) {
        return new TicketConfigDtos.PriorityLevelView(
                String.valueOf(po.getId()),
                String.valueOf(po.getBusinessDomainId()),
                po.getName(),
                po.getName(),
                null,
                po.getSortOrder(),
                po.getIsDefault());
    }

    private long findDefaultTicketTypeId(long domainId) {
        Long id = ticketTypeRepository.findFirstIdByDomainId(domainId);
        if (id == null) {
            throw new IllegalStateException("ticket type required");
        }
        return id;
    }

    private Map<String, Object> buildTicketTemplateContent(Object fieldsSnapshot, String content) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (fieldsSnapshot != null) {
            payload.put("fields_snapshot", fieldsSnapshot);
        }
        if (content != null) {
            payload.put("content", content);
        }
        return payload;
    }

    private TicketTemplateContent readTicketTemplateContent(String contentJson) {
        if (!StringUtils.hasText(contentJson)) {
            return new TicketTemplateContent(null, null);
        }
        try {
            Map<String, Object> content = objectMapper.readValue(contentJson, new TypeReference<Map<String, Object>>() {
            });
            return new TicketTemplateContent(content.get("fields_snapshot"), content.get("content") == null ? null : String.valueOf(content.get("content")));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("invalid ticket template content", ex);
        }
    }

    private Object readJsonObject(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("invalid json payload", ex);
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize json payload", ex);
        }
    }

    private String normalizeTemplateType(String type) {
        if (!StringUtils.hasText(type)) {
            return "internal";
        }
        String normalized = type.trim().toLowerCase();
        if ("customer_content".equals(normalized) || "customer".equals(normalized)) {
            return "customer";
        }
        return "internal";
    }

    private String toResponseTemplateType(String scope) {
        return "customer".equalsIgnoreCase(scope) ? "customer_content" : "internal";
    }

    private String normalizeQuickReplyScope(String scope) {
        if (!StringUtils.hasText(scope)) {
            return "ticket";
        }
        String normalized = scope.trim().toLowerCase();
        return switch (normalized) {
            case "ticket", "consultation", "all" -> normalized;
            default -> "ticket";
        };
    }

    private String resolvePriorityLabel(String name, String displayLabel) {
        if (StringUtils.hasText(displayLabel)) {
            return displayLabel.trim();
        }
        if (StringUtils.hasText(name)) {
            return name.trim();
        }
        throw new IllegalArgumentException("priority label required");
    }

    private String requiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private long parseLong(String value, String fieldName) {
        try {
            return Long.parseLong(requiredText(value, fieldName));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a number", ex);
        }
    }

    private String toDateTimeString(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private record TicketTemplateContent(Object fieldsSnapshot, String content) {
    }
}
