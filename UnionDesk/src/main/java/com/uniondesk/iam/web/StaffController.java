package com.uniondesk.iam.web;

import com.uniondesk.auth.core.AuthVersionService;
import com.uniondesk.auth.core.UserContext;
import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.common.web.PageResult;
import com.uniondesk.common.web.ErrorCodes;
import com.uniondesk.iam.core.IamService;
import com.uniondesk.iam.core.IamService.CreateUserCommand;
import com.uniondesk.iam.core.IamService.UpdateUserCommand;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.PlatformRoleService;
import com.uniondesk.iam.core.RequirePermission;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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

    private final IamService iamService;
    private final PlatformRoleService platformRoleService;
    private final AuthVersionService authVersionService;

    public StaffController(IamService iamService, PlatformRoleService platformRoleService, AuthVersionService authVersionService) {
        this.iamService = iamService;
        this.platformRoleService = platformRoleService;
        this.authVersionService = authVersionService;
    }

    @GetMapping
    @RequirePermission(PermissionCodes.PLATFORM_USER_READ)
    public PageResult<StaffDtos.StaffAccountView> listStaff(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        List<StaffDtos.StaffAccountView> rows = iamService.listUsers(false, null).stream()
                .map(this::toStaffAccountView)
                .filter(view -> matchesStatus(view, status))
                .filter(view -> matchesKeyword(view, keyword))
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
        IamService.UserAccount created = iamService.createUser(new CreateUserCommand(
                request.loginName(),
                request.phone(),
                request.email(),
                null,
                request.password(),
                request.accountType(),
                request.roleCodes(),
                request.businessDomainIds(),
                null));
        return toStaffAccountView(created);
    }

    @PutMapping("/{staffId}")
    @RequirePermission(PermissionCodes.PLATFORM_USER_UPDATE)
    public StaffDtos.StaffAccountView updateStaff(
            @PathVariable long staffId,
            @Valid @RequestBody StaffDtos.UpdateStaffRequest request) {
        IamService.UserAccount updated = iamService.updateUser(staffId, new UpdateUserCommand(
                request.loginName(),
                request.phone(),
                request.email(),
                null,
                request.password(),
                request.accountType(),
                request.roleCodes(),
                request.businessDomainIds(),
                request.status(),
                null));
        authVersionService.incrementVersion(updated.id(), updated.accountType());
        return toStaffAccountView(updated);
    }

    @PostMapping("/{staffId}/disable")
    @RequirePermission(PermissionCodes.PLATFORM_USER_DISABLE)
    public StaffDtos.StaffAccountView disableStaff(@PathVariable long staffId, @RequestParam(required = false) String reason) {
        UserContext context = UserContextHolder.requireCurrent();
        IamService.UserAccount updated = iamService.offboardUser(staffId, context.userId(), reason);
        authVersionService.incrementVersion(updated.id(), updated.accountType());
        return toStaffAccountView(updated);
    }

    @PostMapping("/{staffId}/restore")
    @RequirePermission(PermissionCodes.PLATFORM_USER_RESTORE)
    public StaffDtos.StaffAccountView restoreStaff(@PathVariable long staffId) {
        IamService.UserAccount updated = iamService.restoreUser(staffId);
        authVersionService.incrementVersion(updated.id(), updated.accountType());
        return toStaffAccountView(updated);
    }

    @PutMapping("/{staffId}/status")
    @RequirePermission(PermissionCodes.PLATFORM_USER_UPDATE)
    public StaffDtos.StaffAccountView updateStaffStatus(
            @PathVariable long staffId,
            @Valid @RequestBody StaffDtos.UpdateStaffStatusRequest request) {
        String normalizedStatus = normalizeStatus(request.status());
        IamService.UserAccount updated;
        if ("disabled".equals(normalizedStatus)) {
            UserContext context = UserContextHolder.requireCurrent();
            updated = iamService.offboardUser(staffId, context.userId(), "status_update");
        } else {
            updated = iamService.restoreUser(staffId);
        }
        authVersionService.incrementVersion(updated.id(), updated.accountType());
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

        IamService.UserAccount staff = loadStaff(staffId);
        try {
            platformRoleService.assignPlatformRoles(staffId, request.roleCodes());
            authVersionService.incrementVersion(staff.id(), staff.accountType());
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

    private StaffDtos.StaffAccountView toStaffAccountView(IamService.UserAccount user) {
        return new StaffDtos.StaffAccountView(
                user.id(),
                user.username(),
                user.mobile(),
                user.email(),
                user.status(),
                user.employmentStatus(),
                user.accountType(),
                user.roleCodes(),
                user.businessDomainIds(),
                platformRoleService.getCurrentPlatformRoles(user.id()));
    }

    private IamService.UserAccount loadStaff(long staffId) {
        return iamService.loadUser(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND.message()));
    }

    private boolean matchesStatus(StaffDtos.StaffAccountView view, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "active" -> view.status() == 1 && "active".equalsIgnoreCase(view.employmentStatus());
            case "disabled" -> view.status() == 0;
            case "offboarded" -> "offboarded".equalsIgnoreCase(view.employmentStatus());
            default -> true;
        };
    }

    private boolean matchesKeyword(StaffDtos.StaffAccountView view, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(view.loginName(), normalized)
                || contains(view.phone(), normalized)
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
}
