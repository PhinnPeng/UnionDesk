package com.uniondesk.iam.web;

import com.uniondesk.auth.core.AuthVersionService;
import com.uniondesk.auth.core.UserContext;
import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.common.web.PageResult;
import com.uniondesk.common.web.ErrorCodes;
import com.uniondesk.iam.core.OrganizationService;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.PlatformRoleService;
import com.uniondesk.iam.core.RequirePermission;
import com.uniondesk.iam.core.StaffAccountService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/staff")
public class StaffController {

    private final StaffAccountService staffAccountService;
    private final PlatformRoleService platformRoleService;
    private final OrganizationService organizationService;
    private final AuthVersionService authVersionService;

    public StaffController(
            StaffAccountService staffAccountService,
            PlatformRoleService platformRoleService,
            OrganizationService organizationService,
            AuthVersionService authVersionService) {
        this.staffAccountService = staffAccountService;
        this.platformRoleService = platformRoleService;
        this.organizationService = organizationService;
        this.authVersionService = authVersionService;
    }

    @GetMapping
    @RequirePermission({PermissionCodes.PLATFORM_USER_READ, PermissionCodes.PLATFORM_USER_OFFBOARD_POOL_READ})
    public PageResult<StaffDtos.StaffAccountView> listStaff(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long organizationId) {
        Set<Long> organizationFilter = resolveOrganizationFilter(organizationId);
        List<StaffDtos.StaffAccountView> rows = staffAccountService.listAll().stream()
                .map(this::toStaffAccountView)
                .filter(view -> matchesStatus(view, status))
                .filter(view -> matchesKeyword(view, keyword))
                .filter(view -> matchesOrganization(view, organizationFilter))
                .sorted(Comparator.comparingLong(StaffDtos.StaffAccountView::id).reversed())
                .toList();
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.max(pageSize, 1);
        int fromIndex = Math.min((normalizedPage - 1) * normalizedPageSize, rows.size());
        int toIndex = Math.min(fromIndex + normalizedPageSize, rows.size());
        return new PageResult<>(rows.size(), rows.subList(fromIndex, toIndex));
    }

