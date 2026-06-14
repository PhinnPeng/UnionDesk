package com.uniondesk.domain.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.UserContext;
import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.audit.semantics.AuditLogWriter;
import com.uniondesk.common.audit.AuditActionCodes;
import com.uniondesk.common.audit.AuditDetailTextBuilder;
import com.uniondesk.common.audit.AuditTargetFormatter;
import com.uniondesk.common.web.ErrorCodes;
import com.uniondesk.common.web.PageResult;
import com.uniondesk.domain.entity.BusinessDomainPo;
import com.uniondesk.domain.repository.DomainRepository;
import com.uniondesk.domain.web.DomainDtos;
import com.uniondesk.iam.core.IamService;
import com.uniondesk.iam.core.PermissionCodes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DomainService {

    private static final List<String> DEFAULT_VISIBILITY_POLICY_CODES = List.of("public");

    private final DomainRepository domainRepository;
    private final ObjectMapper objectMapper;
    private final DomainBootstrapService domainBootstrapService;
    private final IamService iamService;
    private final AuditLogWriter auditLogWriter;

    public DomainService(
            DomainRepository domainRepository,
            ObjectMapper objectMapper,
            DomainBootstrapService domainBootstrapService,
            IamService iamService,
            AuditLogWriter auditLogWriter) {
        this.domainRepository = domainRepository;
        this.objectMapper = objectMapper;
        this.domainBootstrapService = domainBootstrapService;
        this.iamService = iamService;
        this.auditLogWriter = auditLogWriter;
    }

    public PageResult<DomainDtos.DomainView> listAdminDomains(
            int page,
            int pageSize,
            String status,
            String keyword,
            LocalDateTime createdFrom,
            LocalDateTime createdTo) {
        Integer resolvedStatus = resolveStatus(status);
        String keywordLike = StringUtils.hasText(keyword) ? "%" + keyword.trim() + "%" : null;
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.max(pageSize, 1);
        long offset = (normalizedPage - 1L) * normalizedPageSize;

        long total = domainRepository.countAdminDomains(resolvedStatus, false, keywordLike, createdFrom, createdTo);
        List<BusinessDomainPo> pos = domainRepository.findAdminDomains(
                resolvedStatus, false, keywordLike, createdFrom, createdTo, normalizedPageSize, offset);
        return new PageResult<>(total, pos.stream().map(this::toDomainView).toList());
    }

    public PageResult<DomainDtos.DomainBriefView> listCustomerDomains(int page, int pageSize, String keyword) {
        String keywordLike = StringUtils.hasText(keyword) ? "%" + keyword.trim() + "%" : null;
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.max(pageSize, 1);
        long offset = (normalizedPage - 1L) * normalizedPageSize;

        long total = domainRepository.countBriefDomains(keywordLike);
        List<BusinessDomainPo> pos = domainRepository.findBriefDomains(keywordLike, normalizedPageSize, offset);
        return new PageResult<>(total, pos.stream().map(po -> new DomainDtos.DomainBriefView(
                po.getId(), po.getCode(), po.getName(), po.getLogo())).toList());
    }

    public DomainDtos.DomainView getDomain(long id) {
        BusinessDomainPo po = domainRepository.findById(id);
        if (po == null) {
            throw new IllegalArgumentException("business domain not found");
        }
        return toDomainView(po);
    }

    @Transactional
    public DomainDtos.DomainCreateResponse createDomain(DomainDtos.CreateDomainRequest request) {
        UserContext context = UserContextHolder.requireCurrent();
        List<String> visibilityPolicyCodes = normalizeVisibilityPolicyCodes(request.visibility_policy_codes());
        String registrationEnabled = DomainAccessPolicy.normalize(request.registration_enabled());
        String invitationEnabled = DomainAccessPolicy.normalize(request.invitation_enabled());
        String legacyVisibilityPolicy = visibilityPolicyCodes.isEmpty() ? "public" : visibilityPolicyCodes.getFirst();

        BusinessDomainPo po = new BusinessDomainPo();
        po.setCode(request.code().trim());
        po.setName(request.name().trim());
        po.setDescription(normalizeDescription(request.description()));
        po.setVisibilityPolicy(legacyVisibilityPolicy);
        po.setRegistrationEnabled(registrationEnabled);
        po.setInvitationEnabled(invitationEnabled);
        po.setVisibilityPolicyCodes(toJson(visibilityPolicyCodes));
        po.setLogo(request.logo());
        po.setCreatedBy(context.userId());
        domainRepository.insert(po);

        Long id = domainRepository.findIdByCode(request.code().trim());
        if (id == null) {
            throw new IllegalStateException("business domain create failed");
        }

        domainBootstrapService.bootstrapNewDomain(id, context.userId());
        auditLogWriter.write(
                id,
                context.userId(),
                "staff",
                AuditTargetFormatter.formatDomain(request.name().trim(), request.code().trim()),
                AuditActionCodes.PLATFORM_DOMAIN_CREATE,
                AuditDetailTextBuilder.buildDomainCreateDetail(request.name().trim(), request.code().trim()),
                "success",
                context.sessionId());

        return new DomainDtos.DomainCreateResponse(id, request.code().trim());
    }

    @Transactional
    public DomainDtos.DomainView updateDomain(long id, DomainDtos.UpdateDomainRequest request) {
        UserContext context = UserContextHolder.requireCurrent();
        DomainDtos.DomainView existing = getDomain(id);
        String code = StringUtils.hasText(request.code()) ? request.code().trim() : existing.code();
        String name = StringUtils.hasText(request.name()) ? request.name().trim() : existing.name();
        String logo = request.logo() == null ? existing.logo() : request.logo();
        String description = request.description() == null ? existing.description() : normalizeDescription(request.description());
        List<String> visibilityPolicyCodes = request.visibility_policy_codes() == null
                ? existing.visibility_policy_codes()
                : normalizeVisibilityPolicyCodes(request.visibility_policy_codes());
        String registrationEnabled = request.registration_enabled() == null
                ? existing.registration_enabled()
                : DomainAccessPolicy.normalize(request.registration_enabled());
        String invitationEnabled = request.invitation_enabled() == null
                ? existing.invitation_enabled()
                : DomainAccessPolicy.normalize(request.invitation_enabled());
        int status = request.status() == null ? existing.status() : request.status();

        boolean statusChanging = request.status() != null && request.status() != existing.status();
        boolean profileChanging = !Objects.equals(code, existing.code())
                || !Objects.equals(name, existing.name())
                || !Objects.equals(logo, existing.logo())
                || !Objects.equals(description, existing.description())
                || !Objects.equals(visibilityPolicyCodes, existing.visibility_policy_codes());
        boolean policyChanging = !Objects.equals(registrationEnabled, existing.registration_enabled())
                || !Objects.equals(invitationEnabled, existing.invitation_enabled());

        if (!statusChanging && !profileChanging && !policyChanging) {
            return existing;
        }

        if (statusChanging
                && !iamService.hasAnyPermission(
                        context, List.of(PermissionCodes.PLATFORM_DOMAIN_CONTROL_GENERAL_UPDATE_STATUS))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN.message());
        }
        if ((profileChanging || policyChanging)
                && !iamService.hasAnyPermission(
                        context, List.of(PermissionCodes.PLATFORM_DOMAIN_CONTROL_GENERAL_UPDATE))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN.message());
        }

        String legacyVisibilityPolicy = visibilityPolicyCodes.isEmpty() ? "public" : visibilityPolicyCodes.getFirst();

        domainRepository.updateDomain(
                code, name, description, logo, legacyVisibilityPolicy, toJson(visibilityPolicyCodes),
                registrationEnabled, invitationEnabled, status, context.userId(), id);

        String target = AuditTargetFormatter.formatDomain(name, code);
        if (statusChanging) {
            auditLogWriter.write(
                    id,
                    context.userId(),
                    "staff",
                    target,
                    AuditActionCodes.PLATFORM_DOMAIN_UPDATE_STATUS,
                    AuditDetailTextBuilder.buildDomainStatusDetail(name, code, existing.status(), status),
                    "success",
                    context.sessionId());
        }
        if (profileChanging || policyChanging) {
            auditLogWriter.write(
                    id,
                    context.userId(),
                    "staff",
                    target,
                    AuditActionCodes.PLATFORM_DOMAIN_UPDATE,
                    AuditDetailTextBuilder.buildDomainUpdateDetail(
                            name,
                            code,
                            existing.name(),
                            name,
                            existing.description(),
                            description,
                            existing.visibility_policy_codes(),
                            visibilityPolicyCodes,
                            existing.registration_enabled(),
                            registrationEnabled,
                            existing.invitation_enabled(),
                            invitationEnabled),
                    "success",
                    context.sessionId());
        }

        return getDomain(id);
    }

    @Transactional
    public void deleteDomain(long id) {
        UserContext context = UserContextHolder.requireCurrent();
        DomainDtos.DomainView existing = getDomain(id);
        int updated = domainRepository.softDelete(id, context.userId());
        if (updated == 0) {
            throw new IllegalArgumentException("business domain not found");
        }

        recordAudit(id, context, existing);
    }

    private void recordAudit(long id, UserContext context, DomainDtos.DomainView existing) {
        auditLogWriter.write(
                id,
                context.userId(),
                "staff",
                AuditTargetFormatter.formatDomain(existing.name(), existing.code()),
                AuditActionCodes.PLATFORM_DOMAIN_DELETE,
                AuditDetailTextBuilder.buildDomainDeleteDetail(existing.name(), existing.code()),
                "success",
                context.sessionId());
    }

    private DomainDtos.DomainView toDomainView(BusinessDomainPo po) {
        return new DomainDtos.DomainView(
                po.getId(),
                po.getCode(),
                po.getName(),
                po.getDescription(),
                po.getLogo(),
                readVisibilityPolicyCodes(po.getVisibilityPolicyCodes()),
                DomainAccessPolicy.normalize(po.getRegistrationEnabled()),
                DomainAccessPolicy.normalize(po.getInvitationEnabled()),
                po.getStatus(),
                po.getCreatedAt(),
                po.getUpdatedAt(),
                po.getDeletedAt(),
                po.getCreatedBy(),
                po.getUpdatedBy(),
                po.getCreatorName(),
                po.getUpdaterName());
    }

    private List<String> readVisibilityPolicyCodes(String json) {
        if (!StringUtils.hasText(json)) {
            return DEFAULT_VISIBILITY_POLICY_CODES;
        }
        String trimmed = json.trim();
        try {
            if (trimmed.startsWith("[")) {
                List<String> values = objectMapper.readValue(trimmed, new TypeReference<List<String>>() {
                });
                return values == null || values.isEmpty() ? DEFAULT_VISIBILITY_POLICY_CODES : List.copyOf(values);
            }
            if (trimmed.startsWith("\"")) {
                String single = objectMapper.readValue(trimmed, String.class);
                return normalizeVisibilityPolicyCodes(List.of(single));
            }
            return normalizeVisibilityPolicyCodes(List.of(trimmed));
        } catch (JsonProcessingException ex) {
            return normalizeVisibilityPolicyCodes(List.of(trimmed.replace("\"", "")));
        }
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null || values.isEmpty() ? DEFAULT_VISIBILITY_POLICY_CODES : values);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize visibility_policy_codes", ex);
        }
    }

    private List<String> normalizeVisibilityPolicyCodes(List<String> values) {
        List<String> normalized = values == null ? List.of() : values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        return normalized.isEmpty() ? DEFAULT_VISIBILITY_POLICY_CODES : normalized;
    }

    private String normalizeDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.length() > 512 ? trimmed.substring(0, 512) : trimmed;
    }

    private Integer resolveStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toLowerCase();
        if ("active".equals(normalized) || "enabled".equals(normalized) || "1".equals(normalized)) {
            return 1;
        }
        if ("disabled".equals(normalized) || "inactive".equals(normalized) || "0".equals(normalized)) {
            return 0;
        }
        return null;
    }
}
