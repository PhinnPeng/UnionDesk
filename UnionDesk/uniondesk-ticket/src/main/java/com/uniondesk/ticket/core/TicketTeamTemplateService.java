package com.uniondesk.ticket.core;

import com.uniondesk.ticket.entity.TicketTeamTemplateItemPo;
import com.uniondesk.ticket.entity.TicketTeamTemplatePo;
import com.uniondesk.ticket.entity.TicketTypePo;
import com.uniondesk.ticket.repository.TicketTeamTemplateRepository;
import com.uniondesk.ticket.repository.TicketTypeRepository;
import com.uniondesk.ticket.web.TicketTeamTemplateDtos;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketTeamTemplateService {

    private final TicketTeamTemplateRepository templateRepository;
    private final TicketTypeRepository ticketTypeRepository;

    public TicketTeamTemplateService(
            TicketTeamTemplateRepository templateRepository,
            TicketTypeRepository ticketTypeRepository) {
        this.templateRepository = templateRepository;
        this.ticketTypeRepository = ticketTypeRepository;
    }

    public TicketTeamTemplateDtos.TeamTemplateListView list(String keyword, Integer page, Integer pageSize) {
        String keywordLike = toKeywordLike(keyword);
        long total = templateRepository.countAll(keywordLike);
        List<TicketTeamTemplatePo> rows;
        if (total <= TicketTeamTemplateRepository.NO_PAGINATION_THRESHOLD || page == null || pageSize == null) {
            rows = templateRepository.findAll(keywordLike);
        }
        else {
            int normalizedPage = Math.max(page, 1);
            int normalizedPageSize = Math.max(pageSize, 1);
            long offset = (long) (normalizedPage - 1) * normalizedPageSize;
            rows = templateRepository.findPage(keywordLike, normalizedPageSize, offset);
        }
        Map<Long, List<TicketTeamTemplateItemPo>> itemsByTemplate = loadItemsGrouped(rows);
        return new TicketTeamTemplateDtos.TeamTemplateListView(
                total,
                rows.stream().map(po -> toView(po, itemsByTemplate.getOrDefault(po.getId(), List.of()))).toList());
    }

    public TicketTeamTemplateDtos.TeamTemplateView get(long templateId) {
        TicketTeamTemplatePo po = templateRepository.findRequiredById(templateId);
        return toView(po, templateRepository.findItemsByTemplateId(templateId));
    }

    public List<TicketTeamTemplateDtos.TeamTemplateOptionView> listActiveOptions() {
        List<TicketTeamTemplatePo> rows = templateRepository.findActiveOptions();
        Map<Long, List<TicketTeamTemplateItemPo>> itemsByTemplate = loadItemsGrouped(rows);
        return rows.stream()
                .map(po -> new TicketTeamTemplateDtos.TeamTemplateOptionView(
                        String.valueOf(po.getId()),
                        po.getCode(),
                        po.getName(),
                        po.getDescription(),
                        po.getIcon(),
                        po.getVersion(),
                        itemsByTemplate.getOrDefault(po.getId(), List.of()).size()))
                .toList();
    }

    @Transactional
    public TicketTeamTemplateDtos.TeamTemplateView create(
            TicketTeamTemplateDtos.CreateTeamTemplateRequest request,
            Long operatorId) {
        String name = requiredText(request.name(), "name");
        String code = resolveCode(request.code(), name);
        List<NormalizedItem> items = normalizeItems(request.items());
        TicketTeamTemplatePo po = new TicketTeamTemplatePo();
        po.setCode(code);
        po.setName(name);
        po.setDescription(trimToEmpty(request.description()));
        po.setIcon(trimToNull(request.icon()));
        po.setStatus(normalizeStatus(request.status(), TicketTeamTemplatePo.STATUS_ACTIVE));
        po.setSystem(false);
        po.setSortOrder(templateRepository.nextSortOrder());
        po.setVersion(1);
        po.setCreatedBy(operatorId);
        po.setUpdatedBy(operatorId);
        try {
            templateRepository.insert(po);
        }
        catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "模板编码已存在");
        }
        replaceItems(po.getId(), items);
        return get(po.getId());
    }

    @Transactional
    public TicketTeamTemplateDtos.TeamTemplateView update(
            long templateId,
            TicketTeamTemplateDtos.UpdateTeamTemplateRequest request,
            Long operatorId) {
        TicketTeamTemplatePo existing = templateRepository.findRequiredById(templateId);
        List<TicketTeamTemplateItemPo> currentItems = templateRepository.findItemsByTemplateId(templateId);

        if (StringUtils.hasText(request.name())) {
            existing.setName(request.name().trim());
        }
        if (request.description() != null) {
            existing.setDescription(trimToEmpty(request.description()));
        }
        if (request.icon() != null) {
            existing.setIcon(trimToNull(request.icon()));
        }
        if (StringUtils.hasText(request.status())) {
            existing.setStatus(normalizeStatus(request.status(), existing.getStatus()));
        }
        if (request.items() != null) {
            List<NormalizedItem> nextItems = normalizeItems(request.items());
            if (!sameItems(currentItems, nextItems)) {
                existing.setVersion(existing.getVersion() + 1);
                replaceItems(templateId, nextItems);
            }
        }
        existing.setUpdatedBy(operatorId);
        templateRepository.update(existing);
        return get(templateId);
    }

    @Transactional
    public void delete(long templateId) {
        TicketTeamTemplatePo existing = templateRepository.findRequiredById(templateId);
        if (existing.isSystem()) {
            throw new IllegalArgumentException("系统模板不可删除");
        }
        templateRepository.deleteItemsByTemplateId(templateId);
        int updated = templateRepository.deleteById(templateId);
        if (updated == 0) {
            throw new IllegalArgumentException("团队模板不存在");
        }
    }

    @Transactional
    public void reorder(TicketTeamTemplateDtos.ReorderTeamTemplatesRequest request, Long operatorId) {
        List<Long> orderedIds = request.ordered_ids();
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < orderedIds.size(); i++) {
            Long id = orderedIds.get(i);
            if (id == null || !seen.add(id)) {
                throw new IllegalArgumentException("排序列表无效");
            }
            templateRepository.findRequiredById(id);
            templateRepository.updateSortOrder(id, i, operatorId);
        }
    }

    private Map<Long, List<TicketTeamTemplateItemPo>> loadItemsGrouped(List<TicketTeamTemplatePo> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = rows.stream().map(TicketTeamTemplatePo::getId).toList();
        return templateRepository.findItemsByTemplateIds(ids).stream()
                .collect(Collectors.groupingBy(
                        TicketTeamTemplateItemPo::getTeamTemplateId,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private void replaceItems(long templateId, List<NormalizedItem> items) {
        templateRepository.deleteItemsByTemplateId(templateId);
        for (NormalizedItem item : items) {
            TicketTeamTemplateItemPo po = new TicketTeamTemplateItemPo();
            po.setTeamTemplateId(templateId);
            po.setTicketTypeId(item.ticketTypeId());
            po.setSortOrder(item.sortOrder());
            po.setIncludeFormSchema(item.includeFormSchema());
            po.setIncludeWorkflow(item.includeWorkflow());
            po.setIncludeDescriptionTemplate(item.includeDescriptionTemplate());
            templateRepository.insertItem(po);
        }
    }

    private List<NormalizedItem> normalizeItems(List<TicketTeamTemplateDtos.TeamTemplateItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        if (requests.size() > 20) {
            throw new IllegalArgumentException("单个模板最多包含 20 个事项类型");
        }
        Set<Long> seenTypeIds = new HashSet<>();
        List<NormalizedItem> result = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            TicketTeamTemplateDtos.TeamTemplateItemRequest req = requests.get(i);
            if (req.ticket_type_id() == null) {
                throw new IllegalArgumentException("ticket_type_id is required");
            }
            long typeId = req.ticket_type_id();
            if (!seenTypeIds.add(typeId)) {
                throw new IllegalArgumentException("事项类型不可重复");
            }
            TicketTypePo type = ticketTypeRepository.findPlatformById(typeId);
            if (type == null || !TicketTypePo.SCOPE_PLATFORM.equals(type.getScope())) {
                throw new IllegalArgumentException("事项类型不存在或非平台类型: " + typeId);
            }
            int sortOrder = req.sort_order() == null ? i : req.sort_order();
            result.add(new NormalizedItem(
                    typeId,
                    sortOrder,
                    req.include_form_schema() == null || req.include_form_schema(),
                    req.include_workflow() == null || req.include_workflow(),
                    req.include_description_template() == null || req.include_description_template()));
        }
        result.sort((a, b) -> Integer.compare(a.sortOrder(), b.sortOrder()));
        for (int i = 0; i < result.size(); i++) {
            NormalizedItem item = result.get(i);
            result.set(i, new NormalizedItem(
                    item.ticketTypeId(),
                    i,
                    item.includeFormSchema(),
                    item.includeWorkflow(),
                    item.includeDescriptionTemplate()));
        }
        return result;
    }

    private boolean sameItems(List<TicketTeamTemplateItemPo> current, List<NormalizedItem> next) {
        if (current.size() != next.size()) {
            return false;
        }
        for (int i = 0; i < current.size(); i++) {
            TicketTeamTemplateItemPo left = current.get(i);
            NormalizedItem right = next.get(i);
            if (left.getTicketTypeId() != right.ticketTypeId()
                    || left.getSortOrder() != right.sortOrder()
                    || left.isIncludeFormSchema() != right.includeFormSchema()
                    || left.isIncludeWorkflow() != right.includeWorkflow()
                    || left.isIncludeDescriptionTemplate() != right.includeDescriptionTemplate()) {
                return false;
            }
        }
        return true;
    }

    private TicketTeamTemplateDtos.TeamTemplateView toView(
            TicketTeamTemplatePo po,
            List<TicketTeamTemplateItemPo> items) {
        return new TicketTeamTemplateDtos.TeamTemplateView(
                String.valueOf(po.getId()),
                po.getCode(),
                po.getName(),
                po.getDescription(),
                po.getIcon(),
                po.getStatus(),
                po.isSystem(),
                po.getSortOrder(),
                po.getVersion(),
                items.stream().map(this::toItemView).toList(),
                po.getCreatedAt() == null ? null : po.getCreatedAt().toString(),
                po.getUpdatedAt() == null ? null : po.getUpdatedAt().toString());
    }

    private TicketTeamTemplateDtos.TeamTemplateItemView toItemView(TicketTeamTemplateItemPo po) {
        return new TicketTeamTemplateDtos.TeamTemplateItemView(
                String.valueOf(po.getId()),
                String.valueOf(po.getTicketTypeId()),
                po.getTicketTypeCode(),
                po.getTicketTypeName(),
                po.getSortOrder(),
                po.isIncludeFormSchema(),
                po.isIncludeWorkflow(),
                po.isIncludeDescriptionTemplate());
    }

    private void assertCodeUnique(String code) {
        TicketTeamTemplatePo existing = templateRepository.findByCode(code);
        if (existing != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "模板编码已存在");
        }
    }

    private String resolveCode(String code, String name) {
        if (StringUtils.hasText(code)) {
            String normalized = normalizeCode(code);
            assertCodeUnique(normalized);
            return normalized;
        }
        return generateUniqueCode(name);
    }

    private String generateUniqueCode(String name) {
        String base = slugifyName(name);
        String candidate = base;
        int suffix = 2;
        while (templateRepository.findByCode(candidate) != null) {
            String suffixText = "_" + suffix;
            int maxBaseLen = Math.max(1, 64 - suffixText.length());
            String truncated = base.length() > maxBaseLen ? base.substring(0, maxBaseLen) : base;
            if (!truncated.isEmpty() && truncated.charAt(truncated.length() - 1) == '_') {
                truncated = truncated.substring(0, truncated.length() - 1);
            }
            if (truncated.isEmpty() || !Character.isLetter(truncated.charAt(0))) {
                truncated = "tpl";
            }
            candidate = truncated + suffixText;
            suffix++;
            if (suffix > 1000) {
                throw new IllegalStateException("无法生成唯一模板编码");
            }
        }
        return candidate;
    }

    private String slugifyName(String name) {
        String raw = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(c);
            }
            else if (c == '_' || c == '-' || Character.isWhitespace(c)) {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '_') {
                    sb.append('_');
                }
            }
        }
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '_') {
            sb.setLength(sb.length() - 1);
        }
        String slug = sb.toString();
        if (slug.isEmpty() || !Character.isLetter(slug.charAt(0))) {
            slug = slug.isEmpty() ? "tpl_template" : "tpl_" + slug;
        }
        if (slug.length() > 64) {
            slug = slug.substring(0, 64);
            while (slug.endsWith("_")) {
                slug = slug.substring(0, slug.length() - 1);
            }
        }
        if (!slug.matches("^[a-z][a-z0-9_]{0,63}$")) {
            slug = "tpl_template";
        }
        return slug;
    }

    private String normalizeCode(String code) {
        String normalized = requiredText(code, "code").toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[a-z][a-z0-9_]{1,63}$")) {
            throw new IllegalArgumentException("编码需以字母开头，仅含小写字母、数字与下划线");
        }
        return normalized;
    }

    private String normalizeStatus(String status, String fallback) {
        if (!StringUtils.hasText(status)) {
            return fallback;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (!TicketTeamTemplatePo.STATUS_ACTIVE.equals(normalized)
                && !TicketTeamTemplatePo.STATUS_DISABLED.equals(normalized)) {
            throw new IllegalArgumentException("invalid status");
        }
        return normalized;
    }

    private String requiredText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String toKeywordLike(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return "%" + keyword.trim() + "%";
    }

    private record NormalizedItem(
            long ticketTypeId,
            int sortOrder,
            boolean includeFormSchema,
            boolean includeWorkflow,
            boolean includeDescriptionTemplate) {
    }
}