    @GetMapping("/{staffId}")
    @RequirePermission(PermissionCodes.PLATFORM_USER_READ)
    public StaffDtos.StaffAccountView getStaff(@PathVariable long staffId) {
        return toStaffAccountView(loadStaff(staffId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.PLATFORM_USER_CREATE)
    public StaffDtos.StaffAccountView createStaff(@Valid @RequestBody StaffDtos.CreateStaffRequest request) {
        StaffAccountService.StaffAccount created = staffAccountService.create(new StaffAccountService.CreateStaffCommand(
                request.username(),
                request.real_name(),
                request.nickname(),
                request.phone(),
                request.email(),
                request.password(),
                request.roleCodes(),
                request.businessDomainIds(),
                request.organizationIds()));
        return toStaffAccountView(created);
    }

    @PutMapping("/{staffId}")
    @RequirePermission(PermissionCodes.PLATFORM_USER_UPDATE)
    public StaffDtos.StaffAccountView updateStaff(
            @PathVariable long staffId,
            @Valid @RequestBody StaffDtos.UpdateStaffRequest request) {
        StaffAccountService.StaffAccount updated = staffAccountService.update(staffId, new StaffAccountService.UpdateStaffCommand(
                request.username(),
                request.real_name(),
                request.nickname(),
                request.phone(),
                request.email(),
                request.password(),
                request.status(),
                request.roleCodes(),
                request.businessDomainIds(),
                request.organizationIds()));
        authVersionService.incrementVersion(updated.id(), "staff");
        return toStaffAccountView(updated);
    }

    @PostMapping("/{staffId}/disable")
    @RequirePermission(PermissionCodes.PLATFORM_USER_DISABLE)
    public StaffDtos.StaffAccountView disableStaff(@PathVariable long staffId, @RequestParam(required = false) String reason) {
        StaffAccountService.StaffAccount updated = staffAccountService.disable(staffId);
        authVersionService.incrementVersion(updated.id(), "staff");
        return toStaffAccountView(updated);
    }

    @PostMapping("/{staffId}/offboard")
    @RequirePermission(PermissionCodes.PLATFORM_USER_DISABLE)
    public StaffDtos.StaffAccountView offboardStaff(
            @PathVariable long staffId,
            @RequestBody(required = false) StaffDtos.OffboardStaffRequest request) {
        long operatorId = currentOperatorId();
        try {
            StaffAccountService.StaffAccount updated = staffAccountService.offboard(
                    staffId,
                    operatorId,
                    request == null ? null : request.reason());
            authVersionService.incrementVersion(updated.id(), "staff");
            return toStaffAccountView(updated);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{staffId}/restore")
    @RequirePermission(PermissionCodes.PLATFORM_USER_RESTORE)
    public StaffDtos.StaffAccountView restoreStaff(@PathVariable long staffId) {
        StaffAccountService.StaffAccount updated = staffAccountService.restore(staffId);
        authVersionService.incrementVersion(updated.id(), "staff");
        return toStaffAccountView(updated);
    }

    @PutMapping("/{staffId}/status")
    @RequirePermission(PermissionCodes.PLATFORM_USER_UPDATE)
    public StaffDtos.StaffAccountView updateStaffStatus(
            @PathVariable long staffId,
            @Valid @RequestBody StaffDtos.UpdateStaffStatusRequest request) {
        String normalizedStatus = normalizeStatus(request.status());
        StaffAccountService.StaffAccount updated;
        if ("disabled".equals(normalizedStatus)) {
            updated = staffAccountService.disable(staffId);
        } else {
            updated = staffAccountService.restore(staffId);
        }
        authVersionService.incrementVersion(updated.id(), "staff");
        return toStaffAccountView(updated);
    }

    @PutMapping("/{staffId}/platform-roles")
    @RequirePermission(PermissionCodes.PLATFORM_ROLE_BIND)
    public StaffDtos.StaffPlatformRolesResponse updatePlatformRoles(
            @PathVariable long staffId,
            @Valid @RequestBody StaffDtos.UpdatePlatformRolesRequest request,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-UD-Step-Up-Token", required = false) String stepUpToken) {
        if (stepUpToken == null || stepUpToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN.message());
        }

        loadStaff(staffId);
        try {
            platformRoleService.assignPlatformRoles(staffId, request.roleCodes());
            authVersionService.incrementVersion(staffId, "staff");
            return new StaffDtos.StaffPlatformRolesResponse(staffId, platformRoleService.getCurrentPlatformRoles(staffId));
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/{staffId}/platform-roles")
    @RequirePermission(PermissionCodes.PLATFORM_USER_READ)
    public StaffDtos.StaffPlatformRolesResponse getPlatformRoles(@PathVariable long staffId) {
        return new StaffDtos.StaffPlatformRolesResponse(staffId, platformRoleService.getCurrentPlatformRoles(staffId));
    }

    private StaffDtos.StaffAccountView toStaffAccountView(StaffAccountService.StaffAccount staff) {
        boolean offboarded = "offboarded".equalsIgnoreCase(staff.employmentStatus());
        boolean active = !offboarded && "active".equalsIgnoreCase(staff.status());
        List<String> platformRoles = platformRoleService.getCurrentPlatformRoles(staff.id());
        List<String> domainRoles = staffAccountService.listDomainRoleCodes(staff.id());
        List<String> roleCodes = mergeRoleCodes(platformRoles, domainRoles);
        String employmentStatus = offboarded
                ? "offboarded"
                : (active ? "active" : "disabled");
        return new StaffDtos.StaffAccountView(
                staff.id(),
                staff.username(),
                staff.realName(),
                staff.nickname(),
                staff.phone(),
                staff.phone(),
                staff.email(),
                active ? 1 : 0,
                employmentStatus,
                "admin",
                roleCodes,
                staffAccountService.listBusinessDomainIds(staff.id()),
                staffAccountService.listOrganizationIds(staff.id()),
                platformRoles,
                staff.offboardedAt(),
                staff.offboardedBy(),
                staff.offboardReason());
    }

    private List<String> mergeRoleCodes(List<String> platformRoles, List<String> domainRoles) {
        Set<String> codes = new LinkedHashSet<>();
        if (platformRoles != null) {
            codes.addAll(platformRoles);
        }
        if (domainRoles != null) {
            codes.addAll(domainRoles);
        }
        return new ArrayList<>(codes);
    }

    private StaffAccountService.StaffAccount loadStaff(long staffId) {
        return staffAccountService.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND.message()));
    }

    private Set<Long> resolveOrganizationFilter(Long organizationId) {
        if (organizationId == null) {
            return Set.of();
        }
        return new LinkedHashSet<>(organizationService.collectDescendantOrgIds(organizationId));
    }

    private boolean matchesOrganization(StaffDtos.StaffAccountView view, Set<Long> organizationFilter) {
        if (organizationFilter == null || organizationFilter.isEmpty()) {
            return true;
        }
        List<Long> organizationIds = view.organizationIds();
        if (organizationIds == null || organizationIds.isEmpty()) {
            return false;
        }
        return organizationIds.stream().anyMatch(organizationFilter::contains);
    }

    private boolean matchesStatus(StaffDtos.StaffAccountView view, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "active" -> view.status() == 1 && "active".equalsIgnoreCase(view.employmentStatus());
            case "disabled" -> view.status() == 0 && !"offboarded".equalsIgnoreCase(view.employmentStatus());
            case "offboarded" -> "offboarded".equalsIgnoreCase(view.employmentStatus());
            default -> true;
        };
    }

    private boolean matchesKeyword(StaffDtos.StaffAccountView view, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(view.username(), normalized)
                || contains(view.realName(), normalized)
                || contains(view.nickname(), normalized)
                || contains(view.phone(), normalized)
                || contains(view.mobile(), normalized)
                || contains(view.email(), normalized);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR.message());
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (!List.of("active", "disabled").contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST.message());
        }
        return normalized;
    }

    private long currentOperatorId() {
        return UserContextHolder.current().map(UserContext::userId).orElse(0L);
    }
}
