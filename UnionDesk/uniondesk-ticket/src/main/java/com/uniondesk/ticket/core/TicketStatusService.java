package com.uniondesk.ticket.core;

import com.uniondesk.ticket.entity.TicketStatusPo;
import com.uniondesk.ticket.repository.TicketStatusRepository;
import com.uniondesk.ticket.web.TicketStatusDtos;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketStatusService {

    private static final Set<String> VALID_CATEGORIES = Set.of(
            TicketStatusPo.CATEGORY_NOT_STARTED,
            TicketStatusPo.CATEGORY_IN_PROGRESS,
            TicketStatusPo.CATEGORY_COMPLETED);

    private final TicketStatusRepository ticketStatusRepository;

    public TicketStatusService(TicketStatusRepository ticketStatusRepository) {
        this.ticketStatusRepository = ticketStatusRepository;
    }

    public TicketStatusDtos.TicketStatusListView listPlatform(String keyword, Integer page, Integer pageSize) {
        String keywordLike = toKeywordLike(keyword);
        long total = ticketStatusRepository.countPlatform(keywordLike);
        List<TicketStatusPo> rows;
        if (total <= TicketStatusRepository.NO_PAGINATION_THRESHOLD || page == null || pageSize == null) {
            rows = ticketStatusRepository.findAllPlatform(keywordLike);
        }
        else {
            int normalizedPage = Math.max(page, 1);
            int normalizedPageSize = Math.max(pageSize, 1);
            long offset = (long) (normalizedPage - 1) * normalizedPageSize;
            rows = ticketStatusRepository.findPagePlatform(keywordLike, normalizedPageSize, offset);
        }
        return new TicketStatusDtos.TicketStatusListView(
                total,
                rows.stream().map(this::toView).toList());
    }

    @Transactional
    public TicketStatusDtos.TicketStatusView createPlatform(
            TicketStatusDtos.CreateTicketStatusRequest request,
            Long operatorId) {
        String name = requiredText(request.name(), "name");
        String description = trimToEmpty(request.description());
        String category = normalizeCategory(request.category());
        String code = resolveCode(request.code(), name);
        assertNameUnique(name, null);
        assertCodeUnique(code, null);
        TicketStatusPo po = new TicketStatusPo();
        po.setScope(TicketStatusPo.SCOPE_PLATFORM);
        po.setBusinessDomainId(null);
        po.setCode(code);
        po.setName(name);
        po.setDescription(description);
        po.setCategory(category);
        po.setStateType(defaultStateTypeForCategory(category));
        po.setConfigJson("{}");
        po.setStatus(TicketStatusPo.STATUS_ACTIVE);
        po.setSortOrder(ticketStatusRepository.nextSortOrderPlatform());
        po.setSystem(false);
        po.setCreatedBy(operatorId);
        po.setUpdatedBy(operatorId);
        try {
            ticketStatusRepository.insert(po);
        }
        catch (DuplicateKeyException ex) {
            throw translateDuplicate(ex);
        }
        return toView(po);
    }

    @Transactional
    public TicketStatusDtos.TicketStatusView updatePlatform(
            long statusId,
            TicketStatusDtos.UpdateTicketStatusRequest request,
            Long operatorId) {
        TicketStatusPo existing = findPlatformStatus(statusId);
        if (existing.isSystem()) {
            existing.setDescription(request.description() == null
                    ? existing.getDescription()
                    : trimToEmpty(request.description()));
            if (StringUtils.hasText(request.name()) && !request.name().trim().equals(existing.getName())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "系统状态名称不可修改");
            }
            if (StringUtils.hasText(request.category()) && !request.category().trim().equals(existing.getCategory())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "系统状态类型不可修改");
            }
        }
        else {
            String name = StringUtils.hasText(request.name()) ? request.name().trim() : existing.getName();
            String description = request.description() == null
                    ? existing.getDescription()
                    : trimToEmpty(request.description());
            String category = StringUtils.hasText(request.category())
                    ? normalizeCategory(request.category())
                    : existing.getCategory();
            assertNameUnique(name, statusId);
            existing.setName(name);
            existing.setDescription(description);
            existing.setCategory(category);
            existing.setStateType(defaultStateTypeForCategory(category));
        }
        existing.setUpdatedBy(operatorId);
        try {
            ticketStatusRepository.update(existing);
        }
        catch (DuplicateKeyException ex) {
            throw translateDuplicate(ex);
        }
        return toView(existing);
    }

    @Transactional
    public void deletePlatform(long statusId) {
        TicketStatusPo existing = findPlatformStatus(statusId);
        if (existing.isSystem()) {
            throw new IllegalArgumentException("系统状态不可删除");
        }
        int updated = ticketStatusRepository.deletePlatform(statusId);
        if (updated == 0) {
            throw new IllegalArgumentException("状态不存在");
        }
    }

    private TicketStatusPo findPlatformStatus(long statusId) {
        TicketStatusPo existing = ticketStatusRepository.findRequiredById(statusId);
        if (!TicketStatusPo.SCOPE_PLATFORM.equals(existing.getScope())) {
            throw new IllegalArgumentException("状态不存在");
        }
        return existing;
    }

    private void assertNameUnique(String name, Long excludeId) {
        TicketStatusPo existing = ticketStatusRepository.findPlatformByName(name);
        if (existing != null && (excludeId == null || existing.getId() != excludeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "状态名称已存在");
        }
    }

    private void assertCodeUnique(String code, Long excludeId) {
        TicketStatusPo existing = ticketStatusRepository.findPlatformByCode(code);
        if (existing != null && (excludeId == null || existing.getId() != excludeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "状态编码已存在");
        }
    }

    private String normalizeCategory(String category) {
        if (!StringUtils.hasText(category)) {
            throw new IllegalArgumentException("category is required");
        }
        String normalized = category.trim().toLowerCase(Locale.ROOT);
        if (!VALID_CATEGORIES.contains(normalized)) {
            throw new IllegalArgumentException("invalid category");
        }
        return normalized;
    }

    static String defaultStateTypeForCategory(String category) {
        return switch (category) {
            case TicketStatusPo.CATEGORY_NOT_STARTED -> TicketStatusPo.STATE_TYPE_PAUSED;
            case TicketStatusPo.CATEGORY_IN_PROGRESS -> TicketStatusPo.STATE_TYPE_IN_PROGRESS;
            case TicketStatusPo.CATEGORY_COMPLETED -> TicketStatusPo.STATE_TYPE_TERMINAL;
            default -> throw new IllegalArgumentException("invalid category");
        };
    }

    private String resolveCode(String code, String name) {
        if (StringUtils.hasText(code)) {
            return code.trim().toLowerCase(Locale.ROOT);
        }
        String base = name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        if (!StringUtils.hasText(base) || "_".equals(base)) {
            base = "status";
        }
        return base + "_" + Long.toHexString(System.nanoTime()).substring(0, 6);
    }

    private TicketStatusDtos.TicketStatusView toView(TicketStatusPo po) {
        return new TicketStatusDtos.TicketStatusView(
                String.valueOf(po.getId()),
                po.getScope(),
                po.getCode(),
                po.getName(),
                po.getDescription(),
                po.getCategory(),
                po.getStateType(),
                po.getStatus(),
                po.getSortOrder(),
                po.isSystem(),
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

    private String trimToEmpty(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim();
    }

    private ResponseStatusException translateDuplicate(DuplicateKeyException ex) {
        return new ResponseStatusException(HttpStatus.CONFLICT, "状态名称或编码已存在", ex);
    }
}
