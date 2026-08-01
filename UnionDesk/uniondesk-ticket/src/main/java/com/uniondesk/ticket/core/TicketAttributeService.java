package com.uniondesk.ticket.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.ticket.entity.TicketAttributePo;
import com.uniondesk.ticket.repository.TicketAttributeRepository;
import com.uniondesk.ticket.repository.TicketTypeAttributeSlotRepository;
import com.uniondesk.ticket.web.TicketAttributeDtos;
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
public class TicketAttributeService {

    private final TicketAttributeRepository ticketAttributeRepository;
    private final TicketTypeAttributeSlotRepository slotRepository;
    private final PlatformTicketTypeCopyService platformTicketTypeCopyService;
    private final ObjectMapper objectMapper;

    public TicketAttributeService(
            TicketAttributeRepository ticketAttributeRepository,
            TicketTypeAttributeSlotRepository slotRepository,
            PlatformTicketTypeCopyService platformTicketTypeCopyService,
            ObjectMapper objectMapper) {
        this.ticketAttributeRepository = ticketAttributeRepository;
        this.slotRepository = slotRepository;
        this.platformTicketTypeCopyService = platformTicketTypeCopyService;
        this.objectMapper = objectMapper;
    }

    public TicketAttributeDtos.TicketAttributeListView listPlatform(String keyword, Integer page, Integer pageSize) {
        return list(null, keyword, page, pageSize);
    }

    public TicketAttributeDtos.TicketAttributeListView listDomain(long domainId, String keyword, Integer page, Integer pageSize) {
        return list(domainId, keyword, page, pageSize);
    }

    @Transactional
    public TicketAttributeDtos.TicketAttributeView createPlatform(
            TicketAttributeDtos.CreateTicketAttributeRequest request,
            Long operatorId) {
        return create(null, request, operatorId);
    }

    @Transactional
    public TicketAttributeDtos.TicketAttributeView createDomain(
            long domainId,
            TicketAttributeDtos.CreateTicketAttributeRequest request,
            Long operatorId) {
        return create(domainId, request, operatorId);
    }

    @Transactional
    public TicketAttributeDtos.TicketAttributeView updatePlatform(
            long attributeId,
            TicketAttributeDtos.UpdateTicketAttributeRequest request,
            Long operatorId) {
        return update(null, attributeId, request, operatorId);
    }

    @Transactional
    public TicketAttributeDtos.TicketAttributeView updateDomain(
            long domainId,
            long attributeId,
            TicketAttributeDtos.UpdateTicketAttributeRequest request,
            Long operatorId) {
        return update(domainId, attributeId, request, operatorId);
    }

    @Transactional
    public void deletePlatform(long attributeId) {
        delete(null, attributeId);
    }

    @Transactional
    public void deleteDomain(long domainId, long attributeId) {
        delete(domainId, attributeId);
    }

    @Transactional
    public void reorderPlatform(TicketAttributeDtos.ReorderTicketAttributesRequest request, Long operatorId) {
        reorder(null, request, operatorId);
    }

    @Transactional
    public void reorderDomain(long domainId, TicketAttributeDtos.ReorderTicketAttributesRequest request, Long operatorId) {
        reorder(domainId, request, operatorId);
    }

    @Transactional
    public List<TicketAttributeDtos.TicketAttributeView> importFromPlatform(
            long domainId,
            TicketAttributeDtos.ImportPlatformTicketAttributesRequest request,
            Long operatorId) {
        if (request == null || request.platform_attribute_ids() == null || request.platform_attribute_ids().isEmpty()) {
            throw new IllegalArgumentException("platform_attribute_ids is required");
        }
        List<TicketAttributeDtos.TicketAttributeView> created = new java.util.ArrayList<>();
        for (String rawId : request.platform_attribute_ids()) {
            long platformAttributeId = parseLong(rawId, "platform_attribute_id");
            TicketAttributePo platform = findPlatformAttribute(platformAttributeId);
            assertPlatformAttributeNotInDomain(domainId, platform);
            TicketAttributePo domainAttr = platformTicketTypeCopyService.ensureDomainAttribute(
                    domainId, platform, operatorId);
            created.add(toView(domainAttr));
        }
        return created;
    }

    private void assertPlatformAttributeNotInDomain(long domainId, TicketAttributePo platform) {
        if (ticketAttributeRepository.findDomainBySourceAttributeId(domainId, platform.getId()) != null) {
            throw new IllegalArgumentException("该平台事项属性已添加到当前域");
        }
        if (StringUtils.hasText(platform.getSystemKey())
                && ticketAttributeRepository.findDomainBySystemKey(domainId, platform.getSystemKey()) != null) {
            throw new IllegalArgumentException("该平台事项属性已添加到当前域");
        }
        if (ticketAttributeRepository.findDomainByName(domainId, platform.getName()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "属性名称已存在");
        }
    }

