package com.uniondesk.ticket.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.ticket.entity.QuickReplyTemplatePo;
import com.uniondesk.ticket.entity.TicketPriorityLevelPo;
import com.uniondesk.ticket.entity.TicketTemplatePo;
import com.uniondesk.ticket.entity.TicketTypePo;
import com.uniondesk.ticket.repository.QuickReplyTemplateRepository;
import com.uniondesk.ticket.repository.TicketFormSchemaRepository;
import com.uniondesk.ticket.repository.TicketPriorityLevelRepository;
import com.uniondesk.ticket.repository.TicketTemplateRepository;
import com.uniondesk.ticket.repository.TicketTypeAttributeSlotRepository;
import com.uniondesk.ticket.repository.TicketTypeRepository;
import com.uniondesk.ticket.web.TicketConfigDtos;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TicketConfigService {

    private final TicketTypeRepository ticketTypeRepository;
    private final TicketTemplateRepository ticketTemplateRepository;
    private final QuickReplyTemplateRepository quickReplyTemplateRepository;
    private final TicketPriorityLevelRepository ticketPriorityLevelRepository;
    private final TicketFormSchemaService ticketFormSchemaService;
    private final TicketTypeAttributeSlotService ticketTypeAttributeSlotService;
    private final TicketTypeAttributeSlotRepository slotRepository;
    private final TicketTransitionRuleService transitionRuleService;
    private final TicketTypeFlowService ticketTypeFlowService;
    private final PlatformTicketTypeCopyService platformTicketTypeCopyService;
    private final ObjectMapper objectMapper;

    public TicketConfigService(
            TicketTypeRepository ticketTypeRepository,
            TicketTemplateRepository ticketTemplateRepository,
            QuickReplyTemplateRepository quickReplyTemplateRepository,
            TicketPriorityLevelRepository ticketPriorityLevelRepository,
            TicketFormSchemaService ticketFormSchemaService,
            TicketTypeAttributeSlotService ticketTypeAttributeSlotService,
            TicketTypeAttributeSlotRepository slotRepository,
            TicketTransitionRuleService transitionRuleService,
            TicketTypeFlowService ticketTypeFlowService,
            PlatformTicketTypeCopyService platformTicketTypeCopyService,
            ObjectMapper objectMapper) {
        this.ticketTypeRepository = ticketTypeRepository;
        this.ticketTemplateRepository = ticketTemplateRepository;
        this.quickReplyTemplateRepository = quickReplyTemplateRepository;
        this.ticketPriorityLevelRepository = ticketPriorityLevelRepository;
        this.ticketFormSchemaService = ticketFormSchemaService;
        this.ticketTypeAttributeSlotService = ticketTypeAttributeSlotService;
        this.slotRepository = slotRepository;
        this.transitionRuleService = transitionRuleService;
        this.ticketTypeFlowService = ticketTypeFlowService;
        this.platformTicketTypeCopyService = platformTicketTypeCopyService;
        this.objectMapper = objectMapper;
    }

    public List<TicketConfigDtos.TicketTypeView> listTicketTypes(long domainId) {
        return ticketTypeRepository.findByDomainId(domainId).stream()
                .map(this::toTicketTypeView)
                .toList();
    }

    /** Customer portal: active domain ticket types only (brief). */
    public List<TicketConfigDtos.CustomerTicketTypeView> listCustomerTicketTypes(long domainId) {
        return ticketTypeRepository.findByDomainId(domainId).stream()
                .filter(po -> TicketTypePo.STATUS_ACTIVE.equalsIgnoreCase(po.getStatus()))
                .map(po -> new TicketConfigDtos.CustomerTicketTypeView(
                        po.getId(),
                        po.getName(),
                        po.getDescription()))
                .toList();
    }

    @Transactional
    public TicketConfigDtos.TicketTypeView createTicketType(long domainId, TicketConfigDtos.CreateTicketTypeRequest request) {
        String name = requiredText(request.name(), "name");
        String icon = StringUtils.hasText(request.icon()) ? request.icon().trim() : "";
        if (!StringUtils.hasText(icon) && StringUtils.hasText(request.template_key())) {
            icon = defaultIcon(request.template_key());
        }
        if (!StringUtils.hasText(icon)) {
            throw new IllegalArgumentException("icon is required");
        }
        String description = trimToNull(request.description());
        if (description == null && StringUtils.hasText(request.template_key())) {
            description = defaultDescription(request.template_key());
        }
        String code = resolveDomainCode(domainId, request.code(), request.template_key(), name);
        assertCodeUnique(domainId, code, null);
        assertNameUnique(domainId, name, null);
        String shortCode = resolveShortCode(request.short_code(), code);
        assertShortCodeUnique(domainId, shortCode, null);
        Object statusFlow = request.status_flow() == null
                ? DefaultStatusFlowProvider.emptyFlow()
                : request.status_flow();
        Map<String, Object> formSchema = FormSchemaValidator.mergeAndValidate(request.form_schema(), objectMapper);
        String formSchemaJson = toJson(formSchema);
        StatusFlowValidator.validate(statusFlow);
        TicketTypePo po = new TicketTypePo();
        po.setScope(TicketTypePo.SCOPE_DOMAIN);
        po.setBusinessDomainId(domainId);
        po.setCode(code);
        po.setShortCode(shortCode);
        po.setName(name);
        po.setDescription(description);
        po.setDescriptionTemplateMd(trimToNull(request.description_template_md()));
        po.setIcon(icon);
        po.setCategory(resolveCategory(request.template_key()));
        po.setStatus(TicketTypePo.STATUS_ACTIVE);
        po.setSortOrder(ticketTypeRepository.nextSortOrderDomain(domainId));
        po.setSystem(false);
        try {
            ticketTypeRepository.save(po);
        } catch (DuplicateKeyException ex) {
            throw translateTicketTypeDuplicate(ex);
        }
        if (StringUtils.hasText(request.template_key())) {
            ticketTypeAttributeSlotService.seedDomainSystemSlots(domainId, po.getId(), request.template_key(), null);
        }
        ticketFormSchemaService.initializeForNewType(domainId, po.getId(), formSchemaJson);
        if (request.status_flow() != null) {
            ticketTypeFlowService.replaceAll(domainId, po.getId(), statusFlow, null);
        }
        return toTicketTypeView(po);
    }

    @Transactional
    public List<TicketConfigDtos.TicketTypeView> importPlatformTicketTypes(
            long domainId,
            TicketConfigDtos.ImportPlatformTicketTypesRequest request) {
        if (request == null || request.platform_type_ids() == null || request.platform_type_ids().isEmpty()) {
            throw new IllegalArgumentException("platform_type_ids is required");
        }
        List<TicketConfigDtos.TicketTypeView> created = new java.util.ArrayList<>();
        for (String rawId : request.platform_type_ids()) {
            long platformTypeId = parseLong(rawId, "platform_type_id");
            TicketTypePo po = platformTicketTypeCopyService.copyToDomain(
                    domainId,
                    platformTypeId,
                    PlatformTicketTypeCopyService.CopyOptions.allIncluded(),
                    null);
            created.add(toTicketTypeView(po));
        }
        return created;
    }

    @Transactional
    public TicketConfigDtos.TicketTypeView updateTicketType(long domainId, long typeId, TicketConfigDtos.UpdateTicketTypeRequest request) {
        TicketTypePo existing = ticketTypeRepository.findRequiredByIdAndDomainId(typeId, domainId);
        String name = StringUtils.hasText(request.name()) ? request.name().trim() : existing.getName();
        assertNameUnique(domainId, name, typeId);
        String description = request.description() == null ? existing.getDescription() : trimToNull(request.description());
        String descriptionTemplateMd = request.description_template_md() == null
                ? existing.getDescriptionTemplateMd()
                : trimToNull(request.description_template_md());
        String icon = request.icon() == null ? existing.getIcon() : trimToNull(request.icon());
        String status = StringUtils.hasText(request.status()) ? request.status().trim() : existing.getStatus();
        String shortCode = request.short_code() == null
                ? existing.getShortCode()
                : resolveShortCode(request.short_code(), existing.getCode());
        assertShortCodeUnique(domainId, shortCode, typeId);
        try {
            ticketTypeRepository.updateMetadata(typeId, domainId, name, description, descriptionTemplateMd, icon, status, shortCode);
        } catch (DuplicateKeyException ex) {
            throw translateTicketTypeDuplicate(ex);
        }
        if (request.status_flow() != null || request.transition_rules() != null) {
            Object statusFlow = request.status_flow() != null
                    ? request.status_flow()
                    : ticketTypeFlowService.loadStatusFlow(domainId, typeId);
            ticketTypeFlowService.replaceAll(domainId, typeId, statusFlow, request.transition_rules());
        }
        existing.setName(name);
        existing.setDescription(description);
        existing.setDescriptionTemplateMd(descriptionTemplateMd);
        existing.setIcon(icon);
        existing.setStatus(status);
        existing.setShortCode(shortCode);
        return toTicketTypeView(existing);
    }

    @Transactional
    public TicketConfigDtos.TicketTypeView saveFormSchemaDraft(long domainId, long typeId, Object schema) {
        ticketTypeRepository.findRequiredByIdAndDomainId(typeId, domainId);
        TicketFormSchemaService.FormSchemaAggregate aggregate = ticketFormSchemaService.saveDraft(domainId, typeId, schema);
        return toTicketTypeView(ticketTypeRepository.findRequiredByIdAndDomainId(typeId, domainId), aggregate);
    }

    @Transactional
    public TicketConfigDtos.TicketTypeView publishFormSchema(long domainId, long typeId, Object schema) {
        ticketTypeRepository.findRequiredByIdAndDomainId(typeId, domainId);
        TicketFormSchemaService.FormSchemaAggregate aggregate = ticketFormSchemaService.publish(domainId, typeId, schema, null);
        return toTicketTypeView(ticketTypeRepository.findRequiredByIdAndDomainId(typeId, domainId), aggregate);
    }

    public TicketConfigDtos.FormSchemaVersionsView listFormSchemaVersions(long domainId, long typeId) {
        ticketTypeRepository.findRequiredByIdAndDomainId(typeId, domainId);
        TicketFormSchemaService.FormSchemaAggregate aggregate = ticketFormSchemaService.loadAggregate(domainId, typeId);
        List<TicketConfigDtos.FormSchemaVersionSummaryView> items = ticketFormSchemaService.listPublishedVersions(domainId, typeId);
        Integer currentVersionNo = aggregate.currentVersionNo() > 0 ? aggregate.currentVersionNo() : null;
        return new TicketConfigDtos.FormSchemaVersionsView(currentVersionNo, items);
    }

    public TicketConfigDtos.FormSchemaVersionDetailView getFormSchemaVersion(long domainId, long typeId, int versionNo) {
        ticketTypeRepository.findRequiredByIdAndDomainId(typeId, domainId);
        return ticketFormSchemaService.getPublishedVersion(domainId, typeId, versionNo);
    }

    @Transactional
    public TicketConfigDtos.TicketTypeView rollbackFormSchemaVersion(long domainId, long typeId, int versionNo) {
        ticketTypeRepository.findRequiredByIdAndDomainId(typeId, domainId);
        TicketFormSchemaService.FormSchemaAggregate aggregate = ticketFormSchemaService.rollback(domainId, typeId, versionNo, null);
        return toTicketTypeView(ticketTypeRepository.findRequiredByIdAndDomainId(typeId, domainId), aggregate);
    }

    @Transactional
    public void deleteTicketType(long domainId, long typeId) {
        ticketTypeRepository.findRequiredByIdAndDomainId(typeId, domainId);
        if (ticketTypeRepository.countTicketsByTypeId(domainId, typeId) > 0) {
            throw new IllegalArgumentException("该工单类型已被使用，无法删除");
        }
        ticketFormSchemaService.deleteByTicketType(domainId, typeId);
        slotRepository.deleteByTicketTypeId(typeId);
        transitionRuleService.deleteByDomainIdAndTypeId(domainId, typeId);
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
        String code = StringUtils.hasText(request.code())
                ? request.code().trim().toLowerCase(Locale.ROOT)
                : label.toLowerCase(Locale.ROOT);
        int sortOrder = request.sort_order() == null ? 0 : request.sort_order();
        boolean isDefault = Boolean.TRUE.equals(request.is_default());
        String color = resolvePriorityColor(code, request.color());
        String icon = resolvePriorityIcon(code, request.icon());
        if (isDefault) {
            ticketPriorityLevelRepository.clearDefaults(domainId);
        }
        TicketPriorityLevelPo po = new TicketPriorityLevelPo();
        po.setBusinessDomainId(domainId);
        po.setCode(code);
        po.setName(label);
        po.setColor(color);
        po.setIcon(icon);
        po.setSortOrder(sortOrder);
        po.setIsDefault(isDefault);
        po.setStatus("active");
        ticketPriorityLevelRepository.save(po);
        return toPriorityLevelView(po);
    }

    @Transactional
    public TicketConfigDtos.PriorityLevelView updatePriorityLevel(long domainId, long levelId, TicketConfigDtos.UpdatePriorityLevelRequest request) {
        TicketPriorityLevelPo existing = ticketPriorityLevelRepository.findRequiredByIdAndDomainId(levelId, domainId);
        String label = StringUtils.hasText(request.display_label())
                ? request.display_label().trim()
                : (StringUtils.hasText(request.name()) ? request.name().trim() : existing.getName());
        String code = StringUtils.hasText(request.code())
                ? request.code().trim().toLowerCase(Locale.ROOT)
                : existing.getCode();
        int sortOrder = request.sort_order() == null ? existing.getSortOrder() : request.sort_order();
        boolean isDefault = request.is_default() == null ? existing.getIsDefault() : request.is_default();
        String color = resolvePriorityColor(code, request.color() != null ? request.color() : existing.getColor());
        String icon = resolvePriorityIcon(code, request.icon() != null ? request.icon() : existing.getIcon());
        if (isDefault) {
            ticketPriorityLevelRepository.clearDefaultsExcept(domainId, levelId);
        }
        ticketPriorityLevelRepository.update(levelId, domainId, code, label, color, icon, sortOrder, isDefault ? 1 : 0);
        existing.setCode(code);
        existing.setName(label);
        existing.setColor(color);
        existing.setIcon(icon);
        existing.setSortOrder(sortOrder);
        existing.setIsDefault(isDefault);
        return toPriorityLevelView(existing);
    }

    @Transactional
    public void deletePriorityLevel(long domainId, long levelId) {
        int updated = ticketPriorityLevelRepository.deleteByIdAndDomainId(levelId, domainId);
        if (updated == 0) {
            throw new IllegalArgumentException("priority level not found");
        }
    }

    private TicketConfigDtos.TicketTypeView toTicketTypeView(TicketTypePo po) {
        String pluginRevision = ticketTypeAttributeSlotService.computePluginRevision(po.getId());
        TicketFormSchemaService.FormSchemaAggregate aggregate = ticketFormSchemaService.loadAggregate(
                po.getBusinessDomainId() == null ? TicketFormSchemaRepository.PLATFORM_SCHEMA_DOMAIN_KEY : po.getBusinessDomainId(),
                po.getId(),
                pluginRevision);
        return toTicketTypeView(po, aggregate);
    }

    private TicketConfigDtos.TicketTypeView toTicketTypeView(
            TicketTypePo po,
            TicketFormSchemaService.FormSchemaAggregate aggregate) {
        Integer currentVersionNo = aggregate.currentVersionNo() > 0 ? aggregate.currentVersionNo() : null;
        long domainId = TicketTypeFlowService.resolveDomainId(po);
        TicketConfigDtos.WorkflowConfigView workflow = ticketTypeFlowService.loadAssembled(domainId, po.getId());
        return new TicketConfigDtos.TicketTypeView(
                String.valueOf(po.getId()),
                po.getBusinessDomainId() == null ? null : String.valueOf(po.getBusinessDomainId()),
                po.getCode(),
                po.getShortCode(),
                po.getName(),
                po.getDescription(),
                po.getDescriptionTemplateMd(),
                po.getIcon(),
                workflow.status_flow(),
                aggregate.publishedSchema(),
                aggregate.draftSchema(),
                currentVersionNo,
                aggregate.hasUnpublished(),
                StringUtils.hasText(po.getStatus()) ? po.getStatus() : "active",
                po.getSourceGlobalTypeId() == null ? null : String.valueOf(po.getSourceGlobalTypeId()),
                workflow.transition_rules());
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
                po.getCode(),
                po.getName(),
                po.getName(),
                po.getColor(),
                po.getIcon(),
                po.getSortOrder(),
                po.getIsDefault());
    }

    private static String resolvePriorityColor(String code, String requested) {
        if (StringUtils.hasText(requested) && requested.trim().matches("^#[0-9A-Fa-f]{6}$")) {
            return requested.trim().toLowerCase(Locale.ROOT);
        }
        return switch (code == null ? "" : code.toLowerCase(Locale.ROOT)) {
            case "urgent" -> "#f5222d";
            case "high" -> "#fa8c16";
            case "low" -> "#8c8c8c";
            default -> "#1677ff";
        };
    }

    private static String resolvePriorityIcon(String code, String requested) {
        if (StringUtils.hasText(requested)) {
            return requested.trim().toLowerCase(Locale.ROOT);
        }
        return switch (code == null ? "" : code.toLowerCase(Locale.ROOT)) {
            case "urgent", "high", "low", "normal" -> code.toLowerCase(Locale.ROOT);
            default -> "normal";
        };
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String resolveDomainCode(long domainId, String code, String templateKey, String name) {
        if (StringUtils.hasText(code)) {
            return code.trim().toLowerCase(Locale.ROOT);
        }
        if (StringUtils.hasText(templateKey)) {
            String base = templateKey.trim().toLowerCase(Locale.ROOT);
            if (ticketTypeRepository.findByDomainIdAndCode(domainId, base) == null) {
                return base;
            }
            return base + "_" + Long.toHexString(System.nanoTime()).substring(0, 6);
        }
        String base = name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        if (!StringUtils.hasText(base) || "_".equals(base)) {
            base = "type";
        }
        return base + "_" + Long.toHexString(System.nanoTime()).substring(0, 6);
    }

    private String resolveCategory(String templateKey) {
        if (!StringUtils.hasText(templateKey)) {
            return "transaction";
        }
        return switch (templateKey.trim().toLowerCase(Locale.ROOT)) {
            case "simple_ticket" -> "feedback";
            default -> "transaction";
        };
    }

    private String defaultDescription(String templateKey) {
        if (!StringUtils.hasText(templateKey)) {
            return null;
        }
        return switch (templateKey.trim().toLowerCase(Locale.ROOT)) {
            case "simple_ticket" -> "适用于快速记录的轻量事项，默认仅描述必填";
            case "standard_ticket" -> "适用于标准工单流程的事项，默认标题与描述均必填";
            default -> null;
        };
    }

    private String defaultIcon(String templateKey) {
        if (!StringUtils.hasText(templateKey)) {
            return "";
        }
        return switch (templateKey.trim().toLowerCase(Locale.ROOT)) {
            case "simple_ticket" -> "mdi:file-document-outline";
            case "standard_ticket" -> "mdi:ticket-outline";
            default -> "";
        };
    }

    private void assertCodeUnique(long domainId, String code, Long excludeTypeId) {
        TicketTypePo existing = ticketTypeRepository.findByDomainIdAndCode(domainId, code);
        if (existing != null && (excludeTypeId == null || existing.getId() != excludeTypeId)) {
            throw new IllegalArgumentException("该域下编码已存在");
        }
    }

    private void assertNameUnique(long domainId, String name, Long excludeTypeId) {
        TicketTypePo existing = ticketTypeRepository.findByDomainIdAndName(domainId, name);
        if (existing != null && (excludeTypeId == null || existing.getId() != excludeTypeId)) {
            throw new IllegalArgumentException("该域下名称已存在");
        }
    }

    /**
     * 解析事项类型短码：显式传入时规范化为大写；为空时默认取 code 前 2 位大写，兜底 TK。
     */
    private String resolveShortCode(String requested, String code) {
        String shortCode;
        if (StringUtils.hasText(requested)) {
            shortCode = requested.trim().toUpperCase(Locale.ROOT);
            if (shortCode.length() > 16) {
                throw new IllegalArgumentException("短码最长 16 个字符");
            }
        }
        else if (StringUtils.hasText(code)) {
            shortCode = code.trim().toUpperCase(Locale.ROOT);
            shortCode = shortCode.length() >= 2 ? shortCode.substring(0, 2) : shortCode;
        }
        else {
            shortCode = "TK";
        }
        return shortCode;
    }

    private void assertShortCodeUnique(long domainId, String shortCode, Long excludeTypeId) {
        TicketTypePo existing = ticketTypeRepository.findByDomainIdAndShortCode(domainId, shortCode);
        if (existing != null && (excludeTypeId == null || existing.getId() != excludeTypeId)) {
            throw new IllegalArgumentException("该域下短码已存在");
        }
    }

    private IllegalArgumentException translateTicketTypeDuplicate(DuplicateKeyException ex) {
        String message = ex.getMostSpecificCause() == null ? null : ex.getMostSpecificCause().getMessage();
        if (message != null && (message.contains("uk_ticket_type_scope_domain_name") || message.contains("uk_ticket_type_domain_name"))) {
            return new IllegalArgumentException("该域下名称已存在", ex);
        }
        return new IllegalArgumentException("该域下编码已存在", ex);
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
