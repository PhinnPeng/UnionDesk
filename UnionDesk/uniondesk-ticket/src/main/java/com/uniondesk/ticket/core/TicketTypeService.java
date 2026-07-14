package com.uniondesk.ticket.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.ticket.entity.TicketTypePo;
import com.uniondesk.ticket.repository.TicketFormSchemaRepository;
import com.uniondesk.ticket.repository.TicketTypeAttributeSlotRepository;
import com.uniondesk.ticket.repository.TicketTypeRepository;
import com.uniondesk.ticket.web.TicketConfigDtos;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketTypeService {

    private final TicketTypeRepository ticketTypeRepository;
    private final TicketFormSchemaService ticketFormSchemaService;
    private final TicketTypeAttributeSlotRepository slotRepository;
    private final ObjectMapper objectMapper;

    private final TicketTypeAttributeSlotService ticketTypeAttributeSlotService;
    private final TicketTypeFlowService ticketTypeFlowService;

    public TicketTypeService(
            TicketTypeRepository ticketTypeRepository,
            TicketFormSchemaService ticketFormSchemaService,
            TicketTypeAttributeSlotRepository slotRepository,
            TicketTypeAttributeSlotService ticketTypeAttributeSlotService,
            TicketTypeFlowService ticketTypeFlowService,
            ObjectMapper objectMapper) {
        this.ticketTypeRepository = ticketTypeRepository;
        this.ticketFormSchemaService = ticketFormSchemaService;
        this.slotRepository = slotRepository;
        this.ticketTypeAttributeSlotService = ticketTypeAttributeSlotService;
        this.ticketTypeFlowService = ticketTypeFlowService;
        this.objectMapper = objectMapper;
    }

    public TicketConfigDtos.PlatformTicketTypeDetailView getPlatformDetail(long typeId) {
        TicketTypePo po = ticketTypeRepository.findRequiredPlatformById(typeId);
        return toDetailView(po, ticketTypeAttributeSlotService.computePluginRevision(typeId));
    }

    public TicketConfigDtos.PlatformTicketTypeListView listPlatform(String keyword, Integer page, Integer pageSize) {
        String keywordLike = toKeywordLike(keyword);
        long total = ticketTypeRepository.countPlatform(keywordLike);
        List<TicketTypePo> rows;
        if (total <= TicketTypeRepository.NO_PAGINATION_THRESHOLD || page == null || pageSize == null) {
            rows = ticketTypeRepository.findAllPlatform(keywordLike);
        }
        else {
            int normalizedPage = Math.max(page, 1);
            int normalizedPageSize = Math.max(pageSize, 1);
            long offset = (long) (normalizedPage - 1) * normalizedPageSize;
            rows = ticketTypeRepository.findPagePlatform(keywordLike, normalizedPageSize, offset);
        }
        return new TicketConfigDtos.PlatformTicketTypeListView(
                total,
                rows.stream().map(this::toView).toList());
    }

    @Transactional
    public TicketConfigDtos.PlatformTicketTypeView createPlatform(TicketConfigDtos.CreatePlatformTicketTypeRequest request) {
        String name = requiredText(request.name(), "name");
        String icon = requiredText(request.icon(), "icon");
        String description = trimToEmpty(request.description());
        if (!StringUtils.hasText(description)) {
            description = defaultDescription(request.template_key());
        }
        String category = resolveCategory(request.category(), request.template_key());
        String code = resolveCode(request.code(), request.template_key(), name);
        assertPlatformCodeUnique(code, null);
        assertPlatformNameUnique(name, null);
        String formSchemaJson;
        TicketTypePo po = new TicketTypePo();
        po.setScope(TicketTypePo.SCOPE_PLATFORM);
        po.setBusinessDomainId(null);
        po.setCode(code);
        po.setName(name);
        po.setDescription(description);
        po.setIcon(icon.trim());
        po.setCategory(category);
        po.setStatus(TicketTypePo.STATUS_ACTIVE);
        po.setSortOrder(ticketTypeRepository.nextSortOrderPlatform());
        po.setSystem(false);
        try {
            ticketTypeRepository.save(po);
        }
        catch (DuplicateKeyException ex) {
            throw translatePlatformDuplicate(ex);
        }
        if (StringUtils.hasText(request.template_key())) {
            ticketTypeAttributeSlotService.seedPlatformSystemSlots(po.getId(), request.template_key(), null);
            formSchemaJson = toJson(ticketTypeAttributeSlotService.buildPlatformSnapshot(po));
        }
        else {
            Map<String, Object> formSchema = FormSnapshotBuilder.build(
                    category,
                    List.of(),
                    objectMapper);
            formSchemaJson = toJson(formSchema);
        }
        ticketFormSchemaService.initializeForNewPlatformType(po.getId(), formSchemaJson);
        return toView(po);
    }

    @Transactional
    public TicketConfigDtos.PlatformTicketTypeView updatePlatform(
            long typeId,
            TicketConfigDtos.UpdatePlatformTicketTypeRequest request) {
        TicketTypePo existing = ticketTypeRepository.findRequiredPlatformById(typeId);
        String name = StringUtils.hasText(request.name()) ? request.name().trim() : existing.getName();
        assertPlatformNameUnique(name, typeId);
        String description = request.description() == null
                ? existing.getDescription()
                : trimToEmpty(request.description());
        String icon = request.icon() == null ? existing.getIcon() : requiredText(request.icon(), "icon");
        String status = StringUtils.hasText(request.status()) ? request.status().trim() : existing.getStatus();
        try {
            ticketTypeRepository.updatePlatformMetadata(typeId, name, description, icon, status);
        }
        catch (DuplicateKeyException ex) {
            throw translatePlatformDuplicate(ex);
        }
        if (request.status_flow() != null) {
            ticketTypeFlowService.replaceAll(0L, typeId, request.status_flow(), null);
        }
        existing.setName(name);
        existing.setDescription(description);
        existing.setIcon(icon);
        existing.setStatus(status);
        return toView(existing);
    }

    @Transactional
    public void deletePlatform(long typeId) {
        TicketTypePo existing = ticketTypeRepository.findRequiredPlatformById(typeId);
        if (existing.isSystem()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "系统内置类型不可删除");
        }
        long linkedCount = ticketTypeRepository.countLinkedDomainsByGlobalTypeId(typeId);
        if (linkedCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该类型已被复制到业务域，无法删除");
        }
        ticketFormSchemaService.deleteByTicketType(TicketFormSchemaRepository.PLATFORM_SCHEMA_DOMAIN_KEY, typeId);
        slotRepository.deleteByTicketTypeId(typeId);
        ticketTypeFlowService.deleteByDomainIdAndTypeId(0L, typeId);
        int deleted = ticketTypeRepository.deletePlatformById(typeId);
        if (deleted == 0) {
            throw new IllegalArgumentException("事项类型不存在");
        }
    }

    @Transactional
    public void reorderPlatform(TicketConfigDtos.ReorderPlatformTicketTypesRequest request) {
        for (TicketConfigDtos.PlatformTicketTypeSortOrderItem item : request.orders()) {
            ticketTypeRepository.findRequiredPlatformById(item.id());
            ticketTypeRepository.updateSortOrder(item.id(), item.sort_order());
        }
    }

    private TicketConfigDtos.PlatformTicketTypeView toView(TicketTypePo po) {
        return new TicketConfigDtos.PlatformTicketTypeView(
                String.valueOf(po.getId()),
                po.getScope(),
                po.getCode(),
                po.getName(),
                po.getDescription(),
                po.getIcon(),
                po.getCategory(),
                po.getStatus(),
                po.getSortOrder(),
                po.isSystem(),
                ticketTypeRepository.countLinkedDomainsByGlobalTypeId(po.getId()),
                toDateTimeString(po.getCreatedAt()),
                toDateTimeString(po.getUpdatedAt()));
    }

    private TicketConfigDtos.PlatformTicketTypeDetailView toDetailView(TicketTypePo po, String pluginRevision) {
        TicketFormSchemaService.FormSchemaAggregate aggregate = ticketFormSchemaService.loadAggregate(
                TicketFormSchemaRepository.PLATFORM_SCHEMA_DOMAIN_KEY,
                po.getId(),
                pluginRevision);
        Integer currentVersionNo = aggregate.currentVersionNo() > 0 ? aggregate.currentVersionNo() : null;
        return new TicketConfigDtos.PlatformTicketTypeDetailView(
                String.valueOf(po.getId()),
                po.getScope(),
                po.getCode(),
                po.getName(),
                po.getDescription(),
                po.getIcon(),
                po.getCategory(),
                po.getStatus(),
                po.getSortOrder(),
                po.isSystem(),
                ticketTypeRepository.countLinkedDomainsByGlobalTypeId(po.getId()),
                ticketTypeFlowService.loadStatusFlow(0L, po.getId()),
                aggregate.publishedSchema(),
                aggregate.draftSchema(),
                currentVersionNo,
                aggregate.hasUnpublished(),
                toDateTimeString(po.getCreatedAt()),
                toDateTimeString(po.getUpdatedAt()));
    }

    private Object readJsonObject(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        }
        catch (JsonProcessingException ex) {
            throw new IllegalStateException("invalid json payload", ex);
        }
    }

    private String resolveCategory(String category, String templateKey) {
        if (StringUtils.hasText(category)) {
            return category.trim().toLowerCase(Locale.ROOT);
        }
        if (StringUtils.hasText(templateKey)) {
            return switch (templateKey.trim().toLowerCase(Locale.ROOT)) {
                case "simple_ticket" -> "feedback";
                case "standard_ticket", "ticket", "requirement", "task" -> "transaction";
                default -> "transaction";
            };
        }
        return "transaction";
    }

    private String defaultDescription(String templateKey) {
        if (!StringUtils.hasText(templateKey)) {
            return "";
        }
        return switch (templateKey.trim().toLowerCase(Locale.ROOT)) {
            case "simple_ticket" -> "适用于快速记录的轻量事项，默认仅描述必填";
            case "standard_ticket", "ticket" -> "适用于标准工单流程的事项，默认标题与描述均必填";
            case "requirement" -> "用于描述软件功能或用户需求";
            case "task" -> "用于跟踪具体工作任务与执行进度";
            default -> "";
        };
    }

    private String resolveCode(String code, String templateKey, String name) {
        if (StringUtils.hasText(code)) {
            return code.trim().toLowerCase(Locale.ROOT);
        }
        if (StringUtils.hasText(templateKey)) {
            String base = templateKey.trim().toLowerCase(Locale.ROOT);
            TicketTypePo existing = ticketTypeRepository.findPlatformByCode(base);
            if (existing == null) {
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

    private void assertPlatformCodeUnique(String code, Long excludeTypeId) {
        TicketTypePo existing = ticketTypeRepository.findPlatformByCode(code);
        if (existing != null && (excludeTypeId == null || existing.getId() != excludeTypeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "编码已存在");
        }
    }

    private void assertPlatformNameUnique(String name, Long excludeTypeId) {
        TicketTypePo existing = ticketTypeRepository.findPlatformByName(name);
        if (existing != null && (excludeTypeId == null || existing.getId() != excludeTypeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "名称已存在");
        }
    }

    private ResponseStatusException translatePlatformDuplicate(DuplicateKeyException ex) {
        String message = ex.getMostSpecificCause() == null ? null : ex.getMostSpecificCause().getMessage();
        if (message != null && message.contains("uk_ticket_type_scope_domain_name")) {
            return new ResponseStatusException(HttpStatus.CONFLICT, "名称已存在", ex);
        }
        return new ResponseStatusException(HttpStatus.CONFLICT, "编码已存在", ex);
    }

    private String toKeywordLike(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return "%" + keyword.trim() + "%";
    }

    private String requiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String trimToEmpty(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (Exception ex) {
            throw new IllegalStateException("failed to serialize json payload", ex);
        }
    }

    private String toDateTimeString(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