    private TicketAttributePo findPlatformAttribute(long attributeId) {
        TicketAttributePo existing = ticketAttributeRepository.findRequiredById(attributeId);
        if (!TicketAttributePo.SCOPE_PLATFORM.equals(existing.getScope())) {
            throw new IllegalArgumentException("属性不存在");
        }
        return existing;
    }

    private long parseLong(String raw, String fieldName) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        try {
            return Long.parseLong(raw.trim());
        }
        catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " is invalid");
        }
    }

    @Transactional
    public TicketAttributeDtos.TicketAttributeView promoteFromDomain(long domainId, long attributeId, Long operatorId) {
        TicketAttributePo source = ticketAttributeRepository.findRequiredById(attributeId);
        if (!TicketAttributePo.SCOPE_DOMAIN.equals(source.getScope())
                || source.getBusinessDomainId() == null
                || source.getBusinessDomainId() != domainId) {
            throw new IllegalArgumentException("属性不存在");
        }
        if (ticketAttributeRepository.findPlatformByName(source.getName()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "全局已存在同名属性");
        }
        Map<String, Object> typeConfig = readJsonMap(source.getTypeConfig());
        TicketAttributePo target = new TicketAttributePo();
        target.setScope(TicketAttributePo.SCOPE_PLATFORM);
        target.setBusinessDomainId(null);
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setFieldType(source.getFieldType());
        target.setTypeConfig(toJson(typeConfig));
        target.setStatus(source.getStatus());
        target.setSortOrder(ticketAttributeRepository.nextSortOrderPlatform());
        target.setSystem(source.isSystem());
        target.setSourceAttributeId(source.getId());
        target.setCreatedBy(operatorId);
        target.setUpdatedBy(operatorId);
        try {
            ticketAttributeRepository.insert(target);
        }
        catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "全局已存在同名属性", ex);
        }
        return toView(target);
    }

    private TicketAttributeDtos.TicketAttributeListView list(Long domainId, String keyword, Integer page, Integer pageSize) {
        String keywordLike = toKeywordLike(keyword);
        long total = domainId == null
                ? ticketAttributeRepository.countPlatform(keywordLike)
                : ticketAttributeRepository.countDomain(domainId, keywordLike);
        List<TicketAttributePo> rows;
        if (total <= TicketAttributeRepository.NO_PAGINATION_THRESHOLD || page == null || pageSize == null) {
            rows = domainId == null
                    ? ticketAttributeRepository.findAllPlatform(keywordLike)
                    : ticketAttributeRepository.findAllDomain(domainId, keywordLike);
        }
        else {
            int normalizedPage = Math.max(page, 1);
            int normalizedPageSize = Math.max(pageSize, 1);
            long offset = (long) (normalizedPage - 1) * normalizedPageSize;
            rows = domainId == null
                    ? ticketAttributeRepository.findPagePlatform(keywordLike, normalizedPageSize, offset)
                    : ticketAttributeRepository.findPageDomain(domainId, keywordLike, normalizedPageSize, offset);
        }
        return new TicketAttributeDtos.TicketAttributeListView(
                total,
                rows.stream().map(this::toView).toList());
    }

    private TicketAttributeDtos.TicketAttributeView create(
            Long domainId,
            TicketAttributeDtos.CreateTicketAttributeRequest request,
            Long operatorId) {
        String name = requiredText(request.name(), "name");
        assertNotReservedSystemName(name);
        String description = trimToEmpty(request.description());
        String fieldType = request.field_type().trim().toLowerCase(Locale.ROOT);
        if ("member".equals(fieldType)) {
            throw new IllegalArgumentException("成员类型仅系统属性可用，请使用系统「处理人/关注人」");
        }
        Map<String, Object> typeConfig = TicketAttributeTypeConfigValidator.validateAndNormalize(
                fieldType, request.type_config(), objectMapper);
        assertNameUnique(domainId, name, null);
        TicketAttributePo po = new TicketAttributePo();
        po.setScope(domainId == null ? TicketAttributePo.SCOPE_PLATFORM : TicketAttributePo.SCOPE_DOMAIN);
        po.setBusinessDomainId(domainId);
        po.setName(name);
        po.setDescription(description);
        po.setFieldType(fieldType);
        po.setTypeConfig(toJson(typeConfig));
        po.setStatus(TicketAttributePo.STATUS_ACTIVE);
        po.setSortOrder(domainId == null
                ? ticketAttributeRepository.nextSortOrderPlatform()
                : ticketAttributeRepository.nextSortOrderDomain(domainId));
        po.setSystem(false);
        po.setCreatedBy(operatorId);
        po.setUpdatedBy(operatorId);
        try {
            ticketAttributeRepository.insert(po);
        }
        catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "属性名称已存在", ex);
        }
        return toView(po);
    }

    private TicketAttributeDtos.TicketAttributeView update(
            Long domainId,
            long attributeId,
            TicketAttributeDtos.UpdateTicketAttributeRequest request,
            Long operatorId) {
        TicketAttributePo existing = findScopedAttribute(domainId, attributeId);
        String name = StringUtils.hasText(request.name()) ? request.name().trim() : existing.getName();
        if (existing.isSystem() && domainId == null && StringUtils.hasText(request.name())
                && !name.equals(existing.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "系统属性名称不可修改");
        }
        String description = request.description() == null
                ? existing.getDescription()
                : trimToEmpty(request.description());
        String fieldType = StringUtils.hasText(request.field_type())
                ? request.field_type().trim().toLowerCase(Locale.ROOT)
                : existing.getFieldType();
        Object typeConfigInput = request.type_config() == null ? readJsonMap(existing.getTypeConfig()) : request.type_config();
        Map<String, Object> typeConfig = TicketAttributeTypeConfigValidator.validateAndNormalize(
                fieldType, typeConfigInput, objectMapper);
        assertNameUnique(domainId, name, attributeId);
        existing.setName(name);
        existing.setDescription(description);
        existing.setFieldType(fieldType);
        existing.setTypeConfig(toJson(typeConfig));
        if (StringUtils.hasText(request.status())) {
            existing.setStatus(request.status().trim());
        }
        existing.setUpdatedBy(operatorId);
        try {
            ticketAttributeRepository.update(existing);
        }
        catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "属性名称已存在", ex);
        }
        return toView(existing);
    }

    private void delete(Long domainId, long attributeId) {
        TicketAttributePo existing = findScopedAttribute(domainId, attributeId);
        if (existing.isSystem()) {
            throw new IllegalArgumentException("系统属性不可删除");
        }
        if (slotRepository.countByAttributeId(attributeId) > 0) {
            throw new IllegalArgumentException("属性已被事项类型引用，无法删除");
        }
        int updated = domainId == null
                ? ticketAttributeRepository.deletePlatform(attributeId)
                : ticketAttributeRepository.deleteDomain(attributeId, domainId);
        if (updated == 0) {
            throw new IllegalArgumentException("属性不存在");
        }
    }

    private void reorder(Long domainId, TicketAttributeDtos.ReorderTicketAttributesRequest request, Long operatorId) {
        if (request.orders() == null || request.orders().isEmpty()) {
            return;
        }
        for (TicketAttributeDtos.SortOrderItem item : request.orders()) {
            TicketAttributePo existing = findScopedAttribute(domainId, item.id());
            ticketAttributeRepository.updateSortOrder(existing.getId(), item.sort_order(), operatorId);
        }
    }

    private TicketAttributePo findScopedAttribute(Long domainId, long attributeId) {
        TicketAttributePo existing = ticketAttributeRepository.findRequiredById(attributeId);
        if (domainId == null) {
            if (!TicketAttributePo.SCOPE_PLATFORM.equals(existing.getScope())) {
                throw new IllegalArgumentException("属性不存在");
            }
            return existing;
        }
        if (!TicketAttributePo.SCOPE_DOMAIN.equals(existing.getScope())
                || existing.getBusinessDomainId() == null
                || existing.getBusinessDomainId() != domainId) {
            throw new IllegalArgumentException("属性不存在");
        }
        return existing;
    }

    private void assertNotReservedSystemName(String name) {
        String normalized = name.trim();
        if (Set.of("标题", "描述", "优先级", "处理人", "关注人").contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该名称已被系统属性占用");
        }
    }

    private void assertNameUnique(Long domainId, String name, Long excludeId) {
        TicketAttributePo existing = domainId == null
                ? ticketAttributeRepository.findPlatformByName(name)
                : ticketAttributeRepository.findDomainByName(domainId, name);
        if (existing != null && (excludeId == null || existing.getId() != excludeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "属性名称已存在");
        }
    }

    private TicketAttributeDtos.TicketAttributeView toView(TicketAttributePo po) {
        return new TicketAttributeDtos.TicketAttributeView(
                String.valueOf(po.getId()),
                po.getScope(),
                po.getBusinessDomainId() == null ? null : String.valueOf(po.getBusinessDomainId()),
                po.getName(),
                po.getDescription(),
                po.getFieldType(),
                readJsonMap(po.getTypeConfig()),
                po.getStatus(),
                po.getSortOrder(),
                po.isSystem(),
                po.getSystemKey(),
                po.getSourceAttributeId() == null ? null : String.valueOf(po.getSourceAttributeId()),
                po.getCreatedAt() == null ? null : po.getCreatedAt().toString(),
                po.getUpdatedAt() == null ? null : po.getUpdatedAt().toString());
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String trimToEmpty(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim();
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
}
