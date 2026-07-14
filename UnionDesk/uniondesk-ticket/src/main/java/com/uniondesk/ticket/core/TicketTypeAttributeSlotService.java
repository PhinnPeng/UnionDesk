package com.uniondesk.ticket.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.ticket.entity.TicketAttributePo;
import com.uniondesk.ticket.entity.TicketTypeAttributeSlotPo;
import com.uniondesk.ticket.entity.TicketTypePo;
import com.uniondesk.ticket.repository.TicketAttributeRepository;
import com.uniondesk.ticket.repository.TicketFormSchemaRepository;
import com.uniondesk.ticket.repository.TicketTypeAttributeSlotRepository;
import com.uniondesk.ticket.repository.TicketTypeRepository;
import com.uniondesk.ticket.web.TicketAttributeDtos;
import com.uniondesk.ticket.web.TicketConfigDtos;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketTypeAttributeSlotService {

    private final TicketTypeRepository ticketTypeRepository;
    private final TicketAttributeRepository ticketAttributeRepository;
    private final TicketTypeAttributeSlotRepository slotRepository;
    private final TicketFormSchemaService ticketFormSchemaService;
    private final TicketTypeFlowService ticketTypeFlowService;
    private final ObjectMapper objectMapper;

    public TicketTypeAttributeSlotService(
            TicketTypeRepository ticketTypeRepository,
            TicketAttributeRepository ticketAttributeRepository,
            TicketTypeAttributeSlotRepository slotRepository,
            TicketFormSchemaService ticketFormSchemaService,
            TicketTypeFlowService ticketTypeFlowService,
            ObjectMapper objectMapper) {
        this.ticketTypeRepository = ticketTypeRepository;
        this.ticketAttributeRepository = ticketAttributeRepository;
        this.slotRepository = slotRepository;
        this.ticketFormSchemaService = ticketFormSchemaService;
        this.ticketTypeFlowService = ticketTypeFlowService;
        this.objectMapper = objectMapper;
    }

    public List<TicketAttributeDtos.AttributeSlotView> listSlots(long domainId, long typeId) {
        TicketTypePo type = ticketTypeRepository.findRequiredByIdAndDomainId(typeId, domainId);
        return mergeSlotViews(type, slotRepository.findByTicketTypeId(typeId));
    }

    @Transactional
    public TicketAttributeDtos.AttributeSlotView insertSlot(
            long domainId,
            long typeId,
            TicketAttributeDtos.InsertAttributeSlotRequest request,
            Long operatorId) {
        ticketTypeRepository.findRequiredByIdAndDomainId(typeId, domainId);
        long attributeId = request.attribute_id();
        TicketAttributePo attribute = ticketAttributeRepository.findRequiredById(attributeId);
        if (!TicketAttributePo.SCOPE_DOMAIN.equals(attribute.getScope())
                || attribute.getBusinessDomainId() == null
                || attribute.getBusinessDomainId() != domainId) {
            throw new IllegalArgumentException("属性不存在");
        }
        if (TicketAttributePo.STATUS_DISABLED.equals(attribute.getStatus())) {
            throw new IllegalArgumentException("属性已停用");
        }
        if (slotRepository.findByTypeAndAttribute(typeId, attributeId) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该属性已插入此事项类型");
        }
        TicketTypeAttributeSlotPo po = new TicketTypeAttributeSlotPo();
        po.setTicketTypeId(typeId);
        po.setAttributeId(attributeId);
        po.setSortOrder(slotRepository.nextSortOrder(typeId));
        po.setSlotConfig(toJson(mergeInitialSlotConfig(request.slot_config())));
        po.setStatus(TicketTypeAttributeSlotPo.STATUS_ENABLED);
        po.setCreatedBy(operatorId);
        po.setUpdatedBy(operatorId);
        try {
            slotRepository.insert(po);
        }
        catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该属性已插入此事项类型", ex);
        }
        return toSlotView(po);
    }

    @Transactional
    public TicketAttributeDtos.AttributeSlotView updateSlot(
            long domainId,
            long typeId,
            long slotId,
            TicketAttributeDtos.UpdateAttributeSlotRequest request,
            Long operatorId) {
        ticketTypeRepository.findRequiredByIdAndDomainId(typeId, domainId);
        TicketTypeAttributeSlotPo slot = slotRepository.findRequiredById(slotId);
        if (slot.getTicketTypeId() != typeId) {
            throw new IllegalArgumentException("属性插槽不存在");
        }
        Map<String, Object> slotConfig = normalizeSlotConfig(request.slot_config());
        slotRepository.updateSlotConfig(slotId, toJson(slotConfig), operatorId);
        slot.setSlotConfig(toJson(slotConfig));
        return toSlotView(slot);
    }

    @Transactional
    public void removeSlot(long domainId, long typeId, long slotId) {
        TicketTypePo type = ticketTypeRepository.findRequiredByIdAndDomainId(typeId, domainId);
        TicketTypeAttributeSlotPo slot = slotRepository.findRequiredById(slotId);
        if (slot.getTicketTypeId() != typeId) {
            throw new IllegalArgumentException("属性插槽不存在");
        }
        TicketAttributePo attribute = ticketAttributeRepository.findById(slot.getAttributeId());
        if (attribute != null && isSystemSlotFixedForType(type, attribute)) {
            throw new IllegalArgumentException("该系统属性不允许移除");
        }
        slotRepository.deleteById(slotId);
    }

    @Transactional
    public void reorderSlots(
            long domainId,
            long typeId,
            TicketAttributeDtos.ReorderAttributeSlotsRequest request,
            Long operatorId) {
        ticketTypeRepository.findRequiredByIdAndDomainId(typeId, domainId);
        for (TicketAttributeDtos.SortOrderItem item : request.orders()) {
            TicketTypeAttributeSlotPo slot = slotRepository.findRequiredById(item.id());
            if (slot.getTicketTypeId() != typeId) {
                throw new IllegalArgumentException("属性插槽不存在");
            }
            slotRepository.updateSortOrder(slot.getId(), item.sort_order(), operatorId);
        }
    }

    public List<TicketAttributeDtos.AttributeSlotView> listPlatformSlots(long typeId) {
        TicketTypePo type = ticketTypeRepository.findRequiredPlatformById(typeId);
        return mergeSlotViews(type, slotRepository.findByTicketTypeId(typeId));
    }

    @Transactional
    public void seedPlatformSystemSlots(long typeId, String templateKey, Long operatorId) {
        ticketTypeRepository.findRequiredPlatformById(typeId);
        String normalizedTemplate = templateKey == null ? "" : templateKey.trim().toLowerCase(Locale.ROOT);
        boolean simpleTicket = "simple_ticket".equals(normalizedTemplate);
        int sortOrder = 0;
        if (!simpleTicket) {
            TicketAttributePo title = requirePlatformSystemAttribute("标题");
            insertSeedSlot(typeId, title, sortOrder++, true, operatorId);
        }
        TicketAttributePo description = requirePlatformSystemAttribute("描述");
        insertSeedSlot(typeId, description, sortOrder, true, operatorId);
    }

    private TicketAttributePo requirePlatformSystemAttribute(String name) {
        TicketAttributePo attribute = ticketAttributeRepository.findPlatformByName(name);
        if (attribute == null || !attribute.isSystem()) {
            throw new IllegalStateException("缺少平台系统属性：" + name);
        }
        return attribute;
    }

    private void insertSeedSlot(
            long typeId,
            TicketAttributePo attribute,
            int sortOrder,
            boolean required,
            Long operatorId) {
        if (slotRepository.findByTypeAndAttribute(typeId, attribute.getId()) != null) {
            return;
        }
        Map<String, Object> config = defaultSlotConfig();
        config.put("required", required);
        TicketTypeAttributeSlotPo po = new TicketTypeAttributeSlotPo();
        po.setTicketTypeId(typeId);
        po.setAttributeId(attribute.getId());
        po.setSortOrder(sortOrder);
        po.setSlotConfig(toJson(config));
        po.setStatus(TicketTypeAttributeSlotPo.STATUS_ENABLED);
        po.setCreatedBy(operatorId);
        po.setUpdatedBy(operatorId);
        slotRepository.insert(po);
    }

    private List<TicketAttributeDtos.AttributeSlotView> mergeSlotViews(
            TicketTypePo type,
            List<TicketTypeAttributeSlotPo> dbSlots) {
        Set<String> coveredSystemKeys = new HashSet<>();
        for (TicketTypeAttributeSlotPo slot : dbSlots) {
            TicketAttributePo attribute = ticketAttributeRepository.findById(slot.getAttributeId());
            String systemFieldKey = resolveSystemFieldKey(attribute);
            if (systemFieldKey != null) {
                coveredSystemKeys.add(systemFieldKey);
            }
        }
        List<TicketAttributeDtos.AttributeSlotView> views = new ArrayList<>();
        buildSystemSlotViews(type).stream()
                .filter(view -> view.system_field_key() == null || !coveredSystemKeys.contains(view.system_field_key()))
                .forEach(views::add);
        dbSlots.stream()
                .map(this::toSlotView)
                .forEach(views::add);
        return views;
    }

    @Transactional
    public TicketAttributeDtos.AttributeSlotView insertPlatformSlot(
            long typeId,
            TicketAttributeDtos.InsertAttributeSlotRequest request,
            Long operatorId) {
        ticketTypeRepository.findRequiredPlatformById(typeId);
        long attributeId = request.attribute_id();
        TicketAttributePo attribute = ticketAttributeRepository.findRequiredById(attributeId);
        if (!TicketAttributePo.SCOPE_PLATFORM.equals(attribute.getScope())) {
            throw new IllegalArgumentException("属性不存在");
        }
        if (TicketAttributePo.STATUS_DISABLED.equals(attribute.getStatus())) {
            throw new IllegalArgumentException("属性已停用");
        }
        if (slotRepository.findByTypeAndAttribute(typeId, attributeId) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该属性已插入此事项类型");
        }
        TicketTypeAttributeSlotPo po = new TicketTypeAttributeSlotPo();
        po.setTicketTypeId(typeId);
        po.setAttributeId(attributeId);
        po.setSortOrder(slotRepository.nextSortOrder(typeId));
        po.setSlotConfig(toJson(mergeInitialSlotConfig(request.slot_config())));
        po.setStatus(TicketTypeAttributeSlotPo.STATUS_ENABLED);
        po.setCreatedBy(operatorId);
        po.setUpdatedBy(operatorId);
        try {
            slotRepository.insert(po);
        }
        catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该属性已插入此事项类型", ex);
        }
        return toSlotView(po);
    }

    @Transactional
    public TicketAttributeDtos.AttributeSlotView updatePlatformSlot(
            long typeId,
            long slotId,
            TicketAttributeDtos.UpdateAttributeSlotRequest request,
            Long operatorId) {
        ticketTypeRepository.findRequiredPlatformById(typeId);
        TicketTypeAttributeSlotPo slot = slotRepository.findRequiredById(slotId);
        if (slot.getTicketTypeId() != typeId) {
            throw new IllegalArgumentException("属性插槽不存在");
        }
        Map<String, Object> slotConfig = normalizeSlotConfig(request.slot_config());
        slotRepository.updateSlotConfig(slotId, toJson(slotConfig), operatorId);
        slot.setSlotConfig(toJson(slotConfig));
        return toSlotView(slot);
    }

    @Transactional
    public void removePlatformSlot(long typeId, long slotId) {
        TicketTypePo type = ticketTypeRepository.findRequiredPlatformById(typeId);
        TicketTypeAttributeSlotPo slot = slotRepository.findRequiredById(slotId);
        if (slot.getTicketTypeId() != typeId) {
            throw new IllegalArgumentException("属性插槽不存在");
        }
        TicketAttributePo attribute = ticketAttributeRepository.findById(slot.getAttributeId());
        if (attribute != null && isSystemSlotFixedForType(type, attribute)) {
            throw new IllegalArgumentException("该系统属性不允许移除");
        }
        slotRepository.deleteById(slotId);
    }

    @Transactional
    public void reorderPlatformSlots(
            long typeId,
            TicketAttributeDtos.ReorderAttributeSlotsRequest request,
            Long operatorId) {
        ticketTypeRepository.findRequiredPlatformById(typeId);
        for (TicketAttributeDtos.SortOrderItem item : request.orders()) {
            TicketTypeAttributeSlotPo slot = slotRepository.findRequiredById(item.id());
            if (slot.getTicketTypeId() != typeId) {
                throw new IllegalArgumentException("属性插槽不存在");
            }
            slotRepository.updateSortOrder(slot.getId(), item.sort_order(), operatorId);
        }
    }

    @Transactional
    public void savePlatformFormReleaseDraft(long typeId, Long operatorId) {
        TicketTypePo type = ticketTypeRepository.findRequiredPlatformById(typeId);
        Map<String, Object> snapshot = buildSnapshot(type);
        String pluginRevision = computePluginRevision(typeId);
        ticketFormSchemaService.saveDraftFromMaterialized(
                TicketFormSchemaRepository.PLATFORM_SCHEMA_DOMAIN_KEY,
                typeId,
                snapshot,
                pluginRevision);
    }

    @Transactional
    public void publishPlatformFormRelease(long typeId, Long operatorId) {
        TicketTypePo type = ticketTypeRepository.findRequiredPlatformById(typeId);
        Map<String, Object> snapshot = buildSnapshot(type);
        String pluginRevision = computePluginRevision(typeId);
        ticketFormSchemaService.publishFromMaterialized(
                TicketFormSchemaRepository.PLATFORM_SCHEMA_DOMAIN_KEY,
                typeId,
                snapshot,
                pluginRevision,
                operatorId);
    }

    @Transactional
    public TicketConfigDtos.TicketTypeView saveFormReleaseDraft(long domainId, long typeId, Long operatorId) {
        TicketTypePo type = ticketTypeRepository.findRequiredByIdAndDomainId(typeId, domainId);
        Map<String, Object> snapshot = buildSnapshot(type);
        String pluginRevision = computePluginRevision(typeId);
        ticketFormSchemaService.saveDraftFromMaterialized(domainId, typeId, snapshot, pluginRevision);
        return loadTicketTypeView(type);
    }

    @Transactional
    public TicketConfigDtos.TicketTypeView publishFormRelease(long domainId, long typeId, Long operatorId) {
        TicketTypePo type = ticketTypeRepository.findRequiredByIdAndDomainId(typeId, domainId);
        Map<String, Object> snapshot = buildSnapshot(type);
        String pluginRevision = computePluginRevision(typeId);
        ticketFormSchemaService.publishFromMaterialized(domainId, typeId, snapshot, pluginRevision, operatorId);
        return loadTicketTypeView(type);
    }

    public String computePluginRevision(long typeId) {
        List<Map<String, Object>> payload = new ArrayList<>();
        for (TicketTypeAttributeSlotPo slot : slotRepository.findByTicketTypeId(typeId)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("attribute_id", slot.getAttributeId());
            item.put("sort_order", slot.getSortOrder());
            item.put("status", slot.getStatus());
            item.put("slot_config", readJsonMap(slot.getSlotConfig()));
            payload.add(item);
        }
        return sha256(toJson(payload));
    }

    public boolean hasUnpublishedSlots(long domainId, long typeId) {
        String currentRevision = computePluginRevision(typeId);
        return ticketFormSchemaService.hasUnpublishedByPluginRevision(domainId, typeId, currentRevision);
    }

    private Map<String, Object> buildSnapshot(TicketTypePo type) {
        List<FormSnapshotBuilder.SlotContext> contexts = new ArrayList<>();
        for (TicketTypeAttributeSlotPo slot : slotRepository.findByTicketTypeId(type.getId())) {
            TicketAttributePo attribute = ticketAttributeRepository.findById(slot.getAttributeId());
            if (attribute == null) {
                continue;
            }
            contexts.add(new FormSnapshotBuilder.SlotContext(slot, attribute));
        }
        String category = type.getCategory() == null ? "transaction" : type.getCategory();
        return FormSnapshotBuilder.build(category, contexts, objectMapper);
    }

    public Map<String, Object> buildPlatformSnapshot(TicketTypePo type) {
        return buildSnapshot(type);
    }

    private List<TicketAttributeDtos.AttributeSlotView> buildSystemSlotViews(TicketTypePo type) {
        String category = type.getCategory() == null ? "transaction" : type.getCategory();
        List<TicketAttributeDtos.AttributeSlotView> views = new ArrayList<>();
        if ("feedback".equalsIgnoreCase(category)) {
            addSystemSlotView(views, type, "description", "描述", 0);
            return views;
        }
        addSystemSlotView(views, type, "title", "标题", 0);
        addSystemSlotView(views, type, "description", "描述", 1);
        return views;
    }

    private void addSystemSlotView(
            List<TicketAttributeDtos.AttributeSlotView> views,
            TicketTypePo type,
            String fieldKey,
            String attributeName,
            int sortOrder) {
        TicketAttributePo attribute = resolveSystemAttribute(type, attributeName);
        if (attribute != null) {
            views.add(systemSlotViewFromAttribute(type.getId(), fieldKey, attribute, sortOrder));
            return;
        }
        views.add(systemSlotView(type.getId(), fieldKey, attributeName, sortOrder));
    }

    private TicketAttributePo resolveSystemAttribute(TicketTypePo type, String attributeName) {
        if (TicketTypePo.SCOPE_PLATFORM.equals(type.getScope())) {
            TicketAttributePo attribute = ticketAttributeRepository.findPlatformByName(attributeName);
            if (attribute != null && attribute.isSystem()) {
                return attribute;
            }
            return null;
        }
        Long domainId = type.getBusinessDomainId();
        if (domainId == null) {
            return null;
        }
        TicketAttributePo attribute = ticketAttributeRepository.findDomainByName(domainId, attributeName);
        if (attribute != null && attribute.isSystem()) {
            return attribute;
        }
        return null;
    }

    private String resolveSystemFieldKey(TicketAttributePo attribute) {
        return FormSnapshotBuilder.systemFieldKeyForAttribute(attribute);
    }

    /**
     * 判断系统属性是否对指定事项类型固定（不可移除）。
     * <p>
     * 固定规则由类型的 category 决定：
     * <ul>
     *   <li>feedback → 仅「描述」固定</li>
     *   <li>transaction 及其他 → 「标题」和「描述」都固定</li>
     * </ul>
     */
    private boolean isSystemSlotFixedForType(TicketTypePo type, TicketAttributePo attribute) {
        String systemFieldKey = resolveSystemFieldKey(attribute);
        if (systemFieldKey == null) {
            return false;
        }
        String category = type.getCategory() == null ? "transaction" : type.getCategory();
        if ("feedback".equalsIgnoreCase(category)) {
            return "description".equals(systemFieldKey);
        }
        return "title".equals(systemFieldKey) || "description".equals(systemFieldKey);
    }

    private TicketAttributeDtos.AttributeSlotView systemSlotViewFromAttribute(
            long typeId,
            String fieldKey,
            TicketAttributePo attribute,
            int sortOrder) {
        TicketAttributeDtos.AttributeSlotConfig slotConfig = new TicketAttributeDtos.AttributeSlotConfig(
                true, null, true, null, null);
        TicketAttributeDtos.TicketAttributeView attributeView = new TicketAttributeDtos.TicketAttributeView(
                String.valueOf(attribute.getId()),
                attribute.getScope(),
                attribute.getBusinessDomainId() == null ? null : String.valueOf(attribute.getBusinessDomainId()),
                attribute.getName(),
                attribute.getDescription(),
                attribute.getFieldType(),
                readJsonMap(attribute.getTypeConfig()),
                attribute.getStatus(),
                attribute.getSortOrder(),
                attribute.isSystem(),
                attribute.getSourceAttributeId() == null ? null : String.valueOf(attribute.getSourceAttributeId()),
                attribute.getCreatedAt() == null ? null : attribute.getCreatedAt().toString(),
                attribute.getUpdatedAt() == null ? null : attribute.getUpdatedAt().toString());
        return new TicketAttributeDtos.AttributeSlotView(
                "system_" + fieldKey + "_" + typeId,
                String.valueOf(typeId),
                String.valueOf(attribute.getId()),
                attributeView,
                sortOrder,
                slotConfig,
                TicketTypeAttributeSlotPo.STATUS_ENABLED,
                true,
                fieldKey);
    }

    private TicketAttributeDtos.AttributeSlotView systemSlotView(
            long typeId,
            String fieldKey,
            String label,
            int sortOrder) {
        TicketAttributeDtos.AttributeSlotConfig slotConfig = new TicketAttributeDtos.AttributeSlotConfig(
                true, null, true, null, null);
        TicketAttributeDtos.TicketAttributeView attribute = new TicketAttributeDtos.TicketAttributeView(
                "system_" + fieldKey,
                "system",
                null,
                label,
                "系统字段",
                fieldKey.equals("title") ? "input" : "input",
                Map.of(),
                TicketAttributePo.STATUS_ACTIVE,
                sortOrder,
                true,
                null,
                null,
                null);
        return new TicketAttributeDtos.AttributeSlotView(
                "system_" + fieldKey + "_" + typeId,
                String.valueOf(typeId),
                attribute.id(),
                attribute,
                sortOrder,
                slotConfig,
                TicketTypeAttributeSlotPo.STATUS_ENABLED,
                true,
                fieldKey);
    }

    private TicketAttributeDtos.AttributeSlotView toSlotView(TicketTypeAttributeSlotPo slot) {
        TicketAttributePo attribute = ticketAttributeRepository.findRequiredById(slot.getAttributeId());
        String systemFieldKey = resolveSystemFieldKey(attribute);
        boolean isSystemSlot = systemFieldKey != null;
        TicketAttributeDtos.TicketAttributeView attributeView = new TicketAttributeDtos.TicketAttributeView(
                String.valueOf(attribute.getId()),
                attribute.getScope(),
                attribute.getBusinessDomainId() == null ? null : String.valueOf(attribute.getBusinessDomainId()),
                attribute.getName(),
                attribute.getDescription(),
                attribute.getFieldType(),
                readJsonMap(attribute.getTypeConfig()),
                attribute.getStatus(),
                attribute.getSortOrder(),
                attribute.isSystem(),
                attribute.getSourceAttributeId() == null ? null : String.valueOf(attribute.getSourceAttributeId()),
                attribute.getCreatedAt() == null ? null : attribute.getCreatedAt().toString(),
                attribute.getUpdatedAt() == null ? null : attribute.getUpdatedAt().toString());
        return new TicketAttributeDtos.AttributeSlotView(
                String.valueOf(slot.getId()),
                String.valueOf(slot.getTicketTypeId()),
                String.valueOf(slot.getAttributeId()),
                attributeView,
                slot.getSortOrder(),
                toSlotConfigView(readJsonMap(slot.getSlotConfig())),
                slot.getStatus(),
                isSystemSlot,
                systemFieldKey);
    }

    private TicketAttributeDtos.AttributeSlotConfig toSlotConfigView(Map<String, Object> config) {
        return new TicketAttributeDtos.AttributeSlotConfig(
                Boolean.TRUE.equals(config.get("required")),
                config.get("placeholder") == null ? null : String.valueOf(config.get("placeholder")),
                config.get("visible_to_customer") == null || Boolean.TRUE.equals(config.get("visible_to_customer")),
                config.get("default_value") == null ? null : String.valueOf(config.get("default_value")),
                config.get("display_name") == null ? null : String.valueOf(config.get("display_name")));
    }

    private Map<String, Object> defaultSlotConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("required", false);
        config.put("placeholder", "");
        config.put("visible_to_customer", true);
        return config;
    }

    private Map<String, Object> mergeInitialSlotConfig(TicketAttributeDtos.AttributeSlotConfig slotConfig) {
        Map<String, Object> config = defaultSlotConfig();
        if (slotConfig == null) {
            return config;
        }
        if (slotConfig.required() != null) {
            config.put("required", slotConfig.required());
        }
        if (slotConfig.visible_to_customer() != null) {
            config.put("visible_to_customer", slotConfig.visible_to_customer());
        }
        if (StringUtils.hasText(slotConfig.placeholder())) {
            config.put("placeholder", slotConfig.placeholder().trim());
        }
        if (StringUtils.hasText(slotConfig.default_value())) {
            config.put("default_value", slotConfig.default_value().trim());
        }
        if (StringUtils.hasText(slotConfig.display_name())) {
            config.put("display_name", slotConfig.display_name().trim());
        }
        return config;
    }

    private Map<String, Object> normalizeSlotConfig(TicketAttributeDtos.AttributeSlotConfig slotConfig) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("required", Boolean.TRUE.equals(slotConfig.required()));
        config.put("visible_to_customer", slotConfig.visible_to_customer() == null || slotConfig.visible_to_customer());
        if (StringUtils.hasText(slotConfig.placeholder())) {
            config.put("placeholder", slotConfig.placeholder().trim());
        }
        else {
            config.put("placeholder", "");
        }
        if (StringUtils.hasText(slotConfig.default_value())) {
            config.put("default_value", slotConfig.default_value().trim());
        }
        if (StringUtils.hasText(slotConfig.display_name())) {
            config.put("display_name", slotConfig.display_name().trim());
        }
        return config;
    }

    private TicketConfigDtos.TicketTypeView loadTicketTypeView(TicketTypePo type) {
        TicketFormSchemaService.FormSchemaAggregate aggregate = ticketFormSchemaService.loadAggregate(
                type.getBusinessDomainId(),
                type.getId(),
                computePluginRevision(type.getId()));
        Integer currentVersionNo = aggregate.currentVersionNo() > 0 ? aggregate.currentVersionNo() : null;
        long domainId = TicketTypeFlowService.resolveDomainId(type);
        TicketConfigDtos.WorkflowConfigView workflow = ticketTypeFlowService.loadAssembled(domainId, type.getId());
        return new TicketConfigDtos.TicketTypeView(
                String.valueOf(type.getId()),
                String.valueOf(type.getBusinessDomainId()),
                type.getCode(),
                type.getName(),
                type.getDescription(),
                type.getIcon(),
                workflow.status_flow(),
                aggregate.publishedSchema(),
                aggregate.draftSchema(),
                currentVersionNo,
                aggregate.hasUnpublished(),
                StringUtils.hasText(type.getStatus()) ? type.getStatus() : "active",
                workflow.transition_rules());
    }

    private Map<String, Object> readJsonMap(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        }
        catch (JsonProcessingException ex) {
            throw new IllegalStateException("invalid json payload", ex);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize json payload", ex);
        }
    }

    private String sha256(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        }
        catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
